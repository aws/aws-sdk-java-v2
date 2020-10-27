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
import static javax.lang.model.element.Modifier.STATIC;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.addS3ArnableFieldCode;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.applyPaginatorUserAgentMethod;
import static software.amazon.awssdk.codegen.poet.client.ClientClassUtils.applySignerOverrideMethod;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.codegen.docs.SimpleMethodOverload;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.UtilitiesMethod;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
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

//TODO Make SyncClientClass extend SyncClientInterface (similar to what we do in AsyncClientClass)
public class SyncClientClass implements ClassSpec {

    private final IntermediateModel model;
    private final PoetExtensions poetExtensions;
    private final ClassName className;
    private final ProtocolSpec protocolSpec;

    public SyncClientClass(GeneratorTaskParams taskParams) {
        this.model = taskParams.getModel();
        this.poetExtensions = taskParams.getPoetExtensions();
        this.className = poetExtensions.getClientClass(model.getMetadata().getSyncClient());
        this.protocolSpec = getProtocolSpecs(poetExtensions, model);
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());

        Builder classBuilder = PoetUtils.createClassBuilder(className)
                                        .addAnnotation(SdkInternalApi.class)
                                        .addModifiers(FINAL)
                                        .addSuperinterface(interfaceClass)
                                        .addJavadoc("Internal implementation of {@link $1T}.\n\n@see $1T#builder()",
                                                    interfaceClass)
                                        .addField(logger())
                                        .addField(SyncClientHandler.class, "clientHandler", PRIVATE, FINAL)
                                        .addField(protocolSpec.protocolFactory(model))
                                        .addField(SdkClientConfiguration.class, "clientConfiguration", PRIVATE, FINAL)
                                        .addMethod(constructor())
                                        .addMethod(nameMethod())
                                        .addMethods(protocolSpec.additionalMethods())
                                        .addMethods(operations())
                                        .addMethod(resolveMetricPublishersMethod());

        protocolSpec.createErrorResponseHandler().ifPresent(classBuilder::addMethod);

        classBuilder.addMethod(protocolSpec.initProtocolFactory(model));

        classBuilder.addMethod(closeMethod());

        if (model.hasPaginators()) {
            classBuilder.addMethod(applyPaginatorUserAgentMethod(poetExtensions, model));
        }

        if (model.containsRequestSigners()) {
            classBuilder.addMethod(applySignerOverrideMethod(poetExtensions, model));
        }

        if (model.getCustomizationConfig().getUtilitiesMethod() != null) {
            classBuilder.addMethod(utilitiesMethod());
        }

        model.getEndpointOperation().ifPresent(
            o -> classBuilder.addField(EndpointDiscoveryRefreshCache.class, "endpointDiscoveryCache", PRIVATE));

        if (model.hasWaiters()) {
            classBuilder.addMethod(waiterMethod());
        }

        return classBuilder.build();
    }

    private FieldSpec logger() {
        return FieldSpec.builder(Logger.class, "log", PRIVATE, STATIC, FINAL)
                        .initializer("$T.loggerFor($T.class)", Logger.class, className)
                        .build();
    }

    private MethodSpec nameMethod() {
        return MethodSpec.methodBuilder("serviceName")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
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
                                               .addModifiers(Modifier.PROTECTED)
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

    private List<MethodSpec> operations() {
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
            method.addStatement("\n\nString key = clientConfiguration.option($T.CREDENTIALS_PROVIDER)." +
                                "resolveCredentials().accessKeyId()", AwsClientOption.class);
            method.addStatement("EndpointDiscoveryRequest endpointDiscoveryRequest = $T.builder().required($L)" +
                                ".defaultEndpoint(clientConfiguration.option($T.ENDPOINT)).build()",
                                EndpointDiscoveryRequest.class,
                                opModel.getInputShape().getEndpointDiscovery().isRequired(),
                                SdkClientOption.class);
            method.addStatement("cachedEndpoint = $L.get(key, endpointDiscoveryRequest)",
                                "endpointDiscoveryCache");
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

    private List<MethodSpec> paginatedMethods(OperationModel opModel) {
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

    private MethodSpec closeMethod() {
        return MethodSpec.methodBuilder("close")
                         .addAnnotation(Override.class)
                         .addStatement("clientHandler.close()")
                         .addModifiers(Modifier.PUBLIC)
                         .build();
    }

    private MethodSpec utilitiesMethod() {
        UtilitiesMethod config = model.getCustomizationConfig().getUtilitiesMethod();
        ClassName returnType = PoetUtils.classNameFromFqcn(config.getReturnType());

        return MethodSpec.methodBuilder(UtilitiesMethod.METHOD_NAME)
                         .returns(returnType)
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .addStatement("return $T.create($L)",
                                       returnType,
                                       String.join(",", config.getCreateMethodParams()))
                         .build();
    }

    static ProtocolSpec getProtocolSpecs(PoetExtensions poetExtensions, IntermediateModel model) {
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
            case ION:
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

    private MethodSpec waiterMethod() {
        return MethodSpec.methodBuilder("waiter")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .addStatement("return $T.builder().client(this).build()",
                                       poetExtensions.getSyncWaiterInterface())
                         .returns(poetExtensions.getSyncWaiterInterface())
                         .build();
    }
}
