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

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.poet.PoetUtils.classNameFromFqcn;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.addS3ArnableFieldCode;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.applySignerOverrideMethod;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.model.service.PreClientExecutionRequestCustomizer;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeSpecUtils;
import software.amazon.awssdk.codegen.poet.client.specs.Ec2ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.JsonProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.QueryProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.XmlProtocolSpec;
import software.amazon.awssdk.codegen.poet.model.ServiceClientConfigurationUtils;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRefreshCache;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRequest;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public class SyncClientClass extends SyncClientInterface {

    private final IntermediateModel model;
    private final PoetExtension poetExtensions;
    private final ClassName className;
    private final ProtocolSpec protocolSpec;
    private final ClassName serviceClientConfigurationClassName;
    private final ServiceClientConfigurationUtils configurationUtils;
    private final boolean useSraAuth;

    public SyncClientClass(GeneratorTaskParams taskParams) {
        super(taskParams.getModel());
        this.model = taskParams.getModel();
        this.poetExtensions = taskParams.getPoetExtensions();
        this.className = poetExtensions.getClientClass(model.getMetadata().getSyncClient());
        this.protocolSpec = getProtocolSpecs(poetExtensions, model);
        this.serviceClientConfigurationClassName = new PoetExtension(model).getServiceConfigClass();
        this.configurationUtils = new ServiceClientConfigurationUtils(model);
        this.useSraAuth = new AuthSchemeSpecUtils(model).useSraAuth();
    }

    @Override
    protected void addInterfaceClass(TypeSpec.Builder type) {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        type.addSuperinterface(interfaceClass)
            .addJavadoc("Internal implementation of {@link $1T}.\n\n@see $1T#builder()", interfaceClass);
    }

    @Override
    protected TypeSpec.Builder createTypeSpec() {
        return PoetUtils.createClassBuilder(className);
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
        type.addField(logger())
            .addField(protocolMetadata())
            .addField(SyncClientHandler.class, "clientHandler", PRIVATE, FINAL)
            .addField(protocolSpec.protocolFactory(model))
            .addField(SdkClientConfiguration.class, "clientConfiguration", PRIVATE, FINAL);
    }

    @Override
    protected void addAdditionalMethods(TypeSpec.Builder type) {
        if (!useSraAuth && model.containsRequestSigners()) {
            type.addMethod(applySignerOverrideMethod(poetExtensions, model));
        }

        model.getEndpointOperation().ifPresent(
            o -> type.addField(EndpointDiscoveryRefreshCache.class, "endpointDiscoveryCache", PRIVATE));

        type.addMethod(constructor())
            .addMethod(nameMethod())
            .addMethods(protocolSpec.additionalMethods())
            .addMethod(resolveMetricPublishersMethod());

        protocolSpec.createErrorResponseHandler().ifPresent(type::addMethod);
        type.addMethod(ClientClassUtils.updateRetryStrategyClientConfigurationMethod());
        type.addMethod(updateSdkClientConfigurationMethod(configurationUtils.serviceClientConfigurationBuilderClassName()));
        type.addMethod(protocolSpec.initProtocolFactory(model));
    }

    private FieldSpec logger() {
        return FieldSpec.builder(Logger.class, "log", PRIVATE, STATIC, FINAL)
                        .initializer("$T.loggerFor($T.class)", Logger.class, className)
                        .build();
    }

    private FieldSpec protocolMetadata() {
        return FieldSpec.builder(AwsProtocolMetadata.class, "protocolMetadata", PRIVATE, STATIC, FINAL)
                        .initializer("$T.builder().serviceProtocol($T.$L).build()",
                                     AwsProtocolMetadata.class, AwsServiceProtocol.class, model.getMetadata().getProtocol())
                        .build();
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
    protected MethodSpec serviceClientConfigMethod() {
        return MethodSpec.methodBuilder("serviceClientConfiguration")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC, FINAL)
                         .returns(serviceClientConfigurationClassName)
                         .addStatement("return new $T(this.clientConfiguration.toBuilder()).build()",
                                       this.configurationUtils.serviceClientConfigurationBuilderClassName())
                         .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder
            = MethodSpec.constructorBuilder()
                        .addModifiers(PROTECTED)
                        .addParameter(SdkClientConfiguration.class, "clientConfiguration")
                        .addStatement("this.clientHandler = new $T(clientConfiguration)", protocolSpec.getClientHandlerClass())
                        .addStatement("this.clientConfiguration = clientConfiguration.toBuilder()"
                                      + ".option($T.SDK_CLIENT, this)"
                                      + ".build()", SdkClientOption.class);

        FieldSpec protocolFactoryField = protocolSpec.protocolFactory(model);
        if (model.getMetadata().isJsonProtocol()) {
            builder.addStatement("this.$N = init($T.builder()).build()", protocolFactoryField.name,
                                 protocolFactoryField.type);
        } else {
            builder.addStatement("this.$N = init()", protocolFactoryField.name);
        }

        if (model.getEndpointOperation().isPresent()) {
            builder.beginControlFlow("if (clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED))");
            builder.addStatement("this.endpointDiscoveryCache = $T.create($T.create(this))",
                                 EndpointDiscoveryRefreshCache.class,
                                 poetExtensions.getClientClass(model.getNamingStrategy().getServiceName() +
                                                               "EndpointDiscoveryCacheLoader"));

            if (model.getCustomizationConfig().allowEndpointOverrideForEndpointDiscoveryRequiredOperations()) {
                builder.beginControlFlow("if (clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER)"
                                         + ".isEndpointOverridden())");
                builder.addStatement("log.warn(() -> $S)",
                                     "Endpoint discovery is enabled for this client, and an endpoint override was also "
                                     + "specified. This will disable endpoint discovery for methods that require it, instead "
                                     + "using the specified endpoint override. This may or may not be what you intended.");
                builder.endControlFlow();
            }

            builder.endControlFlow();
        }

        return builder.build();
    }

    @Override
    protected List<MethodSpec> operations() {
        return model.getOperations().values().stream()
                    .filter(o -> !o.hasEventStreamInput())
                    .filter(o -> !o.hasEventStreamOutput())
                    .flatMap(this::operations)
                    .collect(Collectors.toList());
    }

    private Stream<MethodSpec> operations(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(traditionalMethod(opModel));
        return methods.stream();
    }

    private MethodSpec traditionalMethod(OperationModel opModel) {
        MethodSpec.Builder method = SyncClientInterface.operationMethodSignature(model, opModel)
                                                       .addAnnotation(Override.class);

        addRequestModifierCode(opModel, model).ifPresent(method::addCode);
        if (!useSraAuth) {
            method.addCode(ClientClassUtils.callApplySignerOverrideMethod(opModel));
        }
        method.addCode(protocolSpec.responseHandler(model, opModel));

        protocolSpec.errorResponseHandler(opModel).ifPresent(method::addCode);

        if (opModel.getEndpointDiscovery() != null) {
            method.addStatement("boolean endpointDiscoveryEnabled = "
                                + "clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED)");
            method.addStatement("boolean endpointOverridden = "
                                + "clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER)"
                                + ".isEndpointOverridden()");

            if (opModel.getEndpointDiscovery().isRequired()) {
                if (!model.getCustomizationConfig().allowEndpointOverrideForEndpointDiscoveryRequiredOperations()) {
                    method.beginControlFlow("if (endpointOverridden)");
                    method.addStatement("throw new $T($S)", IllegalStateException.class,
                                        "This operation requires endpoint discovery, but an endpoint override was specified "
                                        + "when the client was created. This is not supported.");
                    method.endControlFlow();

                    method.beginControlFlow("if (!endpointDiscoveryEnabled)");
                    method.addStatement("throw new $T($S)", IllegalStateException.class,
                                        "This operation requires endpoint discovery, but endpoint discovery was disabled on the "
                                        + "client.");
                    method.endControlFlow();
                } else {
                    method.beginControlFlow("if (endpointOverridden)");
                    method.addStatement("endpointDiscoveryEnabled = false");
                    method.nextControlFlow("else if (!endpointDiscoveryEnabled)");
                    method.addStatement("throw new $T($S)", IllegalStateException.class,
                                        "This operation requires endpoint discovery to be enabled, or for you to specify an "
                                        + "endpoint override when the client is created.");
                    method.endControlFlow();
                }
            }

            method.addStatement("$T cachedEndpoint = null", URI.class);
            method.beginControlFlow("if (endpointDiscoveryEnabled)");

            ParameterizedTypeName identityFutureTypeName =
                ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                                          WildcardTypeName.subtypeOf(AwsCredentialsIdentity.class));
            method.addCode("$T identityFuture = $N.overrideConfiguration()",
                           identityFutureTypeName,
                           opModel.getInput().getVariableName())
                  .addCode("    .flatMap($T::credentialsIdentityProvider)", AwsRequestOverrideConfiguration.class)
                  .addCode("    .orElseGet(() -> clientConfiguration.option($T.CREDENTIALS_IDENTITY_PROVIDER))",
                           AwsClientOption.class)
                  .addCode("    .resolveIdentity();");

            method.addCode("$T key = $T.joinLikeSync(identityFuture).accessKeyId();", String.class, CompletableFutureUtils.class);

            method.addCode("$1T endpointDiscoveryRequest = $1T.builder()", EndpointDiscoveryRequest.class)
                  .addCode("    .required($L)", opModel.getInputShape().getEndpointDiscovery().isRequired())
                  .addCode("    .defaultEndpoint(clientConfiguration.option($T.CLIENT_ENDPOINT_PROVIDER).clientEndpoint())",
                           SdkClientOption.class)
                  .addCode("    .overrideConfiguration($N.overrideConfiguration().orElse(null))",
                           opModel.getInput().getVariableName())
                  .addCode("    .build();");

            method.addStatement("cachedEndpoint = endpointDiscoveryCache.get(key, endpointDiscoveryRequest)");
            method.endControlFlow();
        }

        method.addStatement("$T clientConfiguration = updateSdkClientConfiguration($L, this.clientConfiguration)",
                            SdkClientConfiguration.class, opModel.getInput().getVariableName());
        method.addStatement("$T<$T> metricPublishers = "
                            + "resolveMetricPublishers(clientConfiguration, $N.overrideConfiguration().orElse(null))",
                            List.class,
                            MetricPublisher.class,
                            opModel.getInput().getVariableName())
              .addStatement("$1T apiCallMetricCollector = metricPublishers.isEmpty() ? $2T.create() : $1T.create($3S)",
                            MetricCollector.class, NoOpMetricCollector.class, "ApiCall");

        method.beginControlFlow("try")
                .addStatement("apiCallMetricCollector.reportMetric($T.$L, $S)",
                              CoreMetric.class, "SERVICE_ID", model.getMetadata().getServiceId())
                .addStatement("apiCallMetricCollector.reportMetric($T.$L, $S)",
                              CoreMetric.class, "OPERATION_NAME", opModel.getOperationName());

        addS3ArnableFieldCode(opModel, model).ifPresent(method::addCode);
        method.addCode(ClientClassUtils.addEndpointTraitCode(opModel));

        method.addCode(protocolSpec.executionHandler(opModel))
              .endControlFlow()
              .beginControlFlow("finally")
              .addStatement("metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()))")
              .endControlFlow();

        return method.build();
    }

    public static Optional<CodeBlock> addRequestModifierCode(OperationModel opModel, IntermediateModel model) {

        Map<String, PreClientExecutionRequestCustomizer> preClientExecutionRequestCustomizer =
            model.getCustomizationConfig().getPreClientExecutionRequestCustomizer();

        if (!CollectionUtils.isNullOrEmpty(preClientExecutionRequestCustomizer)) {
            PreClientExecutionRequestCustomizer requestCustomizer =
                preClientExecutionRequestCustomizer.get(opModel.getOperationName());
            if (requestCustomizer != null) {
                CodeBlock.Builder builder = CodeBlock.builder();
                ClassName instanceType = classNameFromFqcn(requestCustomizer.getClassName());
                builder.addStatement("$L = $T.$N($L)",
                                     opModel.getInput().getVariableName(),
                                     instanceType,
                                     requestCustomizer.getMethodName(),
                                     opModel.getInput().getVariableName());
                return Optional.of(builder.build());
            }
        }
        return Optional.empty();
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
    protected MethodSpec.Builder utilitiesOperationBody(MethodSpec.Builder builder) {
        UtilitiesMethod config = model.getCustomizationConfig().getUtilitiesMethod();
        String instanceClass = config.getInstanceType();
        if (instanceClass == null) {
            instanceClass = config.getReturnType();
        }
        ClassName instanceType = PoetUtils.classNameFromFqcn(instanceClass);

        return builder.addAnnotation(Override.class)
                      .addStatement("return $T.create($L)", instanceType,
                                    String.join(",", config.getCreateMethodParams()));
    }

    static ProtocolSpec getProtocolSpecs(PoetExtension poetExtensions, IntermediateModel model) {
        Protocol protocol = model.getMetadata().getProtocol();
        switch (protocol) {
            case QUERY:
                return new QueryProtocolSpec(model, poetExtensions);
            case REST_XML:
                return new XmlProtocolSpec(model, poetExtensions);
            case EC2:
                return new Ec2ProtocolSpec(model, poetExtensions);
            case AWS_JSON:
            case REST_JSON:
            case CBOR:
            case SMITHY_RPC_V2_CBOR:
                return new JsonProtocolSpec(poetExtensions, model);
            default:
                throw new RuntimeException("Unknown protocol: " + protocol.name());
        }
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

    @Override
    protected MethodSpec.Builder waiterOperationBody(MethodSpec.Builder builder) {
        return builder.addAnnotation(Override.class)
                      .addStatement("return $T.builder().client(this).build()",
                                    poetExtensions.getSyncWaiterInterface());
    }

    protected MethodSpec updateSdkClientConfigurationMethod(
        TypeName serviceClientConfigurationBuilderClassName) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("updateSdkClientConfiguration")
                                               .addModifiers(PRIVATE)
                                               .addParameter(SdkRequest.class, "request")
                                               .addParameter(SdkClientConfiguration.class, "clientConfiguration")
                                               .returns(SdkClientConfiguration.class);

        builder.addStatement("$T plugins = request.overrideConfiguration()\n"
                             + ".map(c -> c.plugins()).orElse(Collections.emptyList())",
                             ParameterizedTypeName.get(List.class, SdkPlugin.class))
               .addStatement("$T configuration = clientConfiguration.toBuilder()", SdkClientConfiguration.Builder.class);

        builder.beginControlFlow("if (plugins.isEmpty())")
               .addStatement("return configuration.build()")
               .endControlFlow()
               .addStatement("$1T serviceConfigBuilder = new $1T(configuration)", serviceClientConfigurationBuilderClassName)
               .beginControlFlow("for ($T plugin : plugins)", SdkPlugin.class)
               .addStatement("plugin.configureClient(serviceConfigBuilder)")
               .endControlFlow();
        EndpointRulesSpecUtils endpointRulesSpecUtils = new EndpointRulesSpecUtils(this.model);

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
}
