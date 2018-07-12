package software.amazon.awssdk.services.json;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.concurrent.CompletableFuture;
<<<<<<< HEAD
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
=======
import java.util.function.Consumer;
>>>>>>> public/master
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonProtocol;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolMetadata;
<<<<<<< HEAD
import software.amazon.awssdk.core.SdkResponse;
=======
import software.amazon.awssdk.core.ApiName;
>>>>>>> public/master
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.eventstream.EventStreamAsyncResponseTransformer;
import software.amazon.awssdk.core.eventstream.EventStreamTaggedUnionJsonUnmarshaller;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.protocol.json.VoidJsonUnmarshaller;
import software.amazon.awssdk.core.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.core.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.core.util.CompletableFutures;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.EventStream;
import software.amazon.awssdk.services.json.model.EventStreamOperationRequest;
import software.amazon.awssdk.services.json.model.EventStreamOperationResponse;
import software.amazon.awssdk.services.json.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.InvalidInputException;
import software.amazon.awssdk.services.json.model.JsonRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPublisher;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPublisher;
import software.amazon.awssdk.services.json.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.json.transform.APostOperationWithOutputResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.EventOneUnmarshaller;
import software.amazon.awssdk.services.json.transform.EventStreamOperationRequestMarshaller;
import software.amazon.awssdk.services.json.transform.EventStreamOperationResponseUnmarshaller;
import software.amazon.awssdk.services.json.transform.EventTwoUnmarshaller;
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
 * Internal implementation of {@link JsonAsyncClient}.
 *
 * @see JsonAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultJsonAsyncClient implements JsonAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultJsonAsyncClient.class);

    private final AsyncClientHandler clientHandler;

    private final AwsJsonProtocolFactory protocolFactory;

    protected DefaultJsonAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.protocolFactory = init(false);
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
     * @return A Java Future containing the result of the APostOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public CompletableFuture<APostOperationResponse> aPostOperation(APostOperationRequest aPostOperationRequest) {
        try {

            HttpResponseHandler<APostOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                new APostOperationResponseUnmarshaller());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory);

            return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                             .withMarshaller(new APostOperationRequestMarshaller(protocolFactory)).withResponseHandler(responseHandler)
                                             .withErrorResponseHandler(errorResponseHandler).withInput(aPostOperationRequest));
        } catch (Throwable t) {
            return CompletableFutures.failedFuture(t);
        }
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     * @return A Java Future containing the result of the APostOperationWithOutput operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<APostOperationWithOutputResponse> aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) {
        try {

            HttpResponseHandler<APostOperationWithOutputResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                new APostOperationWithOutputResponseUnmarshaller());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory);

            return clientHandler
                .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                             .withMarshaller(new APostOperationWithOutputRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withInput(aPostOperationWithOutputRequest));
        } catch (Throwable t) {
            return CompletableFutures.failedFuture(t);
        }
    }

    /**
     * Invokes the EventStreamOperation operation asynchronously.
     *
     * @param eventStreamOperationRequest
     * @return A Java Future containing the result of the EventStreamOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.EventStreamOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<Void> eventStreamOperation(EventStreamOperationRequest eventStreamOperationRequest,
            EventStreamOperationResponseHandler asyncResponseHandler) {
        try {

            HttpResponseHandler<EventStreamOperationResponse> responseHandler = jsonProtocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new EventStreamOperationResponseUnmarshaller());

            HttpResponseHandler<SdkResponse> voidResponseHandler = jsonProtocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(false).withHasStreamingSuccessResponse(true),
                    new VoidJsonUnmarshaller());

            HttpResponseHandler<? extends EventStream> eventResponseHandler = jsonProtocolFactory.createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    EventStreamTaggedUnionJsonUnmarshaller.builder()
                            .addUnmarshaller("EventOne", EventOneUnmarshaller.getInstance())
                            .addUnmarshaller("EventTwo", EventTwoUnmarshaller.getInstance())
                            .defaultUnmarshaller((in) -> EventStream.UNKNOWN).build());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(jsonProtocolFactory);
            AsyncResponseTransformer<SdkResponse, Void> asyncResponseTransformer = new EventStreamAsyncResponseTransformer<>(
                    asyncResponseHandler, responseHandler, eventResponseHandler);

            return clientHandler.execute(
                    new ClientExecutionParams<EventStreamOperationRequest, SdkResponse>()
                            .withMarshaller(new EventStreamOperationRequestMarshaller(jsonProtocolFactory))
                            .withResponseHandler(voidResponseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withInput(eventStreamOperationRequest), asyncResponseTransformer).whenComplete((r, e) -> {
                if (e != null) {
                    asyncResponseHandler.exceptionOccurred(e);
                }
            });
        } catch (Throwable t) {
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                    () -> asyncResponseHandler.exceptionOccurred(t));
            return CompletableFutures.failedFuture(t);
        }
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param getWithoutRequiredMembersRequest
     * @return A Java Future containing the result of the GetWithoutRequiredMembers operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.GetWithoutRequiredMembers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<GetWithoutRequiredMembersResponse> getWithoutRequiredMembers(
        GetWithoutRequiredMembersRequest getWithoutRequiredMembersRequest) {
        try {

            HttpResponseHandler<GetWithoutRequiredMembersResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                new GetWithoutRequiredMembersResponseUnmarshaller());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory);

            return clientHandler
                .execute(new ClientExecutionParams<GetWithoutRequiredMembersRequest, GetWithoutRequiredMembersResponse>()
                             .withMarshaller(new GetWithoutRequiredMembersRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withInput(getWithoutRequiredMembersRequest));
        } catch (Throwable t) {
            return CompletableFutures.failedFuture(t);
        }
    }

    /**
     * Some paginated operation with result_key in paginators.json file
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return A Java Future containing the result of the PaginatedOperationWithResultKey operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<PaginatedOperationWithResultKeyResponse> paginatedOperationWithResultKey(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) {
        try {

            HttpResponseHandler<PaginatedOperationWithResultKeyResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                new PaginatedOperationWithResultKeyResponseUnmarshaller());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory);

            return clientHandler
                .execute(new ClientExecutionParams<PaginatedOperationWithResultKeyRequest, PaginatedOperationWithResultKeyResponse>()
                             .withMarshaller(new PaginatedOperationWithResultKeyRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withInput(paginatedOperationWithResultKeyRequest));
        } catch (Throwable t) {
            return CompletableFutures.failedFuture(t);
        }
    }

    /**
     * Some paginated operation with result_key in paginators.json file<br/>
     * <p>
     * This is a variant of
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation. The return type is a custom publisher that can be subscribed to request a stream of response pages.
     * SDK will internally handle making service calls for you.
     * </p>
     * <p>
     * When the operation is called, an instance of this class is returned. At this point, no service calls are made yet
     * and so there is no guarantee that the request is valid. If there are errors in your request, you will see the
     * failures only after you start streaming the data. The subscribe method should be called as a request to start
     * streaming data. For more info, see
     * {@link org.reactivestreams.Publisher#subscribe(org.reactivestreams.Subscriber)}. Each call to the subscribe
     * method will result in a new {@link org.reactivestreams.Subscription} i.e., a new contract to stream data from the
     * starting request.
     * </p>
     *
     * <p>
     * The following are few ways to use the response class:
     * </p>
     * 1) Using the forEach helper method
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPublisher publisher = client.paginatedOperationWithResultKeyPaginator(request);
     * CompletableFuture<Void> future = publisher.forEach(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPublisher publisher = client.paginatedOperationWithResultKeyPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation.</b>
     * </p>
     *
     * @param paginatedOperationWithResultKeyRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    public PaginatedOperationWithResultKeyPublisher paginatedOperationWithResultKeyPaginator(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) {
        return new PaginatedOperationWithResultKeyPublisher(this, applyPaginatorUserAgent(paginatedOperationWithResultKeyRequest));
    }

    /**
     * Some paginated operation without result_key in paginators.json file
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return A Java Future containing the result of the PaginatedOperationWithoutResultKey operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithoutResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<PaginatedOperationWithoutResultKeyResponse> paginatedOperationWithoutResultKey(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) {
        try {

            HttpResponseHandler<PaginatedOperationWithoutResultKeyResponse> responseHandler = protocolFactory
                .createResponseHandler(
                    new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                    new PaginatedOperationWithoutResultKeyResponseUnmarshaller());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory);

            return clientHandler
                .execute(new ClientExecutionParams<PaginatedOperationWithoutResultKeyRequest, PaginatedOperationWithoutResultKeyResponse>()
                             .withMarshaller(new PaginatedOperationWithoutResultKeyRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withInput(paginatedOperationWithoutResultKeyRequest));
        } catch (Throwable t) {
            return CompletableFutures.failedFuture(t);
        }
    }

    /**
     * Some paginated operation without result_key in paginators.json file<br/>
     * <p>
     * This is a variant of
     * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest)}
     * operation. The return type is a custom publisher that can be subscribed to request a stream of response pages.
     * SDK will internally handle making service calls for you.
     * </p>
     * <p>
     * When the operation is called, an instance of this class is returned. At this point, no service calls are made yet
     * and so there is no guarantee that the request is valid. If there are errors in your request, you will see the
     * failures only after you start streaming the data. The subscribe method should be called as a request to start
     * streaming data. For more info, see
     * {@link org.reactivestreams.Publisher#subscribe(org.reactivestreams.Subscriber)}. Each call to the subscribe
     * method will result in a new {@link org.reactivestreams.Subscription} i.e., a new contract to stream data from the
     * starting request.
     * </p>
     *
     * <p>
     * The following are few ways to use the response class:
     * </p>
     * 1) Using the forEach helper method
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPublisher publisher = client.paginatedOperationWithoutResultKeyPaginator(request);
     * CompletableFuture<Void> future = publisher.forEach(res -> { // Do something with the response });
     * future.get();
     * }
     * </pre>
     *
     * 2) Using a custom subscriber
     * 
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPublisher publisher = client.paginatedOperationWithoutResultKeyPaginator(request);
     * publisher.subscribe(new Subscriber<software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse>() {
     * 
     * public void onSubscribe(org.reactivestreams.Subscriber subscription) { //... };
     * 
     * 
     * public void onNext(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse response) { //... };
     * });}
     * </pre>
     * 
     * As the response is a publisher, it can work well with third party reactive streams implementations like RxJava2.
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest)}
     * operation.</b>
     * </p>
     *
     * @param paginatedOperationWithoutResultKeyRequest
     * @return A custom publisher that can be subscribed to request a stream of response pages.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithoutResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    public PaginatedOperationWithoutResultKeyPublisher paginatedOperationWithoutResultKeyPaginator(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) {
        return new PaginatedOperationWithoutResultKeyPublisher(this,
                                                               applyPaginatorUserAgent(paginatedOperationWithoutResultKeyRequest));
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows 'This be a stream'
     * @return A Java Future containing the result of the StreamingInputOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, AsyncRequestBody requestBody) {
        try {

            HttpResponseHandler<StreamingInputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(false),
                new StreamingInputOperationResponseUnmarshaller());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory);

            return clientHandler
                .execute(new ClientExecutionParams<StreamingInputOperationRequest, StreamingInputOperationResponse>()
                             .withMarshaller(new StreamingInputOperationRequestMarshaller(protocolFactory))
                             .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                             .withAsyncRequestBody(requestBody).withInput(streamingInputOperationRequest));
        } catch (Throwable t) {
            return CompletableFutures.failedFuture(t);
        }
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param asyncResponseTransformer
     *        The response transformer for processing the streaming response in a non-blocking manner. See
     *        {@link AsyncResponseTransformer} for details on how this callback should be implemented and for links to
     *        precanned implementations for common scenarios like downloading to a file. The service documentation for
     *        the response content is as follows 'This be a stream'.
     * @return A future to the transformed result of the AsyncResponseTransformer.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public <ReturnT> CompletableFuture<ReturnT> streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest,
        AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> asyncResponseTransformer) {
        try {

            HttpResponseHandler<StreamingOutputOperationResponse> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(false).withHasStreamingSuccessResponse(true),
                new StreamingOutputOperationResponseUnmarshaller());

            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory);

            return clientHandler.execute(
<<<<<<< HEAD
                    new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                            .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withInput(streamingOutputOperationRequest), asyncResponseTransformer).whenComplete((r, e) -> {
                if (e != null) {
                    asyncResponseTransformer.exceptionOccurred(e);
                }
            });
=======
                new ClientExecutionParams<StreamingOutputOperationRequest, StreamingOutputOperationResponse>()
                    .withMarshaller(new StreamingOutputOperationRequestMarshaller(protocolFactory))
                    .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                    .withInput(streamingOutputOperationRequest), asyncResponseTransformer);
>>>>>>> public/master
        } catch (Throwable t) {
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                    () -> asyncResponseTransformer.exceptionOccurred(t));
            return CompletableFutures.failedFuture(t);
        }
    }

    @Override
    public void close() {
        clientHandler.close();
    }

    private software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory init(boolean supportsCbor) {
        return new AwsJsonProtocolFactory(
<<<<<<< HEAD
                new JsonClientMetadata()
                        .withSupportsCbor(supportsCbor)
                        .withSupportsIon(false)
                        .withBaseServiceExceptionClass(software.amazon.awssdk.services.json.model.JsonException.class)
                        .withContentTypeOverride("")
                        .addErrorMetadata(
                                new JsonErrorShapeMetadata().withErrorCode("InvalidInput").withModeledClass(
                                        InvalidInputException.class)), AwsJsonProtocolMetadata.builder().protocolVersion("1.1")
                        .protocol(AwsJsonProtocol.REST_JSON).build());
=======
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

    private <T extends JsonRequest> T applyPaginatorUserAgent(T request) {
        Consumer<AwsRequestOverrideConfiguration.Builder> userAgentApplier = b -> b.addApiName(ApiName.builder()
                                                                                                      .version(VersionInfo.SDK_VERSION).name("PAGINATED").build());
        AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration()
                                                                       .map(c -> c.toBuilder().applyMutation(userAgentApplier).build())
                                                                       .orElse((AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build()));
        return (T) request.toBuilder().overrideConfiguration(overrideConfiguration).build();
>>>>>>> public/master
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(AwsJsonProtocolFactory protocolFactory) {
        return protocolFactory.createErrorResponseHandler(new JsonErrorResponseMetadata());
    }
}
