package software.amazon.awssdk.services.json;

import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
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
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public abstract class DelegatingJsonClient implements JsonClient {
    private final JsonClient delegate;

    public DelegatingJsonClient(JsonClient delegate) {
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
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.APostOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     AwsServiceException, SdkClientException, JsonException {
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
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public BearerAuthOperationResponse bearerAuthOperation(BearerAuthOperationRequest bearerAuthOperationRequest)
        throws AwsServiceException, SdkClientException, JsonException {
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
        return invokeOperation(getOperationWithChecksumRequest, request -> delegate.getOperationWithChecksum(request));
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
        return invokeOperation(getWithoutRequiredMembersRequest, request -> delegate.getWithoutRequiredMembers(request));
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
        return invokeOperation(operationWithChecksumRequiredRequest, request -> delegate.operationWithChecksumRequired(request));
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
        return invokeOperation(operationWithRequestCompressionRequest,
                               request -> delegate.operationWithRequestCompression(request));
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
        return invokeOperation(paginatedOperationWithResultKeyRequest,
                               request -> delegate.paginatedOperationWithResultKey(request));
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
        return invokeOperation(paginatedOperationWithoutResultKeyRequest,
                               request -> delegate.paginatedOperationWithoutResultKey(request));
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
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.StreamingInputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StreamingInputOperationResponse streamingInputOperation(StreamingInputOperationRequest streamingInputOperationRequest,
                                                                   RequestBody requestBody) throws AwsServiceException, SdkClientException, JsonException {
        return invokeOperation(streamingInputOperationRequest, request -> delegate.streamingInputOperation(request, requestBody));
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
        return invokeOperation(streamingInputOutputOperationRequest,
                               request -> delegate.streamingInputOutputOperation(request, requestBody, responseTransformer));
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
        return invokeOperation(streamingOutputOperationRequest,
                               request -> delegate.streamingOutputOperation(request, responseTransformer));
    }

    /**
     * Creates an instance of {@link JsonUtilities} object with the configuration set on this client.
     */
    @Override
    public JsonUtilities utilities() {
        return delegate.utilities();
    }

    @Override
    public final String serviceName() {
        return delegate.serviceName();
    }

    public SdkClient delegate() {
        return this.delegate;
    }

    protected <T extends JsonRequest, ReturnT> ReturnT invokeOperation(T request, Function<T, ReturnT> operation) {
        return operation.apply(request);
    }

    @Override
    public final JsonServiceClientConfiguration serviceClientConfiguration() {
        return delegate.serviceClientConfiguration();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
