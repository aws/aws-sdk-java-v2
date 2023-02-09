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
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.addS3ArnableFieldCode;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.applyPaginatorUserAgentMethod;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.applySignerOverrideMethod;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.client.specs.Ec2ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.JsonProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.ProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.QueryProtocolSpec;
import software.amazon.awssdk.codegen.poet.client.specs.XmlProtocolSpec;
import software.amazon.awssdk.codegen.utils.PaginatorUtils;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRefreshCache;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRequest;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.Logger;

public class SyncClientClass extends SyncClientInterface {

    private final IntermediateModel model;
    private final PoetExtension poetExtensions;
    private final ClassName className;
    private final ProtocolSpec protocolSpec;

    public SyncClientClass(GeneratorTaskParams taskParams) {
        super(taskParams.getModel());
        this.model = taskParams.getModel();
        this.poetExtensions = taskParams.getPoetExtensions();
        this.className = poetExtensions.getClientClass(model.getMetadata().getSyncClient());
        this.protocolSpec = getProtocolSpecs(poetExtensions, model);
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
            .addField(SyncClientHandler.class, "clientHandler", PRIVATE, FINAL)
            .addField(protocolSpec.protocolFactory(model))
            .addField(SdkClientConfiguration.class, "clientConfiguration", PRIVATE, FINAL);
    }

    @Override
    protected void addAdditionalMethods(TypeSpec.Builder type) {

        if (model.hasPaginators()) {
            type.addMethod(applyPaginatorUserAgentMethod(poetExtensions, model));
        }

        if (model.containsRequestSigners()) {
            type.addMethod(applySignerOverrideMethod(poetExtensions, model));
        }

        model.getEndpointOperation().ifPresent(
            o -> type.addField(EndpointDiscoveryRefreshCache.class, "endpointDiscoveryCache", PRIVATE));

        type.addMethod(constructor())
            .addMethod(nameMethod())
            .addMethods(protocolSpec.additionalMethods())
            .addMethod(resolveMetricPublishersMethod());

        protocolSpec.createErrorResponseHandler().ifPresent(type::addMethod);

        type.addMethod(protocolSpec.initProtocolFactory(model));
    }

    private FieldSpec logger() {
        return FieldSpec.builder(Logger.class, "log", PRIVATE, STATIC, FINAL)
                        .initializer("$T.loggerFor($T.class)", Logger.class, className)
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
    public ClassName className() {
        return className;
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                                               .addModifiers(PROTECTED)
                                               .addParameter(SdkClientConfiguration.class, "clientConfiguration")
                                               .addStatement("this.clientHandler = new $T(clientConfiguration)",
                                                             protocolSpec.getClientHandlerClass())
                                               .addStatement("this.clientConfiguration = clientConfiguration");
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
                builder.beginControlFlow("if (clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) == "
                                         + "Boolean.TRUE)");
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
                    .map(this::operationMethodSpecs)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
    }

    private List<MethodSpec> operationMethodSpecs(OperationModel opModel) {
        List<MethodSpec> methods = new ArrayList<>();

        MethodSpec.Builder method = SyncClientInterface.operationMethodSignature(model, opModel)
                                                       .addAnnotation(Override.class)
                                                       .addCode(ClientClassUtils.callApplySignerOverrideMethod(opModel))
                                                       .addCode(protocolSpec.responseHandler(model, opModel));

        protocolSpec.errorResponseHandler(opModel).ifPresent(method::addCode);

        if (opModel.getEndpointDiscovery() != null) {
            method.addStatement("boolean endpointDiscoveryEnabled = "
                                + "clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED)");
            method.addStatement("boolean endpointOverridden = "
                                + "clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) == Boolean.TRUE");

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

            method.addCode("$T key = $N.overrideConfiguration()", String.class, opModel.getInput().getVariableName())
                  .addCode("    .flatMap($T::credentialsProvider)", AwsRequestOverrideConfiguration.class)
                  .addCode("    .orElseGet(() -> clientConfiguration.option($T.CREDENTIALS_PROVIDER))", AwsClientOption.class)
                  .addCode("    .resolveCredentials().accessKeyId();");

            method.addCode("$1T endpointDiscoveryRequest = $1T.builder()", EndpointDiscoveryRequest.class)
                  .addCode("    .required($L)", opModel.getInputShape().getEndpointDiscovery().isRequired())
                  .addCode("    .defaultEndpoint(clientConfiguration.option($T.ENDPOINT))", SdkClientOption.class)
                  .addCode("    .overrideConfiguration($N.overrideConfiguration().orElse(null))",
                           opModel.getInput().getVariableName())
                  .addCode("    .build();");

            method.addStatement("cachedEndpoint = endpointDiscoveryCache.get(key, endpointDiscoveryRequest)");
            method.endControlFlow();
        }

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

        methods.add(method.build());

        methods.addAll(paginatedMethods(opModel));

        return methods;
    }

    @Override
    protected List<MethodSpec> paginatedMethods(OperationModel opModel) {
        List<MethodSpec> paginatedMethodSpecs = new ArrayList<>();

        if (opModel.isPaginated()) {
            paginatedMethodSpecs.add(SyncClientInterface.operationMethodSignature(model,
                                                                                  opModel,
                                                                                  SimpleMethodOverload.PAGINATED,
                                                                                  PaginatorUtils.getPaginatedMethodName(
                                                                                      opModel.getMethodName()))
                                                        .addAnnotation(Override.class)
                                                        .returns(poetExtensions.getResponseClassForPaginatedSyncOperation(
                                                            opModel.getOperationName()))
                                                        .addStatement("return new $T(this, applyPaginatorUserAgent($L))",
                                                                      poetExtensions.getResponseClassForPaginatedSyncOperation(
                                                                          opModel.getOperationName()),
                                                                      opModel.getInput().getVariableName())
                                                        .build());
        }

        return paginatedMethodSpecs;
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
}
