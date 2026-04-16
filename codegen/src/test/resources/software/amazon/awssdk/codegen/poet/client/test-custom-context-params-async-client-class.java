package software.amazon.awssdk.services.foobar;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.internal.endpoints.AwsEndpointProviderUtils;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.foobar.auth.scheme.FooBarAuthSchemeParams;
import software.amazon.awssdk.services.foobar.auth.scheme.FooBarAuthSchemeProvider;
import software.amazon.awssdk.services.foobar.endpoints.FooBarClientContextParams;
import software.amazon.awssdk.services.foobar.endpoints.FooBarEndpointParams;
import software.amazon.awssdk.services.foobar.endpoints.FooBarEndpointProvider;
import software.amazon.awssdk.services.foobar.endpoints.internal.FooBarEndpointResolverUtils;
import software.amazon.awssdk.services.foobar.internal.FooBarServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.foobar.internal.ServiceVersionInfo;
import software.amazon.awssdk.services.foobar.model.FooBarException;
import software.amazon.awssdk.services.foobar.model.GetDatabaseVersionRequest;
import software.amazon.awssdk.services.foobar.model.GetDatabaseVersionResponse;
import software.amazon.awssdk.services.foobar.transform.GetDatabaseVersionRequestMarshaller;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal implementation of {@link FooBarAsyncClient}.
 *
 * @see FooBarAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultFooBarAsyncClient implements FooBarAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultFooBarAsyncClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.REST_JSON).build();

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultFooBarAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this)
                                                      .option(SdkClientOption.API_METADATA, "Foo_Bar" + "#" + ServiceVersionInfo.VERSION).build();
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
    }

    /**
     * <p>
     * Performs a get database version operation
     * </p>
     *
     * @param getDatabaseVersionRequest
     * @return A Java Future containing the result of the GetDatabaseVersion operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>FooBarException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample FooBarAsyncClient.GetDatabaseVersion
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/GetDatabaseVersion"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<GetDatabaseVersionResponse> getDatabaseVersion(GetDatabaseVersionRequest getDatabaseVersionRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getDatabaseVersionRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getDatabaseVersionRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Foo Bar");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetDatabaseVersion");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<GetDatabaseVersionResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, GetDatabaseVersionResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<GetDatabaseVersionResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<GetDatabaseVersionRequest, GetDatabaseVersionResponse>()
                             .withOperationName("GetDatabaseVersion")
                             .withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new GetDatabaseVersionRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
                             .withMetricCollector(apiCallMetricCollector)
                             .withAuthSchemeOptionsResolver(
                                 r -> resolveAuthSchemeOptions(r, "GetDatabaseVersion", clientConfiguration))
                             .withEndpointResolver((r, a) -> resolveEndpoint(r, a, "GetDatabaseVersion"))
                             .withInput(getDatabaseVersionRequest));
            CompletableFuture<GetDatabaseVersionResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    @Override
    public final FooBarServiceClientConfiguration serviceClientConfiguration() {
        return new FooBarServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(FooBarException::builder)
                      .protocol(AwsJsonProtocol.REST_JSON).protocolVersion("1.1");
    }

    private static List<MetricPublisher> resolveMetricPublishers(SdkClientConfiguration clientConfiguration,
                                                                 RequestOverrideConfiguration requestOverrideConfiguration) {
        List<MetricPublisher> publishers = null;
        if (requestOverrideConfiguration != null) {
            publishers = requestOverrideConfiguration.metricPublishers();
        }
        if (publishers == null || publishers.isEmpty()) {
            publishers = clientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS);
        }
        if (publishers == null) {
            publishers = Collections.emptyList();
        }
        return publishers;
    }

    private List<AuthSchemeOption> resolveAuthSchemeOptions(SdkRequest request, String operationName,
                                                            SdkClientConfiguration clientConfiguration) {
        FooBarAuthSchemeProvider authSchemeProvider = Validate.isInstanceOf(FooBarAuthSchemeProvider.class,
                                                                            clientConfiguration.option(SdkClientOption.AUTH_SCHEME_PROVIDER),
                                                                            "Expected an instance of FooBarAuthSchemeProvider");
        FooBarAuthSchemeParams.Builder paramsBuilder = FooBarAuthSchemeParams.builder().operation(operationName);
        paramsBuilder.region(clientConfiguration.option(AwsClientOption.AWS_REGION));
        List<AuthSchemeOption> options = authSchemeProvider.resolveAuthScheme(paramsBuilder.build());
        return options;
    }

    private Endpoint resolveEndpoint(SdkRequest request, ExecutionAttributes executionAttributes, String operationName) {
        FooBarEndpointProvider provider = (FooBarEndpointProvider) executionAttributes
            .getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        try {
            FooBarEndpointParams endpointParams = FooBarEndpointResolverUtils.ruleParams(request, executionAttributes);
            Endpoint endpoint = provider.resolveEndpoint(endpointParams).join();
            if (!AwsEndpointProviderUtils.disableHostPrefixInjection(executionAttributes)) {
                Optional<String> hostPrefix = FooBarEndpointResolverUtils.hostPrefix(operationName, request);
                if (hostPrefix.isPresent()) {
                    endpoint = AwsEndpointProviderUtils.addHostPrefix(endpoint, hostPrefix.get());
                }
            }
            List<EndpointAuthScheme> endpointAuthSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
            SelectedAuthScheme<?> selectedAuthScheme = executionAttributes
                .getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            if (endpointAuthSchemes != null && selectedAuthScheme != null) {
                selectedAuthScheme = FooBarEndpointResolverUtils.authSchemeWithEndpointSignerProperties(endpointAuthSchemes,
                                                                                                        selectedAuthScheme);
                executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);
            }
            FooBarEndpointResolverUtils.setMetricValues(endpoint, executionAttributes);
            return endpoint;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SdkClientException) {
                throw (SdkClientException) cause;
            }
            throw SdkClientException.create("Endpoint resolution failed: " + cause.getMessage(), cause);
        }
    }

    private void updateRetryStrategyClientConfiguration(SdkClientConfiguration.Builder configuration) {
        ClientOverrideConfiguration.Builder builder = configuration.asOverrideConfigurationBuilder();
        RetryMode retryMode = builder.retryMode();
        if (retryMode != null) {
            configuration.option(SdkClientOption.RETRY_STRATEGY, AwsRetryStrategy.forRetryMode(retryMode));
        } else {
            Consumer<RetryStrategy.Builder<?, ?>> configurator = builder.retryStrategyConfigurator();
            if (configurator != null) {
                RetryStrategy.Builder<?, ?> defaultBuilder = AwsRetryStrategy.defaultRetryStrategy().toBuilder();
                configurator.accept(defaultBuilder);
                configuration.option(SdkClientOption.RETRY_STRATEGY, defaultBuilder.build());
            } else {
                RetryStrategy retryStrategy = builder.retryStrategy();
                if (retryStrategy != null) {
                    configuration.option(SdkClientOption.RETRY_STRATEGY, retryStrategy);
                }
            }
        }
        configuration.option(SdkClientOption.CONFIGURED_RETRY_MODE, null);
        configuration.option(SdkClientOption.CONFIGURED_RETRY_STRATEGY, null);
        configuration.option(SdkClientOption.CONFIGURED_RETRY_CONFIGURATOR, null);
    }

    private SdkClientConfiguration updateSdkClientConfiguration(SdkRequest request, SdkClientConfiguration clientConfiguration) {
        List<SdkPlugin> plugins = request.overrideConfiguration().map(c -> c.plugins()).orElse(Collections.emptyList());
        if (plugins.isEmpty()) {
            return clientConfiguration;
        }
        SdkClientConfiguration.Builder configuration = clientConfiguration.toBuilder();
        FooBarServiceClientConfigurationBuilder serviceConfigBuilder = new FooBarServiceClientConfigurationBuilder(configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        AttributeMap newContextParams = configuration.option(SdkClientOption.CLIENT_CONTEXT_PARAMS);
        AttributeMap originalContextParams = clientConfiguration.option(SdkClientOption.CLIENT_CONTEXT_PARAMS);
        newContextParams = (newContextParams != null) ? newContextParams : AttributeMap.empty();
        originalContextParams = originalContextParams != null ? originalContextParams : AttributeMap.empty();
        Validate.validState(
            Objects.equals(originalContextParams.get(FooBarClientContextParams.CROSS_REGION_ACCESS_ENABLED),
                           newContextParams.get(FooBarClientContextParams.CROSS_REGION_ACCESS_ENABLED)),
            "CROSS_REGION_ACCESS_ENABLED cannot be modified by request level plugins");
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
                                                                                JsonOperationMetadata operationMetadata, Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper) {
        return protocolFactory.createErrorResponseHandler(operationMetadata, exceptionMetadataMapper);
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
