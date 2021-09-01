package software.amazon.awssdk.services.batchmanagertest;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestAsyncBatchManager;
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
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Internal implementation of {@link BatchManagerTestAsyncClient}.
 *
 * @see BatchManagerTestAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultBatchManagerTestAsyncClient implements BatchManagerTestAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultBatchManagerTestAsyncClient.class);

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final ScheduledExecutorService executorService;

    protected DefaultBatchManagerTestAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
        this.executorService = clientConfiguration.option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * Invokes the DeleteRequest operation asynchronously.
     *
     * @param deleteRequestRequest
     * @return A Java Future containing the result of the DeleteRequest operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.DeleteRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequest"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<DeleteRequestResponse> deleteRequest(DeleteRequestRequest deleteRequestRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteRequestRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "BatchManagerTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteRequest");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<DeleteRequestResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                               DeleteRequestResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata);

            CompletableFuture<DeleteRequestResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<DeleteRequestRequest, DeleteRequestResponse>()
                             .withOperationName("DeleteRequest")
                             .withMarshaller(new DeleteRequestRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withMetricCollector(apiCallMetricCollector).withInput(deleteRequestRequest));
            AwsRequestOverrideConfiguration requestOverrideConfig = deleteRequestRequest.overrideConfiguration().orElse(null);
            CompletableFuture<DeleteRequestResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the DeleteRequestBatch operation asynchronously.
     *
     * @param deleteRequestBatchRequest
     * @return A Java Future containing the result of the DeleteRequestBatch operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.DeleteRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<DeleteRequestBatchResponse> deleteRequestBatch(DeleteRequestBatchRequest deleteRequestBatchRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteRequestBatchRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "BatchManagerTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteRequestBatch");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<DeleteRequestBatchResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, DeleteRequestBatchResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata);

            CompletableFuture<DeleteRequestBatchResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<DeleteRequestBatchRequest, DeleteRequestBatchResponse>()
                             .withOperationName("DeleteRequestBatch")
                             .withMarshaller(new DeleteRequestBatchRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withMetricCollector(apiCallMetricCollector).withInput(deleteRequestBatchRequest));
            AwsRequestOverrideConfiguration requestOverrideConfig = deleteRequestBatchRequest.overrideConfiguration()
                                                                                             .orElse(null);
            CompletableFuture<DeleteRequestBatchResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the SendRequest operation asynchronously.
     *
     * @param sendRequestRequest
     * @return A Java Future containing the result of the SendRequest operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.SendRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequest" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<SendRequestResponse> sendRequest(SendRequestRequest sendRequestRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, sendRequestRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "BatchManagerTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "SendRequest");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<SendRequestResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                             SendRequestResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata);

            CompletableFuture<SendRequestResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<SendRequestRequest, SendRequestResponse>()
                             .withOperationName("SendRequest").withMarshaller(new SendRequestRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withMetricCollector(apiCallMetricCollector).withInput(sendRequestRequest));
            AwsRequestOverrideConfiguration requestOverrideConfig = sendRequestRequest.overrideConfiguration().orElse(null);
            CompletableFuture<SendRequestResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * Invokes the SendRequestBatch operation asynchronously.
     *
     * @param sendRequestBatchRequest
     * @return A Java Future containing the result of the SendRequestBatch operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.SendRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<SendRequestBatchResponse> sendRequestBatch(SendRequestBatchRequest sendRequestBatchRequest) {
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, sendRequestBatchRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "BatchManagerTest");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "SendRequestBatch");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<SendRequestBatchResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, SendRequestBatchResponse::builder);

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata);

            CompletableFuture<SendRequestBatchResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<SendRequestBatchRequest, SendRequestBatchResponse>()
                             .withOperationName("SendRequestBatch")
                             .withMarshaller(new SendRequestBatchRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withMetricCollector(apiCallMetricCollector).withInput(sendRequestBatchRequest));
            AwsRequestOverrideConfiguration requestOverrideConfig = sendRequestBatchRequest.overrideConfiguration().orElse(null);
            CompletableFuture<SendRequestBatchResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
                      .defaultServiceExceptionSupplier(BatchManagerTestException::builder).protocol(AwsJsonProtocol.REST_JSON)
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

    @Override
    public BatchManagerTestAsyncBatchManager batchManager() {
        return BatchManagerTestAsyncBatchManager.builder().client(this).scheduledExecutor(executorService).build();
    }
}
