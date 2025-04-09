package software.amazon.awssdk.services.query;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.codegen.internal.UtilsTest;
import software.amazon.awssdk.core.CredentialType;
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
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.query.AwsQueryProtocolFactory;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.query.internal.QueryServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.query.model.BearerAuthOperationRequest;
import software.amazon.awssdk.services.query.model.BearerAuthOperationResponse;
import software.amazon.awssdk.services.query.model.GetOperationWithChecksumRequest;
import software.amazon.awssdk.services.query.model.GetOperationWithChecksumResponse;
import software.amazon.awssdk.services.query.model.InvalidInputException;
import software.amazon.awssdk.services.query.model.OperationWithChecksumRequiredRequest;
import software.amazon.awssdk.services.query.model.OperationWithChecksumRequiredResponse;
import software.amazon.awssdk.services.query.model.OperationWithContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithCustomMemberRequest;
import software.amazon.awssdk.services.query.model.OperationWithCustomMemberResponse;
import software.amazon.awssdk.services.query.model.OperationWithCustomizedOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithCustomizedOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithMapOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithMapOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithNoneAuthTypeRequest;
import software.amazon.awssdk.services.query.model.OperationWithNoneAuthTypeResponse;
import software.amazon.awssdk.services.query.model.OperationWithOperationContextParamRequest;
import software.amazon.awssdk.services.query.model.OperationWithOperationContextParamResponse;
import software.amazon.awssdk.services.query.model.OperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.query.model.OperationWithRequestCompressionResponse;
import software.amazon.awssdk.services.query.model.OperationWithStaticContextParamsRequest;
import software.amazon.awssdk.services.query.model.OperationWithStaticContextParamsResponse;
import software.amazon.awssdk.services.query.model.PutOperationWithChecksumRequest;
import software.amazon.awssdk.services.query.model.PutOperationWithChecksumResponse;
import software.amazon.awssdk.services.query.model.QueryException;
import software.amazon.awssdk.services.query.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.query.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.query.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.query.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.query.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.query.transform.BearerAuthOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.GetOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithChecksumRequiredRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithContextParamRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithCustomMemberRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithCustomizedOperationContextParamRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithMapOperationContextParamRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithNoneAuthTypeRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithOperationContextParamRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithRequestCompressionRequestMarshaller;
import software.amazon.awssdk.services.query.transform.OperationWithStaticContextParamsRequestMarshaller;
import software.amazon.awssdk.services.query.transform.PutOperationWithChecksumRequestMarshaller;
import software.amazon.awssdk.services.query.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.services.query.waiters.QueryWaiter;
import software.amazon.awssdk.utils.Logger;

/**
 * Internal implementation of {@link QueryClient}.
 *
 * @see QueryClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultQueryClient implements QueryClient {
    private static final Logger log = Logger.loggerFor(DefaultQueryClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
                                                                                   .serviceProtocol(AwsServiceProtocol.QUERY).build();

    private final SyncClientHandler clientHandler;

    private final AwsQueryProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultQueryClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this).build();
        this.protocolFactory = init();
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     AwsServiceException, SdkClientException, QueryException {

        HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory
            .createResponseHandler(APostOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(aPostOperationRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperation");
            String hostPrefix = "foo-";
            String resolvedHostExpression = "foo-";

            return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                             .withOperationName("APostOperation").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .hostPrefixExpression(resolvedHostExpression).withRequestConfiguration(clientConfiguration)
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperationWithOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, AwsServiceException,
                                                                                SdkClientException, QueryException {

        HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory
            .createResponseHandler(APostOperationWithOutputResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(aPostOperationWithOutputRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, aPostOperationWithOutputRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "APostOperationWithOutput");

            return clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                             .withOperationName("APostOperationWithOutput").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(aPostOperationWithOutputRequest)
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public BearerAuthOperationResponse bearerAuthOperation(BearerAuthOperationRequest bearerAuthOperationRequest)
        throws AwsServiceException, SdkClientException, QueryException {

        HttpResponseHandler<BearerAuthOperationResponse> responseHandler = protocolFactory
            .createResponseHandler(BearerAuthOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(bearerAuthOperationRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, bearerAuthOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "BearerAuthOperation");

            return clientHandler.execute(new ClientExecutionParams<BearerAuthOperationRequest, BearerAuthOperationResponse>()
                                             .withOperationName("BearerAuthOperation").withProtocolMetadata(protocolMetadata)
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .credentialType(CredentialType.TOKEN).withRequestConfiguration(clientConfiguration)
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GetOperationWithChecksumResponse getOperationWithChecksum(
        GetOperationWithChecksumRequest getOperationWithChecksumRequest) throws AwsServiceException, SdkClientException,
                                                                                QueryException {

        HttpResponseHandler<GetOperationWithChecksumResponse> responseHandler = protocolFactory
            .createResponseHandler(GetOperationWithChecksumResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getOperationWithChecksumRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getOperationWithChecksumRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetOperationWithChecksum");

            return clientHandler
                .execute(new ClientExecutionParams<GetOperationWithChecksumRequest, GetOperationWithChecksumResponse>()
                             .withOperationName("GetOperationWithChecksum")
                             .withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(getOperationWithChecksumRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(
                                 SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                                 HttpChecksum.builder().requestChecksumRequired(true).isRequestStreaming(false)
                                             .requestAlgorithm(getOperationWithChecksumRequest.checksumAlgorithmAsString())
                                             .requestAlgorithmHeader("x-amz-sdk-checksum-algorithm").build())
                             .withMarshaller(new GetOperationWithChecksumRequestMarshaller(protocolFactory)));
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithChecksumRequiredResponse operationWithChecksumRequired(
        OperationWithChecksumRequiredRequest operationWithChecksumRequiredRequest) throws AwsServiceException,
                                                                                          SdkClientException, QueryException {

        HttpResponseHandler<OperationWithChecksumRequiredResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithChecksumRequiredResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithChecksumRequiredRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithChecksumRequiredRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithChecksumRequired");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithChecksumRequiredRequest, OperationWithChecksumRequiredResponse>()
                             .withOperationName("OperationWithChecksumRequired")
                             .withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
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
     * Invokes the OperationWithContextParam operation.
     *
     * @param operationWithContextParamRequest
     * @return Result of the OperationWithContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithContextParam
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithContextParamResponse operationWithContextParam(
        OperationWithContextParamRequest operationWithContextParamRequest) throws AwsServiceException, SdkClientException,
                                                                                  QueryException {

        HttpResponseHandler<OperationWithContextParamResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithContextParamResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithContextParamRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithContextParamRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithContextParam");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithContextParamRequest, OperationWithContextParamResponse>()
                             .withOperationName("OperationWithContextParam").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(operationWithContextParamRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OperationWithContextParamRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithCustomMember operation.
     *
     * @param operationWithCustomMemberRequest
     * @return Result of the OperationWithCustomMember operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithCustomMember
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithCustomMember"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithCustomMemberResponse operationWithCustomMember(
        OperationWithCustomMemberRequest operationWithCustomMemberRequest) throws AwsServiceException, SdkClientException,
                                                                                  QueryException {
        operationWithCustomMemberRequest = UtilsTest.dummyRequestModifier(operationWithCustomMemberRequest);

        HttpResponseHandler<OperationWithCustomMemberResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithCustomMemberResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithCustomMemberRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithCustomMemberRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithCustomMember");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithCustomMemberRequest, OperationWithCustomMemberResponse>()
                             .withOperationName("OperationWithCustomMember").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(operationWithCustomMemberRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OperationWithCustomMemberRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithCustomizedOperationContextParam operation.
     *
     * @param operationWithCustomizedOperationContextParamRequest
     * @return Result of the OperationWithCustomizedOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithCustomizedOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithCustomizedOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithCustomizedOperationContextParamResponse operationWithCustomizedOperationContextParam(
        OperationWithCustomizedOperationContextParamRequest operationWithCustomizedOperationContextParamRequest)
        throws AwsServiceException, SdkClientException, QueryException {

        HttpResponseHandler<OperationWithCustomizedOperationContextParamResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithCustomizedOperationContextParamResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(
            operationWithCustomizedOperationContextParamRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithCustomizedOperationContextParamRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithCustomizedOperationContextParam");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithCustomizedOperationContextParamRequest, OperationWithCustomizedOperationContextParamResponse>()
                             .withOperationName("OperationWithCustomizedOperationContextParam")
                             .withProtocolMetadata(protocolMetadata).withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler).withRequestConfiguration(clientConfiguration)
                             .withInput(operationWithCustomizedOperationContextParamRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OperationWithCustomizedOperationContextParamRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithMapOperationContextParam operation.
     *
     * @param operationWithMapOperationContextParamRequest
     * @return Result of the OperationWithMapOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithMapOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithMapOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithMapOperationContextParamResponse operationWithMapOperationContextParam(
        OperationWithMapOperationContextParamRequest operationWithMapOperationContextParamRequest)
        throws AwsServiceException, SdkClientException, QueryException {

        HttpResponseHandler<OperationWithMapOperationContextParamResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithMapOperationContextParamResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithMapOperationContextParamRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithMapOperationContextParamRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithMapOperationContextParam");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithMapOperationContextParamRequest, OperationWithMapOperationContextParamResponse>()
                             .withOperationName("OperationWithMapOperationContextParam").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(operationWithMapOperationContextParamRequest).withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OperationWithMapOperationContextParamRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithNoneAuthType operation.
     *
     * @param operationWithNoneAuthTypeRequest
     * @return Result of the OperationWithNoneAuthType operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithNoneAuthType
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithNoneAuthType"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithNoneAuthTypeResponse operationWithNoneAuthType(
        OperationWithNoneAuthTypeRequest operationWithNoneAuthTypeRequest) throws AwsServiceException, SdkClientException,
                                                                                  QueryException {

        HttpResponseHandler<OperationWithNoneAuthTypeResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithNoneAuthTypeResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithNoneAuthTypeRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, operationWithNoneAuthTypeRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithNoneAuthType");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithNoneAuthTypeRequest, OperationWithNoneAuthTypeResponse>()
                             .withOperationName("OperationWithNoneAuthType").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(operationWithNoneAuthTypeRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OperationWithNoneAuthTypeRequestMarshaller(protocolFactory)));
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Invokes the OperationWithOperationContextParam operation.
     *
     * @param operationWithOperationContextParamRequest
     * @return Result of the OperationWithOperationContextParam operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithOperationContextParam
     * @see <a
     *      href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithOperationContextParam"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithOperationContextParamResponse operationWithOperationContextParam(
        OperationWithOperationContextParamRequest operationWithOperationContextParamRequest) throws AwsServiceException,
                                                                                                    SdkClientException, QueryException {

        HttpResponseHandler<OperationWithOperationContextParamResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithOperationContextParamResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithOperationContextParamRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithOperationContextParamRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithOperationContextParam");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithOperationContextParamRequest, OperationWithOperationContextParamResponse>()
                             .withOperationName("OperationWithOperationContextParam").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(operationWithOperationContextParamRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OperationWithOperationContextParamRequestMarshaller(protocolFactory)));
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithRequestCompressionResponse operationWithRequestCompression(
        OperationWithRequestCompressionRequest operationWithRequestCompressionRequest) throws AwsServiceException,
                                                                                              SdkClientException, QueryException {

        HttpResponseHandler<OperationWithRequestCompressionResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithRequestCompressionResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithRequestCompressionRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithRequestCompressionRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithRequestCompression");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithRequestCompressionRequest, OperationWithRequestCompressionResponse>()
                             .withOperationName("OperationWithRequestCompression")
                             .withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
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
     * Invokes the OperationWithStaticContextParams operation.
     *
     * @param operationWithStaticContextParamsRequest
     * @return Result of the OperationWithStaticContextParams operation returned by the service.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.OperationWithStaticContextParams
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/OperationWithStaticContextParams"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public OperationWithStaticContextParamsResponse operationWithStaticContextParams(
        OperationWithStaticContextParamsRequest operationWithStaticContextParamsRequest) throws AwsServiceException,
                                                                                                SdkClientException, QueryException {

        HttpResponseHandler<OperationWithStaticContextParamsResponse> responseHandler = protocolFactory
            .createResponseHandler(OperationWithStaticContextParamsResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(operationWithStaticContextParamsRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration,
                                                                         operationWithStaticContextParamsRequest.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "OperationWithStaticContextParams");

            return clientHandler
                .execute(new ClientExecutionParams<OperationWithStaticContextParamsRequest, OperationWithStaticContextParamsResponse>()
                             .withOperationName("OperationWithStaticContextParams").withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration).withInput(operationWithStaticContextParamsRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .withMarshaller(new OperationWithStaticContextParamsRequestMarshaller(protocolFactory)));
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT putOperationWithChecksum(PutOperationWithChecksumRequest putOperationWithChecksumRequest,
                                                      RequestBody requestBody, ResponseTransformer<PutOperationWithChecksumResponse, ReturnT> responseTransformer)
        throws AwsServiceException, SdkClientException, QueryException {

        HttpResponseHandler<PutOperationWithChecksumResponse> responseHandler = protocolFactory
            .createResponseHandler(PutOperationWithChecksumResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(putOperationWithChecksumRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, putOperationWithChecksumRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutOperationWithChecksum");

            return clientHandler
                .execute(new ClientExecutionParams<PutOperationWithChecksumRequest, PutOperationWithChecksumResponse>()
                             .withOperationName("PutOperationWithChecksum")
                             .withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
                             .withInput(putOperationWithChecksumRequest)
                             .withMetricCollector(apiCallMetricCollector)
                             .putExecutionAttribute(
                                 SdkInternalExecutionAttribute.HTTP_CHECKSUM,
                                 HttpChecksum
                                     .builder()
                                     .requestChecksumRequired(false)
                                     .isRequestStreaming(true)
                                     .requestValidationMode(putOperationWithChecksumRequest.checksumModeAsString())
                                     .responseAlgorithmsV2(DefaultChecksumAlgorithm.CRC32C,
                                                           DefaultChecksumAlgorithm.CRC32, DefaultChecksumAlgorithm.SHA1,
                                                           DefaultChecksumAlgorithm.SHA256).build())
                             .withRequestBody(requestBody)
                             .withMarshaller(
                                 StreamingRequestMarshaller.builder()
                                                           .delegateMarshaller(new PutOperationWithChecksumRequestMarshaller(protocolFactory))
                                                           .requestBody(requestBody).build()));
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingInputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StreamingInputOperationResponse streamingInputOperation(StreamingInputOperationRequest streamingInputOperationRequest,
                                                                   RequestBody requestBody) throws AwsServiceException, SdkClientException, QueryException {

        HttpResponseHandler<StreamingInputOperationResponse> responseHandler = protocolFactory
            .createResponseHandler(StreamingInputOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(streamingInputOperationRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingInputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingInputOperation");

            return clientHandler
                .execute(new ClientExecutionParams<StreamingInputOperationRequest, StreamingInputOperationResponse>()
                             .withOperationName("StreamingInputOperation")
                             .withProtocolMetadata(protocolMetadata)
                             .withResponseHandler(responseHandler)
                             .withErrorResponseHandler(errorResponseHandler)
                             .withRequestConfiguration(clientConfiguration)
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
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.StreamingOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT streamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest,
                                                      ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                                                                 SdkClientException, QueryException {

        HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory
            .createResponseHandler(StreamingOutputOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = protocolFactory.createErrorResponseHandler();
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(streamingOutputOperationRequest,
                                                                                  this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, streamingOutputOperationRequest
            .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
            .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Query Service");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "StreamingOutputOperation");

            return clientHandler.execute(
                new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                    .withOperationName("StreamingOutputOperation").withProtocolMetadata(protocolMetadata)
                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                    .withRequestConfiguration(clientConfiguration).withInput(streamingOutputOperationRequest)
                    .withMetricCollector(apiCallMetricCollector)
                    .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory)), responseTransformer);
        } finally {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
        }
    }

    /**
     * Create an instance of {@link QueryWaiter} using this client.
     * <p>
     * Waiters created via this method are managed by the SDK and resources will be released when the service client is
     * closed.
     *
     * @return an instance of {@link QueryWaiter}
     */
    @Override
    public QueryWaiter waiter() {
        return QueryWaiter.builder().client(this).build();
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
        QueryServiceClientConfigurationBuilder serviceConfigBuilder = new QueryServiceClientConfigurationBuilder(configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    private AwsQueryProtocolFactory init() {
        return AwsQueryProtocolFactory
            .builder()
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InvalidInput")
                                 .exceptionBuilderSupplier(InvalidInputException::builder).httpStatusCode(400).build())
            .clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(QueryException::builder).build();
    }

    @Override
    public final QueryServiceClientConfiguration serviceClientConfiguration() {
        return new QueryServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
