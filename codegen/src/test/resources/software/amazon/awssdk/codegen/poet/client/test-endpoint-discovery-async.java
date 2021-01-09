package software.amazon.awssdk.services.endpointdiscoverytest;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRefreshCache;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRequest;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.endpointdiscoverytest.model.DescribeEndpointsRequest;
import software.amazon.awssdk.services.endpointdiscoverytest.model.DescribeEndpointsResponse;
import software.amazon.awssdk.services.endpointdiscoverytest.model.EndpointDiscoveryTestException;
import software.amazon.awssdk.services.endpointdiscoverytest.model.TestDiscoveryIdentifiersRequiredRequest;
import software.amazon.awssdk.services.endpointdiscoverytest.model.TestDiscoveryIdentifiersRequiredResponse;
import software.amazon.awssdk.services.endpointdiscoverytest.model.TestDiscoveryOptionalRequest;
import software.amazon.awssdk.services.endpointdiscoverytest.model.TestDiscoveryOptionalResponse;
import software.amazon.awssdk.services.endpointdiscoverytest.model.TestDiscoveryRequiredRequest;
import software.amazon.awssdk.services.endpointdiscoverytest.model.TestDiscoveryRequiredResponse;
import software.amazon.awssdk.services.endpointdiscoverytest.transform.DescribeEndpointsRequestMarshaller;
import software.amazon.awssdk.services.endpointdiscoverytest.transform.TestDiscoveryIdentifiersRequiredRequestMarshaller;
import software.amazon.awssdk.services.endpointdiscoverytest.transform.TestDiscoveryOptionalRequestMarshaller;
import software.amazon.awssdk.services.endpointdiscoverytest.transform.TestDiscoveryRequiredRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Internal implementation of {@link EndpointDiscoveryTestAsyncClient}.
 *
 * @see EndpointDiscoveryTestAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultEndpointDiscoveryTestAsyncClient implements EndpointDiscoveryTestAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultEndpointDiscoveryTestAsyncClient.class);

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private EndpointDiscoveryRefreshCache endpointDiscoveryCache;

    protected DefaultEndpointDiscoveryTestAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
        if (clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED)) {
            this.endpointDiscoveryCache = EndpointDiscoveryRefreshCache
                    .create(EndpointDiscoveryTestAsyncEndpointDiscoveryCacheLoader.create(this));
        }
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * Invokes the DescribeEndpoints operation asynchronously.
     *
     * @param describeEndpointsRequest
     * @return A Java Future containing the result of the DescribeEndpoints operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>EndpointDiscoveryTestException Base class for all service exceptions. Unknown exceptions will be
     *         thrown as an instance of this type.</li>
     *         </ul>
     * @sample EndpointDiscoveryTestAsyncClient.DescribeEndpoints
     */
    @Override
    public CompletableFuture<DescribeEndpointsResponse> describeEndpoints(DescribeEndpointsRequest describeEndpointsRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, describeEndpointsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AwsEndpointDiscoveryTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DescribeEndpoints");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<DescribeEndpointsResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, DescribeEndpointsResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);

            CompletableFuture<DescribeEndpointsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<DescribeEndpointsRequest, DescribeEndpointsResponse>()
                            .withOperationName("DescribeEndpoints")
                            .withMarshaller(new DescribeEndpointsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).withInput(describeEndpointsRequest));
            AwsRequestOverrideConfiguration requestOverrideConfig = describeEndpointsRequest.overrideConfiguration().orElse(null);
            CompletableFuture<DescribeEndpointsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the TestDiscoveryIdentifiersRequired operation asynchronously.
     *
     * @param testDiscoveryIdentifiersRequiredRequest
     * @return A Java Future containing the result of the TestDiscoveryIdentifiersRequired operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>EndpointDiscoveryTestException Base class for all service exceptions. Unknown exceptions will be
     *         thrown as an instance of this type.</li>
     *         </ul>
     * @sample EndpointDiscoveryTestAsyncClient.TestDiscoveryIdentifiersRequired
     */
    @Override
    public CompletableFuture<TestDiscoveryIdentifiersRequiredResponse> testDiscoveryIdentifiersRequired(
            TestDiscoveryIdentifiersRequiredRequest testDiscoveryIdentifiersRequiredRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                testDiscoveryIdentifiersRequiredRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AwsEndpointDiscoveryTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "TestDiscoveryIdentifiersRequired");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<TestDiscoveryIdentifiersRequiredResponse> responseHandler = protocolFactory
                    .createResponseHandler(operationMetadata, TestDiscoveryIdentifiersRequiredResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);
            boolean endpointDiscoveryEnabled = clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED);
            boolean endpointOverridden = clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) == Boolean.TRUE;
            if (endpointOverridden) {
                throw new IllegalStateException(
                        "This operation requires endpoint discovery, but an endpoint override was specified when the client was created. This is not supported.");
            }
            if (!endpointDiscoveryEnabled) {
                throw new IllegalStateException(
                        "This operation requires endpoint discovery, but endpoint discovery was disabled on the client.");
            }
            URI cachedEndpoint = null;
            if (endpointDiscoveryEnabled) {

                String key = clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER).resolveCredentials().accessKeyId();
                EndpointDiscoveryRequest endpointDiscoveryRequest = EndpointDiscoveryRequest.builder().required(true)
                        .defaultEndpoint(clientConfiguration.option(SdkClientOption.ENDPOINT)).build();
                cachedEndpoint = endpointDiscoveryCache.get(key, endpointDiscoveryRequest);
            }

            CompletableFuture<TestDiscoveryIdentifiersRequiredResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<TestDiscoveryIdentifiersRequiredRequest, TestDiscoveryIdentifiersRequiredResponse>()
                            .withOperationName("TestDiscoveryIdentifiersRequired")
                            .withMarshaller(new TestDiscoveryIdentifiersRequiredRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).discoveredEndpoint(cachedEndpoint)
                            .withInput(testDiscoveryIdentifiersRequiredRequest));
            AwsRequestOverrideConfiguration requestOverrideConfig = testDiscoveryIdentifiersRequiredRequest
                    .overrideConfiguration().orElse(null);
            CompletableFuture<TestDiscoveryIdentifiersRequiredResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the TestDiscoveryOptional operation asynchronously.
     *
     * @param testDiscoveryOptionalRequest
     * @return A Java Future containing the result of the TestDiscoveryOptional operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>EndpointDiscoveryTestException Base class for all service exceptions. Unknown exceptions will be
     *         thrown as an instance of this type.</li>
     *         </ul>
     * @sample EndpointDiscoveryTestAsyncClient.TestDiscoveryOptional
     */
    @Override
    public CompletableFuture<TestDiscoveryOptionalResponse> testDiscoveryOptional(
            TestDiscoveryOptionalRequest testDiscoveryOptionalRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, testDiscoveryOptionalRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AwsEndpointDiscoveryTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "TestDiscoveryOptional");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<TestDiscoveryOptionalResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, TestDiscoveryOptionalResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);
            boolean endpointDiscoveryEnabled = clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED);
            boolean endpointOverridden = clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) == Boolean.TRUE;
            URI cachedEndpoint = null;
            if (endpointDiscoveryEnabled) {

                String key = clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER).resolveCredentials().accessKeyId();
                EndpointDiscoveryRequest endpointDiscoveryRequest = EndpointDiscoveryRequest.builder().required(false)
                        .defaultEndpoint(clientConfiguration.option(SdkClientOption.ENDPOINT)).build();
                cachedEndpoint = endpointDiscoveryCache.get(key, endpointDiscoveryRequest);
            }

            CompletableFuture<TestDiscoveryOptionalResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<TestDiscoveryOptionalRequest, TestDiscoveryOptionalResponse>()
                            .withOperationName("TestDiscoveryOptional")
                            .withMarshaller(new TestDiscoveryOptionalRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).discoveredEndpoint(cachedEndpoint)
                            .withInput(testDiscoveryOptionalRequest));
            AwsRequestOverrideConfiguration requestOverrideConfig = testDiscoveryOptionalRequest.overrideConfiguration().orElse(
                    null);
            CompletableFuture<TestDiscoveryOptionalResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the TestDiscoveryRequired operation asynchronously.
     *
     * @param testDiscoveryRequiredRequest
     * @return A Java Future containing the result of the TestDiscoveryRequired operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>EndpointDiscoveryTestException Base class for all service exceptions. Unknown exceptions will be
     *         thrown as an instance of this type.</li>
     *         </ul>
     * @sample EndpointDiscoveryTestAsyncClient.TestDiscoveryRequired
     */
    @Override
    public CompletableFuture<TestDiscoveryRequiredResponse> testDiscoveryRequired(
            TestDiscoveryRequiredRequest testDiscoveryRequiredRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, testDiscoveryRequiredRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AwsEndpointDiscoveryTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "TestDiscoveryRequired");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<TestDiscoveryRequiredResponse> responseHandler = protocolFactory.createResponseHandler(
                    operationMetadata, TestDiscoveryRequiredResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata);
            boolean endpointDiscoveryEnabled = clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED);
            boolean endpointOverridden = clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN) == Boolean.TRUE;
            if (endpointOverridden) {
                throw new IllegalStateException(
                        "This operation requires endpoint discovery, but an endpoint override was specified when the client was created. This is not supported.");
            }
            if (!endpointDiscoveryEnabled) {
                throw new IllegalStateException(
                        "This operation requires endpoint discovery, but endpoint discovery was disabled on the client.");
            }
            URI cachedEndpoint = null;
            if (endpointDiscoveryEnabled) {

                String key = clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER).resolveCredentials().accessKeyId();
                EndpointDiscoveryRequest endpointDiscoveryRequest = EndpointDiscoveryRequest.builder().required(true)
                        .defaultEndpoint(clientConfiguration.option(SdkClientOption.ENDPOINT)).build();
                cachedEndpoint = endpointDiscoveryCache.get(key, endpointDiscoveryRequest);
            }

            CompletableFuture<TestDiscoveryRequiredResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<TestDiscoveryRequiredRequest, TestDiscoveryRequiredResponse>()
                            .withOperationName("TestDiscoveryRequired")
                            .withMarshaller(new TestDiscoveryRequiredRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withMetricCollector(apiCallMetricCollector).discoveredEndpoint(cachedEndpoint)
                            .withInput(testDiscoveryRequiredRequest));
            AwsRequestOverrideConfiguration requestOverrideConfig = testDiscoveryRequiredRequest.overrideConfiguration().orElse(
                    null);
            CompletableFuture<TestDiscoveryRequiredResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
    public void close() {
        clientHandler.close();
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration)
                .defaultServiceExceptionSupplier(EndpointDiscoveryTestException::builder).protocol(AwsJsonProtocol.AWS_JSON)
                .protocolVersion("1.1");
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

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
            JsonOperationMetadata operationMetadata) {
        return protocolFactory.createErrorResponseHandler(operationMetadata);
    }
}

