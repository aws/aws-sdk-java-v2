package software.amazon.awssdk.services.database;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.core.sync.RequestBody;
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
import software.amazon.awssdk.services.database.internal.ServiceVersionUserAgent;
import software.amazon.awssdk.services.database.model.DatabaseException;
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
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link DatabaseClient}.
 *
 * @see DatabaseClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultDatabaseClient implements DatabaseClient {
    private static final Logger log = Logger.loggerFor(DefaultDatabaseClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.REST_JSON).build();

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultDatabaseClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this)
                                                      .option(SdkClientOption.API_METADATA, ServiceVersionUserAgent.USER_AGENT).build();
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
    }

    /**
     * <p>
     * Performs a DELETE request to delete-row with auth as sigv4.
     * </p>
     *
     * @param deleteRowRequest
     * @return Result of the DeleteRow operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.DeleteRow
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/DeleteRow" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public DeleteRowResponse deleteRow(DeleteRowRequest deleteRowRequest) throws InvalidInputException, AwsServiceException,
                                                                                 SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(deleteRowRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, deleteRowRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "DeleteRow");

            return clientHandler.execute(new ClientExecutionParams<DeleteRowRequest, DeleteRowResponse>()
                                             .withOperationName("DeleteRow").withProtocolMetadata(protocolMetadata).withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withRequestConfiguration(clientConfiguration)
                                             .withInput(deleteRowRequest).withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new DeleteRowRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET request to get-row with default payload signing.
     * </p>
     *
     * @param getRowRequest
     * @return Result of the GetRow operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.GetRow
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/GetRow" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public GetRowResponse getRow(GetRowRequest getRowRequest) throws InvalidInputException, AwsServiceException,
                                                                     SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getRowRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getRowRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetRow");

            return clientHandler.execute(new ClientExecutionParams<GetRowRequest, GetRowResponse>().withOperationName("GetRow")
                                                                                                   .withProtocolMetadata(protocolMetadata).withResponseHandler(responseHandler)
                                                                                                   .withErrorResponseHandler(errorResponseHandler).withRequestConfiguration(clientConfiguration)
                                                                                                   .withInput(getRowRequest).withMetricCollector(apiCallMetricCollector)
                                                                                                   .withMarshaller(new GetRowRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a PUT request to put-row with auth as sigv4.
     * </p>
     *
     * @param putRowRequest
     * @return Result of the PutRow operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.PutRow
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/PutRow" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public PutRowResponse putRow(PutRowRequest putRowRequest) throws InvalidInputException, AwsServiceException,
                                                                     SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(putRowRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, putRowRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutRow");

            return clientHandler.execute(new ClientExecutionParams<PutRowRequest, PutRowResponse>().withOperationName("PutRow")
                                                                                                   .withProtocolMetadata(protocolMetadata).withResponseHandler(responseHandler)
                                                                                                   .withErrorResponseHandler(errorResponseHandler).withRequestConfiguration(clientConfiguration)
                                                                                                   .withInput(putRowRequest).withMetricCollector(apiCallMetricCollector)
                                                                                                   .withMarshaller(new PutRowRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET request to get-row with unsigned payload and auth as sigv4 and sigv4a.
     * </p>
     *
     * @param opWithSigv4AndSigv4AUnSignedPayloadRequest
     * @return Result of the opWithSigv4AndSigv4aUnSignedPayload operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.opWithSigv4AndSigv4aUnSignedPayload
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4AndSigv4aUnSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OpWithSigv4AndSigv4AUnSignedPayloadResponse opWithSigv4AndSigv4aUnSignedPayload(
        OpWithSigv4AndSigv4AUnSignedPayloadRequest opWithSigv4AndSigv4AUnSignedPayloadRequest) throws InvalidInputException,
                                                                                                      AwsServiceException, SdkClientException, DatabaseException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<OpWithSigv4AndSigv4AUnSignedPayloadResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, OpWithSigv4AndSigv4AUnSignedPayloadResponse::builder);
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4AndSigv4AUnSignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         opWithSigv4AndSigv4AUnSignedPayloadRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4AndSigv4aUnSignedPayload");

            return clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4AndSigv4AUnSignedPayloadRequest, OpWithSigv4AndSigv4AUnSignedPayloadResponse>()
                             .withOperationName("opWithSigv4AndSigv4aUnSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(opWithSigv4AndSigv4AUnSignedPayloadRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OpWithSigv4AndSigv4AUnSignedPayloadRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET request to get-row with signed payload and auth as sigv4.
     * </p>
     *
     * @param opWithSigv4SignedPayloadRequest
     * @return Result of the opWithSigv4SignedPayload operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.opWithSigv4SignedPayload
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4SignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OpWithSigv4SignedPayloadResponse opWithSigv4SignedPayload(
        OpWithSigv4SignedPayloadRequest opWithSigv4SignedPayloadRequest) throws InvalidInputException, AwsServiceException,
                                                                                SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4SignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, opWithSigv4SignedPayloadRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4SignedPayload");

            return clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4SignedPayloadRequest, OpWithSigv4SignedPayloadResponse>()
                             .withOperationName("opWithSigv4SignedPayload").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(opWithSigv4SignedPayloadRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OpWithSigv4SignedPayloadRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET request to get-row with unsigned payload and auth as sigv4.
     * </p>
     *
     * @param opWithSigv4UnSignedPayloadRequest
     * @return Result of the opWithSigv4UnSignedPayload operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.opWithSigv4UnSignedPayload
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4UnSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OpWithSigv4UnSignedPayloadResponse opWithSigv4UnSignedPayload(
        OpWithSigv4UnSignedPayloadRequest opWithSigv4UnSignedPayloadRequest) throws InvalidInputException,
                                                                                    AwsServiceException, SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4UnSignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, opWithSigv4UnSignedPayloadRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4UnSignedPayload");

            return clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4UnSignedPayloadRequest, OpWithSigv4UnSignedPayloadResponse>()
                             .withOperationName("opWithSigv4UnSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(opWithSigv4UnSignedPayloadRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OpWithSigv4UnSignedPayloadRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET with unsignedPayload streaming.
     * </p>
     *
     * @param opWithSigv4UnSignedPayloadAndStreamingRequest
     * @param requestBody
     *        The content to send to the service. A {@link RequestBody} can be created using one of several factory
     *        methods for various sources of data. For example, to create a request body from a file you can do the
     *        following.
     *
     *        <pre>
     * {@code RequestBody.fromFile(new File("myfile.txt"))}
     * </pre>
     *
     *        See documentation in {@link RequestBody} for additional details and which sources of data are supported.
     *        The service documentation for the request content is as follows ''
     * @return Result of the opWithSigv4UnSignedPayloadAndStreaming operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.opWithSigv4UnSignedPayloadAndStreaming
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4UnSignedPayloadAndStreaming"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OpWithSigv4UnSignedPayloadAndStreamingResponse opWithSigv4UnSignedPayloadAndStreaming(
        OpWithSigv4UnSignedPayloadAndStreamingRequest opWithSigv4UnSignedPayloadAndStreamingRequest, RequestBody requestBody)
        throws InvalidInputException, AwsServiceException, SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4UnSignedPayloadAndStreamingRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         opWithSigv4UnSignedPayloadAndStreamingRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4UnSignedPayloadAndStreaming");

            return clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4UnSignedPayloadAndStreamingRequest, OpWithSigv4UnSignedPayloadAndStreamingResponse>()
                             .withOperationName("opWithSigv4UnSignedPayloadAndStreaming")
                             .withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(opWithSigv4UnSignedPayloadAndStreamingRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withRequestBody(requestBody)
                             .withMarshaller(
                                 StreamingRequestMarshaller
                                     .builder()
                                     .delegateMarshaller(
                                         new OpWithSigv4UnSignedPayloadAndStreamingRequestMarshaller(protocolFactory))
                                     .requestBody(requestBody).transferEncoding(true).build()));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET request to get-row with signed payload and auth as sigv4a.
     * </p>
     *
     * @param opWithSigv4ASignedPayloadRequest
     * @return Result of the opWithSigv4aSignedPayload operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.opWithSigv4aSignedPayload
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4aSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OpWithSigv4ASignedPayloadResponse opWithSigv4aSignedPayload(
        OpWithSigv4ASignedPayloadRequest opWithSigv4ASignedPayloadRequest) throws InvalidInputException, AwsServiceException,
                                                                                  SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4ASignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, opWithSigv4ASignedPayloadRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4aSignedPayload");

            return clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4ASignedPayloadRequest, OpWithSigv4ASignedPayloadResponse>()
                             .withOperationName("opWithSigv4aSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(opWithSigv4ASignedPayloadRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OpWithSigv4ASignedPayloadRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET request to get-row with unsigned payload and auth as sigv4a.
     * </p>
     *
     * @param opWithSigv4AUnSignedPayloadRequest
     * @return Result of the opWithSigv4aUnSignedPayload operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.opWithSigv4aUnSignedPayload
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opWithSigv4aUnSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OpWithSigv4AUnSignedPayloadResponse opWithSigv4aUnSignedPayload(
        OpWithSigv4AUnSignedPayloadRequest opWithSigv4AUnSignedPayloadRequest) throws InvalidInputException,
                                                                                      AwsServiceException, SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opWithSigv4AUnSignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, opWithSigv4AUnSignedPayloadRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opWithSigv4aUnSignedPayload");

            return clientHandler
                .execute(new ClientExecutionParams<OpWithSigv4AUnSignedPayloadRequest, OpWithSigv4AUnSignedPayloadResponse>()
                             .withOperationName("opWithSigv4aUnSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(opWithSigv4AUnSignedPayloadRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OpWithSigv4AUnSignedPayloadRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET request to get-row with signed payload and auth as sigv4 and sigv4a.
     * </p>
     *
     * @param opsWithSigv4AndSigv4ASignedPayloadRequest
     * @return Result of the opsWithSigv4andSigv4aSignedPayload operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.opsWithSigv4andSigv4aSignedPayload
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/opsWithSigv4andSigv4aSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OpsWithSigv4AndSigv4ASignedPayloadResponse opsWithSigv4andSigv4aSignedPayload(
        OpsWithSigv4AndSigv4ASignedPayloadRequest opsWithSigv4AndSigv4ASignedPayloadRequest) throws InvalidInputException,
                                                                                                    AwsServiceException, SdkClientException, DatabaseException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<OpsWithSigv4AndSigv4ASignedPayloadResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, OpsWithSigv4AndSigv4ASignedPayloadResponse::builder);
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(opsWithSigv4AndSigv4ASignedPayloadRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         opsWithSigv4AndSigv4ASignedPayloadRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "opsWithSigv4andSigv4aSignedPayload");

            return clientHandler
                .execute(new ClientExecutionParams<OpsWithSigv4AndSigv4ASignedPayloadRequest, OpsWithSigv4AndSigv4ASignedPayloadResponse>()
                             .withOperationName("opsWithSigv4andSigv4aSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(opsWithSigv4AndSigv4ASignedPayloadRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OpsWithSigv4AndSigv4ASignedPayloadRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a GET request to get-row with signed payload and auth as sigv4 and sigv4a.
     * </p>
     *
     * @param secondOpsWithSigv4AndSigv4ASignedPayloadRequest
     * @return Result of the secondOpsWithSigv4andSigv4aSignedPayload operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws DatabaseException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample DatabaseClient.secondOpsWithSigv4andSigv4aSignedPayload
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/database-service-2023-06-08/secondOpsWithSigv4andSigv4aSignedPayload"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public SecondOpsWithSigv4AndSigv4ASignedPayloadResponse secondOpsWithSigv4andSigv4aSignedPayload(
        SecondOpsWithSigv4AndSigv4ASignedPayloadRequest secondOpsWithSigv4AndSigv4ASignedPayloadRequest)
        throws InvalidInputException, AwsServiceException, SdkClientException, DatabaseException {
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
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(
            secondOpsWithSigv4AndSigv4ASignedPayloadRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         secondOpsWithSigv4AndSigv4ASignedPayloadRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Database Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "secondOpsWithSigv4andSigv4aSignedPayload");

            return clientHandler
                .execute(new ClientExecutionParams<SecondOpsWithSigv4AndSigv4ASignedPayloadRequest, SecondOpsWithSigv4AndSigv4ASignedPayloadResponse>()
                             .withOperationName("secondOpsWithSigv4andSigv4aSignedPayload").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(secondOpsWithSigv4AndSigv4ASignedPayloadRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new SecondOpsWithSigv4AndSigv4ASignedPayloadRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
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
                                                                                JsonOperationMetadata operationMetadata, Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper) {
        return protocolFactory.createErrorResponseHandler(operationMetadata, exceptionMetadataMapper);
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

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(DatabaseException::builder)
                      .protocol(AwsJsonProtocol.REST_JSON).protocolVersion("1.1");
    }

    @Override
    public final DatabaseServiceClientConfiguration serviceClientConfiguration() {
        return new DatabaseServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
