package software.amazon.awssdk.services.query;

import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
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
import software.amazon.awssdk.services.query.model.QueryRequest;
import software.amazon.awssdk.services.query.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.query.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.query.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.query.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.query.presignedurl.PresignedUrlManager;
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public abstract class DelegatingQueryClient implements QueryClient {
    private final QueryClient delegate;

    public DelegatingQueryClient(QueryClient delegate) {
        Validate.paramNotNull(delegate, "delegate");
        this.delegate = delegate;
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
        return invokeOperation(aPostOperationRequest, request -> delegate.aPostOperation(request));
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
        return invokeOperation(aPostOperationWithOutputRequest, request -> delegate.aPostOperationWithOutput(request));
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
        return invokeOperation(bearerAuthOperationRequest, request -> delegate.bearerAuthOperation(request));
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
        return invokeOperation(getOperationWithChecksumRequest, request -> delegate.getOperationWithChecksum(request));
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
        return invokeOperation(operationWithChecksumRequiredRequest, request -> delegate.operationWithChecksumRequired(request));
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
        return invokeOperation(operationWithContextParamRequest, request -> delegate.operationWithContextParam(request));
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
        return invokeOperation(operationWithCustomMemberRequest, request -> delegate.operationWithCustomMember(request));
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
        return invokeOperation(operationWithCustomizedOperationContextParamRequest,
                               request -> delegate.operationWithCustomizedOperationContextParam(request));
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
        return invokeOperation(operationWithMapOperationContextParamRequest,
                               request -> delegate.operationWithMapOperationContextParam(request));
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
        return invokeOperation(operationWithNoneAuthTypeRequest, request -> delegate.operationWithNoneAuthType(request));
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
        return invokeOperation(operationWithOperationContextParamRequest,
                               request -> delegate.operationWithOperationContextParam(request));
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
        return invokeOperation(operationWithRequestCompressionRequest,
                               request -> delegate.operationWithRequestCompression(request));
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
        return invokeOperation(operationWithStaticContextParamsRequest,
                               request -> delegate.operationWithStaticContextParams(request));
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
        return invokeOperation(putOperationWithChecksumRequest,
                               request -> delegate.putOperationWithChecksum(request, requestBody, responseTransformer));
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
        return invokeOperation(streamingInputOperationRequest, request -> delegate.streamingInputOperation(request, requestBody));
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
        return invokeOperation(streamingOutputOperationRequest,
                               request -> delegate.streamingOutputOperation(request, responseTransformer));
    }

    /**
     * Creates an instance of {@link PresignedUrlManager} object with the configuration set on this client.
     */
    @Override
    public PresignedUrlManager presignedUrlManager() {
        return delegate.presignedUrlManager();
    }

    @Override
    public final String serviceName() {
        return delegate.serviceName();
    }

    public SdkClient delegate() {
        return this.delegate;
    }

    protected <T extends QueryRequest, ReturnT> ReturnT invokeOperation(T request, Function<T, ReturnT> operation) {
        return operation.apply(request);
    }

    @Override
    public final QueryServiceClientConfiguration serviceClientConfiguration() {
        return delegate.serviceClientConfiguration();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
