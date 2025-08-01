package software.amazon.awssdk.services.json;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.services.json.model.APostOperationRequest;
import software.amazon.awssdk.services.json.model.APostOperationResponse;
import software.amazon.awssdk.services.json.presignedurl.AsyncPresignedUrlExtension;

/**
 * Service client for accessing Json Service asynchronously. This can be created using the static {@link #builder()}
 * method.The asynchronous client performs non-blocking I/O when configured with any {@code SdkAsyncHttpClient}
 * supported in the SDK. However, full non-blocking is not guaranteed as the async client may perform blocking calls in
 * some cases such as credentials retrieval and endpoint discovery as part of the async API call.
 *
 * A service that is implemented using the json protocol
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
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param aPostOperationRequest
     * @return A Java Future containing the result of the APostOperation operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
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
     */
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
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
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
     */
    default CompletableFuture<APostOperationResponse> aPostOperation(Consumer<APostOperationRequest.Builder> aPostOperationRequest) {
        return aPostOperation(APostOperationRequest.builder().applyMutation(aPostOperationRequest).build());
    }

    /**
     * Creates an instance of {@link AsyncPresignedUrlExtension} object with the configuration set on this client.
     */
    default AsyncPresignedUrlExtension presignedUrlExtension() {
        throw new UnsupportedOperationException();
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
}
