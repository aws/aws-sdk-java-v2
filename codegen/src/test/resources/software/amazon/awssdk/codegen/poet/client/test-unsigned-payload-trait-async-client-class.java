package software.amazon.awssdk.services.database;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AsyncAws4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.runtime.transform.AsyncStreamingRequestMarshaller;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.database.internal.DatabaseServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.database.internal.ServiceVersionInfo;
import software.amazon.awssdk.services.database.model.DatabaseException;
import software.amazon.awssdk.services.database.model.DatabaseRequest;
import software.amazon.awssdk.services.database.model.DeleteRowRequest;
import software.amazon.awssdk.services.database.model.DeleteRowResponse;
import software.amazon.awssdk.services.database.model.GetRowRequest;
import software.amazon.awssdk.services.database.model.GetRowResponse;
import software.amazon.awssdk.services.database.model.InvalidInputException;
import software.amazon.awssdk.services.database.model.OpWithSigv4ASignedPayloadRequest;
import software.amazon.awssdk.services.database.model.OpWithSigv4ASignedPayloadResponse;
import software.amazon.awssdk.services.database.model.OpWithSigv4AUnSignedPayloadRequest;
import software.amazon.awssdk.services.database.model.OpWithSigv4AUnSignedPayloadResponse;
import software.amazon.awssdk.services.database.model.OpWithSigv4AndSigv4AUnSignedPayloadRequest;
import software.amazon.awssdk.services.database.model.OpWithSigv4AndSigv4AUnSignedPayloadResponse;
import software.amazon.awssdk.services.database.model.OpWithSigv4SignedPayloadRequest;
import software.amazon.awssdk.services.database.model.OpWithSigv4SignedPayloadResponse;
import software.amazon.awssdk.services.database.model.OpWithSigv4UnSignedPayloadAndStreamingRequest;
import software.amazon.awssdk.services.database.model.OpWithSigv4UnSignedPayloadAndStreamingResponse;
import software.amazon.awssdk.services.database.model.OpWithSigv4UnSignedPayloadRequest;
import software.amazon.awssdk.services.database.model.OpWithSigv4UnSignedPayloadResponse;
import software.amazon.awssdk.services.database.model.OpsWithSigv4AndSigv4ASignedPayloadRequest;
import software.amazon.awssdk.services.database.model.OpsWithSigv4AndSigv4ASignedPayloadResponse;
import software.amazon.awssdk.services.database.model.PutRowRequest;
import software.amazon.awssdk.services.database.model.PutRowResponse;
import software.amazon.awssdk.services.database.model.SecondOpsWithSigv4AndSigv4ASignedPayloadRequest;
import software.amazon.awssdk.services.database.model.SecondOpsWithSigv4AndSigv4ASignedPayloadResponse;
import software.amazon.awssdk.services.database.transform.DeleteRowRequestMarshaller;
import software.amazon.awssdk.services.database.transform.GetRowRequestMarshaller;
import software.amazon.awssdk.services.database.transform.OpWithSigv4ASignedPayloadRequestMarshaller;
import software.amazon.awssdk.services.database.transform.OpWithSigv4AUnSignedPayloadRequestMarshaller;
import software.amazon.awssdk.services.database.transform.OpWithSigv4AndSigv4AUnSignedPayloadRequestMarshaller;
import software.amazon.awssdk.services.database.transform.OpWithSigv4SignedPayloadRequestMarshaller;
import software.amazon.awssdk.services.database.transform.OpWithSigv4UnSignedPayloadAndStreamingRequestMarshaller;
import software.amazon.awssdk.services.database.transform.OpWithSigv4UnSignedPayloadRequestMarshaller;
import software.amazon.awssdk.services.database.transform.OpsWithSigv4AndSigv4ASignedPayloadRequestMarshaller;
import software.amazon.awssdk.services.database.transform.PutRowRequestMarshaller;
import software.amazon.awssdk.services.database.transform.SecondOpsWithSigv4AndSigv4ASignedPayloadRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Internal implementation of {@link DatabaseAsyncClient}.
 *
 * @see DatabaseAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultDatabaseAsyncClient implements DatabaseAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultDatabaseAsyncClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.REST_JSON).build();

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultDatabaseAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this)
                                                      .option(SdkClientOption.API_METADATA, "Database_Service" + "#" + ServiceVersionInfo.VERSION).build();
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
    }

    /**
     * <p>
     * Performs a DELETE request to delete-row with auth as sigv4.
     * </p>
     *
     * @param deleteRowRequest
     * @return A Java Future containing the result of the DeleteRow operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.DeleteRow
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/DeleteRow" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<DeleteRowResponse> deleteRow(DeleteRowRequest deleteRowRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(deleteRowRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteRowRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteRow");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<DeleteRowResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                           DeleteRowResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<DeleteRowResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<DeleteRowRequest, DeleteRowResponse>().withOperationName("DeleteRow")
                                                                                         .withProtocolMetadata(protocolMetadata)
                                                                                         .withMarshaller(new DeleteRowRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                                                                         .withErrorResponseHandler(errorResponseHandler).withRequestConfiguration(clientConfiguration)
                                                                                         .withMetricCollector(apiCallMetricCollector).withInput(deleteRowRequest));
            CompletableFuture<DeleteRowResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET request to get-row with default payload signing.
     * </p>
     *
     * @param getRowRequest
     * @return A Java Future containing the result of the GetRow operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.GetRow
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/GetRow" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<GetRowResponse> getRow(GetRowRequest getRowRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getRowRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getRowRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetRow");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<GetRowResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                        GetRowResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<GetRowResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<GetRowRequest, GetRowResponse>().withOperationName("GetRow")
                                                                                   .withProtocolMetadata(protocolMetadata).withMarshaller(new GetRowRequestMarshaller(protocolFactory))
                                                                                   .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                                                                   .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                                                                                   .withInput(getRowRequest));
            CompletableFuture<GetRowResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET request to get-row with unsigned payload and auth as sigv4 and sigv4a.
     * </p>
     *
     * @param opWithSigv4AndSigv4AUnSignedPayloadRequest
     * @return A Java Future containing the result of the opWithSigv4AndSigv4aUnSignedPayload operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.opWithSigv4AndSigv4aUnSignedPayload
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4AndSigv4aUnSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OpWithSigv4AndSigv4AUnSignedPayloadResponse> opWithSigv4AndSigv4aUnSignedPayload(
        OpWithSigv4AndSigv4AUnSignedPayloadRequest opWithSigv4AndSigv4AUnSignedPayloadRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4AndSigv4AUnSignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         opWithSigv4AndSigv4AUnSignedPayloadRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4AndSigv4aUnSignedPayload");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OpWithSigv4AndSigv4AUnSignedPayloadResponse> responseHandler = protocolFactory
                .createResponseHandler(operationMetadata, OpWithSigv4AndSigv4AUnSignedPayloadResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OpWithSigv4AndSigv4AUnSignedPayloadResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4AndSigv4AUnSignedPayloadRequest, OpWithSigv4AndSigv4AUnSignedPayloadResponse>()
                             .withOperationName("opWithSigv4AndSigv4aUnSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OpWithSigv4AndSigv4AUnSignedPayloadRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(opWithSigv4AndSigv4AUnSignedPayloadRequest));
            CompletableFuture<OpWithSigv4AndSigv4AUnSignedPayloadResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET request to get-row with signed payload and auth as sigv4.
     * </p>
     *
     * @param opWithSigv4SignedPayloadRequest
     * @return A Java Future containing the result of the opWithSigv4SignedPayload operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.opWithSigv4SignedPayload
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4SignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OpWithSigv4SignedPayloadResponse> opWithSigv4SignedPayload(
        OpWithSigv4SignedPayloadRequest opWithSigv4SignedPayloadRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4SignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, opWithSigv4SignedPayloadRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4SignedPayload");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OpWithSigv4SignedPayloadResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, OpWithSigv4SignedPayloadResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OpWithSigv4SignedPayloadResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4SignedPayloadRequest, OpWithSigv4SignedPayloadResponse>()
                             .withOperationName("opWithSigv4SignedPayload").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OpWithSigv4SignedPayloadRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(opWithSigv4SignedPayloadRequest));
            CompletableFuture<OpWithSigv4SignedPayloadResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET request to get-row with unsigned payload and auth as sigv4.
     * </p>
     *
     * @param opWithSigv4UnSignedPayloadRequest
     * @return A Java Future containing the result of the opWithSigv4UnSignedPayload operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.opWithSigv4UnSignedPayload
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4UnSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OpWithSigv4UnSignedPayloadResponse> opWithSigv4UnSignedPayload(
        OpWithSigv4UnSignedPayloadRequest opWithSigv4UnSignedPayloadRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4UnSignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, opWithSigv4UnSignedPayloadRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4UnSignedPayload");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OpWithSigv4UnSignedPayloadResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, OpWithSigv4UnSignedPayloadResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OpWithSigv4UnSignedPayloadResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4UnSignedPayloadRequest, OpWithSigv4UnSignedPayloadResponse>()
                             .withOperationName("opWithSigv4UnSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OpWithSigv4UnSignedPayloadRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(opWithSigv4UnSignedPayloadRequest));
            CompletableFuture<OpWithSigv4UnSignedPayloadResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET with unsignedPayload streaming.
     * </p>
     *
     * @param opWithSigv4UnSignedPayloadAndStreamingRequest
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows ''
     * @return A Java Future containing the result of the opWithSigv4UnSignedPayloadAndStreaming operation returned by
     *         the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.opWithSigv4UnSignedPayloadAndStreaming
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4UnSignedPayloadAndStreaming"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OpWithSigv4UnSignedPayloadAndStreamingResponse> opWithSigv4UnSignedPayloadAndStreaming(
        OpWithSigv4UnSignedPayloadAndStreamingRequest opWithSigv4UnSignedPayloadAndStreamingRequest,
        AsyncRequestBody requestBody) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4UnSignedPayloadAndStreamingRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         opWithSigv4UnSignedPayloadAndStreamingRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4UnSignedPayloadAndStreaming");
            if (!isSignerOverridden(clientConfiguration)) {
                opWithSigv4UnSignedPayloadAndStreamingRequest = applySignerOverride(
                    opWithSigv4UnSignedPayloadAndStreamingRequest, AsyncAws4Signer.create());
            }
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OpWithSigv4UnSignedPayloadAndStreamingResponse> responseHandler = protocolFactory
                .createResponseHandler(operationMetadata, OpWithSigv4UnSignedPayloadAndStreamingResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OpWithSigv4UnSignedPayloadAndStreamingResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4UnSignedPayloadAndStreamingRequest, OpWithSigv4UnSignedPayloadAndStreamingResponse>()
                             .withOperationName("opWithSigv4UnSignedPayloadAndStreaming")
                             .withProtocolMetadata(protocolMetadata)
                             .withMarshaller(
                                 AsyncStreamingRequestMarshaller
                                     .builder()
                                     .delegateMarshaller(
                                         new OpWithSigv4UnSignedPayloadAndStreamingRequestMarshaller(protocolFactory))
                                     .asyncRequestBody(requestBody).transferEncoding(true).build())
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withAsyncRequestBody(requestBody).withInput(opWithSigv4UnSignedPayloadAndStreamingRequest));
            CompletableFuture<OpWithSigv4UnSignedPayloadAndStreamingResponse> whenCompleted = executeFuture
                .whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET request to get-row with signed payload and auth as sigv4a.
     * </p>
     *
     * @param opWithSigv4ASignedPayloadRequest
     * @return A Java Future containing the result of the opWithSigv4aSignedPayload operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.opWithSigv4aSignedPayload
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4aSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OpWithSigv4ASignedPayloadResponse> opWithSigv4aSignedPayload(
        OpWithSigv4ASignedPayloadRequest opWithSigv4ASignedPayloadRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4ASignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, opWithSigv4ASignedPayloadRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4aSignedPayload");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OpWithSigv4ASignedPayloadResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, OpWithSigv4ASignedPayloadResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OpWithSigv4ASignedPayloadResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4ASignedPayloadRequest, OpWithSigv4ASignedPayloadResponse>()
                             .withOperationName("opWithSigv4aSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OpWithSigv4ASignedPayloadRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(opWithSigv4ASignedPayloadRequest));
            CompletableFuture<OpWithSigv4ASignedPayloadResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET request to get-row with unsigned payload and auth as sigv4a.
     * </p>
     *
     * @param opWithSigv4AUnSignedPayloadRequest
     * @return A Java Future containing the result of the opWithSigv4aUnSignedPayload operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.opWithSigv4aUnSignedPayload
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4aUnSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OpWithSigv4AUnSignedPayloadResponse> opWithSigv4aUnSignedPayload(
        OpWithSigv4AUnSignedPayloadRequest opWithSigv4AUnSignedPayloadRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4AUnSignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, opWithSigv4AUnSignedPayloadRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4aUnSignedPayload");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OpWithSigv4AUnSignedPayloadResponse> responseHandler = protocolFactory.createResponseHandler(
                operationMetadata, OpWithSigv4AUnSignedPayloadResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OpWithSigv4AUnSignedPayloadResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4AUnSignedPayloadRequest, OpWithSigv4AUnSignedPayloadResponse>()
                             .withOperationName("opWithSigv4aUnSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OpWithSigv4AUnSignedPayloadRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(opWithSigv4AUnSignedPayloadRequest));
            CompletableFuture<OpWithSigv4AUnSignedPayloadResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET request to get-row with signed payload and auth as sigv4 and sigv4a.
     * </p>
     *
     * @param opsWithSigv4AndSigv4ASignedPayloadRequest
     * @return A Java Future containing the result of the opsWithSigv4andSigv4aSignedPayload operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.opsWithSigv4andSigv4aSignedPayload
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opsWithSigv4andSigv4aSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<OpsWithSigv4AndSigv4ASignedPayloadResponse> opsWithSigv4andSigv4aSignedPayload(
        OpsWithSigv4AndSigv4ASignedPayloadRequest opsWithSigv4AndSigv4ASignedPayloadRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opsWithSigv4AndSigv4ASignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         opsWithSigv4AndSigv4ASignedPayloadRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opsWithSigv4andSigv4aSignedPayload");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<OpsWithSigv4AndSigv4ASignedPayloadResponse> responseHandler = protocolFactory
                .createResponseHandler(operationMetadata, OpsWithSigv4AndSigv4ASignedPayloadResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<OpsWithSigv4AndSigv4ASignedPayloadResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<OpsWithSigv4AndSigv4ASignedPayloadRequest, OpsWithSigv4AndSigv4ASignedPayloadResponse>()
                             .withOperationName("opsWithSigv4andSigv4aSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new OpsWithSigv4AndSigv4ASignedPayloadRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(opsWithSigv4AndSigv4ASignedPayloadRequest));
            CompletableFuture<OpsWithSigv4AndSigv4ASignedPayloadResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a PUT request to put-row with auth as sigv4.
     * </p>
     *
     * @param putRowRequest
     * @return A Java Future containing the result of the PutRow operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.PutRow
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/PutRow" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<PutRowResponse> putRow(PutRowRequest putRowRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(putRowRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, putRowRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutRow");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<PutRowResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                        PutRowResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<PutRowResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<PutRowRequest, PutRowResponse>().withOperationName("PutRow")
                                                                                   .withProtocolMetadata(protocolMetadata).withMarshaller(new PutRowRequestMarshaller(protocolFactory))
                                                                                   .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                                                                   .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                                                                                   .withInput(putRowRequest));
            CompletableFuture<PutRowResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
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
     * <p>
     * Performs a GET request to get-row with signed payload and auth as sigv4 and sigv4a.
     * </p>
     *
     * @param secondOpsWithSigv4AndSigv4ASignedPayloadRequest
     * @return A Java Future containing the result of the secondOpsWithSigv4andSigv4aSignedPayload operation returned by
     *         the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DatabaseException Base class for all service exceptions. Unknown exceptions will be thrown as an
     *         instance of this type.</li>
     *         </ul>
     * @sample DatabaseAsyncClient.secondOpsWithSigv4andSigv4aSignedPayload
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/secondOpsWithSigv4andSigv4aSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<SecondOpsWithSigv4AndSigv4ASignedPayloadResponse> secondOpsWithSigv4andSigv4aSignedPayload(
        SecondOpsWithSigv4AndSigv4ASignedPayloadRequest secondOpsWithSigv4AndSigv4ASignedPayloadRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(
            secondOpsWithSigv4AndSigv4ASignedPayloadRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         secondOpsWithSigv4AndSigv4ASignedPayloadRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "secondOpsWithSigv4andSigv4aSignedPayload");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                           .isPayloadJson(true).build();

            HttpResponseHandler<SecondOpsWithSigv4AndSigv4ASignedPayloadResponse> responseHandler = protocolFactory
                .createResponseHandler(operationMetadata, SecondOpsWithSigv4AndSigv4ASignedPayloadResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                    case "InvalidInput":
                        return Optional.of(ExceptionMetadata.builder().errorCode("InvalidInput").httpStatusCode(400)
                                                            .exceptionBuilderSupplier(InvalidInputException::builder).build());
                    default:
                        return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                       operationMetadata, exceptionMetadataMapper);

            CompletableFuture<SecondOpsWithSigv4AndSigv4ASignedPayloadResponse> executeFuture = clientHandler
                .execute(new ClientExecutionParams<SecondOpsWithSigv4AndSigv4ASignedPayloadRequest, SecondOpsWithSigv4AndSigv4ASignedPayloadResponse>()
                             .withOperationName("secondOpsWithSigv4andSigv4aSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withMarshaller(new SecondOpsWithSigv4AndSigv4ASignedPayloadRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                             .withInput(secondOpsWithSigv4AndSigv4ASignedPayloadRequest));
            CompletableFuture<SecondOpsWithSigv4AndSigv4ASignedPayloadResponse> whenCompleted = executeFuture
                .whenComplete((r, e) -> {
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
    public final DatabaseServiceClientConfiguration serviceClientConfiguration() {
        return new DatabaseServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(DatabaseException::builder)
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

    private <T extends DatabaseRequest> T applySignerOverride(T request, Signer signer) {
        if (request.overrideConfiguration().flatMap(c -> c.signer()).isPresent()) {
            return request;
        }
        Consumer<AwsRequestOverrideConfiguration.Builder> signerOverride = b -> b.signer(signer).build();
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                                                                       .map(c -> c.toBuilder().applyMutation(signerOverride).build())
                                                                       .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(signerOverride).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
    }

    private static boolean isSignerOverridden(SdkClientConfiguration clientConfiguration) {
        return Boolean.TRUE.equals(clientConfiguration.option(SdkClientOption.SIGNER_OVERRIDDEN));
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
        SdkClientConfiguration.Builder configuration = clientConfiguration.toBuilder();
        if (plugins.isEmpty()) {
            return configuration.build();
        }
        DatabaseServiceClientConfigurationBuilder serviceConfigBuilder = new DatabaseServiceClientConfigurationBuilder(
            configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
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
