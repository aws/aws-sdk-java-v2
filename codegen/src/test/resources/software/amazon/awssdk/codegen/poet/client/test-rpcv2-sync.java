package software.amazon.awssdk.services.smithyrpcv2protocol;

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
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.smithyrpcv2protocol.internal.ServiceVersionUserAgent;
import software.amazon.awssdk.services.smithyrpcv2protocol.internal.SmithyRpcV2ProtocolServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.ComplexErrorException;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.EmptyInputOutputRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.EmptyInputOutputResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.Float16Request;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.Float16Response;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.FractionalSecondsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.FractionalSecondsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.GreetingWithErrorsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.GreetingWithErrorsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.InvalidGreetingException;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.NoInputOutputRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.NoInputOutputResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.OperationWithDefaultsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.OperationWithDefaultsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.OptionalInputOutputRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.OptionalInputOutputResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RecursiveShapesRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RecursiveShapesResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborDenseMapsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborDenseMapsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborListsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborListsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborSparseMapsRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.RpcV2CborSparseMapsResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SimpleScalarPropertiesRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SimpleScalarPropertiesResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SmithyRpcV2ProtocolException;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SparseNullsOperationRequest;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.SparseNullsOperationResponse;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.ValidationException;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.EmptyInputOutputRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.Float16RequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.FractionalSecondsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.GreetingWithErrorsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.NoInputOutputRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.OperationWithDefaultsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.OptionalInputOutputRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.RecursiveShapesRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.RpcV2CborDenseMapsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.RpcV2CborListsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.RpcV2CborSparseMapsRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.SimpleScalarPropertiesRequestMarshaller;
import software.amazon.awssdk.services.smithyrpcv2protocol.transform.SparseNullsOperationRequestMarshaller;
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link SmithyRpcV2ProtocolClient}.
 *
 * @see SmithyRpcV2ProtocolClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultSmithyRpcV2ProtocolClient implements SmithyRpcV2ProtocolClient {
    private static final Logger log = Logger.loggerFor(DefaultSmithyRpcV2ProtocolClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.SMITHY_RPC_V2_CBOR).build();

    private final SyncClientHandler clientHandler;

    private final SmithyRpcV2CborProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultSmithyRpcV2ProtocolClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this)
                                                      .option(SdkClientOption.API_METADATA, ServiceVersionUserAgent.USER_AGENT).build();
        this.protocolFactory = init(SmithyRpcV2CborProtocolFactory.builder()).build();
    }

    /**
     * Invokes the EmptyInputOutput operation.
     *
     * @param emptyInputOutputRequest
     * @return Result of the EmptyInputOutput operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.EmptyInputOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/EmptyInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public EmptyInputOutputResponse emptyInputOutput(EmptyInputOutputRequest emptyInputOutputRequest) throws AwsServiceException,
                                                                                                             SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<EmptyInputOutputResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                              EmptyInputOutputResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(emptyInputOutputRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, emptyInputOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "EmptyInputOutput");

            return clientHandler.execute(new ClientExecutionParams<EmptyInputOutputRequest, EmptyInputOutputResponse>()
                                             .withOperationName("EmptyInputOutput").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(emptyInputOutputRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new EmptyInputOutputRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the Float16 operation.
     *
     * @param float16Request
     * @return Result of the Float16 operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.Float16
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/Float16" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public Float16Response float16(Float16Request float16Request) throws AwsServiceException, SdkClientException,
                                                                         SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<Float16Response> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                     Float16Response::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(float16Request, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, float16Request
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "Float16");

            return clientHandler.execute(new ClientExecutionParams<Float16Request, Float16Response>()
                                             .withOperationName("Float16").withProtocolMetadata(protocolMetadata).withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withRequestConfiguration(clientConfiguration)
                                             .withInput(float16Request).withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new Float16RequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the FractionalSeconds operation.
     *
     * @param fractionalSecondsRequest
     * @return Result of the FractionalSeconds operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.FractionalSeconds
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/FractionalSeconds"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public FractionalSecondsResponse fractionalSeconds(FractionalSecondsRequest fractionalSecondsRequest)
        throws AwsServiceException, SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<FractionalSecondsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                               FractionalSecondsResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(fractionalSecondsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, fractionalSecondsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "FractionalSeconds");

            return clientHandler.execute(new ClientExecutionParams<FractionalSecondsRequest, FractionalSecondsResponse>()
                                             .withOperationName("FractionalSeconds").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(fractionalSecondsRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new FractionalSecondsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the GreetingWithErrors operation.
     *
     * @param greetingWithErrorsRequest
     * @return Result of the GreetingWithErrors operation returned by the service.
     * @throws ComplexErrorException
     * @throws InvalidGreetingException
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.GreetingWithErrors
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/GreetingWithErrors"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GreetingWithErrorsResponse greetingWithErrors(GreetingWithErrorsRequest greetingWithErrorsRequest)
        throws ComplexErrorException, InvalidGreetingException, AwsServiceException, SdkClientException,
               SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<GreetingWithErrorsResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, GreetingWithErrorsResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(greetingWithErrorsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, greetingWithErrorsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GreetingWithErrors");

            return clientHandler.execute(new ClientExecutionParams<GreetingWithErrorsRequest, GreetingWithErrorsResponse>()
                                             .withOperationName("GreetingWithErrors").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(greetingWithErrorsRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new GreetingWithErrorsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the NoInputOutput operation.
     *
     * @param noInputOutputRequest
     * @return Result of the NoInputOutput operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.NoInputOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/NoInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public NoInputOutputResponse noInputOutput(NoInputOutputRequest noInputOutputRequest) throws AwsServiceException,
                                                                                                 SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<NoInputOutputResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                           NoInputOutputResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(noInputOutputRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, noInputOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "NoInputOutput");

            return clientHandler.execute(new ClientExecutionParams<NoInputOutputRequest, NoInputOutputResponse>()
                                             .withOperationName("NoInputOutput").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(noInputOutputRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new NoInputOutputRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithDefaults operation.
     *
     * @param operationWithDefaultsRequest
     * @return Result of the OperationWithDefaults operation returned by the service.
     * @throws ValidationException
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.OperationWithDefaults
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/OperationWithDefaults"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithDefaultsResponse operationWithDefaults(OperationWithDefaultsRequest operationWithDefaultsRequest)
        throws ValidationException, AwsServiceException, SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<OperationWithDefaultsResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, OperationWithDefaultsResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithDefaultsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithDefaultsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithDefaults");

            return clientHandler.execute(new ClientExecutionParams<OperationWithDefaultsRequest, OperationWithDefaultsResponse>()
                                             .withOperationName("OperationWithDefaults").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(operationWithDefaultsRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new OperationWithDefaultsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OptionalInputOutput operation.
     *
     * @param optionalInputOutputRequest
     * @return Result of the OptionalInputOutput operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.OptionalInputOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/OptionalInputOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OptionalInputOutputResponse optionalInputOutput(OptionalInputOutputRequest optionalInputOutputRequest)
        throws AwsServiceException, SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<OptionalInputOutputResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, OptionalInputOutputResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(optionalInputOutputRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, optionalInputOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OptionalInputOutput");

            return clientHandler.execute(new ClientExecutionParams<OptionalInputOutputRequest, OptionalInputOutputResponse>()
                                             .withOperationName("OptionalInputOutput").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(optionalInputOutputRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new OptionalInputOutputRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the RecursiveShapes operation.
     *
     * @param recursiveShapesRequest
     * @return Result of the RecursiveShapes operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.RecursiveShapes
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/RecursiveShapes"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public RecursiveShapesResponse recursiveShapes(RecursiveShapesRequest recursiveShapesRequest) throws AwsServiceException,
                                                                                                         SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<RecursiveShapesResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                             RecursiveShapesResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(recursiveShapesRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, recursiveShapesRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "RecursiveShapes");

            return clientHandler.execute(new ClientExecutionParams<RecursiveShapesRequest, RecursiveShapesResponse>()
                                             .withOperationName("RecursiveShapes").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(recursiveShapesRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new RecursiveShapesRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the RpcV2CborDenseMaps operation.
     *
     * @param rpcV2CborDenseMapsRequest
     * @return Result of the RpcV2CborDenseMaps operation returned by the service.
     * @throws ValidationException
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.RpcV2CborDenseMaps
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/RpcV2CborDenseMaps"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public RpcV2CborDenseMapsResponse rpcV2CborDenseMaps(RpcV2CborDenseMapsRequest rpcV2CborDenseMapsRequest)
        throws ValidationException, AwsServiceException, SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<RpcV2CborDenseMapsResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, RpcV2CborDenseMapsResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(rpcV2CborDenseMapsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, rpcV2CborDenseMapsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "RpcV2CborDenseMaps");

            return clientHandler.execute(new ClientExecutionParams<RpcV2CborDenseMapsRequest, RpcV2CborDenseMapsResponse>()
                                             .withOperationName("RpcV2CborDenseMaps").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(rpcV2CborDenseMapsRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new RpcV2CborDenseMapsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the RpcV2CborLists operation.
     *
     * @param rpcV2CborListsRequest
     * @return Result of the RpcV2CborLists operation returned by the service.
     * @throws ValidationException
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.RpcV2CborLists
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/RpcV2CborLists"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public RpcV2CborListsResponse rpcV2CborLists(RpcV2CborListsRequest rpcV2CborListsRequest) throws ValidationException,
                                                                                                     AwsServiceException, SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<RpcV2CborListsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                            RpcV2CborListsResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(rpcV2CborListsRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, rpcV2CborListsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "RpcV2CborLists");

            return clientHandler.execute(new ClientExecutionParams<RpcV2CborListsRequest, RpcV2CborListsResponse>()
                                             .withOperationName("RpcV2CborLists").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(rpcV2CborListsRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new RpcV2CborListsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the RpcV2CborSparseMaps operation.
     *
     * @param rpcV2CborSparseMapsRequest
     * @return Result of the RpcV2CborSparseMaps operation returned by the service.
     * @throws ValidationException
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.RpcV2CborSparseMaps
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/RpcV2CborSparseMaps"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public RpcV2CborSparseMapsResponse rpcV2CborSparseMaps(RpcV2CborSparseMapsRequest rpcV2CborSparseMapsRequest)
        throws ValidationException, AwsServiceException, SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<RpcV2CborSparseMapsResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, RpcV2CborSparseMapsResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(rpcV2CborSparseMapsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, rpcV2CborSparseMapsRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "RpcV2CborSparseMaps");

            return clientHandler.execute(new ClientExecutionParams<RpcV2CborSparseMapsRequest, RpcV2CborSparseMapsResponse>()
                                             .withOperationName("RpcV2CborSparseMaps").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(rpcV2CborSparseMapsRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new RpcV2CborSparseMapsRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the SimpleScalarProperties operation.
     *
     * @param simpleScalarPropertiesRequest
     * @return Result of the SimpleScalarProperties operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.SimpleScalarProperties
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/SimpleScalarProperties"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public SimpleScalarPropertiesResponse simpleScalarProperties(SimpleScalarPropertiesRequest simpleScalarPropertiesRequest)
        throws AwsServiceException, SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<SimpleScalarPropertiesResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, SimpleScalarPropertiesResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(simpleScalarPropertiesRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, simpleScalarPropertiesRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "SimpleScalarProperties");

            return clientHandler
                .execute(new ClientExecutionParams<SimpleScalarPropertiesRequest, SimpleScalarPropertiesResponse>()
                             .withOperationName("SimpleScalarProperties").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(simpleScalarPropertiesRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new SimpleScalarPropertiesRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the SparseNullsOperation operation.
     *
     * @param sparseNullsOperationRequest
     * @return Result of the SparseNullsOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws SmithyRpcV2ProtocolException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample SmithyRpcV2ProtocolClient.SparseNullsOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/smithy-rpcv2protocol-2023-03-10/SparseNullsOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public SparseNullsOperationResponse sparseNullsOperation(SparseNullsOperationRequest sparseNullsOperationRequest)
        throws AwsServiceException, SdkClientException, SmithyRpcV2ProtocolException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<SparseNullsOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, SparseNullsOperationResponse::builder);
        Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
            if (errorCode == null) {
                return Optional.empty();
            }
            switch (errorCode) {
                case "ValidationException":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ValidationException")
                                                        .exceptionBuilderSupplier(ValidationException::builder).build());
                case "InvalidGreeting":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InvalidGreeting")
                                                        .exceptionBuilderSupplier(InvalidGreetingException::builder).build());
                case "ComplexError":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ComplexError")
                                                        .exceptionBuilderSupplier(ComplexErrorException::builder).build());
                default:
                    return Optional.empty();
            }
        };
        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata, exceptionMetadataMapper);
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(sparseNullsOperationRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, sparseNullsOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "SmithyRpcV2Protocol");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "SparseNullsOperation");

            return clientHandler.execute(new ClientExecutionParams<SparseNullsOperationRequest, SparseNullsOperationResponse>()
                                             .withOperationName("SparseNullsOperation").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withRequestConfiguration(clientConfiguration).withInput(sparseNullsOperationRequest)
                                             .withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new SparseNullsOperationRequestMarshaller(protocolFactory)));
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
        SmithyRpcV2ProtocolServiceClientConfigurationBuilder serviceConfigBuilder = new SmithyRpcV2ProtocolServiceClientConfigurationBuilder(
            configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration)
                      .defaultServiceExceptionSupplier(SmithyRpcV2ProtocolException::builder)
                      .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR).protocolVersion("1.1");
    }

    @Override
    public final SmithyRpcV2ProtocolServiceClientConfiguration serviceClientConfiguration() {
        return new SmithyRpcV2ProtocolServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
