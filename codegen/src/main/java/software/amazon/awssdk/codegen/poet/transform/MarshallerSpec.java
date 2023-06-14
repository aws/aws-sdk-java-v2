/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.poet.transform;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.intermediate.*;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.transform.protocols.EventStreamJsonMarshallerSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.JsonMarshallerSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.MarshallerProtocolSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.QueryMarshallerSpec;
import software.amazon.awssdk.codegen.poet.transform.protocols.XmlMarshallerSpec;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

public class MarshallerSpec implements ClassSpec {

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final ClassName baseMashallerName;
    private final TypeName httpRequestName;
    private final ClassName requestName;
    private final ClassName className;
    private final ClassName requestClassName;
    private final MarshallerProtocolSpec protocolSpec;
    private final PoetExtension poetExtensions;
    private final TypeUtils typeUtils;
    private int uuid = 0;

    private final Set<String> marshallMethodsCreated = new HashSet<>();

    public MarshallerSpec(IntermediateModel intermediateModel, ShapeModel shapeModel) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        String modelPackage = intermediateModel.getMetadata().getFullModelPackageName();
        this.baseMashallerName = ClassName.get(Marshaller.class);
        this.httpRequestName = ClassName.get(SdkHttpFullRequest.class);
        this.requestName = ClassName.get(modelPackage, shapeModel.getShapeName());
        this.poetExtensions = new PoetExtension(intermediateModel);
        this.className = poetExtensions.getRequestTransformClass(shapeModel.getShapeName() + "Marshaller");
        this.requestClassName = ClassName.get(modelPackage, shapeModel.getShapeName());
        this.protocolSpec = getProtocolSpecs(intermediateModel.getMetadata().getProtocol());
        this.typeUtils = new TypeUtils(intermediateModel.getNamingStrategy());
    }

    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className)
                       .addJavadoc("{@link $T} Marshaller", requestClassName)
                       .addModifiers(Modifier.PUBLIC)
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addAnnotation(SdkInternalApi.class)
                       .addSuperinterface(ParameterizedTypeName.get(baseMashallerName, requestName))
                       .addFields(protocolSpec.memberVariables())
                       .addFields(protocolSpec.additionalFields())
                       .addMethods(methods())
                       .build();
    }


    @Override
    public ClassName className() {
        return className;
    }

    private List<MethodSpec> methods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        protocolSpec.constructor().ifPresent(methodSpecs::add);
        methodSpecs.add(marshallMethod());
        methodSpecs.addAll(protocolSpec.additionalMethods());

        Protocol protocol = intermediateModel.getMetadata().getProtocol();
        if ((protocol == Protocol.REST_JSON || protocol == Protocol.AWS_JSON) &&
                shapeModel.getShapeType() == ShapeType.Request &&
                !shapeModel.isHasStreamingMember() &&
                !shapeModel.getMembers().isEmpty()) {
            methodSpecs.addAll(fastMarshallMethod());
        }

        return methodSpecs;
    }

    private MethodSpec marshallMethod() {
        String variableName = shapeModel.getVariable().getVariableName();
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("marshall")
                                                         .addAnnotation(Override.class)
                                                         .addModifiers(Modifier.PUBLIC)
                                                         .addParameter(requestClassName, variableName)
                                                         .returns(httpRequestName);

        methodSpecBuilder.addStatement("$T.paramNotNull($L, $S)", ClassName.get(Validate.class), variableName, variableName);
        methodSpecBuilder.beginControlFlow("try");

        methodSpecBuilder.addCode(protocolSpec.marshalCodeBlock(requestClassName));

        methodSpecBuilder.endControlFlow();
        methodSpecBuilder.beginControlFlow("catch (Exception e)");
        methodSpecBuilder.addStatement("throw $T.builder().message(\"Unable to marshall request to JSON: \" + " +
                "e.getMessage()).cause(e).build()", ClassName
            .get(SdkClientException.class));
        methodSpecBuilder.endControlFlow();
        return methodSpecBuilder.build();
    }

    private MarshallerProtocolSpec getProtocolSpecs(software.amazon.awssdk.codegen.model.intermediate.Protocol protocol) {
        switch (protocol) {
            case REST_JSON:
            case CBOR:
            case AWS_JSON:
                return getJsonMarshallerSpec();

            case QUERY:
            case EC2:
                return new QueryMarshallerSpec(intermediateModel, shapeModel);

            case REST_XML:
                return new XmlMarshallerSpec(intermediateModel, shapeModel);

            default:
                throw new RuntimeException("Unknown protocol: " + protocol.name());
        }
    }

    private MarshallerProtocolSpec getJsonMarshallerSpec() {
        if (shapeModel.isEvent()) {
            return new EventStreamJsonMarshallerSpec(intermediateModel, shapeModel);
        }
        return new JsonMarshallerSpec(shapeModel);
    }

    private List<MethodSpec> fastMarshallMethod() {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(MethodSpec.methodBuilder("fastMarshall")
                                     .addModifiers(Modifier.PUBLIC)
                                     .addParameter(requestClassName, "input")
                                     .addCode(fastMarshallMethodBody(methods))
                                     .returns(httpRequestName)
                                     .build());
        return methods;
    }

    private CodeBlock fastMarshallMethodBody(List<MethodSpec> methods) {
        return CodeBlock.builder()
                        .add("return $T.builder()", SdkHttpFullRequest.class)
                        .add(".method(SDK_OPERATION_BINDING.httpMethod())")
                        .add(uri())
                        .add(headerAppends())
                        .add(payload(methods))
                        .add(".build();")
                        .build();
    }

    private CodeBlock uri() {
        return CodeBlock.of(".uri($T.create(\"https://localhost\" + SDK_OPERATION_BINDING.requestUri()))", URI.class);
    }

    private CodeBlock headerAppends() {
        CodeBlock.Builder result = CodeBlock.builder();
        shapeModel.getMembers().forEach(member -> {
            if (member.getHttp().isHeader()) {
                // TODO: handle header types better
                result.add(".appendHeader($S, input.$L().toString())", member.getName(), member.getFluentGetterMethodName());
            }
        });
        return result.build();
    }

    private CodeBlock payload(List<MethodSpec> methods) {
        if (shapeModel.hasPayloadMembers()) {
            String marshallMethod = getShapeMarshallMethod(methods, shapeModel);
            return CodeBlock.of(".contentStreamProvider(() -> new $T($L(new $T(), input).toString()))",
                                StringInputStream.class,
                                marshallMethod, StringBuilder.class);
        }
        return CodeBlock.of("");
    }

    private String getShapeMarshallMethod(List<MethodSpec> methods, ShapeModel shape) {
        String marshallMethodName = "marshall" + shape.getShapeName();
        if (marshallMethodsCreated.contains(marshallMethodName)) {
            return marshallMethodName;
        }
        marshallMethodsCreated.add(marshallMethodName);

        methods.add(createShapeMarshallMethod(methods, shape, marshallMethodName));
        return marshallMethodName;
    }

    private MethodSpec createShapeMarshallMethod(List<MethodSpec> methods, ShapeModel shape, String marshalMethodName) {
        return MethodSpec.methodBuilder(marshalMethodName)
                         .addParameter(StringBuilder.class, "result")
                         .addParameter(poetExtensions.getModelClassFromShape(shape), "input")
                         .returns(StringBuilder.class)
                         .addCode(shapeMarshallMethodBody(methods, shape))
                         .addCode("return result;")
                         .build();
    }

    private CodeBlock shapeMarshallMethodBody(List<MethodSpec> methods, ShapeModel shape) {
        CodeBlock.Builder result = CodeBlock.builder();
        result.add("if (input == null) {");
        result.add("result.append(\"null\");");
        result.add("return result;");
        result.add("}");

        result.add("boolean isEmpty = true;");
        result.add("result.append('{');");
        shape.getMembers().forEach(member -> {
            if (member.getShape() != null && (member.getShape().isEvent() || member.getShape().isEventStream())) {
                // Ignore for now, we shouldn't generate the fast marshall at all for this?
            } else {
                String memberAccessor = "input." + member.getFluentGetterMethodName() + "()";

                if (member.isList()) {
                    result.add("if (!($L instanceof $T)) {", memberAccessor, SdkAutoConstructList.class);
                } else if (member.isMap()) {
                    result.add("if (!($L instanceof $T)) {", memberAccessor, SdkAutoConstructMap.class);
                } else {
                    result.add("if ($L != null) {", memberAccessor);
                }
                result.add("result.append('\"');");
                result.add("result.append($S);", member.getName());
                result.add("result.append(\"\\\":\");");
                result.add(appendToResult(methods, member, memberAccessor));
                result.add("result.append(',');");
                result.add("isEmpty = false;");
                result.add("}");
            }
        });
        result.add("if (!isEmpty) {");
        result.add("result.setLength(result.length() - 1);");
        result.add("}");
        result.add("result.append('}');");
        return result.build();
    }

    private CodeBlock appendToResult(List<MethodSpec> methods, MemberModel member, String memberAccessor) {

        if (member.isList()) {
            return appendListToResult(methods, member, memberAccessor);
        }

        if (member.isMap()) {
            return appendMapToResult(methods, member, memberAccessor);
        }

        if (member.getEnumType() != null) {
            return appendJsonLiteralToResult(member, memberAccessor);
        }

        if (member.getShape() != null) {
            if (member.getShape().isEvent() || member.getShape().isEventStream()) {
                return CodeBlock.of("// uh-oh, event stream!\n");
                // Ignore for now, we shouldn't generate the fast marshall at all for this?
            }

            String marshallMethod = getShapeMarshallMethod(methods, member.getShape());
            return CodeBlock.of("$L(result, $L);", marshallMethod, memberAccessor);
        }

        return appendJsonLiteralToResult(member, memberAccessor);
    }

    private CodeBlock appendListToResult(List<MethodSpec> methods, MemberModel member, String memberAccessor) {
        String listMemberName = "member" + uuid++;

        // TODO: why do I have to do this?
        ShapeModel listShape = intermediateModel.getShapes().get(member.getListModel().getListMemberModel().getC2jShape());
        member.getListModel().getListMemberModel().setShape(listShape);

        CodeBlock.Builder result = CodeBlock.builder();
        result.add("result.append('[');");
        result.add("if (!$L.isEmpty()) {", memberAccessor);
        result.add("$L.forEach($L -> {", memberAccessor, listMemberName);
        result.add(appendToResult(methods, member.getListModel().getListMemberModel(), listMemberName));
        result.add("result.append(',');");
        result.add("});");
        result.add("result.setLength(result.length() - 1);");
        result.add("}");
        result.add("result.append(']');");
        return result.build();
    }

    private CodeBlock appendMapToResult(List<MethodSpec> methods, MemberModel member, String memberAccessor) {
        String keyMemberName = "key" + uuid++;
        String valueMemberName = "value" + uuid++;

        // TODO: why do I have to do this?
        ShapeModel keyShape = intermediateModel.getShapes().get(member.getMapModel().getKeyModel().getC2jShape());
        member.getMapModel().getKeyModel().setShape(keyShape);
        ShapeModel valueShape = intermediateModel.getShapes().get(member.getMapModel().getValueModel().getC2jShape());
        member.getMapModel().getValueModel().setShape(valueShape);

        CodeBlock.Builder result = CodeBlock.builder();
        result.add("result.append('{');");
        result.add("if (!$L.isEmpty()) {", memberAccessor);
        result.add("$L.forEach(($L, $L) -> {", memberAccessor, keyMemberName, valueMemberName);
        result.add(appendToResult(methods, member.getMapModel().getKeyModel(), keyMemberName));
        result.add("result.append(':');");
        result.add(appendToResult(methods, member.getMapModel().getValueModel(), valueMemberName));
        result.add("result.append(',');");
        result.add("});");
        result.add("result.setLength(result.length() - 1);");
        result.add("}");
        result.add("result.append('}');");
        return result.build();
    }

    private CodeBlock appendJsonLiteralToResult(MemberModel member, String memberAccessor) {
        CodeBlock.Builder result = CodeBlock.builder();

        result.add("if ($L != null) {", memberAccessor);

        if (member.getEnumType() != null) {
            result.add("result.append('\"');")
                  .add("result.append($T.replace($L, $S, $S));", StringUtils.class, memberAccessor, "\"", "\\\"")
                  .add("result.append('\"');");
        } else {
            switch (member.getMarshallingType()) {
                case "STRING":
                case "BIG_DECIMAL":
                case "DOCUMENT":
                    result.add("result.append('\"');")
                          .add("result.append($T.replace($L.toString(), $S, $S));", StringUtils.class, memberAccessor, "\"", "\\\"")
                          .add("result.append('\"');");
                    break;
                case "INTEGER":
                case "LONG":
                case "FLOAT":
                case "DOUBLE":
                case "BOOLEAN":
                case "SHORT":
                case "INSTANT": // TODO: don't use toString for instants
                    result.add("result.append($L);", memberAccessor);
                    break;
                case "SDK_BYTES":
                    result.add("result.append('\"');")
                          .add("result.append($T.toBase64($L.asByteArrayUnsafe()));", BinaryUtils.class, memberAccessor)
                          .add("result.append('\"');");
                    break;
                default:
                    throw new IllegalArgumentException("JSON literals not supported for: " + member.getMarshallingType());
            }
        }
        result.add("} else {");
        result.add("result.append(\"null\");");
        result.add("}");
        return result.build();
    }
}
