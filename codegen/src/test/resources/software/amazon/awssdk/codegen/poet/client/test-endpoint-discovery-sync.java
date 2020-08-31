package software.amazon.awssdk.services.endpointdiscoverytest;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRefreshCache;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
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
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link EndpointDiscoveryTestClient}.
 *
 * @see EndpointDiscoveryTestClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultEndpointDiscoveryTestClient implements EndpointDiscoveryTestClient {
    private static final Logger log = Logger.loggerFor(DefaultEndpointDiscoveryTestClient.class);

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private EndpointDiscoveryRefreshCache endpointDiscoveryCache;

    protected DefaultEndpointDiscoveryTestClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
        if (clientConfiguration.option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED)) {
            this.endpointDiscoveryCache = EndpointDiscoveryRefreshCache.create(EndpointDiscoveryTestEndpointDiscoveryCacheLoader
                                                                                   .create(this));
        }
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * Invokes the DescribeEndpoints operation.
     *
     * @param describeEndpointsRequest
     * @return Result of the DescribeEndpoints operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws EndpointDiscoveryTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample EndpointDiscoveryTestClient.DescribeEndpoints
     */
    @Override
    public DescribeEndpointsResponse describeEndpoints(DescribeEndpointsRequest describeEndpointsRequest)
        throws AwsServiceException, SdkClientException, EndpointDiscoveryTestException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<DescribeEndpointsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                               DescribeEndpointsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, describeEndpointsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AwsEndpointDiscoveryTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DescribeEndpoints");

            return clientHandler.execute(new ClientExecutionParams<DescribeEndpointsRequest, DescribeEndpointsResponse>()
                                             .withOperationName("DescribeEndpoints").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withInput(describeEndpointsRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new DescribeEndpointsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the TestDiscoveryIdentifiersRequired operation.
     *
     * @param testDiscoveryIdentifiersRequiredRequest
     * @return Result of the TestDiscoveryIdentifiersRequired operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws EndpointDiscoveryTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample EndpointDiscoveryTestClient.TestDiscoveryIdentifiersRequired
     */
    @Override
    public TestDiscoveryIdentifiersRequiredResponse testDiscoveryIdentifiersRequired(
        TestDiscoveryIdentifiersRequiredRequest testDiscoveryIdentifiersRequiredRequest) throws AwsServiceException,
                                                                                                SdkClientException, EndpointDiscoveryTestException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<TestDiscoveryIdentifiersRequiredResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, TestDiscoveryIdentifiersRequiredResponse::builder);

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
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         testDiscoveryIdentifiersRequiredRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AwsEndpointDiscoveryTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "TestDiscoveryIdentifiersRequired");

            return clientHandler
                .execute(new ClientExecutionParams<TestDiscoveryIdentifiersRequiredRequest, TestDiscoveryIdentifiersRequiredResponse>()
                             .withOperationName("TestDiscoveryIdentifiersRequired").withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler).discoveredEndpoint(cachedEndpoint)
                             .withInput(testDiscoveryIdentifiersRequiredRequest).withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new TestDiscoveryIdentifiersRequiredRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the TestDiscoveryOptional operation.
     *
     * @param testDiscoveryOptionalRequest
     * @return Result of the TestDiscoveryOptional operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws EndpointDiscoveryTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample EndpointDiscoveryTestClient.TestDiscoveryOptional
     */
    @Override
    public TestDiscoveryOptionalResponse testDiscoveryOptional(TestDiscoveryOptionalRequest testDiscoveryOptionalRequest)
        throws AwsServiceException, SdkClientException, EndpointDiscoveryTestException {
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
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, testDiscoveryOptionalRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AwsEndpointDiscoveryTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "TestDiscoveryOptional");

            return clientHandler.execute(new ClientExecutionParams<TestDiscoveryOptionalRequest, TestDiscoveryOptionalResponse>()
                                             .withOperationName("TestDiscoveryOptional").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).discoveredEndpoint(cachedEndpoint)
                                             .withInput(testDiscoveryOptionalRequest).withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new TestDiscoveryOptionalRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the TestDiscoveryRequired operation.
     *
     * @param testDiscoveryRequiredRequest
     * @return Result of the TestDiscoveryRequired operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws EndpointDiscoveryTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample EndpointDiscoveryTestClient.TestDiscoveryRequired
     */
    @Override
    public TestDiscoveryRequiredResponse testDiscoveryRequired(TestDiscoveryRequiredRequest testDiscoveryRequiredRequest)
        throws AwsServiceException, SdkClientException, EndpointDiscoveryTestException {
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
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, testDiscoveryRequiredRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "AwsEndpointDiscoveryTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "TestDiscoveryRequired");

            return clientHandler.execute(new ClientExecutionParams<TestDiscoveryRequiredRequest, TestDiscoveryRequiredResponse>()
                                             .withOperationName("TestDiscoveryRequired").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).discoveredEndpoint(cachedEndpoint)
                                             .withInput(testDiscoveryRequiredRequest).withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new TestDiscoveryRequiredRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
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

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration)
                      .defaultServiceExceptionSupplier(EndpointDiscoveryTestException::builder).protocol(AwsJsonProtocol.AWS_JSON)
                      .protocolVersion("1.1");
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
