package software.amazon.awssdk.services.json;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Generated;
import software.amazon.awssdk.core.async.AsyncRequestProvider;
import software.amazon.awssdk.core.async.AsyncResponseHandler;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.json.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersRequest;
import software.amazon.awssdk.services.json.model.GetWithoutRequiredMembersResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithResultKeyResponse;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyRequest;
import software.amazon.awssdk.services.json.model.PaginatedOperationWithoutResultKeyResponse;
import software.amazon.awssdk.services.json.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingInputOperationResponse;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.json.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Service client for accessing Json Service asynchronously. This can be created using the static {@link #builder()}
 * method.
 *
 * A service that is implemented using the query protocol
 */
@Generated("software.amazon.awssdk:codegen")
public interface JsonAsyncClient extends SdkAutoCloseable {
    /**
     * Create a {@link JsonAsyncClient} with the region loaded from the
     * {@link software.amazon.awssdk.core.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from
     * the {@link software.amazon.awssdk.core.auth.DefaultCredentialsProvider}.
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<APostOperationResponse> aPostOperation(APostOperationRequest aPostOperationRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     * <br/>
     * This is a convenience which creates an instance of the {@link APostOperationRequest.Builder} avoiding the need to
     * create one manually via {@link APostOperationRequest#builder()}
     *
     * @param aPostOperationRequest
     *        a {@link Consumer} that will call methods on {@link APostOperationRequest.Builder}.
     * @return A Java Future containing the result of the APostOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<APostOperationResponse> aPostOperation(Consumer<APostOperationRequest.Builder> aPostOperationRequest) {
        return aPostOperation(APostOperationRequest.builder().apply(aPostOperationRequest).build());
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
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
     * This is a convenience which creates an instance of the {@link APostOperationWithOutputRequest.Builder} avoiding
     * the need to create one manually via {@link APostOperationWithOutputRequest#builder()}
     *
     * @param aPostOperationWithOutputRequest
     *        a {@link Consumer} that will call methods on {@link APostOperationWithOutputRequest.Builder}.
     * @return A Java Future containing the result of the APostOperationWithOutput operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<APostOperationWithOutputResponse> aPostOperationWithOutput(
        Consumer<APostOperationWithOutputRequest.Builder> aPostOperationWithOutputRequest) {
        return aPostOperationWithOutput(APostOperationWithOutputRequest.builder().apply(aPostOperationWithOutputRequest).build());
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.GetWithoutRequiredMembers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
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
     *
     * @return A Java Future containing the result of the GetWithoutRequiredMembers operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.GetWithoutRequiredMembers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<GetWithoutRequiredMembersResponse> getWithoutRequiredMembers() {
        return getWithoutRequiredMembers(GetWithoutRequiredMembersRequest.builder().build());
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     * <br/>
     * This is a convenience which creates an instance of the {@link GetWithoutRequiredMembersRequest.Builder} avoiding
     * the need to create one manually via {@link GetWithoutRequiredMembersRequest#builder()}
     *
     * @param getWithoutRequiredMembersRequest
     *        a {@link Consumer} that will call methods on {@link GetWithoutRequiredMembersRequest.Builder}.
     * @return A Java Future containing the result of the GetWithoutRequiredMembers operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>InvalidInputException The request was rejected because an invalid or out-of-range value was supplied
     *         for an input parameter.</li>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.GetWithoutRequiredMembers
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/GetWithoutRequiredMembers"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<GetWithoutRequiredMembersResponse> getWithoutRequiredMembers(
        Consumer<GetWithoutRequiredMembersRequest.Builder> getWithoutRequiredMembersRequest) {
        return getWithoutRequiredMembers(GetWithoutRequiredMembersRequest.builder().apply(getWithoutRequiredMembersRequest)
                                                                         .build());
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithResultKeyResponse> paginatedOperationWithResultKey(
        PaginatedOperationWithResultKeyRequest paginatedOperationWithResultKeyRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation with result_key in paginators.json file<br/>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithResultKeyRequest#builder()}
     *
     * @param paginatedOperationWithResultKeyRequest
     *        a {@link Consumer} that will call methods on {@link PaginatedOperationWithResultKeyRequest.Builder}.
     * @return A Java Future containing the result of the PaginatedOperationWithResultKey operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithResultKeyResponse> paginatedOperationWithResultKey(
        Consumer<PaginatedOperationWithResultKeyRequest.Builder> paginatedOperationWithResultKeyRequest) {
        return paginatedOperationWithResultKey(PaginatedOperationWithResultKeyRequest.builder()
                                                                                     .apply(paginatedOperationWithResultKeyRequest).build());
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
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithoutResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithoutResultKeyResponse> paginatedOperationWithoutResultKey(
        PaginatedOperationWithoutResultKeyRequest paginatedOperationWithoutResultKeyRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some paginated operation without result_key in paginators.json file<br/>
     * This is a convenience which creates an instance of the {@link PaginatedOperationWithoutResultKeyRequest.Builder}
     * avoiding the need to create one manually via {@link PaginatedOperationWithoutResultKeyRequest#builder()}
     *
     * @param paginatedOperationWithoutResultKeyRequest
     *        a {@link Consumer} that will call methods on {@link PaginatedOperationWithoutResultKeyRequest.Builder}.
     * @return A Java Future containing the result of the PaginatedOperationWithoutResultKey operation returned by the
     *         service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.PaginatedOperationWithoutResultKey
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/PaginatedOperationWithoutResultKey"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<PaginatedOperationWithoutResultKeyResponse> paginatedOperationWithoutResultKey(
        Consumer<PaginatedOperationWithoutResultKeyRequest.Builder> paginatedOperationWithoutResultKeyRequest) {
        return paginatedOperationWithoutResultKey(PaginatedOperationWithoutResultKeyRequest.builder()
                                                                                           .apply(paginatedOperationWithoutResultKeyRequest).build());
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param requestProvider
     *        Functional interface that can be implemented to produce the request content in a non-blocking manner. The
     *        size of the content is expected to be known up front. See {@link AsyncRequestProvider} for specific
     *        details on implementing this interface as well as links to precanned implementations for common scenarios
     *        like uploading from a file. The service documentation for the request content is as follows 'This be a
     *        stream'
     * @return A Java Future containing the result of the StreamingInputOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, AsyncRequestProvider requestProvider) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming input
     *
     * @param streamingInputOperationRequest
     * @param path
     *        {@link Path} to file containing data to send to the service. File will be read entirely and may be read
     *        multiple times in the event of a retry. If the file does not exist or the current user does not have
     *        access to read it then an exception will be thrown. The service documentation for the request content is
     *        as follows 'This be a stream'
     * @return A Java Future containing the result of the StreamingInputOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingInputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingInputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingInputOperationResponse> streamingInputOperation(
        StreamingInputOperationRequest streamingInputOperationRequest, Path path) {
        return streamingInputOperation(streamingInputOperationRequest, AsyncRequestProvider.fromFile(path));
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param asyncResponseHandler
     *        The response handler for processing the streaming response in a non-blocking manner. See
     *        {@link AsyncResponseHandler} for details on how this callback should be implemented and for links to
     *        precanned implementations for common scenarios like downloading to a file. The service documentation for
     *        the response content is as follows 'This be a stream'.
     * @return A future to the transformed result of the AsyncResponseHandler.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default <ReturnT> CompletableFuture<ReturnT> streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest,
        AsyncResponseHandler<StreamingOutputOperationResponse, ReturnT> asyncResponseHandler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Some operation with a streaming output
     *
     * @param streamingOutputOperationRequest
     * @param path
     *        {@link Path} to file that response contents will be written to. The file must not exist or this method
     *        will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *        The service documentation for the response content is as follows 'This be a stream'.
     * @return A future to the transformed result of the AsyncResponseHandler.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkBaseException Base class for all exceptions that can be thrown by the SDK (both service and
     *         client). Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>JsonException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample JsonAsyncClient.StreamingOutputOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/json-service-2010-05-08/StreamingOutputOperation"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<StreamingOutputOperationResponse> streamingOutputOperation(
        StreamingOutputOperationRequest streamingOutputOperationRequest, Path path) {
        return streamingOutputOperation(streamingOutputOperationRequest, AsyncResponseHandler.toFile(path));
    }
}
