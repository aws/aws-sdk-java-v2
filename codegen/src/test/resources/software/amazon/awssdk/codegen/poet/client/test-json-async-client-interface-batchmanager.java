package software.amazon.awssdk.services.batchmanagertest;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestAsyncBatchManager;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;

/**
 * Service client for accessing BatchManagerTest asynchronously. This can be created using the static {@link #builder()}
 * method.The asynchronous client performs non-blocking I/O when configured with any {@code SdkAsyncHttpClient}
 * supported in the SDK. However, full non-blocking is not guaranteed as the async client may perform blocking calls in
 * some cases such as credentials retrieval and endpoint discovery as part of the async API call.
 *
 * A service that implements the batchManager() method
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface BatchManagerTestAsyncClient extends AwsClient {
    String SERVICE_NAME = "batchmanagertest";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "batchmanagertest";

    /**
     * Invokes the SendRequest operation asynchronously.
     *
     * @param sendRequestRequest
     * @return A Java Future containing the result of the SendRequest operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.SendRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequest" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<SendRequestResponse> sendRequest(SendRequestRequest sendRequestRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the SendRequest operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SendRequestRequest.Builder} avoiding the need to
     * create one manually via {@link SendRequestRequest#builder()}
     * </p>
     *
     * @param sendRequestRequest
     *        A {@link Consumer} that will call methods on
     *        {@link software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the SendRequest operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.SendRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequest" target="_top">AWS
     *      API Documentation</a>
     */
    default CompletableFuture<SendRequestResponse> sendRequest(Consumer<SendRequestRequest.Builder> sendRequestRequest) {
        return sendRequest(SendRequestRequest.builder().applyMutation(sendRequestRequest).build());
    }

    /**
     * Creates an instance of {@link BatchManagerTestAsyncBatchManager} object with the configuration set on this
     * client.
     */
    default BatchManagerTestAsyncBatchManager batchManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    default BatchManagerTestServiceClientConfiguration serviceClientConfiguration() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a {@link BatchManagerTestAsyncClient} with the region loaded from the
     * {@link software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain} and credentials loaded from the
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider}.
     */
    static BatchManagerTestAsyncClient create() {
        return builder().build();
    }

    /**
     * Create a builder that can be used to configure and create a {@link BatchManagerTestAsyncClient}.
     */
    static BatchManagerTestAsyncClientBuilder builder() {
        return new DefaultBatchManagerTestAsyncClientBuilder();
    }
}