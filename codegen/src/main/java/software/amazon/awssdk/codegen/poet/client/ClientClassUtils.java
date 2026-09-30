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

package software.amazon.awssdk.codegen.poet.client;

import static javax.lang.model.element.Modifier.PRIVATE;
import static software.amazon.awssdk.codegen.internal.Constant.ASYNC_STREAMING_INPUT_PARAM;
import static software.amazon.awssdk.codegen.internal.Constant.ASYNC_STREAMING_OUTPUT_PARAM;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_STREAMING_INPUT_PARAM;
import static software.amazon.awssdk.codegen.internal.Constant.SYNC_STREAMING_OUTPUT_PARAM;
import static software.amazon.awssdk.codegen.poet.PoetUtils.classNameFromFqcn;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.auth.signer.EventStreamAws4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.codegen.model.config.customization.S3ArnableFieldConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.model.service.HostPrefixProcessor;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeSpecUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

public final class ClientClassUtils {
    // The bridge types below are referenced by name rather than by class so that the code generator does
    // not need a compile-time dependency on the bridge, which depends on the generated code's runtime in turn.
    static final ClassName SMITHY_BRIDGE_CLIENT =
        ClassName.get("software.amazon.awssdk.bridge.smithyjava.client", "SmithyBridgeClient");
    private static final ClassName HTTP_CHECKSUM_VALIDATION_INTERCEPTOR =
        ClassName.get("software.amazon.awssdk.core.internal.interceptor", "HttpChecksumValidationInterceptor");
    private static final ClassName SDK_INTERNAL_EXECUTION_ATTRIBUTE =
        ClassName.get("software.amazon.awssdk.core.interceptor", "SdkInternalExecutionAttribute");
    private static final ClassName V2_ENDPOINT_RESOLVER_BRIDGE =
        ClassName.get("software.amazon.awssdk.bridge.smithyjava.endpoints", "V2EndpointResolverBridge");
    private static final ClassName V2_CONFIG_TRANSLATOR =
        ClassName.get("software.amazon.awssdk.bridge.smithyjava.client", "V2ConfigTranslator");
    private static final ClassName AWS_JSON_1_PROTOCOL =
        ClassName.get("software.amazon.smithy.java.aws.client.awsjson", "AwsJson1Protocol");
    private static final ClassName REST_XML_PROTOCOL =
        ClassName.get("software.amazon.smithy.java.aws.client.restxml", "RestXmlClientProtocol");
    private static final ClassName ASYNC_STREAMING_INVOKER =
        ClassName.get("software.amazon.awssdk.bridge.smithyjava.streaming", "V2AsyncStreamingInvoker");
    private static final ClassName STREAMING_INVOKER =
        ClassName.get("software.amazon.awssdk.bridge.smithyjava.streaming", "V2StreamingInvoker");

    private ClientClassUtils() {
    }

    /**
     * Whether this operation's sync and async implementations delegate to the smithy-java pipeline instead
     * of the v2 {@code ClientExecutionParams} pipeline.
     *
     * <p>The method bodies in {@code SyncClientClass} and {@code AsyncClientClass} and the execution handler
     * in the protocol specs must agree on this, which is why the predicate lives here.
     *
     * <p>Streaming operations are included; see {@code V2StreamingInvoker} and
     * {@code V2AsyncStreamingInvoker}. Event streams are not. The sync client filters them out before this
     * is reached, in {@code SyncClientClass#operations}; the async client keeps them, on the stock
     * pipeline, which is why the check has to be here as well.
     */
    public static boolean usesSmithyPipeline(IntermediateModel model, OperationModel opModel) {
        return model.getCustomizationConfig() != null
               && model.getCustomizationConfig().isGenerateSmithyJavaSerde()
               && !opModel.hasEventStreamInput()
               && !opModel.hasEventStreamOutput();
    }

    /**
     * Generates the smithy-java execution path: hand the request to the smithy-java client and let it
     * run the whole call — serialization, endpoint resolution, auth, signing, retries, deserialization,
     * and interceptors. The v2 {@code ClientExecutionParams} pipeline is bypassed entirely.
     *
     * <p>Error translation happens inside {@code SmithyBridgeClient#invoke}, so nothing
     * service-specific is emitted per operation. Identical for every protocol — the protocol only
     * decides the wire format, which the {@code ClientProtocol} passed at client construction owns —
     * so it lives here rather than in each {@code ProtocolSpec}.
     *
     * <p>A streaming operation goes through {@code V2StreamingInvoker} instead, which threads the
     * caller's {@code RequestBody} and {@code ResponseTransformer} past the schema layer. It has to be a
     * separate entry point rather than an extra argument to {@code invoke}, because the body is not part
     * of the shape: v2's generator strips a streaming member from the POJO, so there is nothing for the
     * serializer to write.
     */
    public static CodeBlock smithyJavaExecutionHandler(IntermediateModel model, OperationModel opModel) {
        String operationsPackage = model.getMetadata().getFullModelPackageName().replace(".model", ".operations");
        ClassName operationClass = ClassName.get(operationsPackage, opModel.getOperationName() + "Operation");
        String input = opModel.getInput().getVariableName();

        CodeBlock.Builder body = CodeBlock.builder().add("\n\n");

        if (opModel.hasStreamingOutput()) {
            // The request body argument is null for an operation that only streams its response; the
            // invoker takes it either way so that the both-directions case needs no third overload.
            return body.addStatement("return $T.invoke(smithyClient, $L, $T.instance(), $L, $L)",
                                     STREAMING_INVOKER, input, operationClass,
                                     opModel.hasStreamingInput() ? SYNC_STREAMING_INPUT_PARAM : "null",
                                     SYNC_STREAMING_OUTPUT_PARAM)
                       .build();
        }
        if (opModel.hasStreamingInput()) {
            return body.addStatement("return $T.invoke(smithyClient, $L, $T.instance(), $L)",
                                     STREAMING_INVOKER, input, operationClass, SYNC_STREAMING_INPUT_PARAM)
                       .build();
        }
        return body.addStatement("return smithyClient.invoke($L, $T.instance())", input, operationClass)
                   .build();
    }

    /**
     * Emits the construction of the one smithy-java client the generated operations delegate to.
     *
     * <p>Everything service-specific is passed in here rather than looked up inside the bridge: the
     * generated {@code ApiService}, a lambda composing the endpoint provider with the generated
     * {@code ruleParams}, the built-in interceptors smithy-java replaces, and the service base
     * exception used for unmodeled failures.
     */
    static void addSmithyClientConstruction(MethodSpec.Builder builder, IntermediateModel model,
                                            PoetExtension poetExtensions) {
        EndpointRulesSpecUtils endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        AuthSchemeSpecUtils authSchemeSpecUtils = new AuthSchemeSpecUtils(model);
        String operationsPackage = model.getMetadata().getFullModelPackageName().replace(".model", ".operations");
        ClassName apiService = ClassName.get(operationsPackage, model.getMetadata().getServiceName() + "ApiService");
        ClassName baseException = poetExtensions.getModelClass(model.getMetadata().getBaseExceptionName());

        builder.addCode("this.smithyClient = $T.newClientBuilder(this.clientConfiguration,\n", V2_CONFIG_TRANSLATOR);
        builder.addCode("    $T.instance(),\n", apiService);
        builder.addCode("    new $T($T.instance().schema().id()),\n", smithyProtocolClass(model), apiService);
        // The endpoint, then the operation's @endpoint host prefix, as the stock resolver interceptor does both.
        // The provider is read from the call's attributes rather than captured from the client, as the stock
        // interceptor reads it: that is where a request-level overrideConfiguration().endpointProvider(...)
        // lands (V2RequestOverrides).
        builder.addCode("    (request, executionAttributes) -> $T.withHostPrefix((($T) executionAttributes.getAttribute("
                        + "$T.ENDPOINT_PROVIDER))\n"
                        + "        .resolveEndpoint($T.ruleParams(request, executionAttributes)).join(),\n"
                        + "        $T.hostPrefix(executionAttributes.getAttribute($T.OPERATION_NAME), request), "
                        + "executionAttributes),\n",
                        V2_ENDPOINT_RESOLVER_BRIDGE,
                        endpointRulesSpecUtils.providerInterfaceName(),
                        SDK_INTERNAL_EXECUTION_ATTRIBUTE,
                        endpointRulesSpecUtils.resolverInterceptorName(),
                        endpointRulesSpecUtils.resolverInterceptorName(),
                        ClassName.get("software.amazon.awssdk.core.interceptor", "SdkExecutionAttribute"));
        // These three do work that smithy-java now owns; running them again would resolve an endpoint
        // and an auth scheme that nothing downstream reads. The fourth, when present, is v2's response
        // checksum validator, which every v2 client carries: it acts only for an operation whose
        // httpChecksum trait names a validation-mode member, so for a service with none it is inert, and
        // listing it here keeps it from being the one interceptor that installs the interceptor bridge on
        // an otherwise default client (the bridge's cost is ledger 2.1).
        boolean validatesResponses = model.getOperations().values().stream()
                                          .anyMatch(o -> o.getHttpChecksum() != null
                                                         && o.getHttpChecksum().getRequestValidationModeMember() != null);
        if (validatesResponses) {
            builder.addCode("    $T.of($T.class, $T.class, $T.class),\n",
                            Set.class,
                            authSchemeSpecUtils.authSchemeInterceptor(),
                            endpointRulesSpecUtils.resolverInterceptorName(),
                            endpointRulesSpecUtils.requestModifierInterceptorName());
        } else {
            builder.addCode("    $T.of($T.class, $T.class, $T.class, $T.class),\n",
                            Set.class,
                            authSchemeSpecUtils.authSchemeInterceptor(),
                            endpointRulesSpecUtils.resolverInterceptorName(),
                            endpointRulesSpecUtils.requestModifierInterceptorName(),
                            HTTP_CHECKSUM_VALIDATION_INTERCEPTOR);
        }
        // The service's base exception, used for any failure with no more specific v2 type: a transport
        // error, or an error response whose shape is not in the operation's registry. Passed as a
        // builder supplier rather than a finished exception because V2ErrorEnricher populates it from
        // the HTTP response -- status code, request ID, awsErrorDetails -- which is what makes an
        // unmodeled error retryable at all.
        builder.addCode("    $T::builder,\n", baseException);
        // v2's auth-scheme options, for the signer bridge: the configured provider composed with the
        // generated params builder, exactly as the stock auth-scheme interceptor composes them.
        builder.addCode("    (request, executionAttributes) -> (($T) executionAttributes.getAttribute("
                        + "$T.AUTH_SCHEME_RESOLVER))\n",
                        authSchemeSpecUtils.providerInterfaceName(), SDK_INTERNAL_EXECUTION_ATTRIBUTE);
        builder.addCode("        .resolveAuthScheme($T.authSchemeParams(request, executionAttributes)))\n",
                        authSchemeSpecUtils.authSchemeInterceptor());
        // The stock per-request configuration update (request-level SdkPlugins), handed to the bridge so a
        // plugin-bearing request can be translated as stock v2 would build it. Returns the client's own
        // configuration, unallocated, for a request without plugins.
        builder.addStatement("    .requestConfigurationUpdater(request -> updateSdkClientConfiguration(request, "
                             + "this.clientConfiguration))\n    .build()");
    }

    /**
     * The smithy-java protocol implementation for this service's wire protocol.
     *
     * <p>Fails loudly rather than defaulting: silently generating an awsJson client for, say, a query
     * service would produce a client that compiles, builds, and then sends the wrong bytes at runtime.
     * A protocol added here also needs its binding traits mapped in {@code SdkSchemaFactory}.
     */
    private static ClassName smithyProtocolClass(IntermediateModel model) {
        switch (model.getMetadata().getProtocol()) {
            case AWS_JSON:
                return AWS_JSON_1_PROTOCOL;
            case REST_XML:
                return REST_XML_PROTOCOL;
            default:
                throw new UnsupportedOperationException(
                    "generateSmithyJavaSerde is enabled for " + model.getMetadata().getServiceName()
                    + ", but its protocol " + model.getMetadata().getProtocol().getValue()
                    + " has no smithy-java protocol wired up in ClientClassUtils.");
        }
    }

    /** Whether the service opted in to the smithy-java pipeline at all; per operation, see {@link #usesSmithyPipeline}. */
    static boolean usesSmithyJavaSerde(IntermediateModel model) {
        return model.getCustomizationConfig() != null && model.getCustomizationConfig().isGenerateSmithyJavaSerde();
    }

    /**
     * The body of a generated <em>async</em> operation on the smithy-java pipeline, from the invocation to
     * the returned future.
     *
     * <p>The async counterpart of {@link #smithyJavaExecutionHandler}. The call itself is one line, because
     * {@code SmithyBridgeClient#runAsync} owns the threading; what is left here is v2's own per-call
     * bookkeeping around the future: metrics are published when it completes, and cancelling the future
     * the caller holds is forwarded to the one the call produced, as {@code CompletableFutureUtils
     * .forwardExceptionTo} does in every stock async operation.
     */
    public static CodeBlock smithyJavaAsyncExecutionHandler(IntermediateModel model, PoetExtension poetExtensions,
                                                            OperationModel opModel) {
        String operationsPackage = model.getMetadata().getFullModelPackageName().replace(".model", ".operations");
        ClassName operationClass = ClassName.get(operationsPackage, opModel.getOperationName() + "Operation");
        String input = opModel.getInput().getVariableName();

        TypeName resultType = opModel.hasStreamingOutput()
                              ? TypeVariableName.get("ReturnT")
                              : poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        TypeName futureType = ParameterizedTypeName.get(ClassName.get(CompletableFuture.class), resultType);

        CodeBlock.Builder body = CodeBlock.builder().add("\n\n");
        if (opModel.hasStreamingOutput()) {
            body.addStatement("$T executeFuture = $T.invoke(smithyClient, $L, $T.instance(), $L, $L)",
                              futureType, ASYNC_STREAMING_INVOKER, input, operationClass,
                              opModel.hasStreamingInput() ? ASYNC_STREAMING_INPUT_PARAM : "null",
                              ASYNC_STREAMING_OUTPUT_PARAM);
        } else if (opModel.hasStreamingInput()) {
            body.addStatement("$T executeFuture = $T.invoke(smithyClient, $L, $T.instance(), $L)",
                              futureType, ASYNC_STREAMING_INVOKER, input, operationClass, ASYNC_STREAMING_INPUT_PARAM);
        } else {
            body.addStatement("$T executeFuture = smithyClient.invokeAsync($L, $T.instance())",
                              futureType, input, operationClass);
        }
        return body.addStatement("$T whenCompleted = executeFuture.whenComplete((r, e) -> "
                                 + "metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect())))",
                                 futureType)
                   .addStatement("return $T.forwardExceptionTo(whenCompleted, executeFuture)", CompletableFutureUtils.class)
                   .build();
    }

    static MethodSpec consumerBuilderVariant(MethodSpec spec, String javadoc) {
        Validate.validState(spec.parameters.size() > 0, "A first parameter is required to generate a consumer-builder method.");
        Validate.validState(spec.parameters.get(0).type instanceof ClassName, "The first parameter must be a class.");

        ParameterSpec firstParameter = spec.parameters.get(0);
        ClassName firstParameterClass = (ClassName) firstParameter.type;
        TypeName consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), firstParameterClass.nestedClass("Builder"));

        MethodSpec.Builder result = MethodSpec.methodBuilder(spec.name)
                                              .returns(spec.returnType)
                                              .addExceptions(spec.exceptions)
                                              .addAnnotations(spec.annotations)
                                              .addJavadoc(javadoc)
                                              .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                                              .addTypeVariables(spec.typeVariables)
                                              .addParameter(ParameterSpec.builder(consumer, firstParameter.name).build());


        // Parameters
        StringBuilder methodBody = new StringBuilder("return $L($T.builder().applyMutation($L).build()");
        for (int i = 1; i < spec.parameters.size(); i++) {
            ParameterSpec parameter = spec.parameters.get(i);
            methodBody.append(", ").append(parameter.name);
            result.addParameter(parameter);
        }
        methodBody.append(")");

        result.addStatement(methodBody.toString(), spec.name, firstParameterClass, firstParameter.name);

        return result.build();
    }

    static MethodSpec applySignerOverrideMethod(PoetExtension poetExtensions, IntermediateModel model) {
        String signerOverrideVariable = "signerOverride";

        TypeVariableName typeVariableName =
            TypeVariableName.get("T", poetExtensions.getModelClass(model.getSdkRequestBaseClassName()));

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName
            .get(ClassName.get(Consumer.class), ClassName.get(AwsRequestOverrideConfiguration.Builder.class));

        CodeBlock codeBlock = CodeBlock.builder()
                                       .beginControlFlow("if (request.overrideConfiguration().flatMap(c -> c.signer())"
                                                         + ".isPresent())")
                                       .addStatement("return request")
                                       .endControlFlow()
                                       .addStatement("$T $L = b -> b.signer(signer).build()",
                                                     parameterizedTypeName,
                                                     signerOverrideVariable)
                                       .addStatement("$1T overrideConfiguration =\n"
                                                     + "            request.overrideConfiguration().map(c -> c.toBuilder()"
                                                     + ".applyMutation($2L).build())\n"
                                                     + "            .orElse((AwsRequestOverrideConfiguration.builder()"
                                                     + ".applyMutation($2L).build()))",
                                                     AwsRequestOverrideConfiguration.class,
                                                     signerOverrideVariable)
                                       .addStatement("return (T) request.toBuilder().overrideConfiguration"
                                                     + "(overrideConfiguration).build()")
                                       .build();

        return MethodSpec.methodBuilder("applySignerOverride")
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(typeVariableName, "request")
                         .addParameter(Signer.class, "signer")
                         .addTypeVariable(typeVariableName)
                         .addCode(codeBlock)
                         .returns(typeVariableName)
                         .build();
    }

    static CodeBlock callApplySignerOverrideMethod(OperationModel opModel) {
        CodeBlock.Builder code = CodeBlock.builder();
        ShapeModel inputShape = opModel.getInputShape();

        if (inputShape.getRequestSignerClassFqcn() != null) {
            code.addStatement("$1L = applySignerOverride($1L, $2T.create())",
                              opModel.getInput().getVariableName(),
                              PoetUtils.classNameFromFqcn(inputShape.getRequestSignerClassFqcn()));
        } else if (opModel.hasEventStreamInput()) {
            code.addStatement("$1L = applySignerOverride($1L, $2T.create())",
                              opModel.getInput().getVariableName(), EventStreamAws4Signer.class);
        }

        return code.build();
    }

    static CodeBlock addEndpointTraitCode(OperationModel opModel) {
        CodeBlock.Builder builder = CodeBlock.builder();

        if (opModel.getEndpointTrait() != null && !StringUtils.isEmpty(opModel.getEndpointTrait().getHostPrefix())) {
            String hostPrefix = opModel.getEndpointTrait().getHostPrefix();
            HostPrefixProcessor processor = new HostPrefixProcessor(hostPrefix);

            builder.addStatement("String hostPrefix = $S", hostPrefix);

            if (processor.c2jNames().isEmpty()) {
                builder.addStatement("String resolvedHostExpression = $S", processor.hostWithStringSpecifier());
            } else {
                processor.c2jNames()
                         .forEach(name -> builder.addStatement("$T.validateHostnameCompliant($L, $S, $S)",
                                                               HostnameValidator.class,
                                                               inputShapeMemberGetter(opModel, name),
                                                               name, opModel.getInput().getVariableName()));

                builder.addStatement("String resolvedHostExpression = String.format($S, $L)",
                                     processor.hostWithStringSpecifier(),
                                     processor.c2jNames().stream()
                                              .map(n -> inputShapeMemberGetter(opModel, n))
                                              .collect(Collectors.joining(",")));
            }
        }

        return builder.build();
    }

    static MethodSpec updateSdkClientConfigurationMethod(
        TypeName serviceClientConfigurationBuilderClassName, IntermediateModel model) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("updateSdkClientConfiguration")
                                               .addModifiers(PRIVATE)
                                               .addParameter(SdkRequest.class, "request")
                                               .addParameter(SdkClientConfiguration.class, "clientConfiguration")
                                               .returns(SdkClientConfiguration.class);

        builder.addStatement("$T plugins = request.overrideConfiguration()\n"
                             + ".map(c -> c.plugins()).orElse(Collections.emptyList())",
                             ParameterizedTypeName.get(List.class, SdkPlugin.class));

        builder.beginControlFlow("if (plugins.isEmpty())")
               .addStatement("return clientConfiguration")
               .endControlFlow();

        builder.addStatement("$T configuration = clientConfiguration.toBuilder()", SdkClientConfiguration.Builder.class)
               .addStatement("$1T serviceConfigBuilder = new $1T(configuration)", serviceClientConfigurationBuilderClassName)
               .beginControlFlow("for ($T plugin : plugins)", SdkPlugin.class)
               .addStatement("plugin.configureClient(serviceConfigBuilder)")
               .endControlFlow();
        EndpointRulesSpecUtils endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);

        if (model.getCustomizationConfig() == null ||
            CollectionUtils.isNullOrEmpty(model.getCustomizationConfig().getCustomClientContextParams())) {
            builder.addStatement("updateRetryStrategyClientConfiguration(configuration)");
            builder.addStatement("return configuration.build()");
            return builder.build();
        }

        Map<String, ClientContextParam> customClientConfigParams = model.getCustomizationConfig().getCustomClientContextParams();

        builder.addCode("$1T newContextParams = configuration.option($2T.CLIENT_CONTEXT_PARAMS);\n"
                        + "$1T originalContextParams = clientConfiguration.option($2T.CLIENT_CONTEXT_PARAMS);",
                        AttributeMap.class, SdkClientOption.class);

        builder.addCode("newContextParams = (newContextParams != null) ? newContextParams : $1T.empty();\n"
                        + "originalContextParams = originalContextParams != null ? originalContextParams : $1T.empty();",
                        AttributeMap.class);

        customClientConfigParams.forEach((n, m) -> {
            String keyName = model.getNamingStrategy().getEnumValueName(n);
            builder.addStatement("$1T.validState($2T.equals(originalContextParams.get($3T.$4N), newContextParams.get($3T.$4N)),"
                                 + " $5S)",
                                 Validate.class, Objects.class, endpointRulesSpecUtils.clientContextParamsName(), keyName,
                                 keyName + " cannot be modified by request level plugins");
        });
        builder.addStatement("updateRetryStrategyClientConfiguration(configuration)");
        builder.addStatement("return configuration.build()");
        return builder.build();
    }

    static Optional<CodeBlock> addS3ArnableFieldCode(OperationModel opModel, IntermediateModel model) {
        CodeBlock.Builder builder = CodeBlock.builder();
        Map<String, S3ArnableFieldConfig> s3ArnableFields = model.getCustomizationConfig().getS3ArnableFields();

        if (s3ArnableFields != null &&
            s3ArnableFields.containsKey(opModel.getInputShape().getShapeName())) {
            S3ArnableFieldConfig s3ArnableField = s3ArnableFields.get(opModel.getInputShape().getShapeName());
            String fieldName = s3ArnableField.getField();
            MemberModel arnableMember = opModel.getInputShape().tryFindMemberModelByC2jName(fieldName, true);
            ClassName arnResourceFqcn = classNameFromFqcn(s3ArnableField.getArnResourceFqcn());

            builder.addStatement("String $N = $N.$N()", fieldName,
                                 opModel.getInput().getVariableName(), arnableMember.getFluentGetterMethodName());
            builder.addStatement("$T arn = null", Arn.class);
            builder.beginControlFlow("if ($N != null && $N.startsWith(\"arn:\"))", fieldName, fieldName)
                   .addStatement("arn = $T.fromString($N)", Arn.class, fieldName)
                   .addStatement("$T s3Resource = $T.getInstance().convertArn(arn)",
                                 classNameFromFqcn(s3ArnableField.getBaseArnResourceFqcn()),
                                 classNameFromFqcn(s3ArnableField.getArnConverterFqcn()))
                   .beginControlFlow("if (!(s3Resource instanceof $T))", arnResourceFqcn)
                   .addStatement("throw new $T(String.format(\"Unsupported ARN type: %s\", s3Resource.type()))",
                                 IllegalArgumentException.class)
                   .endControlFlow()
                   .addStatement("$T resource = ($T) s3Resource", arnResourceFqcn, arnResourceFqcn);

            Map<String, String> otherFieldsToPopulate = s3ArnableField.getOtherFieldsToPopulate();

            for (Map.Entry<String, String> entry : otherFieldsToPopulate.entrySet()) {
                MemberModel memberModel = opModel.getInputShape().tryFindMemberModelByC2jName(entry.getKey(), true);
                String variableName = memberModel.getVariable().getVariableName();
                String arnVariableName = variableName + "InArn";
                builder.addStatement("String $N = $N.$N()", variableName,
                                     opModel.getInput().getVariableName(),
                                     memberModel.getFluentGetterMethodName());
                builder.addStatement("String $N = resource.$N",
                                     arnVariableName,
                                     entry.getValue());
                builder.beginControlFlow("if ($N != null && !$N.equals($N))",
                                         variableName,
                                         variableName,
                                         arnVariableName)
                       .addStatement("throw new $T(String.format(\"%s field provided from the request (%s) is different from "
                                     + "the one in the ARN (%s)\", $S, $N, $N))",
                                     IllegalArgumentException.class,
                                     variableName,
                                     variableName, arnVariableName)
                       .endControlFlow();
            }

            builder.add("$N = $N.toBuilder().$N(resource.$N())",
                        opModel.getInput().getVariableName(),
                        opModel.getInput().getVariableName(),
                        arnableMember.getFluentSetterMethodName(),
                        s3ArnableField.getArnResourceSubstitutionGetter());

            for (Map.Entry<String, String> entry : otherFieldsToPopulate.entrySet()) {
                MemberModel memberModel = opModel.getInputShape().tryFindMemberModelByC2jName(entry.getKey(), true);
                String variableName = memberModel.getVariable().getVariableName();
                String arnVariableName = variableName + "InArn";
                builder.add(".$N($N)", memberModel.getFluentSetterMethodName(), arnVariableName);
            }

            return Optional.of(builder.addStatement(".build()").endControlFlow().build());
        }
        return Optional.empty();
    }

    /**
     * Given operation and c2j name, returns the String that represents calling the
     * c2j member's getter method in the opmodel input shape.
     *
     * For example, Operation is CreateConnection and c2j name is CatalogId,
     * returns "createConnectionRequest.catalogId()"
     */
    private static String inputShapeMemberGetter(OperationModel opModel, String c2jName) {
        return opModel.getInput().getVariableName() + "." +
               opModel.getInputShape().getMemberByC2jName(c2jName).getFluentGetterMethodName() + "()";
    }

    public static MethodSpec updateRetryStrategyClientConfigurationMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("updateRetryStrategyClientConfiguration")
                                               .addModifiers(Modifier.PRIVATE)
                                               .addParameter(SdkClientConfiguration.Builder.class, "configuration");
        builder.addStatement("$T builder = configuration.asOverrideConfigurationBuilder()",
                             ClientOverrideConfiguration.Builder.class);
        builder.addStatement("$T retryMode = builder.retryMode()", RetryMode.class);
        builder.beginControlFlow("if (retryMode != null)")
               .addStatement("configuration.option($T.RETRY_STRATEGY, $T.forRetryMode(retryMode))", SdkClientOption.class,
                             AwsRetryStrategy.class);
        builder.nextControlFlow("else");
        builder.addStatement("$T<$T<?, ?>> configurator = builder.retryStrategyConfigurator()", Consumer.class,
                             RetryStrategy.Builder.class);
        builder.beginControlFlow("if (configurator != null)")
               .addStatement("$T<?, ?>  defaultBuilder = $T.defaultRetryStrategy().toBuilder()", RetryStrategy.Builder.class,
                             AwsRetryStrategy.class)
               .addStatement("configurator.accept(defaultBuilder)")
               .addStatement("configuration.option($T.RETRY_STRATEGY, defaultBuilder.build())", SdkClientOption.class);
        builder.nextControlFlow("else");
        builder.addStatement("$T retryStrategy = builder.retryStrategy()", RetryStrategy.class);
        builder.beginControlFlow("if (retryStrategy != null)")
               .addStatement("configuration.option($T.RETRY_STRATEGY, retryStrategy)", SdkClientOption.class)
               .endControlFlow();
        builder.endControlFlow();
        builder.endControlFlow();
        builder.addStatement("configuration.option($T.CONFIGURED_RETRY_MODE, null)", SdkClientOption.class);
        builder.addStatement("configuration.option($T.CONFIGURED_RETRY_STRATEGY, null)", SdkClientOption.class);
        builder.addStatement("configuration.option($T.CONFIGURED_RETRY_CONFIGURATOR, null)", SdkClientOption.class);
        return builder.build();
    }

    // According to User Agent 2.0 spec, replace spaces with underscores
    static String transformServiceId(String serviceId) {
        return serviceId.replace(" ", "_");
    }
}
