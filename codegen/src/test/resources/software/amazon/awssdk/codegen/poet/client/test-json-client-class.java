package software.amazon.awssdk.services.json;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4UnsignedPayloadSigner;
import software.amazon.awssdk.auth.token.signer.aws.BearerTokenSigner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.BearerAuthOperationRequest;
import software.amazon.awssdk.services.json.model.BearerAuthOperationResponse;
import software.amazon.awssdk.services.json.model.GetOperationWithChecksumRequest;
import software.amazon.awssdk.services.json.model.GetOperationWithChecksumResponse;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.InvalidInputException;
import software.amazon.awssdk.services.json.model.JsonException;
import software.amazon.awssdk.services.json.model.JsonRequest;
import software.amazon.awssdk.services.json.model.OperationWithChecksumRequiredRequest;
import software.amazon.awssdk.services.json.model.OperationWithChecksumRequiredResponse;
import software.amazon.awssdk.services.json.model.OperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.json.model.OperationWithRequestCompressionResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse;
import software.amazon.awssdk.services.json.model.PutOperationWithChecksumRequest;
import software.amazon.awssdk.services.json.model.PutOperationWithChecksumResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOutputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.json.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.BearerAuthOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.GetOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.json.transform.GetWithoutRequiredMembersRequestMarshaller;
import software.amazon.awssdk.services.json.transform.OperationWithChecksumRequiredRequestMarshaller;
import software.amazon.awssdk.services.json.transform.OperationWithRequestCompressionRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithResultKeyRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithoutResultKeyRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PutOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOutputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link JsonClient}.
 *
 * @see JsonClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonClient implements JsonClient {
    private static final Logger log = Logger.loggerFor(DefaultJsonClient.class);

    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final JsonServiceClientConfiguration serviceClientConfiguration;

    protected DefaultJsonClient(JsonServiceClientConfiguration serviceClientConfiguration,
                                SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.serviceClientConfiguration = serviceClientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param aPostOperationRequest
     * @return Result of the APostOperation operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.APostOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     AwsServiceException, SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                            APostOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperation");
            String hostPrefix = "{StringMember}-foo.";
            HostnameValidator.validateHostnameCompliant(aPostOperationRequest.stringMember(), "StringMember",
                                                        "aPostOperationRequest");
            String resolvedHostExpression = String.format("%s-foo.", aPostOperationRequest.stringMember());

            return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                             .withOperationName("APostOperation").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).hostPrefixExpression(resolvedHostExpression)
                                             .withInput(aPostOperationRequest).withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new APostOperationRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     * @return Result of the APostOperationWithOutput operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.APostOperationWithOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, AwsServiceException,
                                                                                SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, APostOperationWithOutputResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationWithOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperationWithOutput");

            return clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                             .withOperationName("APostOperationWithOutput").withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler).withInput(aPostOperationWithOutputRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the BearerAuthOperation operation.
     *
     * @param bearerAuthOperationRequest
     * @return Result of the BearerAuthOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public BearerAuthOperationResponse bearerAuthOperation(BearerAuthOperationRequest bearerAuthOperationRequest)
        throws AwsServiceException, SdkClientException, JsonException {
        bearerAuthOperationRequest = applySignerOverride(bearerAuthOperationRequest, BearerTokenSigner.create());
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<BearerAuthOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, BearerAuthOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, bearerAuthOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "BearerAuthOperation");

            return clientHandler.execute(new ClientExecutionParams<BearerAuthOperationRequest, BearerAuthOperationResponse>()
                                             .withOperationName("BearerAuthOperation").withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).credentialType(CredentialType.TOKEN)
                                             .withInput(bearerAuthOperationRequest).withMetricCollector(apiCallMetricCollector)
                                             .withMarshaller(new BearerAuthOperationRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the GetOperationWithChecksum operation.
     *
     * @param getOperationWithChecksumRequest
     * @return Result of the GetOperationWithChecksum operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GetOperationWithChecksumResponse getOperationWithChecksum(
        GetOperationWithChecksumRequest getOperationWithChecksumRequest) throws AwsServiceException, SdkClientException,
                                                                                JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<GetOperationWithChecksumResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, GetOperationWithChecksumResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getOperationWithChecksumRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetOperationWithChecksum");

            return clientHandler
                .execute(new ClientExecutionParams<GetOperationWithChecksumRequest, GetOperationWithChecksumResponse>()
                             .withOperationName("GetOperationWithChecksum")
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withInput(getOperationWithChecksumRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(
                                 SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                                 HttpChecksum.builder().requestChecksumRequired(true)
                                             .requestAlgorithm(getOperationWithChecksumRequest.checksumAlgorithmAsString())
                                             .isRequestStreaming(false).build())
                             .withMarshaller(new GetOperationWithChecksumRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param getWithoutRequiredMembersRequest
     * @return Result of the GetWithoutRequiredMembers operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.GetWithoutRequiredMembers
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GetWithoutRequiredMembersResponse getWithoutRequiredMembers(
        GetWithoutRequiredMembersRequest getWithoutRequiredMembersRequest) throws InvalidInputException, AwsServiceException,
                                                                                  SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<GetWithoutRequiredMembersResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, GetWithoutRequiredMembersResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getWithoutRequiredMembersRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetWithoutRequiredMembers");

            return clientHandler
                .execute(new ClientExecutionParams<GetWithoutRequiredMembersRequest, GetWithoutRequiredMembersResponse>()
                             .withOperationName("GetWithoutRequiredMembers").withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler).withInput(getWithoutRequiredMembersRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new GetWithoutRequiredMembersRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithChecksumRequired operation.
     *
     * @param operationWithChecksumRequiredRequest
     * @return Result of the OperationWithChecksumRequired operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithChecksumRequiredResponse operationWithChecksumRequired(
        OperationWithChecksumRequiredRequest operationWithChecksumRequiredRequest) throws AwsServiceException,
                                                                                          SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<OperationWithChecksumRequiredResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, OperationWithChecksumRequiredResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithChecksumRequiredRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithChecksumRequired");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithChecksumRequiredRequest, OperationWithChecksumRequiredResponse>()
                             .withOperationName("OperationWithChecksumRequired")
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withInput(operationWithChecksumRequiredRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED,
                                                    HttpChecksumRequired.create())
                             .withMarshaller(new OperationWithChecksumRequiredRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithRequestCompression operation.
     *
     * @param operationWithRequestCompressionRequest
     * @return Result of the OperationWithRequestCompression operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithRequestCompressionResponse operationWithRequestCompression(
        OperationWithRequestCompressionRequest operationWithRequestCompressionRequest) throws AwsServiceException,
                                                                                              SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<OperationWithRequestCompressionResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, OperationWithRequestCompressionResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithRequestCompressionRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithRequestCompression");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithRequestCompressionRequest, OperationWithRequestCompressionResponse>()
                             .withOperationName("OperationWithRequestCompression")
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withInput(operationWithRequestCompressionRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION,
                                                    RequestCompression.builder().encodings("gzip").isStreaming(false).build())
                             .withMarshaller(new OperationWithRequestCompressionRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Some paginated operation with result_key in paginators.json file
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return Result of the PaginatedOperationWithResultKey operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithResultKey
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public PaginatedOperationWithResultKeyResponse paginatedOperationWithResultKey(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) throws AwsServiceException,
                                                                                              SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<PaginatedOperationWithResultKeyResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, PaginatedOperationWithResultKeyResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         paginatedOperationWithResultKeyRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PaginatedOperationWithResultKey");

            return clientHandler
                .execute(new ClientExecutionParams<PaginatedOperationWithResultKeyRequest, PaginatedOperationWithResultKeyResponse>()
                             .withOperationName("PaginatedOperationWithResultKey").withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler).withInput(paginatedOperationWithResultKeyRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new PaginatedOperationWithResultKeyRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Some paginated operation without result_key in paginators.json file
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return Result of the PaginatedOperationWithoutResultKey operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithoutResultKey
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public PaginatedOperationWithoutResultKeyResponse paginatedOperationWithoutResultKey(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) throws AwsServiceException,
                                                                                                    SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<PaginatedOperationWithoutResultKeyResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, PaginatedOperationWithoutResultKeyResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         paginatedOperationWithoutResultKeyRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PaginatedOperationWithoutResultKey");

            return clientHandler
                .execute(new ClientExecutionParams<PaginatedOperationWithoutResultKeyRequest, PaginatedOperationWithoutResultKeyResponse>()
                             .withOperationName("PaginatedOperationWithoutResultKey").withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler).withInput(paginatedOperationWithoutResultKeyRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new PaginatedOperationWithoutResultKeyRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the PutOperationWithChecksum operation.
     *
     * @param putOperationWithChecksumRequest
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
     *        The service documentation for the request content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        PutOperationWithChecksumResponse and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT putOperationWithChecksum(PutOperationWithChecksumRequest putOperationWithChecksumRequest,
                                                      RequestBody requestBody, ResponseTransformer<PutOperationWithChecksumResponse, ReturnT> responseTransformer)
        throws AwsServiceException, SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(true)
                                                                       .isPayloadJson(false).build();

        HttpResponseHandler<PutOperationWithChecksumResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, PutOperationWithChecksumResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, putOperationWithChecksumRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutOperationWithChecksum");

            return clientHandler.execute(
                new ClientExecutionParams<PutOperationWithChecksumRequest, PutOperationWithChecksumResponse>()
                    .withOperationName("PutOperationWithChecksum")
                    .withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler)
                    .withInput(putOperationWithChecksumRequest)
                    .withMetricCollector(apiCallMetricCollector)
                    .putExecutionAttribute(
                        SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                        HttpChecksum.builder().requestChecksumRequired(false)
                                    .requestValidationMode(putOperationWithChecksumRequest.checksumModeAsString())
                                    .responseAlgorithms("CRC32C", "CRC32", "SHA1", "SHA256").isRequestStreaming(true)
                                    .build())
                    .withRequestBody(requestBody)
                    .withMarshaller(
                        StreamingRequestMarshaller.builder()
                                                  .delegateMarshaller(new PutOperationWithChecksumRequestMarshaller(protocolFactory))
                                                  .requestBody(requestBody).build()), responseTransformer);
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
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
     *        The service documentation for the request content is as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StreamingInputOperationResponse streamingInputOperation(StreamingInputOperationRequest streamingInputOperationRequest,
                                                                   RequestBody requestBody) throws AwsServiceException, SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<StreamingInputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, StreamingInputOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingInputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingInputOperation");

            return clientHandler
                .execute(new ClientExecutionParams<StreamingInputOperationRequest, StreamingInputOperationResponse>()
                             .withOperationName("StreamingInputOperation")
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withInput(streamingInputOperationRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withRequestBody(requestBody)
                             .withMarshaller(
                                 StreamingRequestMarshaller.builder()
                                                           .delegateMarshaller(new StreamingInputOperationRequestMarshaller(protocolFactory))
                                                           .requestBody(requestBody).build()));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Some operation with streaming input and streaming output
     *
     * @param streamingInputOutputOperationRequest
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
     *        The service documentation for the request content is as follows 'This be a stream'
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingInputOutputOperationResponse and an InputStream to the response content are provided as
     *        parameters to the callback. The callback may return a transformed type which will be the return value of
     *        this method. See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing
     *        this interface and for links to pre-canned implementations for common scenarios like downloading to a
     *        file. The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT streamingInputOutputOperation(
        StreamingInputOutputOperationRequest streamingInputOutputOperationRequest, RequestBody requestBody,
        ResponseTransformer<StreamingInputOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                        SdkClientException, JsonException {
        streamingInputOutputOperationRequest = applySignerOverride(streamingInputOutputOperationRequest,
                                                                   Aws4UnsignedPayloadSigner.create());
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(true)
                                                                       .isPayloadJson(false).build();

        HttpResponseHandler<StreamingInputOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, StreamingInputOutputOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         streamingInputOutputOperationRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingInputOutputOperation");

            return clientHandler.execute(
                new ClientExecutionParams<StreamingInputOutputOperationRequest, StreamingInputOutputOperationResponse>()
                    .withOperationName("StreamingInputOutputOperation")
                    .withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler)
                    .withInput(streamingInputOutputOperationRequest)
                    .withMetricCollector(apiCallMetricCollector)
                    .withRequestBody(requestBody)
                    .withMarshaller(
                        StreamingRequestMarshaller
                            .builder()
                            .delegateMarshaller(
                                new StreamingInputOutputOperationRequestMarshaller(protocolFactory))
                            .requestBody(requestBody).transferEncoding(true).build()), responseTransformer);
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingOutputOperationResponse and an InputStream to the response content are provided as parameters to
     *        the callback. The callback may return a transformed type which will be the return value of this method.
     *        See {@link software.amazon.awssdk.core.sync.ResponseTransformer} for details on implementing this
     *        interface and for links to pre-canned implementations for common scenarios like downloading to a file. The
     *        service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the ResponseTransformer.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT streamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest,
                                                      ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                                                                 SdkClientException, JsonException {
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(true)
                                                                       .isPayloadJson(false).build();

        HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, StreamingOutputOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingOutputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Json Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingOutputOperation");

            return clientHandler.execute(
                new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                    .withOperationName("StreamingOutputOperation").withResponseHandler(responseHandler)
                    .withErrorResponseHandler(errorResponseHandler).withInput(streamingOutputOperationRequest)
                    .withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory)), responseTransformer);
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Creates an instance of {@link JsonUtilities} object with the configuration set on this client.
     */
    @Override
    public JsonUtilities utilities() {
        return JsonUtilities.create(param1, param2, param3);
    }

    private <T extends JsonRequest> T applySignerOverride(T request, Signer signer) {
        if (request.overrideConfiguration().flatMap(c -> c.signer()).isPresent()) {
            return request;
        }
        Consumer<AwsRequestOverrideConfiguration.Builder> signerOverride = b -> b.signer(signer).build();
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                                                                       .map(c -> c.toBuilder().applyMutation(signerOverride).build())
                                                                       .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(signerOverride).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
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
                                                                                JsonOperationMetadata operationMetadata) {
        return protocolFactory.createErrorResponseHandler(operationMetadata);
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder
            .clientConfiguration(clientConfiguration)
            .defaultServiceExceptionSupplier(JsonException::builder)
            .protocol(AwsJsonProtocol.REST_JSON)
            .protocolVersion("1.1")
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InvalidInput")
                                 .exceptionBuilderSupplier(InvalidInputException::builder).httpStatusCode(400).build());
    }

    @Override
    public final JsonServiceClientConfiguration serviceClientConfiguration() {
        return this.serviceClientConfiguration;
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
