package software.amazon.awssdk.services.json;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.Aws4UnsignedPayloadSigner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.InvalidInputException;
import software.amazon.awssdk.services.json.model.JsonException;
import software.amazon.awssdk.services.json.model.JsonRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOutputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyIterable;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyIterable;
import software.amazon.awssdk.services.json.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.GetWithoutRequiredMembersRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithResultKeyRequestMarshaller;
import software.amazon.awssdk.services.json.transform.PaginatedOperationWithoutResultKeyRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingInputOutputOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.StreamingOutputOperationRequestMarshaller;
import software.amazon.awssdk.utils.Validate;

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

    private final SdkClientConfiguration clientConfiguration;

    protected DefaultJsonClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration;
        this.protocolFactory = init(AwsJsonProtocolFactory.builder()).build();
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
        String hostPrefix = "{StringMember}-foo.";
        Validate.paramNotBlank(aPostOperationRequest.stringMember(), "StringMember");
        String resolvedHostExpression = String.format("%s-foo.", aPostOperationRequest.stringMember());
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                                                                                                            APostOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);

        return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                         .withOperationName("APostOperation")
                                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                         .hostPrefixExpression(resolvedHostExpression).withInput(aPostOperationRequest)
                                         .withMarshaller(new APostOperationRequestMarshaller(protocolFactory)));
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
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, APostOperationWithOutputResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);

        return clientHandler
            .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                         .withOperationName("APostOperationWithOutput")
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
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<GetWithoutRequiredMembersResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, GetWithoutRequiredMembersResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);

        return clientHandler
            .execute(new ClientExecutionParams<GetWithoutRequiredMembersRequest, GetWithoutRequiredMembersResponse>()
                         .withOperationName("GetWithoutRequiredMembers")
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
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<PaginatedOperationWithResultKeyResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, PaginatedOperationWithResultKeyResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);

        return clientHandler
            .execute(new ClientExecutionParams<PaginatedOperationWithResultKeyRequest, PaginatedOperationWithResultKeyResponse>()
                         .withOperationName("PaginatedOperationWithResultKey")
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
        return new PaginatedOperationWithResultKeyIterable(this, applyPaginatorUserAgent(paginatedOperationWithResultKeyRequest));
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
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<PaginatedOperationWithoutResultKeyResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, PaginatedOperationWithoutResultKeyResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);

        return clientHandler
            .execute(new ClientExecutionParams<PaginatedOperationWithoutResultKeyRequest, PaginatedOperationWithoutResultKeyResponse>()
                         .withOperationName("PaginatedOperationWithoutResultKey")
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
        return new PaginatedOperationWithoutResultKeyIterable(this,
                                                              applyPaginatorUserAgent(paginatedOperationWithoutResultKeyRequest));
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
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                                                                       .isPayloadJson(true).build();

        HttpResponseHandler<StreamingInputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, StreamingInputOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);

        return clientHandler.execute(new ClientExecutionParams<StreamingInputOperationRequest, StreamingInputOperationResponse>()
                                         .withOperationName("StreamingInputOperation")
                                         .withResponseHandler(responseHandler)
                                         .withErrorResponseHandler(errorResponseHandler)
                                         .withInput(streamingInputOperationRequest)
                                         .withRequestBody(requestBody)
                                         .withMarshaller(
                                             new StreamingRequestMarshaller<StreamingInputOperationRequest>(
                                                 new StreamingInputOperationRequestMarshaller(protocolFactory), requestBody)));
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
     *        StreamingInputOutputOperationRequest and an InputStream to the response content are provided as parameters
     *        to the callback. The callback may return a transformed type which will be the return value of this method.
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
     * @sample JsonClient.StreamingInputOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
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

        return clientHandler.execute(
            new ClientExecutionParams<StreamingInputOutputOperationRequest, StreamingInputOutputOperationResponse>()
                .withOperationName("StreamingInputOutputOperation")
                .withResponseHandler(responseHandler)
                .withErrorResponseHandler(errorResponseHandler)
                .withInput(streamingInputOutputOperationRequest)
                .withRequestBody(requestBody)
                .withMarshaller(
                    new StreamingRequestMarshaller<StreamingInputOutputOperationRequest>(
                        new StreamingInputOutputOperationRequestMarshaller(protocolFactory), requestBody)),
            responseTransformer);
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param responseTransformer
     *        Functional interface for processing the streamed response content. The unmarshalled
     *        StreamingInputOutputOperationRequest and an InputStream to the response content are provided as parameters
     *        to the callback. The callback may return a transformed type which will be the return value of this method.
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
        JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(true)
                                                                       .isPayloadJson(false).build();

        HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
            operationMetadata, StreamingOutputOperationResponse::builder);

        HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                                                                                                   operationMetadata);

        return clientHandler.execute(
            new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                .withOperationName("StreamingOutputOperation")
                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                .withInput(streamingOutputOperationRequest)
                .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory)), responseTransformer);
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
    public void close() {
        clientHandler.close();
    }

    private <T extends JsonRequest> T applyPaginatorUserAgent(T request) {
        Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier = b -> b.addApiName(ApiName.builder()
                                                                                                      .version(VersionInfo.SDK_VERSION).name("PAGINATED").build());
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                                                                       .map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
                                                                       .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
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
    public JsonUtilities utilities() {
        return JsonUtilities.create(param1, param2, param3);
    }
}
