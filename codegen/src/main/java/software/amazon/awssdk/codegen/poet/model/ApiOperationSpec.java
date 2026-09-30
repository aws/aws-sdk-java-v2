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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.client.traits.HttpChecksumRequiredTrait;
import software.amazon.awssdk.codegen.poet.client.traits.HttpChecksumTrait;
import software.amazon.awssdk.codegen.poet.client.traits.RequestCompressionTrait;

/**
 * Generates a static singleton {@code ApiOperation} implementation per service operation.
 * These provide the smithy-java protocol layer with operation/input/output schemas,
 * error registries, and builder factories — without needing DynamicClient or a Smithy model at runtime.
 */
public class ApiOperationSpec implements ClassSpec {

    private static final ClassName SCHEMA = ClassName.get("software.amazon.smithy.java.core.schema", "Schema");
    private static final ClassName SHAPE_ID = ClassName.get("software.amazon.smithy.model.shapes", "ShapeId");
    private static final ClassName HTTP_TRAIT = ClassName.get("software.amazon.smithy.model.traits", "HttpTrait");
    private static final ClassName URI_PATTERN = ClassName.get("software.amazon.smithy.model.pattern", "UriPattern");
    private static final ClassName API_OPERATION = ClassName.get("software.amazon.smithy.java.core.schema", "ApiOperation");
    private static final ClassName API_SERVICE = ClassName.get("software.amazon.smithy.java.core.schema", "ApiService");
    private static final ClassName SHAPE_BUILDER = ClassName.get("software.amazon.smithy.java.core.schema", "ShapeBuilder");
    private static final ClassName SERIALIZABLE_STRUCT =
        ClassName.get("software.amazon.smithy.java.core.schema", "SerializableStruct");
    private static final ClassName TYPE_REGISTRY =
        ClassName.get("software.amazon.smithy.java.core.serde", "TypeRegistry");
    private static final ClassName V2_MODELED_ERROR =
        ClassName.get("software.amazon.awssdk.bridge.smithyjava.error", "V2ModeledError");
    private static final ClassName LIST = ClassName.get("java.util", "List");
    private static final ClassName V2_OPERATION_METADATA =
        ClassName.get("software.amazon.awssdk.bridge.smithyjava.client", "V2OperationMetadata");

    private final IntermediateModel model;
    private final OperationModel operationModel;
    private final PoetExtension poetExtensions;
    private final ClassName inputType;
    private final ClassName outputType;

    public ApiOperationSpec(IntermediateModel model, OperationModel operationModel) {
        this.model = model;
        this.operationModel = operationModel;
        this.poetExtensions = new PoetExtension(model);
        this.inputType = operationModel.getInputShape() != null
            ? poetExtensions.getModelClass(operationModel.getInputShape().getShapeName())
            : ClassName.get("software.amazon.smithy.java.core.schema", "SerializableStruct");
        this.outputType = operationModel.getOutputShape() != null
            ? poetExtensions.getModelClass(operationModel.getOutputShape().getShapeName())
            : poetExtensions.getModelClass(operationModel.getOperationName() + "Response");
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName self = className();
        ParameterizedTypeName apiOpType = ParameterizedTypeName.get(API_OPERATION, inputType, outputType);

        CodeBlock executionAttributes = executionAttributes();
        TypeSpec.Builder type = TypeSpec.classBuilder(self);
        if (!executionAttributes.isEmpty()) {
            type.addSuperinterface(ParameterizedTypeName.get(V2_OPERATION_METADATA, inputType))
                .addMethod(putExecutionAttributesMethod(executionAttributes));
        }

        return type.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                   .addAnnotation(PoetUtils.generatedAnnotation())
                   .addSuperinterface(apiOpType)
                   .addField(instanceField(self))
                   .addField(schemaField())
                   .addField(typeRegistryField())
                   .addField(schemesField())
                   .addMethod(instanceMethod(self))
                   .addMethod(privateConstructor())
                   .addMethod(inputBuilderMethod())
                   .addMethod(outputBuilderMethod())
                   .addMethod(schemaMethod())
                   .addMethod(inputSchemaMethod())
                   .addMethod(outputSchemaMethod())
                   .addMethod(errorRegistryMethod())
                   .addMethod(errorSchemasMethod())
                   .addMethod(effectiveAuthSchemesMethod())
                   .addMethod(serviceMethod())
                   .build();
    }

    /**
     * The operation-level traits stock v2 writes into a call's execution attributes, emitted by the same
     * generators the stock client method uses, so the bridge writes exactly what stock v2 writes.
     *
     * <p>{@code httpChecksum} and {@code httpChecksumRequired} decide request checksums and response
     * validation; {@code requestCompression} decides compression. None of them is a shape trait in v2 —
     * they live on the call — and none of them has a working smithy-java 1.6.1 consumer except
     * {@code @httpChecksumRequired}, which smithy implements as {@code Content-MD5} where current v2 sends a
     * CRC32. So they travel as v2 attributes, to v2's signer, rather than as smithy traits; see
     * {@code V2OperationMetadata}.
     */
    private CodeBlock executionAttributes() {
        if (operationModel.getInputShape() == null) {
            return CodeBlock.of("");
        }
        return CodeBlock.builder()
                        .add(HttpChecksumRequiredTrait.putHttpChecksumAttribute(operationModel))
                        .add(HttpChecksumTrait.create(operationModel))
                        .add(RequestCompressionTrait.create(operationModel, model))
                        .build();
    }

    private MethodSpec putExecutionAttributesMethod(CodeBlock executionAttributes) {
        return MethodSpec.methodBuilder("putExecutionAttributes")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(inputType, operationModel.getInput().getVariableName())
                         .addParameter(V2_OPERATION_METADATA.nestedClass("Attributes"), "attributes")
                         .addCode("attributes")
                         .addCode(executionAttributes)
                         .addCode(";\n")
                         .build();
    }

    @Override
    public ClassName className() {
        String basePackage = model.getMetadata().getFullModelPackageName();
        // Place operations in a sibling "operations" package
        String operationsPackage = basePackage.replace(".model", ".operations");
        return ClassName.get(operationsPackage, operationModel.getOperationName() + "Operation");
    }

    private FieldSpec instanceField(ClassName self) {
        return FieldSpec.builder(self, "INSTANCE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", self)
                        .build();
    }

    /**
     * The operation schema, carrying an {@code @http} trait whenever the model has one.
     *
     * <p>awsJson never needed it — every call is {@code POST /} and the operation is named by the
     * {@code X-Amz-Target} header — so the original version of this method emitted a bare
     * {@code createOperation(id)}. A REST protocol cannot build a request line without it:
     * {@code HttpBindingClientProtocol} takes the method, the URI pattern that
     * {@code @httpLabel} members are substituted into, and the success status code from this trait.
     * Without it every member bound to the URI has nowhere to go.
     *
     * <p>The values come from the input shape's marshaller, which is where C2J's {@code http} block
     * lands in the intermediate model. Only the REST protocols get the trait (see
     * {@link #httpTraitInfo()}), so awsJson output is byte-for-byte what it was.
     */
    private FieldSpec schemaField() {
        HttpTraitInfo http = httpTraitInfo();
        if (http == null) {
            return FieldSpec.builder(SCHEMA, "$SCHEMA", Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.createOperation($T.from($S))",
                                         SCHEMA, SHAPE_ID, smithyOperationId())
                            .build();
        }
        CodeBlock.Builder init = CodeBlock.builder()
            .add("$T.createOperation($T.from($S),\n", SCHEMA, SHAPE_ID, smithyOperationId())
            .add("    $T.builder().method($S).uri($T.parse($S))",
                 HTTP_TRAIT, http.method, URI_PATTERN, http.uri);
        if (http.code != null) {
            init.add(".code($L)", http.code);
        }
        init.add(".build())");
        return FieldSpec.builder(SCHEMA, "$SCHEMA", Modifier.STATIC, Modifier.FINAL)
                        .initializer(init.build())
                        .build();
    }

    private static final class HttpTraitInfo {
        private final String method;
        private final String uri;
        private final String code;

        private HttpTraitInfo(String method, String uri, String code) {
            this.method = method;
            this.uri = uri;
            this.code = code;
        }
    }

    /**
     * Returns null for the RPC protocols, which do not want an {@code @http} trait.
     *
     * <p>The gate is on protocol rather than on the marshaller having a verb, because C2J writes
     * {@code {"method": "POST", "requestUri": "/"}} for awsJson and query too — reading it
     * unconditionally would emit a trait on every existing service and change generated output that
     * is currently byte-stable and benchmarked.
     */
    private HttpTraitInfo httpTraitInfo() {
        Protocol protocol = model.getMetadata().getProtocol();
        if (protocol != Protocol.REST_XML && protocol != Protocol.REST_JSON) {
            return null;
        }
        if (operationModel.getInputShape() == null || operationModel.getInputShape().getMarshaller() == null) {
            return null;
        }
        ShapeMarshaller marshaller = operationModel.getInputShape().getMarshaller();
        String verb = marshaller.getVerb();
        String requestUri = marshaller.getRequestUri();
        if (verb == null || requestUri == null) {
            return null;
        }
        // smithy requires an absolute path pattern; C2J writes "/" for the RPC protocols and a real
        // path for the REST ones, but be defensive about a leading slash either way.
        String uri = requestUri.startsWith("/") ? requestUri : "/" + requestUri;
        return new HttpTraitInfo(verb, uri, successStatusCode(marshaller));
    }

    /**
     * The modeled success status code, or null to let smithy default it to 200.
     *
     * <p>Most operations do not model one; S3 is unusual in that several do (204 on DeleteObject, 206
     * on a ranged GetObject). An unparseable value defers to smithy rather than guessing, since a
     * wrong number here would misdescribe a successful response.
     */
    private String successStatusCode(ShapeMarshaller marshaller) {
        String code = marshaller.getResponseCode();
        if (code == null) {
            return null;
        }
        try {
            return String.valueOf(Integer.parseInt(code.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Registers every modeled error of this operation under the {@code V2ModeledError} shim.
     *
     * <p>The obvious registration — {@code putType(id, SomeException.class, SomeException::builder)} —
     * cannot work: smithy-java's {@code HttpErrorDeserializer} looks builders up with
     * {@code createBuilder(id, ModeledException.class)}, and a v2 exception already extends
     * {@code AwsServiceException} so it cannot also extend {@code ModeledException}. The shim is a
     * {@code ModeledException} whose builder delegates to the v2 exception's generated builder, so
     * deserialization still produces a real v2 exception instance.
     */
    private FieldSpec typeRegistryField() {
        CodeBlock.Builder builder = CodeBlock.builder()
            .add("$T.builder()\n", TYPE_REGISTRY);

        // Every error the service models, not only the operation's own: stock v2's generated error mapping
        // for each operation lists them all (DynamoDB's GetItem maps ConditionalCheckFailedException), so a
        // service error an operation does not declare still arrives as its modeled type. Registering only the
        // operation's errors turned those into the service's base exception. Operation errors first, so an
        // operation-specific registration wins any duplicate.
        List<String> exceptions = java.util.stream.Stream.concat(
                operationModel.getExceptions().stream().map(e -> e.getExceptionName()),
                model.getShapes().values().stream()
                     .filter(shape -> shape.getShapeType() == ShapeType.Exception)
                     .map(shape -> shape.getShapeName()))
            .distinct()
            .collect(Collectors.toList());

        for (String exceptionName : exceptions) {
            ClassName exceptionClass = poetExtensions.getModelClass(exceptionName);
            builder.add("    .putType($T.$$SCHEMA.id(), $T.class, $T.builderFactory($T.$$SCHEMA, $T::builder))\n",
                        exceptionClass, V2_MODELED_ERROR, V2_MODELED_ERROR, exceptionClass, exceptionClass);
        }
        builder.add("    .build()");

        return FieldSpec.builder(TYPE_REGISTRY, "TYPE_REGISTRY", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(builder.build())
                        .build();
    }

    private FieldSpec schemesField() {
        return FieldSpec.builder(ParameterizedTypeName.get(LIST, SHAPE_ID), "SCHEMES",
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.of($T.from($S))", LIST, SHAPE_ID, "aws.auth#sigv4")
                        .build();
    }

    private MethodSpec instanceMethod(ClassName self) {
        return MethodSpec.methodBuilder("instance")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(self)
                         .addStatement("return INSTANCE")
                         .build();
    }

    private MethodSpec privateConstructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .build();
    }

    private MethodSpec inputBuilderMethod() {
        ParameterizedTypeName returnType = ParameterizedTypeName.get(SHAPE_BUILDER, inputType);
        return MethodSpec.methodBuilder("inputBuilder")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(returnType)
                         .addStatement("return $T.builder()", inputType)
                         .build();
    }

    private MethodSpec outputBuilderMethod() {
        ParameterizedTypeName returnType = ParameterizedTypeName.get(SHAPE_BUILDER, outputType);
        return MethodSpec.methodBuilder("outputBuilder")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(returnType)
                         .addStatement("return $T.builder()", outputType)
                         .build();
    }

    private MethodSpec schemaMethod() {
        return MethodSpec.methodBuilder("schema")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(SCHEMA)
                         .addStatement("return $$SCHEMA")
                         .build();
    }

    private MethodSpec inputSchemaMethod() {
        return MethodSpec.methodBuilder("inputSchema")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(SCHEMA)
                         .addStatement("return $T.$$SCHEMA", inputType)
                         .build();
    }

    private MethodSpec outputSchemaMethod() {
        return MethodSpec.methodBuilder("outputSchema")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(SCHEMA)
                         .addStatement("return $T.$$SCHEMA", outputType)
                         .build();
    }

    private MethodSpec errorRegistryMethod() {
        return MethodSpec.methodBuilder("errorRegistry")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(TYPE_REGISTRY)
                         .addStatement("return TYPE_REGISTRY")
                         .build();
    }

    private MethodSpec errorSchemasMethod() {
        CodeBlock.Builder listBuilder = CodeBlock.builder().add("$T.of(", LIST);
        List<String> exceptions = operationModel.getExceptions().stream()
            .map(e -> e.getExceptionName())
            .collect(Collectors.toList());
        for (int i = 0; i < exceptions.size(); i++) {
            ClassName exceptionClass = poetExtensions.getModelClass(exceptions.get(i));
            listBuilder.add("$T.$$SCHEMA", exceptionClass);
            if (i < exceptions.size() - 1) {
                listBuilder.add(", ");
            }
        }
        listBuilder.add(")");

        return MethodSpec.methodBuilder("errorSchemas")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(ParameterizedTypeName.get(LIST, SCHEMA))
                         .addStatement("return $L", listBuilder.build())
                         .build();
    }

    private MethodSpec effectiveAuthSchemesMethod() {
        return MethodSpec.methodBuilder("effectiveAuthSchemes")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(ParameterizedTypeName.get(LIST, SHAPE_ID))
                         .addStatement("return SCHEMES")
                         .build();
    }

    private MethodSpec serviceMethod() {
        ClassName serviceClass = apiServiceClassName();
        return MethodSpec.methodBuilder("service")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(API_SERVICE)
                         .addStatement("return $T.instance()", serviceClass)
                         .build();
    }

    private String smithyOperationId() {
        String namespace = "com.amazonaws." + model.getMetadata().getEndpointPrefix();
        return namespace + "#" + operationModel.getOperationName();
    }

    private ClassName apiServiceClassName() {
        String basePackage = model.getMetadata().getFullModelPackageName();
        String operationsPackage = basePackage.replace(".model", ".operations");
        String serviceName = model.getMetadata().getServiceName();
        return ClassName.get(operationsPackage, serviceName + "ApiService");
    }
}
