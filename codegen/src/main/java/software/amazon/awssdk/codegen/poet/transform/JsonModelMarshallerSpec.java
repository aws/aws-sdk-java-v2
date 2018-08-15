/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingInfo;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Create ModelMarshaller for Json protocol
 */
public class JsonModelMarshallerSpec implements ClassSpec {

    private static final String PROTOCOL_FACTORY_LITERAL = "protocolFactory";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final ClassName className;
    private final ClassName requestClassName;
    private final PoetExtensions poetExtensions;

    public JsonModelMarshallerSpec(IntermediateModel intermediateModel, ShapeModel shapeModel, String className) {
        this.poetExtensions = new PoetExtensions(intermediateModel);
        String modelPackage = intermediateModel.getMetadata().getFullModelPackageName();
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.className = poetExtensions.getTransformClass(shapeModel.getShapeName() + className);
        this.requestClassName = ClassName.get(modelPackage, shapeModel.getShapeName());
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder =  TypeSpec.classBuilder(className)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addAnnotation(PoetUtils.generatedAnnotation())
                                            .addAnnotation(SdkInternalApi.class)
                                            .addJavadoc("{@link $T} Marshaller", requestClassName)
                                            .addFields(memberVariables())
                                            .addMethods(methods());

        if (isRequestEvent()) {
            builder.addField(operationInfoField());

            builder.addSuperinterface(ParameterizedTypeName.get(ClassName.get(Marshaller.class),
                                                                ParameterizedTypeName.get(ClassName.get(Request.class),
                                                                                          requestClassName),
                                                                requestClassName));

            builder.addMethod(overridenMarshallMethod());
        }

        return builder.build();
    }

    private FieldSpec operationInfoField() {
        CodeBlock.Builder initializationCodeBlockBuilder = CodeBlock.builder()
                                                                    .add("$T.builder()", OperationInfo.class);
        initializationCodeBlockBuilder.add(".hasExplicitPayloadMember($L)", shapeModel.getExplicitEventPayloadMember() != null)
                                      .add(".hasPayloadMembers($L)", shapeModel.hasPayloadMembers())
                                      .add(".build()");

        return FieldSpec.builder(ClassName.get(OperationInfo.class), "SDK_OPERATION_BINDING")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                        .initializer(initializationCodeBlockBuilder.build())
                        .build();

    }

    private boolean isRequestEvent() {
        return shapeModel.isEvent() && EventStreamUtils.isRequestEvent(intermediateModel, shapeModel);
    }

    @Override
    public ClassName className() {
        return className;
    }

    private List<MethodSpec> methods() {
        List<MethodSpec> methodSpecs = new ArrayList<>();

        methodSpecs.add(constructor());
        methodSpecs.add(getInstanceMethod());
        methodSpecs.add(marshallMethod());
        return methodSpecs;
    }

    private List<FieldSpec> memberVariables() {
        List<FieldSpec> fields = new ArrayList<>();

        for (MemberModel memberModel : shapeModel.getNonStreamingMembers()) {
            TypeName typeName = ParameterizedTypeName.get(ClassName.get(MarshallingInfo.class),
                                                          marshallingTargetClass(memberModel.getMarshallingTargetClass(),
                                                                                 memberModel.getVariable().getVariableType()));

            CodeBlock.Builder initializationCodeBlockBuilder = CodeBlock.builder()
                                                                        .add("$T.builder($T.$L).marshallLocation($T.$L)",
                                                                             MarshallingInfo.class,
                                                                             MarshallingType.class,
                                                                             memberModel.getMarshallingType(),
                                                                             ClassName.get(MarshallLocation.class),
                                                                             memberModel.getHttp().getMarshallLocation());

            if (memberModel.getHttp().getIsPayload() || memberModel.isEventPayload()) {
                initializationCodeBlockBuilder.add(".isExplicitPayloadMember(true)");
            } else {
                initializationCodeBlockBuilder.add(".marshallLocationName($S)", memberModel.getHttp().getMarshallLocationName());
            }

            // TODO investigate isBinary and how to handle it properly for events
            // Consider moving the logic to MemberModel#getIsBinary method
            initializationCodeBlockBuilder.add(".isBinary($L)", memberModel.getIsBinary());

            Optional.ofNullable(intermediateModel.getCustomizationConfig().getModelMarshallerDefaultValueSupplier())
                    .map(defaultValueSupplier -> defaultValueSupplier.get(memberModel.getName()))
                    .ifPresent(value -> initializationCodeBlockBuilder.add(".defaultValueSupplier($L)", value));

            if (memberModel.isIdempotencyToken()) {
                initializationCodeBlockBuilder.add(".defaultValueSupplier($T.getGenerator())", ClassName.get(IdempotentUtils
                                                                                                                 .class));
            }

            CodeBlock codeBlock = initializationCodeBlockBuilder.add(".build()").build();

            FieldSpec fieldSpec = FieldSpec.builder(typeName, memberModel.getMarshallerBindingFieldName())
                                           .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                                           .initializer(codeBlock)
                                           .build();
            fields.add(fieldSpec);
        }

        FieldSpec.Builder instance = FieldSpec.builder(className, "INSTANCE")
                                      .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC);

        if (isRequestEvent()) {
            instance.initializer("null");

            fields.add(FieldSpec.builder(AwsJsonProtocolFactory.class, PROTOCOL_FACTORY_LITERAL)
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build());
        } else {
            instance.initializer("new $T()", className);
        }

        fields.add(instance.build());
        return fields;
    }

    private MethodSpec constructor() {
        if (isRequestEvent()) {
            return MethodSpec.constructorBuilder()
                             .addModifiers(Modifier.PUBLIC)
                             .addParameter(ParameterSpec.builder(AwsJsonProtocolFactory.class, PROTOCOL_FACTORY_LITERAL).build())
                             .addStatement("this.$1L = $1L", PROTOCOL_FACTORY_LITERAL)
                             .build();

        } else {
            return MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
        }
    }

    private MethodSpec getInstanceMethod() {
        return MethodSpec.methodBuilder("getInstance").addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                         .addStatement("return INSTANCE")
                         .returns(className)
                         .build();
    }

    private MethodSpec marshallMethod() {
        String variableName = shapeModel.getVariable().getVariableName();
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("marshall")
                                                         .addJavadoc("Marshall the given parameter object")
                                                         .addModifiers(Modifier.PUBLIC)
                                                         .addParameter(requestClassName, variableName)
                                                         .addParameter(ProtocolMarshaller.class, "protocolMarshaller");

        if (shapeModel.getNonStreamingMembers().isEmpty()) {
            return methodSpecBuilder.build();
        }

        methodSpecBuilder.addStatement("$T.paramNotNull($L, $S)", ClassName.get(Validate.class), variableName, variableName);
        methodSpecBuilder.addStatement("$T.paramNotNull($L, $S)", ClassName.get(Validate.class), "protocolMarshaller",
                                       "protocolMarshaller");

        methodSpecBuilder.beginControlFlow("try");
        shapeModel.getNonStreamingMembers().forEach(
            memberModel -> methodSpecBuilder.addStatement("protocolMarshaller.marshall($L.$L(), $L)",
                                                          variableName,
                                                          memberModel.getFluentGetterMethodName(),
                                                          memberModel.getMarshallerBindingFieldName()));

        methodSpecBuilder.endControlFlow();
        methodSpecBuilder.beginControlFlow("catch (Exception e)");
        methodSpecBuilder.addStatement("throw $T.builder().message(\"Unable to marshall request to JSON: \" + " +
                "e.getMessage()).cause(e).build()", ClassName
            .get(SdkClientException.class));
        methodSpecBuilder.endControlFlow();
        return methodSpecBuilder.build();
    }

    /**
     * The generated marshall method is only present in marshallers which are events. The method takes in a event and
     * return a typed {@link Request} of that event type.
     */
    private MethodSpec overridenMarshallMethod() {
        String variableName = shapeModel.getVariable().getVariableName();
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("marshall")
                                                         .addAnnotation(Override.class)
                                                         .addModifiers(Modifier.PUBLIC)
                                                         .addParameter(requestClassName, variableName)
                                                         .returns(ParameterizedTypeName.get(ClassName.get(Request.class),
                                                                                            requestClassName));

        methodSpecBuilder.addStatement("$T.paramNotNull($L, $S)", ClassName.get(Validate.class), variableName, variableName);
        methodSpecBuilder.beginControlFlow("try")
                         .addStatement("$T<?> protocolMarshaller = protocolFactory.createProtocolMarshaller"
                                       + "(SDK_OPERATION_BINDING,  null)", ProtocolRequestMarshaller.class)
                         .addStatement("protocolMarshaller.startMarshalling()")
                         .addStatement("marshall($L, protocolMarshaller)", variableName)
                         .addStatement("$T request = protocolMarshaller.finishMarshalling()", Request.class);


        methodSpecBuilder.addStatement("request.addHeader(\":message-type\", \"event\")");
        methodSpecBuilder.addStatement("request.addHeader(\":event-type\", \"$L\")", getMemberNameFromEventStream());
        // Add :content-type header only if payload is present
        if (!shapeModel.hasNoEventPayload()) {
            methodSpecBuilder.addStatement("request.addHeader(\":content-type\", \"$L\")", determinePayloadContentType());
        }

        methodSpecBuilder.addStatement("return request")
                         .endControlFlow()
                         .beginControlFlow("catch (Exception e)")
                         .addStatement("throw $T.builder().message(\"Unable to marshall request to JSON: \" + " +
                                       "e.getMessage()).cause(e).build()", SdkClientException.class)
                         .endControlFlow();

        return methodSpecBuilder.build();
    }

    private String determinePayloadContentType() {
        MemberModel explicitEventPayload = shapeModel.getExplicitEventPayloadMember();
        if (explicitEventPayload != null) {
            return getPayloadContentType(explicitEventPayload);
        }

        return JSON_CONTENT_TYPE;
    }

    private String getPayloadContentType(MemberModel memberModel) {
        final String blobContentType = "application/octet-stream";
        final String stringContentType = "text/plain";
        final String variableType = memberModel.getVariable().getVariableType();

        if ("software.amazon.awssdk.core.SdkBytes".equals(variableType)) {
            return blobContentType;
        } else if ("String".equals(variableType)) {
            return stringContentType;
        }

        return JSON_CONTENT_TYPE;
    }

    private String getMemberNameFromEventStream() {
        ShapeModel eventStream = EventStreamUtils.getBaseEventStreamShape(intermediateModel, shapeModel);
        return eventStream.getMembers().stream()
                          .filter(memberModel -> memberModel.getShape().equals(shapeModel))
                          .findAny()
                          .map(MemberModel::getC2jName)
                          .orElseThrow(() -> new IllegalStateException(
                              String.format("Unable to find %s from its parent event stream", shapeModel.getC2jName())));
    }

    private ClassName marshallingTargetClass(String marshallerTargetClass, String variableType) {
        if ("List".equals(marshallerTargetClass)) {
            return ClassName.get(List.class);
        } else if ("String".equals(marshallerTargetClass)) {
            return ClassName.get(String.class);
        } else if ("Map".equals(marshallerTargetClass)) {
            return ClassName.get(Map.class);
        } else if ("StructuredPojo".equals(marshallerTargetClass)) {
            return ClassName.get(StructuredPojo.class);
        } else {
            return ClassName.bestGuess(variableType);
        }
    }
}
