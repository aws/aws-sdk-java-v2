package software.amazon.awssdk.services.json;

import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.config.AwsSyncClientConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocol;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.core.client.ClientExecutionParams;
import software.amazon.awssdk.core.client.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.core.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.InvalidInputException;
import software.amazon.awssdk.services.json.model.JsonException;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable;
import software.amazon.awssdk.services.json.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.GetWithoutRequiredMembersRequestMarshaller;
import software.amazon.awssdk.services.json.transform.GetWithoutRequiredMembersResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithResultKeyRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithResultKeyResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithoutResultKeyRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithoutResultKeyResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOperationResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingOutputOperationResponseUnmarshaller;

/**
 * Internal implementation of {@link JsonClient}.
 *
 * @see JsonClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonClient implements JsonClient {
    private final SyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    private final AwsSyncClientConfiguration clientConfiguration;

    protected DefaultJsonClient(AwsSyncClientConfiguration clientConfiguration, ServiceConfiguration serviceConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration, serviceConfiguration);
        this.protocolFactory = init();
        this.clientConfiguration = clientConfiguration;
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     AwsServiceException, SdkClientException, JsonException {

        HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new APostOperationResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                         .withInput(aPostOperationRequest).withMarshaller(new APostOperationRequestMarshaller(protocolFactory)));
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, AwsServiceException,
                                                                                SdkClientException, JsonException {

        HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new APostOperationWithOutputResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(aPostOperationWithOutputRequest)
                         .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory)));
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public GetWithoutRequiredMembersResponse getWithoutRequiredMembers(
        GetWithoutRequiredMembersRequest getWithoutRequiredMembersRequest) throws InvalidInputException, AwsServiceException,
                                                                                  SdkClientException, JsonException {

        HttpResponseHandler<GetWithoutRequiredMembersResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new GetWithoutRequiredMembersResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<GetWithoutRequiredMembersRequest, GetWithoutRequiredMembersResponse>()
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(getWithoutRequiredMembersRequest)
                         .withMarshaller(new GetWithoutRequiredMembersRequestMarshaller(protocolFactory)));
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public PaginatedOperationWithResultKeyResponse paginatedOperationWithResultKey(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) throws AwsServiceException,
                                                                                              SdkClientException, JsonException {

        HttpResponseHandler<PaginatedOperationWithResultKeyResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new PaginatedOperationWithResultKeyResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<PaginatedOperationWithResultKeyRequest, PaginatedOperationWithResultKeyResponse>()
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(paginatedOperationWithResultKeyRequest)
                         .withMarshaller(new PaginatedOperationWithResultKeyRequestMarshaller(protocolFactory)));
    }

    /**
     * Some paginated operation with result_key in paginators.json file<br/>
     * <p>
     * This is a variant of
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client.paginatedOperationWithResultKeyPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client
     *             .paginatedOperationWithResultKeyPaginator(request);
     *     for (software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable responses = client.paginatedOperationWithResultKeyPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation.</b>
     * </p>
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public PaginatedOperationWithResultKeyIterable paginatedOperationWithResultKeyPaginator(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) throws AwsServiceException,
                                                                                              SdkClientException, JsonException {
        return new PaginatedOperationWithResultKeyIterable(this, paginatedOperationWithResultKeyRequest);
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public PaginatedOperationWithoutResultKeyResponse paginatedOperationWithoutResultKey(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) throws AwsServiceException,
                                                                                                    SdkClientException, JsonException {

        HttpResponseHandler<PaginatedOperationWithoutResultKeyResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new PaginatedOperationWithoutResultKeyResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
            .execute(new ClientExecutionParams<PaginatedOperationWithoutResultKeyRequest, PaginatedOperationWithoutResultKeyResponse>()
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(paginatedOperationWithoutResultKeyRequest)
                         .withMarshaller(new PaginatedOperationWithoutResultKeyRequestMarshaller(protocolFactory)));
    }

    /**
     * Some paginated operation without result_key in paginators.json file<br/>
     * <p>
     * This is a variant of
     * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest)}
     * operation. The return type is a custom iterable that can be used to iterate through all the pages. SDK will
     * internally handle making service calls for you.
     * </p>
     * <p>
     * When this operation is called, a custom iterable is returned but no service calls are made yet. So there is no
     * guarantee that the request is valid. As you iterate through the iterable, SDK will start lazily loading response
     * pages by making service calls until there are no pages left or your iteration stops. If there are errors in your
     * request, you will see the failures only after you start iterating through the iterable.
     * </p>
     *
     * <p>
     * The following are few ways to iterate through the response pages:
     * </p>
     * 1) Using a Stream
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client.paginatedOperationWithoutResultKeyPaginator(request);
     * responses.stream().forEach(....);
     * }
     * </pre>
     *
     * 2) Using For loop
     *
     * <pre>
     * {
     *     &#064;code
     *     software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client
     *             .paginatedOperationWithoutResultKeyPaginator(request);
     *     for (software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse response : responses) {
     *         // do something;
     *     }
     * }
     * </pre>
     *
     * 3) Use iterator directly
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable responses = client.paginatedOperationWithoutResultKeyPaginator(request);
     * responses.iterator().forEachRemaining(....);
     * }
     * </pre>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest)}
     * operation.</b>
     * </p>
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return A custom iterable that can be used to iterate through all the response pages.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws JsonException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample JsonClient.PaginatedOperationWithoutResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public PaginatedOperationWithoutResultKeyIterable paginatedOperationWithoutResultKeyPaginator(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) throws AwsServiceException,
                                                                                                    SdkClientException, JsonException {
        return new PaginatedOperationWithoutResultKeyIterable(this, paginatedOperationWithoutResultKeyRequest);
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public StreamingInputOperationResponse streamingInputOperation(StreamingInputOperationRequest streamingInputOperationRequest,
                                                                   RequestBody requestBody) throws AwsServiceException, SdkClientException, JsonException {

        HttpResponseHandler<StreamingInputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
            new StreamingInputOperationResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<StreamingInputOperationRequest, StreamingInputOperationResponse>()
                                         .withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler)
                                         .withInput(streamingInputOperationRequest)
                                         .withMarshaller(
                                             new StreamingRequestMarshaller<StreamingInputOperationRequest>(
                                                 new StreamingInputOperationRequestMarshaller(protocolFactory), requestBody)));
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param streamingHandler
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingInputOperationRequest and an InputStream to the response content are provided as parameters to
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
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> ReturnT streamingOutputOperation(StreamingOutputOperationRequest streamingOutputOperationRequest,
                                                      ResponseTransformer<StreamingOutputOperationResponse, ReturnT> responseTransformer) throws AwsServiceException,
                                                                                                                                                 SdkClientException, JsonException {

        HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            new JsonOperationMetadata().withPayloadJson(false).withHasStreamingSuccessResponse(true),
            new StreamingOutputOperationResponseUnmarshaller());

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(
            new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                .withInput(streamingOutputOperationRequest)
                .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory)), responseTransformer);
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler() {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());
    }

    private software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory init() {
        return new AwsJsonProtocolFactory(
            new JsonClientMetadata()
                .withSupportsCbor(false)
                .withSupportsIon(false)
                .withBaseServiceExceptionClass(software.amazon.awssdk.services.json.model.JsonException.class)
                .withContentTypeOverride("")
                .addErrorMetadata(
                    new JsonErrorShapeMetadata().withErrorCode("InvalidInput").withModeledClass(
                        InvalidInputException.class)), AwsJsonProtocolMetadata.builder().protocolVersion("1.1")
                                                                              .protocol(AwsJsonProtocol.REST_JSON).build());
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
