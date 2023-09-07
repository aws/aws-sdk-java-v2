package software.amazon.awssdk.services.json;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.builder.Builder;
import software.amazon.awssdk.services.builder.CustomBuilder;
import software.amazon.awssdk.services.builder.DefaultBuilder;
import software.amazon.awssdk.services.builder.DefaultBuilderTwo;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.BearerAuthOperationRequest;
import software.amazon.awssdk.services.json.model.BearerAuthOperationResponse;
import software.amazon.awssdk.services.json.model.EventStreamOperationRequest;
import software.amazon.awssdk.services.json.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyInputRequest;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyInputResponse;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyOutputRequest;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyOutputResponseHandler;
import software.amazon.awssdk.services.json.model.GetOperationWithChecksumRequest;
import software.amazon.awssdk.services.json.model.GetOperationWithChecksumResponse;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.InputEventStream;
import software.amazon.awssdk.services.json.model.InputEventStreamTwo;
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
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPublisher;
import software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPublisher;

/**
 * Service client for accessing Json Service asynchronously. This can be created using the static {@link #builder()}
 * method.
 *
 * A service that is implemented using the query protocol
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface JsonAsyncClient extends AwsClient {
    String SERVICE_NAME = "json-service";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "json-service-endpoint";

    /**
     * Creates an instance of {@link JsonUtilities} object with the configuration set on this client.
     */
    default JsonUtilities utilities() {
        throw new UnsupportedOperationException();
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     *
     * @deprecated This API is deprecated, use something else
     */
    @Deprecated
    default CompletableFuture<APostOperationResponse> aPostOperation(APostOperationRequest aPostOperationRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link APostOperationRequest.Builder} avoiding the need to
     * create one manually via {@link APostOperationRequest#builder()}
     * </p>
     *
     * @param aPostOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.APostOperationRequest.Builder} to create a request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     *
     * @deprecated This API is deprecated, use something else
     */
    @Deprecated
    default CompletableFuture<APostOperationResponse> aPostOperation(Consumer<APostOperationRequest.Builder> aPostOperationRequest) {
        return aPostOperation(APostOperationRequest.builder().applyMutation(aPostOperationRequest).build());
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<APostOperationWithOutputResponse> aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link APostOperationWithOutputRequest.Builder} avoiding
     * the need to create one manually via {@link APostOperationWithOutputRequest#builder()}
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest.Builder} to create a
     *        request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<APostOperationWithOutputResponse> aPostOperationWithOutput(
        Consumer<APostOperationWithOutputRequest.Builder> aPostOperationWithOutputRequest) {
        return aPostOperationWithOutput(APostOperationWithOutputRequest.builder().applyMutation(aPostOperationWithOutputRequest)
                                                                       .build());
    }

    /**
     * Invokes the BearerAuthOperation operation asynchronously.
     *
     * @param bearerAuthOperationRequest
     * @return A Java Future containing the result of the BearerAuthOperation operation returned by the service.<br/>
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
     * @sample JsonAsyncClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<BearerAuthOperationResponse> bearerAuthOperation(
        BearerAuthOperationRequest bearerAuthOperationRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the BearerAuthOperation operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link BearerAuthOperationRequest.Builder} avoiding the
     * need to create one manually via {@link BearerAuthOperationRequest#builder()}
     * </p>
     *
     * @param bearerAuthOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.BearerAuthOperationRequest.Builder} to create a request.
     * @return A Java Future containing the result of the BearerAuthOperation operation returned by the service.<br/>
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
     * @sample JsonAsyncClient.BearerAuthOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/BearerAuthOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<BearerAuthOperationResponse> bearerAuthOperation(
        Consumer<BearerAuthOperationRequest.Builder> bearerAuthOperationRequest) {
        return bearerAuthOperation(BearerAuthOperationRequest.builder().applyMutation(bearerAuthOperationRequest).build());
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<Void> eventStreamOperation(EventStreamOperationRequest eventStreamOperationRequest,
                                                         Publisher<InputEventStream> requestStream, EventStreamOperationResponseHandler asyncResponseHandler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the EventStreamOperation operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link EventStreamOperationRequest.Builder} avoiding the
     * need to create one manually via {@link EventStreamOperationRequest#builder()}
     * </p>
     *
     * @param eventStreamOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.EventStreamOperationRequest.Builder} to create a
     *        request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<Void> eventStreamOperation(
        Consumer<EventStreamOperationRequest.Builder> eventStreamOperationRequest, Publisher<InputEventStream> requestStream,
        EventStreamOperationResponseHandler asyncResponseHandler) {
        return eventStreamOperation(EventStreamOperationRequest.builder().applyMutation(eventStreamOperationRequest).build(),
                                    requestStream, asyncResponseHandler);
    }

    /**
     * Invokes the EventStreamOperationWithOnlyInput operation asynchronously.
     *
     * @param eventStreamOperationWithOnlyInputRequest
     * @return A Java Future containing the result of the EventStreamOperationWithOnlyInput operation returned by the
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
     * @sample JsonAsyncClient.EventStreamOperationWithOnlyInput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperationWithOnlyInput"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<EventStreamOperationWithOnlyInputResponse> eventStreamOperationWithOnlyInput(
        EventStreamOperationWithOnlyInputRequest eventStreamOperationWithOnlyInputRequest,
        Publisher<InputEventStreamTwo> requestStream) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the EventStreamOperationWithOnlyInput operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link EventStreamOperationWithOnlyInputRequest.Builder}
     * avoiding the need to create one manually via {@link EventStreamOperationWithOnlyInputRequest#builder()}
     * </p>
     *
     * @param eventStreamOperationWithOnlyInputRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyInputRequest.Builder} to
     *        create a request.
     * @return A Java Future containing the result of the EventStreamOperationWithOnlyInput operation returned by the
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
     * @sample JsonAsyncClient.EventStreamOperationWithOnlyInput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperationWithOnlyInput"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<EventStreamOperationWithOnlyInputResponse> eventStreamOperationWithOnlyInput(
        Consumer<EventStreamOperationWithOnlyInputRequest.Builder> eventStreamOperationWithOnlyInputRequest,
        Publisher<InputEventStreamTwo> requestStream) {
        return eventStreamOperationWithOnlyInput(
            EventStreamOperationWithOnlyInputRequest.builder().applyMutation(eventStreamOperationWithOnlyInputRequest)
                                                    .build(), requestStream);
    }

    /**
     * Invokes the EventStreamOperationWithOnlyOutput operation asynchronously.
     *
     * @param eventStreamOperationWithOnlyOutputRequest
     * @return A Java Future containing the result of the EventStreamOperationWithOnlyOutput operation returned by the
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
     * @sample JsonAsyncClient.EventStreamOperationWithOnlyOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperationWithOnlyOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<Void> eventStreamOperationWithOnlyOutput(
        EventStreamOperationWithOnlyOutputRequest eventStreamOperationWithOnlyOutputRequest,
        EventStreamOperationWithOnlyOutputResponseHandler asyncResponseHandler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the EventStreamOperationWithOnlyOutput operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link EventStreamOperationWithOnlyOutputRequest.Builder}
     * avoiding the need to create one manually via {@link EventStreamOperationWithOnlyOutputRequest#builder()}
     * </p>
     *
     * @param eventStreamOperationWithOnlyOutputRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyOutputRequest.Builder} to
     *        create a request.
     * @return A Java Future containing the result of the EventStreamOperationWithOnlyOutput operation returned by the
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
     * @sample JsonAsyncClient.EventStreamOperationWithOnlyOutput
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/EventStreamOperationWithOnlyOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<Void> eventStreamOperationWithOnlyOutput(
        Consumer<EventStreamOperationWithOnlyOutputRequest.Builder> eventStreamOperationWithOnlyOutputRequest,
        EventStreamOperationWithOnlyOutputResponseHandler asyncResponseHandler) {
        return eventStreamOperationWithOnlyOutput(
            EventStreamOperationWithOnlyOutputRequest.builder().applyMutation(eventStreamOperationWithOnlyOutputRequest)
                                                     .build(), asyncResponseHandler);
    }

    /**
     * Invokes the GetOperationWithChecksum operation asynchronously.
     *
     * @param getOperationWithChecksumRequest
     * @return A Java Future containing the result of the GetOperationWithChecksum operation returned by the service.<br/>
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
     * @sample JsonAsyncClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<GetOperationWithChecksumResponse> getOperationWithChecksum(
        GetOperationWithChecksumRequest getOperationWithChecksumRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the GetOperationWithChecksum operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetOperationWithChecksumRequest.Builder} avoiding
     * the need to create one manually via {@link GetOperationWithChecksumRequest#builder()}
     * </p>
     *
     * @param getOperationWithChecksumRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.GetOperationWithChecksumRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the GetOperationWithChecksum operation returned by the service.<br/>
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
     * @sample JsonAsyncClient.GetOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<GetOperationWithChecksumResponse> getOperationWithChecksum(
        Consumer<GetOperationWithChecksumRequest.Builder> getOperationWithChecksumRequest) {
        return getOperationWithChecksum(GetOperationWithChecksumRequest.builder().applyMutation(getOperationWithChecksumRequest)
                                                                       .build());
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<GetWithoutRequiredMembersResponse> getWithoutRequiredMembers(
        GetWithoutRequiredMembersRequest getWithoutRequiredMembersRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     * <br/>
     * <p>
     * This is a convenience which creates an instance of the {@link GetWithoutRequiredMembersRequest.Builder} avoiding
     * the need to create one manually via {@link GetWithoutRequiredMembersRequest#builder()}
     * </p>
     *
     * @param getWithoutRequiredMembersRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest.Builder} to create a
     *        request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<GetWithoutRequiredMembersResponse> getWithoutRequiredMembers(
        Consumer<GetWithoutRequiredMembersRequest.Builder> getWithoutRequiredMembersRequest) {
        return getWithoutRequiredMembers(GetWithoutRequiredMembersRequest.builder()
                                                                         .applyMutation(getWithoutRequiredMembersRequest).build());
    }

    /**
     * Invokes the OperationWithChecksumRequired operation asynchronously.
     *
     * @param operationWithChecksumRequiredRequest
     * @return A Java Future containing the result of the OperationWithChecksumRequired operation returned by the
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
     * @sample JsonAsyncClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<OperationWithChecksumRequiredResponse> operationWithChecksumRequired(
        OperationWithChecksumRequiredRequest operationWithChecksumRequiredRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithChecksumRequired operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithChecksumRequiredRequest.Builder}
     * avoiding the need to create one manually via {@link OperationWithChecksumRequiredRequest#builder()}
     * </p>
     *
     * @param operationWithChecksumRequiredRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.OperationWithChecksumRequiredRequest.Builder} to create
     *        a request.
     * @return A Java Future containing the result of the OperationWithChecksumRequired operation returned by the
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
     * @sample JsonAsyncClient.OperationWithChecksumRequired
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/OperationWithChecksumRequired"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<OperationWithChecksumRequiredResponse> operationWithChecksumRequired(
        Consumer<OperationWithChecksumRequiredRequest.Builder> operationWithChecksumRequiredRequest) {
        return operationWithChecksumRequired(OperationWithChecksumRequiredRequest.builder()
                                                                                 .applyMutation(operationWithChecksumRequiredRequest).build());
    }

    /**
     * Invokes the OperationWithRequestCompression operation asynchronously.
     *
     * @param operationWithRequestCompressionRequest
     * @return A Java Future containing the result of the OperationWithRequestCompression operation returned by the
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
     * @sample JsonAsyncClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<OperationWithRequestCompressionResponse> operationWithRequestCompression(
        OperationWithRequestCompressionRequest operationWithRequestCompressionRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the OperationWithRequestCompression operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link OperationWithRequestCompressionRequest.Builder}
     * avoiding the need to create one manually via {@link OperationWithRequestCompressionRequest#builder()}
     * </p>
     *
     * @param operationWithRequestCompressionRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.OperationWithRequestCompressionRequest.Builder} to
     *        create a request.
     * @return A Java Future containing the result of the OperationWithRequestCompression operation returned by the
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
     * @sample JsonAsyncClient.OperationWithRequestCompression
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/OperationWithRequestCompression"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<OperationWithRequestCompressionResponse> operationWithRequestCompression(
        Consumer<OperationWithRequestCompressionRequest.Builder> operationWithRequestCompressionRequest) {
        return operationWithRequestCompression(OperationWithRequestCompressionRequest.builder()
                                                                                     .applyMutation(operationWithRequestCompressionRequest).build());
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithResultKeyResponse> paginatedOperationWithResultKey(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation with result_key in paginators.json file<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithResultKeyRequest#builder()}
     * </p>
     *
     * @param paginatedOperationWithResultKeyRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest.Builder} to
     *        create a request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithResultKeyResponse> paginatedOperationWithResultKey(
        Consumer<PaginatedOperationWithResultKeyRequest.Builder> paginatedOperationWithResultKeyRequest) {
        return paginatedOperationWithResultKey(PaginatedOperationWithResultKeyRequest.builder()
                                                                                     .applyMutation(paginatedOperationWithResultKeyRequest).build());
    }

    /**
     * Some paginated operation with result_key in paginators.json file
     *
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithResultKeyResponse> paginatedOperationWithResultKey() {
        return paginatedOperationWithResultKey(PaginatedOperationWithResultKeyRequest.builder().build());
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
     * 1) Using the subscribe helper method
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPublisher publisher = client.paginatedOperationWithResultKeyPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
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
     * <b>Please notice that the configuration of MaxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation.</b>
     * </p>
     *
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithResultKeyPublisher paginatedOperationWithResultKeyPaginator() {
        return paginatedOperationWithResultKeyPaginator(PaginatedOperationWithResultKeyRequest.builder().build());
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
     * 1) Using the subscribe helper method
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPublisher publisher = client.paginatedOperationWithResultKeyPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
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
     * <b>Please notice that the configuration of MaxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithResultKeyPublisher paginatedOperationWithResultKeyPaginator(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) {
        return new PaginatedOperationWithResultKeyPublisher(this, paginatedOperationWithResultKeyRequest);
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
     * 1) Using the subscribe helper method
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithResultKeyPublisher publisher = client.paginatedOperationWithResultKeyPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
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
     * <b>Please notice that the configuration of MaxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest)}
     * operation.</b>
     * </p>
     * <p>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithResultKeyRequest#builder()}
     * </p>
     *
     * @param paginatedOperationWithResultKeyRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest.Builder} to
     *        create a request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithResultKeyPublisher paginatedOperationWithResultKeyPaginator(
        Consumer<PaginatedOperationWithResultKeyRequest.Builder> paginatedOperationWithResultKeyRequest) {
        return paginatedOperationWithResultKeyPaginator(PaginatedOperationWithResultKeyRequest.builder()
                                                                                              .applyMutation(paginatedOperationWithResultKeyRequest).build());
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithoutResultKeyResponse> paginatedOperationWithoutResultKey(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation without result_key in paginators.json file<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithoutResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithoutResultKeyRequest#builder()}
     * </p>
     *
     * @param paginatedOperationWithoutResultKeyRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest.Builder} to
     *        create a request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithoutResultKeyResponse> paginatedOperationWithoutResultKey(
        Consumer<PaginatedOperationWithoutResultKeyRequest.Builder> paginatedOperationWithoutResultKeyRequest) {
        return paginatedOperationWithoutResultKey(PaginatedOperationWithoutResultKeyRequest.builder()
                                                                                           .applyMutation(paginatedOperationWithoutResultKeyRequest).build());
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
     * 1) Using the subscribe helper method
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPublisher publisher = client.paginatedOperationWithoutResultKeyPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
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
     * <b>Please notice that the configuration of MaxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithoutResultKeyPublisher paginatedOperationWithoutResultKeyPaginator(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) {
        return new PaginatedOperationWithoutResultKeyPublisher(this, paginatedOperationWithoutResultKeyRequest);
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
     * 1) Using the subscribe helper method
     *
     * <pre>
     * {@code
     * software.amazon.awssdk.services.json.paginators.PaginatedOperationWithoutResultKeyPublisher publisher = client.paginatedOperationWithoutResultKeyPaginator(request);
     * CompletableFuture<Void> future = publisher.subscribe(res -> { // Do something with the response });
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
     * <b>Please notice that the configuration of MaxResults won't limit the number of results you get with the
     * paginator. It only limits the number of results in each page.</b>
     * </p>
     * <p>
     * <b>Note: If you prefer to have control on service calls, use the
     * {@link #paginatedOperationWithoutResultKey(software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest)}
     * operation.</b>
     * </p>
     * <p>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithoutResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithoutResultKeyRequest#builder()}
     * </p>
     *
     * @param paginatedOperationWithoutResultKeyRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest.Builder} to
     *        create a request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default PaginatedOperationWithoutResultKeyPublisher paginatedOperationWithoutResultKeyPaginator(
        Consumer<PaginatedOperationWithoutResultKeyRequest.Builder> paginatedOperationWithoutResultKeyRequest) {
        return paginatedOperationWithoutResultKeyPaginator(PaginatedOperationWithoutResultKeyRequest.builder()
                                                                                                    .applyMutation(paginatedOperationWithoutResultKeyRequest).build());
    }

    /**
     * Invokes the PutOperationWithChecksum operation asynchronously.
     *
     * @param putOperationWithChecksumRequest
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param asyncResponseTransformer
     *        The response transformer for processing the streaming response in a non-blocking manner. See
     *        {@link AsyncResponseTransformer} for details on how this callback should be implemented and for links to
     *        precanned implementations for common scenarios like downloading to a file. The service documentation for
     *        the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
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
     * @sample JsonAsyncClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> CompletableFuture<ReturnT> putOperationWithChecksum(
        PutOperationWithChecksumRequest putOperationWithChecksumRequest, AsyncRequestBody requestBody,
        AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT> asyncResponseTransformer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the PutOperationWithChecksum operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PutOperationWithChecksumRequest.Builder} avoiding
     * the need to create one manually via {@link PutOperationWithChecksumRequest#builder()}
     * </p>
     *
     * @param putOperationWithChecksumRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.PutOperationWithChecksumRequest.Builder} to create a
     *        request.
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param asyncResponseTransformer
     *        The response transformer for processing the streaming response in a non-blocking manner. See
     *        {@link AsyncResponseTransformer} for details on how this callback should be implemented and for links to
     *        precanned implementations for common scenarios like downloading to a file. The service documentation for
     *        the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
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
     * @sample JsonAsyncClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> CompletableFuture<ReturnT> putOperationWithChecksum(
        Consumer<PutOperationWithChecksumRequest.Builder> putOperationWithChecksumRequest, AsyncRequestBody requestBody,
        AsyncResponseTransformer<PutOperationWithChecksumResponse, ReturnT> asyncResponseTransformer) {
        return putOperationWithChecksum(PutOperationWithChecksumRequest.builder().applyMutation(putOperationWithChecksumRequest)
                                                                       .build(), requestBody, asyncResponseTransformer);
    }

    /**
     * Invokes the PutOperationWithChecksum operation asynchronously.
     *
     * @param putOperationWithChecksumRequest
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
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
     * @sample JsonAsyncClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PutOperationWithChecksumResponse> putOperationWithChecksum(
        PutOperationWithChecksumRequest putOperationWithChecksumRequest, Path sourcePath, Path destinationPath) {
        return putOperationWithChecksum(putOperationWithChecksumRequest, AsyncRequestBody.fromFile(sourcePath),
                                        AsyncResponseTransformer.toFile(destinationPath));
    }

    /**
     * Invokes the PutOperationWithChecksum operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link PutOperationWithChecksumRequest.Builder} avoiding
     * the need to create one manually via {@link PutOperationWithChecksumRequest#builder()}
     * </p>
     *
     * @param putOperationWithChecksumRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.PutOperationWithChecksumRequest.Builder} to create a
     *        request.
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows '
     *        <p>
     *        Object data.
     *        </p>
     *        '.
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
     * @sample JsonAsyncClient.PutOperationWithChecksum
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PutOperationWithChecksum"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PutOperationWithChecksumResponse> putOperationWithChecksum(
        Consumer<PutOperationWithChecksumRequest.Builder> putOperationWithChecksumRequest, Path sourcePath,
        Path destinationPath) {
        return putOperationWithChecksum(PutOperationWithChecksumRequest.builder().applyMutation(putOperationWithChecksumRequest)
                                                                       .build(), sourcePath, destinationPath);
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, AsyncRequestBody requestBody) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming input<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingInputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.StreamingInputOperationRequest.Builder} to create a
     *        request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
        Consumer<StreamingInputOperationRequest.Builder> streamingInputOperationRequest, AsyncRequestBody requestBody) {
        return streamingInputOperation(StreamingInputOperationRequest.builder().applyMutation(streamingInputOperationRequest)
                                                                     .build(), requestBody);
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, Path sourcePath) {
        return streamingInputOperation(streamingInputOperationRequest, AsyncRequestBody.fromFile(sourcePath));
    }

    /**
     * Some operation with a streaming input<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingInputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.StreamingInputOperationRequest.Builder} to create a
     *        request.
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
        Consumer<StreamingInputOperationRequest.Builder> streamingInputOperationRequest, Path sourcePath) {
        return streamingInputOperation(StreamingInputOperationRequest.builder().applyMutation(streamingInputOperationRequest)
                                                                     .build(), sourcePath);
    }

    /**
     * Some operation with streaming input and streaming output
     *
     * @param streamingInputOutputOperationRequest
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows 'This be a stream'
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
     * @sample JsonAsyncClient.StreamingInputOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> CompletableFuture<ReturnT> streamingInputOutputOperation(
        StreamingInputOutputOperationRequest streamingInputOutputOperationRequest, AsyncRequestBody requestBody,
        AsyncResponseTransformer<StreamingInputOutputOperationResponse, ReturnT> asyncResponseTransformer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with streaming input and streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOutputOperationRequest.Builder}
     * avoiding the need to create one manually via {@link StreamingInputOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOutputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.StreamingInputOutputOperationRequest.Builder} to create
     *        a request.
     * @param requestBody
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestBody} for specific details on
     *        implementing this interface as well as links to precanned implementations for common scenarios like
     *        uploading from a file. The service documentation for the request content is as follows 'This be a stream'
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
     * @sample JsonAsyncClient.StreamingInputOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> CompletableFuture<ReturnT> streamingInputOutputOperation(
        Consumer<StreamingInputOutputOperationRequest.Builder> streamingInputOutputOperationRequest,
        AsyncRequestBody requestBody,
        AsyncResponseTransformer<StreamingInputOutputOperationResponse, ReturnT> asyncResponseTransformer) {
        return streamingInputOutputOperation(
            StreamingInputOutputOperationRequest.builder().applyMutation(streamingInputOutputOperationRequest).build(),
            requestBody, asyncResponseTransformer);
    }

    /**
     * Some operation with streaming input and streaming output
     *
     * @param streamingInputOutputOperationRequest
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
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
     * @sample JsonAsyncClient.StreamingInputOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingInputOutputOperationResponse> streamingInputOutputOperation(
        StreamingInputOutputOperationRequest streamingInputOutputOperationRequest, Path sourcePath, Path destinationPath) {
        return streamingInputOutputOperation(streamingInputOutputOperationRequest, AsyncRequestBody.fromFile(sourcePath),
                                             AsyncResponseTransformer.toFile(destinationPath));
    }

    /**
     * Some operation with streaming input and streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingInputOutputOperationRequest.Builder}
     * avoiding the need to create one manually via {@link StreamingInputOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingInputOutputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.StreamingInputOutputOperationRequest.Builder} to create
     *        a request.
     * @param sourcePath
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
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
     * @sample JsonAsyncClient.StreamingInputOutputOperation
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingInputOutputOperationResponse> streamingInputOutputOperation(
        Consumer<StreamingInputOutputOperationRequest.Builder> streamingInputOutputOperationRequest, Path sourcePath,
        Path destinationPath) {
        return streamingInputOutputOperation(
            StreamingInputOutputOperationRequest.builder().applyMutation(streamingInputOutputOperationRequest).build(),
            sourcePath, destinationPath);
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> CompletableFuture<ReturnT> streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest,
        AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> asyncResponseTransformer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest.Builder} to create a
     *        request.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> CompletableFuture<ReturnT> streamingOutputOperation(
        Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest,
        AsyncResponseTransformer<StreamingOutputOperationResponse, ReturnT> asyncResponseTransformer) {
        return streamingOutputOperation(StreamingOutputOperationRequest.builder().applyMutation(streamingOutputOperationRequest)
                                                                       .build(), asyncResponseTransformer);
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingOutputOperationResponse> streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest, Path destinationPath) {
        return streamingOutputOperation(streamingOutputOperationRequest, AsyncResponseTransformer.toFile(destinationPath));
    }

    /**
     * Some operation with a streaming output<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link StreamingOutputOperationRequest.Builder} avoiding
     * the need to create one manually via {@link StreamingOutputOperationRequest#builder()}
     * </p>
     *
     * @param streamingOutputOperationRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest.Builder} to create a
     *        request.
     * @param destinationPath
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
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
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingOutputOperationResponse> streamingOutputOperation(
        Consumer<StreamingOutputOperationRequest.Builder> streamingOutputOperationRequest, Path destinationPath) {
        return streamingOutputOperation(StreamingOutputOperationRequest.builder().applyMutation(streamingOutputOperationRequest)
                                                                       .build(), destinationPath);
    }

    @Override
    default JsonServiceClientConfiguration serviceClientConfiguration() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a {@link JsonAsyncClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static JsonAsyncClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link JsonAsyncClient}.
     */
    static JsonAsyncClientBuilder builder() {
        return new DefaultJsonAsyncClientBuilder();
    }

    /**
     * Create a default builder
     */
    static CustomBuilder builderOne() {
        return DefaultBuilder.builder().build();
    }

    /**
     * Create a default builder two
     */
    static Builder builderTwo() {
        return DefaultBuilderTwo.builder2().build();
    }
}
