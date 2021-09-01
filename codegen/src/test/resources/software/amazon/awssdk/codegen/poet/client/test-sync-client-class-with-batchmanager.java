package software.amazon.awssdk.services.batchmanagertest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
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
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestBatchManager;
import software.amazon.awssdk.services.batchmanagertest.model.BatchManagerTestException;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.transform.DeleteRequestBatchRequestMarshaller;
import software.amazon.awssdk.services.batchmanagertest.transform.DeleteRequestRequestMarshaller;
import software.amazon.awssdk.services.batchmanagertest.transform.SendRequestBatchRequestMarshaller;
import software.amazon.awssdk.services.batchmanagertest.transform.SendRequestRequestMarshaller;
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link BatchManagerTestClient}.
 *
 * @see BatchManagerTestClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultBatchManagerTestClient implements BatchManagerTestClient {
    private static final Logger log = Logger.loggerFor(DefaultBatchManagerTestClient.class);

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final ScheduledExecutorService executorService;

    protected DefaultBatchManagerTestClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
        this.executorService = clientConfiguration.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * Invokes the DeleteRequest operation.
     *
     * @param deleteRequestRequest
     * @return Result of the DeleteRequest operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.DeleteRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequest"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public DeleteRequestResponse deleteRequest(DeleteRequestRequest deleteRequestRequest) throws AwsServiceException,
                                                                                                 SdkClientException, BatchManagerTestException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<DeleteRequestResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                           DeleteRequestResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteRequestRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "BatchManagerTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteRequest");

            return clientHandler.execute(new ClientExecutionParams<DeleteRequestRequest, DeleteRequestResponse>()
                                             .withOperationName("DeleteRequest").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withInput(deleteRequestRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new DeleteRequestRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the DeleteRequestBatch operation.
     *
     * @param deleteRequestBatchRequest
     * @return Result of the DeleteRequestBatch operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.DeleteRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public DeleteRequestBatchResponse deleteRequestBatch(DeleteRequestBatchRequest deleteRequestBatchRequest)
        throws AwsServiceException, SdkClientException, BatchManagerTestException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<DeleteRequestBatchResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, DeleteRequestBatchResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteRequestBatchRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "BatchManagerTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteRequestBatch");

            return clientHandler.execute(new ClientExecutionParams<DeleteRequestBatchRequest, DeleteRequestBatchResponse>()
                                             .withOperationName("DeleteRequestBatch").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withInput(deleteRequestBatchRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new DeleteRequestBatchRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the SendRequest operation.
     *
     * @param sendRequestRequest
     * @return Result of the SendRequest operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.SendRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequest" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public SendRequestResponse sendRequest(SendRequestRequest sendRequestRequest) throws AwsServiceException, SdkClientException,
                                                                                         BatchManagerTestException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<SendRequestResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                         SendRequestResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, sendRequestRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "BatchManagerTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "SendRequest");

            return clientHandler.execute(new ClientExecutionParams<SendRequestRequest, SendRequestResponse>()
                                             .withOperationName("SendRequest").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withInput(sendRequestRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new SendRequestRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the SendRequestBatch operation.
     *
     * @param sendRequestBatchRequest
     * @return Result of the SendRequestBatch operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws BatchManagerTestException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample BatchManagerTestClient.SendRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public SendRequestBatchResponse sendRequestBatch(SendRequestBatchRequest sendRequestBatchRequest) throws AwsServiceException,
                                                                                                             SdkClientException, BatchManagerTestException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<SendRequestBatchResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                              SendRequestBatchResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, sendRequestBatchRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "BatchManagerTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "SendRequestBatch");

            return clientHandler.execute(new ClientExecutionParams<SendRequestBatchRequest, SendRequestBatchResponse>()
                                             .withOperationName("SendRequestBatch").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withInput(sendRequestBatchRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new SendRequestBatchRequestMarshaller(protocolFactory)));
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
                      .defaultServiceExceptionSupplier(BatchManagerTestException::builder).protocol(AwsJsonProtocol.REST_JSON)
                      .protocolVersion("1.1");
    }

    @Override
    public void close() {
        clientHandler.close();
    }

    @Override
    public BatchManagerTestBatchManager batchManager() {
        return BatchManagerTestBatchManager.builder().client(this).scheduledExecutor(executorService).build();
    }
}
