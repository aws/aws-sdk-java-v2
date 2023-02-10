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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.internal.Constant.EVENT_PUBLISHER_PARAM_NAME;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.addS3ArnableFieldCode;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.applyPaginatorUserAgentMethod;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.applySignerOverrideMethod;
import static software.amazon.awssdk.codegen.poet.client.SyncClientClass.getProtocolSpecs;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AsyncAws4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.client.handler.AwsClientHandlerUtils;
import software.amazon.awssdk.awscore.eventstream.EventStreamTaggedUnionJsonMarshaller;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.StaticImport;
import software.amazon.awssdk.codegen.poet.client.specs.ProtocolSpec;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;
import software.amazon.awssdk.codegen.poet.model.EventStreamSpecHelper;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.AsyncResponseTransformerUtils;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRefreshCache;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRequest;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.Pair;

public final class AsyncClientClass extends AsyncClientInterface {
    private final IntermediateModel model;
    private final PoetExtension poetExtensions;
    private final ClassName className;
    private final ProtocolSpec protocolSpec;

    public AsyncClientClass(GeneratorTaskParams dependencies) {
        super(dependencies.getModel());
        this.model = dependencies.getModel();
        this.poetExtensions = dependencies.getPoetExtensions();
        this.className = poetExtensions.getClientClass(model.getMetadata().getAsyncClient());
        this.protocolSpec = getProtocolSpecs(poetExtensions, model);
    }

    @Override
    protected TypeSpec.Builder createTypeSpec() {
        return PoetUtils.createClassBuilder(className);
    }

    @Override
    protected void addInterfaceClass(TypeSpec.Builder type) {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getAsyncInterface());
        type.addSuperinterface(interfaceClass)
            .addJavadoc("Internal implementation of {@link $1T}.\n\n@see $1T#builder()", interfaceClass);
    }

    @Override
    protected void addAnnotations(TypeSpec.Builder type) {
        type.addAnnotation(SdkInternalApi.class);
    }

    @Override
    protected void addModifiers(TypeSpec.Builder type) {
        type.addModifiers(FINAL);
    }

    @Override
    protected void addFields(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(ClassName.get(Logger.class), "log")
                               .addModifiers(PRIVATE, STATIC, FINAL)
                               .initializer("$T.getLogger($T.class)", LoggerFactory.class,
                                            className)
                               .build())
            .addField(AsyncClientHandler.class, "clientHandler", PRIVATE, FINAL)
            .addField(protocolSpec.protocolFactory(model))
            .addField(SdkClientConfiguration.class, "clientConfiguration", PRIVATE, FINAL);

        // Kinesis doesn't support CBOR for STS yet so need another protocol factory for JSON
        if (model.getMetadata().isCborProtocol()) {
            type.addField(AwsJsonProtocolFactory.class, "jsonProtocolFactory", PRIVATE, FINAL);
        }

        model.getEndpointOperation().ifPresent(
            o -> type.addField(EndpointDiscoveryRefreshCache.class, "endpointDiscoveryCache", PRIVATE));
    }

    @Override
    protected void addAdditionalMethods(TypeSpec.Builder type) {
        type.addMethod(constructor(type))
            .addMethod(nameMethod())
            .addMethods(protocolSpec.additionalMethods())
            .addMethod(protocolSpec.initProtocolFactory(model))
            .addMethod(resolveMetricPublishersMethod());

        if (model.hasPaginators()) {
            type.addMethod(applyPaginatorUserAgentMethod(poetExtensions, model));
        }

        if (model.containsRequestSigners() || model.containsRequestEventStreams() || hasStreamingV4AuthOperations()) {
            type.addMethod(applySignerOverrideMethod(poetExtensions, model));
            type.addMethod(isSignerOverriddenOnClientMethod());
        }

        protocolSpec.createErrorResponseHandler().ifPresent(type::addMethod);
    }

    @Override
    protected void addWaiterMethod(TypeSpec.Builder type) {
        type.addField(FieldSpec.builder(ClassName.get(ScheduledExecutorService.class), "executorService")
                                       .addModifiers(PRIVATE, FINAL)
                                       .build());

        MethodSpec waiter = MethodSpec.methodBuilder("waiter")
                                      .addModifiers(PUBLIC)
                                      .addAnnotation(Override.class)
                                      .addStatement("return $T.builder().client(this)"
                                                    + ".scheduledExecutorService(executorService).build()",
                                                    poetExtensions.getAsyncWaiterInterface())
                                      .returns(poetExtensions.getAsyncWaiterInterface())
                                      .build();

        type.addMethod(waiter);
    }

    @Override
    protected List<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    .flatMap(this::operations)
                    .sorted(Comparator.comparing(m -> m.name))
                    .collect(toList());
    }

    private Stream<MethodSpec> operations(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(traditionalMethod(opModel));
        if (opModel.isPaginated()) {
            methods.add(paginatedTraditionalMethod(opModel));
        }
        return methods.stream();
    }

    private MethodSpec constructor(TypeSpec.Builder classBuilder) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                                               .addModifiers(PROTECTED)
                                               .addParameter(SdkClientConfiguration.class, "clientConfiguration")
                                               .addStatement("this.clientHandler = new $T(clientConfiguration)",
                                                             AwsAsyncClientHandler.class)
                                               .addStatement("this.clientConfiguration = clientConfiguration");
        FieldSpec protocolFactoryField = protocolSpec.protocolFactory(model);
        if (model.getMetadata().isJsonProtocol()) {
            builder.addStatement("this.$N = init($T.builder()).build()", protocolFactoryField.name,
                                 protocolFactoryField.type);
        } else {
            builder.addStatement("this.$N = init()", protocolFactoryField.name);
        }
        if (model.getMetadata().isCborProtocol()) {
            builder.addStatement("this.jsonProtocolFactory = init($T.builder()).build()", AwsJsonProtocolFactory.class);
        }
        if (hasOperationWithEventStreamOutput()) {
            classBuilder.addField(FieldSpec.builder(ClassName.get(Executor.class), "executor", PRIVATE, FINAL)
                                           .build());
            builder.addStatement("this.executor = clientConfiguration.option($T.FUTURE_COMPLETION_EXECUTOR)",
                                 SdkAdvancedAsyncClientOption.class);
        }

        if (model.getEndpointOperation().isPresent()) {
            builder.beginControlFlow("if (clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED))");
            builder.addStatement("this.endpointDiscoveryCache = $T.create($T.create(this))",
                                 EndpointDiscoveryRefreshCache.class,
                                 poetExtensions.getClientClass(model.getNamingStrategy().getServiceName() +
                                                               "AsyncEndpointDiscoveryCacheLoader"));

            if (model.getCustomizationConfig().allowEndpointOverrideForEndpointDiscoveryRequiredOperations()) {
                builder.beginControlFlow("if (clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) == "
                                        + "Boolean.TRUE)");
                builder.addStatement("log.warn($S)",
                                     "Endpoint discovery is enabled for this client, and an endpoint override was also "
                                     + "specified. This will disable endpoint discovery for methods that require it, instead "
                                     + "using the specified endpoint override. This may or may not be what you intended.");
                builder.endControlFlow();
            }

            builder.endControlFlow();
        }

        if (model.hasWaiters()) {
            builder.addStatement("this.executorService = clientConfiguration.option($T.SCHEDULED_EXECUTOR_SERVICE)",
                                 SdkClientOption.class);
        }

        return builder.build();
    }

    private boolean hasOperationWithEventStreamOutput() {
        return model.getOperations().values().stream().anyMatch(OperationModel::hasEventStreamOutput);
    }

    private MethodSpec nameMethod() {
        return MethodSpec.methodBuilder("serviceName")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC, FINAL)
                         .returns(String.class)
                         .addStatement("return SERVICE_NAME")
                         .build();
    }

    @Override
    protected void addCloseMethod(TypeSpec.Builder type) {
        MethodSpec method = MethodSpec.methodBuilder("close")
                                      .addAnnotation(Override.class)
                                      .addModifiers(PUBLIC)
                                      .addStatement("$N.close()", "clientHandler")
                                      .build();

        type.addMethod(method);
    }

    @Override
    protected MethodSpec.Builder operationBody(MethodSpec.Builder builder, OperationModel opModel) {

        builder.addModifiers(PUBLIC)
               .addAnnotation(Override.class);

        builder.addStatement("$T<$T> metricPublishers = "
                             + "resolveMetricPublishers(clientConfiguration, $N.overrideConfiguration().orElse(null))",
                             List.class,
                             MetricPublisher.class,
                             opModel.getInput().getVariableName())
               .addStatement("$1T apiCallMetricCollector = metricPublishers.isEmpty() ? $2T.create() : $1T.create($3S)",
                             MetricCollector.class, NoOpMetricCollector.class, "ApiCall");
        builder.beginControlFlow("try");

        builder.addStatement("apiCallMetricCollector.reportMetric($T.$L, $S)",
                             CoreMetric.class, "SERVICE_ID", model.getMetadata().getServiceId());
        builder.addStatement("apiCallMetricCollector.reportMetric($T.$L, $S)",
                             CoreMetric.class, "OPERATION_NAME", opModel.getOperationName());
        
        if (opModel.hasStreamingOutput()) {
            ClassName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
            
            builder.addStatement("$T<$T<$T, ReturnT>, $T<$T>> $N = $T.wrapWithEndOfStreamFuture($N)",
                                 Pair.class,
                                 AsyncResponseTransformer.class,
                                 responseType,
                                 CompletableFuture.class,
                                 Void.class,
                                 "pair",
                                 AsyncResponseTransformerUtils.class,
                                 "asyncResponseTransformer");
            
            builder.addStatement("$N = $N.left()",
                                 "asyncResponseTransformer",
                                 "pair");
            
            builder.addStatement("$T<$T> $N = $N.right()",
                                 CompletableFuture.class,
                                 Void.class,
                                 "endOfStreamFuture",
                                 "pair");
        }
        
        if (shouldUseAsyncWithBodySigner(opModel)) {
            builder.addCode(applyAsyncWithBodyV4SignerOverride(opModel));
        } else {
            builder.addCode(ClientClassUtils.callApplySignerOverrideMethod(opModel));
        }

        builder.addCode(protocolSpec.responseHandler(model, opModel));
        protocolSpec.errorResponseHandler(opModel).ifPresent(builder::addCode);
        builder.addCode(eventToByteBufferPublisher(opModel));

        if (opModel.getEndpointDiscovery() != null) {
            builder.addStatement("boolean endpointDiscoveryEnabled = "
                                 + "clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED)");
            builder.addStatement("boolean endpointOverridden = "
                                 + "clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) == Boolean.TRUE");

            if (opModel.getEndpointDiscovery().isRequired()) {
                if (!model.getCustomizationConfig().allowEndpointOverrideForEndpointDiscoveryRequiredOperations()) {
                    builder.beginControlFlow("if (endpointOverridden)");
                    builder.addStatement("throw new $T($S)", IllegalStateException.class,
                                         "This operation requires endpoint discovery, but an endpoint override was specified "
                                         + "when the client was created. This is not supported.");
                    builder.endControlFlow();

                    builder.beginControlFlow("if (!endpointDiscoveryEnabled)");
                    builder.addStatement("throw new $T($S)", IllegalStateException.class,
                                         "This operation requires endpoint discovery, but endpoint discovery was disabled on the "
                                         + "client.");
                    builder.endControlFlow();
                } else {
                    builder.beginControlFlow("if (endpointOverridden)");
                    builder.addStatement("endpointDiscoveryEnabled = false");
                    builder.nextControlFlow("else if (!endpointDiscoveryEnabled)");
                    builder.addStatement("throw new $T($S)", IllegalStateException.class,
                                         "This operation requires endpoint discovery to be enabled, or for you to specify an "
                                         + "endpoint override when the client is created.");
                    builder.endControlFlow();
                }
            }

            builder.addStatement("$T cachedEndpoint = null", URI.class);
            builder.beginControlFlow("if (endpointDiscoveryEnabled)");

            builder.addCode("$T key = $N.overrideConfiguration()", String.class, opModel.getInput().getVariableName())
                   .addCode("    .flatMap($T::credentialsProvider)", AwsRequestOverrideConfiguration.class)
                   .addCode("    .orElseGet(() -> clientConfiguration.option($T.CREDENTIALS_PROVIDER))", AwsClientOption.class)
                   .addCode("    .resolveCredentials().accessKeyId();");

            builder.addCode("$1T endpointDiscoveryRequest = $1T.builder()", EndpointDiscoveryRequest.class)
                   .addCode("    .required($L)", opModel.getInputShape().getEndpointDiscovery().isRequired())
                   .addCode("    .defaultEndpoint(clientConfiguration.option($T.ENDPOINT))", SdkClientOption.class)
                   .addCode("    .overrideConfiguration($N.overrideConfiguration().orElse(null))",
                            opModel.getInput().getVariableName())
                   .addCode("    .build();");

            builder.addStatement("cachedEndpoint = endpointDiscoveryCache.get(key, endpointDiscoveryRequest)");
            builder.endControlFlow();
        }

        addS3ArnableFieldCode(opModel, model).ifPresent(builder::addCode);
        builder.addCode(ClientClassUtils.addEndpointTraitCode(opModel));

        builder.addCode(protocolSpec.asyncExecutionHandler(model, opModel))
               .endControlFlow()
               .beginControlFlow("catch ($T t)", Throwable.class);

        // For streaming operations we also want to notify the response handler of any exception.
        if (opModel.hasStreamingOutput()) {
            ClassName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
            builder.addStatement("$T<$T, ReturnT> finalAsyncResponseTransformer = asyncResponseTransformer",
                                 AsyncResponseTransformer.class,
                                 responseType);
        }
        if (opModel.hasStreamingOutput() || opModel.hasEventStreamOutput()) {
            String paramName = opModel.hasStreamingOutput() ? "finalAsyncResponseTransformer" : "asyncResponseHandler";
            builder.addStatement("runAndLogError(log, \"Exception thrown in exceptionOccurred callback, ignoring\",\n" +
                                 "() -> $N.exceptionOccurred(t))", paramName);
        }

        builder.addStatement("metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()))")
               .addStatement("return $T.failedFuture(t)", CompletableFutureUtils.class)
               .endControlFlow();

        return builder;
    }

    @Override
    protected MethodSpec.Builder paginatedMethodBody(MethodSpec.Builder builder, OperationModel opModel) {
        return builder.addModifiers(PUBLIC)
                      .addStatement("return new $T(this, applyPaginatorUserAgent($L))",
                                    poetExtensions.getResponseClassForPaginatedAsyncOperation(opModel.getOperationName()),
                                    opModel.getInput().getVariableName());
    }

    @Override
    public ClassName className() {
        return className;
    }

    @Override
    public Iterable<StaticImport> staticImports() {
        return singletonList(StaticImport.staticMethodImport(FunctionalUtils.class, "runAndLogError"));
    }

    private CodeBlock eventToByteBufferPublisher(OperationModel opModel) {
        if (!opModel.hasEventStreamInput()) {
            return CodeBlock.builder().build();
        }

        ShapeModel eventStreamShape = EventStreamUtils.getEventStreamInRequest(opModel.getInputShape());
        CodeBlock code = CodeBlock.builder()
                                  .add(createEventStreamTaggedUnionJsonMarshaller(eventStreamShape))
                                  .addStatement("$1T eventPublisher = $2T.adapt($3L)",
                                                ParameterizedTypeName.get(
                                                    ClassName.get(SdkPublisher.class),
                                                    eventStreamType(eventStreamShape)),
                                                SdkPublisher.class,
                                                EVENT_PUBLISHER_PARAM_NAME)
                                  .add("$T adapted = eventPublisher.map(event -> eventMarshaller.marshall(event))",
                                       ParameterizedTypeName.get(Publisher.class, ByteBuffer.class))
                                  .add(".map($T::encodeEventStreamRequestToByteBuffer);", AwsClientHandlerUtils.class)
                                  .build();

        return code;
    }

    private CodeBlock createEventStreamTaggedUnionJsonMarshaller(ShapeModel eventStreamShape) {
        EventStreamSpecHelper specHelper = new EventStreamSpecHelper(eventStreamShape, model);

        CodeBlock.Builder builder = CodeBlock.builder().add("$1T eventMarshaller = $1T.builder()",
                                                            EventStreamTaggedUnionJsonMarshaller.class);

        List<MemberModel> eventMembers = EventStreamUtils.getEventMembers(eventStreamShape)
                                                  .collect(Collectors.toList());

        eventMembers.forEach(event -> builder.add(".putMarshaller($T.class, new $T(protocolFactory))",
                                                specHelper.eventClassName(event),
                                                poetExtensions.getTransformClass(event.getShape() + "Marshaller")));

        builder.add(".build();");
        return builder.build();
    }

    private TypeName eventStreamType(ShapeModel shapeModel) {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
    }

    @Override
    protected MethodSpec utilitiesMethod() {
        UtilitiesMethod config = model.getCustomizationConfig().getUtilitiesMethod();
        ClassName returnType = PoetUtils.classNameFromFqcn(config.getReturnType());
        String instanceClass = config.getInstanceType();
        if (instanceClass == null) {
            instanceClass = config.getReturnType();
        }

        ClassName instanceType = PoetUtils.classNameFromFqcn(instanceClass);

        return MethodSpec.methodBuilder(UtilitiesMethod.METHOD_NAME)
                         .returns(returnType)
                         .addModifiers(PUBLIC)
                         .addAnnotation(Override.class)
                         .addStatement("return $T.create($L)",
                                       instanceType,
                                       String.join(",", config.getCreateMethodParams()))
                         .build();
    }

    private MethodSpec resolveMetricPublishersMethod() {
        String clientConfigName = "clientConfiguration";
        String requestOverrideConfigName = "requestOverrideConfiguration";

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("resolveMetricPublishers")
                .addModifiers(PRIVATE, STATIC)
                .returns(ParameterizedTypeName.get(List.class, MetricPublisher.class))
                .addParameter(SdkClientConfiguration.class, clientConfigName)
                .addParameter(RequestOverrideConfiguration.class, requestOverrideConfigName);

        String publishersName = "publishers";

        methodBuilder.addStatement("$T $N = null", ParameterizedTypeName.get(List.class, MetricPublisher.class), publishersName);

        methodBuilder.beginControlFlow("if ($N != null)", requestOverrideConfigName)
                .addStatement("$N = $N.metricPublishers()", publishersName, requestOverrideConfigName)
                .endControlFlow();

        methodBuilder.beginControlFlow("if ($1N == null || $1N.isEmpty())", publishersName)
                .addStatement("$N = $N.option($T.$N)",
                              publishersName,
                              clientConfigName,
                              SdkClientOption.class,
                              "METRIC_PUBLISHERS")
                .endControlFlow();

        methodBuilder.beginControlFlow("if ($1N == null)", publishersName)
                .addStatement("$N = $T.emptyList()", publishersName, Collections.class)
                .endControlFlow();

        methodBuilder.addStatement("return $N", publishersName);

        return methodBuilder.build();
    }

    private boolean shouldUseAsyncWithBodySigner(OperationModel opModel) {
        if (opModel.getInputShape().getRequestSignerClassFqcn() != null) {
            return false;
        }

        AuthType authTypeForOperation = opModel.getAuthType();

        if (authTypeForOperation == null) {
            authTypeForOperation = model.getMetadata().getAuthType();
        }

        return authTypeForOperation == AuthType.V4 && opModel.hasStreamingInput();
    }

    private CodeBlock applyAsyncWithBodyV4SignerOverride(OperationModel opModel) {
        return CodeBlock.builder()
                .beginControlFlow("if (!isSignerOverridden($N))", "clientConfiguration")
                .addStatement("$1L = applySignerOverride($1L, $2T.create())",
                        opModel.getInput().getVariableName(), AsyncAws4Signer.class)
                .endControlFlow()
                .build();
    }

    private MethodSpec isSignerOverriddenOnClientMethod() {
        String clientConfigurationName = "clientConfiguration";

        return MethodSpec.methodBuilder("isSignerOverridden")
                .returns(boolean.class)
                .addModifiers(PRIVATE, STATIC)
                .addParameter(SdkClientConfiguration.class, clientConfigurationName)
                .addStatement("return $T.TRUE.equals($N.option($T.$N))", Boolean.class, clientConfigurationName,
                        SdkClientOption.class, "SIGNER_OVERRIDDEN")
                .build();
    }

    private boolean hasStreamingV4AuthOperations() {
        return model.getOperations().values().stream()
                .anyMatch(this::shouldUseAsyncWithBodySigner);
    }
}
