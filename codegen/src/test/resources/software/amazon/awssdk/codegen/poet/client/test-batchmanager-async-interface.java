package software.amazon.awssdk.services.batchmanagertest;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestAsyncBatchManager;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;

/**
 * Service client for accessing BatchManagerTest asynchronously. This can be created using the static {@link #builder()}
 * method.
 *
 * A service that implements the batchManager() method
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
@ThreadSafe
public interface BatchManagerTestAsyncClient extends SdkClient {
    String SERVICE_NAME = "batchmanagertest";

    /**
     * Value for looking up the service's metadata from the
     * {@link software.amazon.awssdk.regions.ServiceMetadataProvider}.
     */
    String SERVICE_METADATA_ID = "batchmanagertest";

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

    /**
     * Invokes the DeleteRequest operation asynchronously.
     *
     * @param deleteRequestRequest
     * @return A Java Future containing the result of the DeleteRequest operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.DeleteRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequest"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DeleteRequestResponse> deleteRequest(DeleteRequestRequest deleteRequestRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DeleteRequest operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteRequestRequest.Builder} avoiding the need to
     * create one manually via {@link DeleteRequestRequest#builder()}
     * </p>
     *
     * @param deleteRequestRequest
     *        A {@link Consumer} that will call methods on {@link DeleteRequestRequest.Builder} to create a request.
     * @return A Java Future containing the result of the DeleteRequest operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.DeleteRequest
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequest"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DeleteRequestResponse> deleteRequest(Consumer<DeleteRequestRequest.Builder> deleteRequestRequest) {
        return deleteRequest(DeleteRequestRequest.builder().applyMutation(deleteRequestRequest).build());
    }

    /**
     * Invokes the DeleteRequestBatch operation asynchronously.
     *
     * @param deleteRequestBatchRequest
     * @return A Java Future containing the result of the DeleteRequestBatch operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.DeleteRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DeleteRequestBatchResponse> deleteRequestBatch(DeleteRequestBatchRequest deleteRequestBatchRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the DeleteRequestBatch operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link DeleteRequestBatchRequest.Builder} avoiding the
     * need to create one manually via {@link DeleteRequestBatchRequest#builder()}
     * </p>
     *
     * @param deleteRequestBatchRequest
     *        A {@link Consumer} that will call methods on {@link DeleteRequestBatchRequest.Builder} to create a
     *        request.
     * @return A Java Future containing the result of the DeleteRequestBatch operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.DeleteRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/DeleteRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<DeleteRequestBatchResponse> deleteRequestBatch(
        Consumer<DeleteRequestBatchRequest.Builder> deleteRequestBatchRequest) {
        return deleteRequestBatch(DeleteRequestBatchRequest.builder().applyMutation(deleteRequestBatchRequest).build());
    }

    /**
     * Invokes the SendRequest operation asynchronously.
     *
     * @param sendRequestRequest
     * @return A Java Future containing the result of the SendRequest operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
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
     *        A {@link Consumer} that will call methods on {@link SendRequestRequest.Builder} to create a request.
     * @return A Java Future containing the result of the SendRequest operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
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
     * Invokes the SendRequestBatch operation asynchronously.
     *
     * @param sendRequestBatchRequest
     * @return A Java Future containing the result of the SendRequestBatch operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.SendRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<SendRequestBatchResponse> sendRequestBatch(SendRequestBatchRequest sendRequestBatchRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes the SendRequestBatch operation asynchronously.<br/>
     * <p>
     * This is a convenience which creates an instance of the {@link SendRequestBatchRequest.Builder} avoiding the need
     * to create one manually via {@link SendRequestBatchRequest#builder()}
     * </p>
     *
     * @param sendRequestBatchRequest
     *        A {@link Consumer} that will call methods on {@link SendRequestBatchRequest.Builder} to create a request.
     * @return A Java Future containing the result of the SendRequestBatch operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions.
     *         <ul>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>BatchManagerTestException Base class for all service exceptions. Unknown exceptions will be thrown as
     *         an instance of this type.</li>
     *         </ul>
     * @sample BatchManagerTestAsyncClient.SendRequestBatch
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/batchmanagertest-2016-03-11/SendRequestBatch"
     *      target="_top">AWS API Documentation</a>
     */
    default CompletableFuture<SendRequestBatchResponse> sendRequestBatch(
        Consumer<SendRequestBatchRequest.Builder> sendRequestBatchRequest) {
        return sendRequestBatch(SendRequestBatchRequest.builder().applyMutation(sendRequestBatchRequest).build());
    }

    /**
     * Creates an instance of {@link BatchManagerTestAsyncBatchManager} object with the configuration set on this
     * client.
     */
    default BatchManagerTestAsyncBatchManager batchManager() {
        throw new UnsupportedOperationException();
    }
}
