package software.amazon.awssdk.services.json;

import javax.annotation.Generated;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.presign.PresignerParams;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.SdkClientHandler;
import software.amazon.awssdk.config.AdvancedClientOption;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.config.SyncClientConfiguration;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory;
import software.amazon.awssdk.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.services.acm.presign.AcmClientPresigners;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.InvalidInputException;
import software.amazon.awssdk.services.json.model.JsonException;
import software.amazon.awssdk.services.json.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.json.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.GetWithoutRequiredMembersRequestMarshaller;
import software.amazon.awssdk.services.json.transform.GetWithoutRequiredMembersResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOperationResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingOutputOperationResponseUnmarshaller;
import software.amazon.awssdk.sync.RequestBody;
import software.amazon.awssdk.sync.StreamingResponseHandler;

/**
 * Internal implementation of {@link JsonClient}.
 *
 * @see JsonClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonClient implements JsonClient {
    private final ClientHandler clientHandler;

    private final SdkJsonProtocolFactory protocolFactory;

    private final ClientConfiguration clientConfiguration;

    protected DefaultJsonClient(SyncClientConfiguration clientConfiguration) {
        this.clientHandler = new SdkClientHandler(clientConfiguration, null);
        this.protocolFactory = init();
        this.clientConfiguration = clientConfiguration;
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
     * @throws SdkBaseException
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
                                                                                                     SdkBaseException, SdkClientException, JsonException {

        HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true), new APostOperationResponseUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

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
     * @throws SdkBaseException
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
            APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, SdkBaseException,
                                                                                    SdkClientException, JsonException {

        HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true), new APostOperationWithOutputResponseUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

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
     * @throws SdkBaseException
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
            GetWithoutRequiredMembersRequest getWithoutRequiredMembersRequest) throws InvalidInputException, SdkBaseException,
                                                                                      SdkClientException, JsonException {

        HttpResponseHandler<GetWithoutRequiredMembersResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true), new GetWithoutRequiredMembersResponseUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler
                .execute(new ClientExecutionParams<GetWithoutRequiredMembersRequest, GetWithoutRequiredMembersResponse>()
                                 .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                 .withInput(getWithoutRequiredMembersRequest)
                                 .withMarshaller(new GetWithoutRequiredMembersRequestMarshaller(protocolFactory)));
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
     * {@code RequestBody.of(new File("myfile.txt"))}
     * </pre>
     *
     *        See documentation in {@link RequestBody} for additional details and which sources of data are supported.
     *        The service documentation for the request content is as follows 'This be a stream'
     * @return Result of the StreamingInputOperation operation returned by the service.
     * @throws SdkBaseException
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
                                                                   RequestBody requestBody) throws SdkBaseException, SdkClientException, JsonException {

        HttpResponseHandler<StreamingInputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true), new StreamingInputOperationResponseUnmarshaller());

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

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
     *        See {@link software.amazon.awssdk.runtime.transform.StreamingResponseHandler} for details on implementing
     *        this interface and for links to pre-canned implementations for common scenarios like downloading to a
     *        file. The service documentation for the response content is as follows 'This be a stream'.
     * @return The transformed result of the StreamingResponseHandler.
     * @throws SdkBaseException
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
                                                      StreamingResponseHandler<StreamingOutputOperationResponse, ReturnT> streamingHandler) throws SdkBaseException,
                                                                                                                                                   SdkClientException, JsonException {
        HttpResponseHandler<ReturnT> responseHandler = protocolFactory.createStreamingResponseHandler(
                new StreamingOutputOperationResponseUnmarshaller(), streamingHandler);

        HttpResponseHandler<AmazonServiceException> errorResponseHandler = createErrorResponseHandler();

        return clientHandler.execute(new ClientExecutionParams<StreamingOutputOperationRequest, ReturnT>()
                                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                             .withInput(streamingOutputOperationRequest)
                                             .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory)));
    }

    private HttpResponseHandler<AmazonServiceException> createErrorResponseHandler() {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());
    }

    private software.amazon.awssdk.protocol.json.SdkJsonProtocolFactory init() {
        return new SdkJsonProtocolFactory(new JsonClientMetadata()
                                                  .withProtocolVersion("1.1")
                                                  .withSupportsCbor(false)
                                                  .withSupportsIon(false)
                                                  .withBaseServiceExceptionClass(software.amazon.awssdk.services.json.model.JsonException.class)
                                                  .withContentTypeOverride("")
                                                  .addErrorMetadata(
                                                          new JsonErrorShapeMetadata().withErrorCode("InvalidInput").withModeledClass(InvalidInputException.class)));
    }

    public AcmClientPresigners presigners() {
        return new AcmClientPresigners(PresignerParams.builder().endpoint(clientConfiguration.endpoint())
                                                      .credentialsProvider(clientConfiguration.credentialsProvider())
                                                      .signerProvider(clientConfiguration.overrideConfiguration().advancedOption(AdvancedClientOption.SIGNER_PROVIDER))
                                                      .build());
    }

    @Override
    public void close() throws Exception {
        clientHandler.close();
    }
}
