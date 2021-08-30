package software.amazon.awssdk.services.batchmanagertest.batchmanager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.batchmanagertest.BatchManagerTestAsyncClient;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.internal.DefaultBatchManagerTestAsyncBatchManager;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Batch manager class that implements automatic batching features for a BatchManagerTest async client. This can be
 * created using the static {@link #builder()} method.
 * <p>
 * The batch manager's automatic batching features allows for request batching using client-side buffering. This means
 * that calls made from the client are first buffered and then sent as a batch request to the service. Client side
 * buffering allows buffering a number of requests up to a service or user defined limit before being sent as a batch
 * request. Outgoing calls are also periodically flushed after a defined period of time if batch requests do not reach
 * the defined batch size limit.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface BatchManagerTestAsyncBatchManager extends SdkAutoCloseable {
    /**
     * Buffers outgoing {@link SendRequestRequest}s on the client and sends them as a
     * {@link software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchRequest} to BatchManagerTest.
     * Requests are batched together according to a batchKey calculated from the request's destination and
     * overrideConfiguration which are then sent periodically to BatchManagerTest. If the number of requests for a
     * batchKey reaches or exceeds the configured max items, then the requests are immediately flushed and the timeout
     * on the periodic flush is reset.
     *
     * @param request
     *        the outgoing SendRequestRequest
     * @return CompletableFuture of the corresponding {@link SendRequestResponse}
     */
    default CompletableFuture<SendRequestResponse> sendRequest(SendRequestRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Buffers outgoing {@link DeleteRequestRequest}s on the client and sends them as a
     * {@link software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchRequest} to BatchManagerTest.
     * Requests are batched together according to a batchKey calculated from the request's destination and
     * overrideConfiguration which are then sent periodically to BatchManagerTest. If the number of requests for a
     * batchKey reaches or exceeds the configured max items, then the requests are immediately flushed and the timeout
     * on the periodic flush is reset.
     *
     * @param request
     *        the outgoing DeleteRequestRequest
     * @return CompletableFuture of the corresponding {@link DeleteRequestResponse}
     */
    default CompletableFuture<DeleteRequestResponse> deleteRequest(DeleteRequestRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a builder that can be used to configure and create a {@link BatchManagerTestAsyncBatchManager}.
     *
     * @return a builder
     */
    static Builder builder() {
        return DefaultBatchManagerTestAsyncBatchManager.builder();
    }

    interface Builder {
        /**
         * Defines overrides to the default BatchManager configuration.
         *
         * @param overrideConfiguration
         *        the override configuration to set
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfiguration);

        /**
         * Sets a custom {@link software.amazon.awssdk.services.batchmanagertest.BatchManagerTestClient} that will be
         * used to poll the resource.
         * <p>
         * This client must be closed by the caller when it is ready to be disposed. The SDK will not close the client
         * when the BatchManager is closed.
         *
         * @param client
         *        the client used to send and receive batch messages.
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder client(BatchManagerTestAsyncClient client);

        /**
         * Sets a custom {@link ScheduledExecutorService} that will be used to schedule periodic buffer flushes.
         * <p>
         * Creating a BatchManagerTestAsyncBatchManager directly from the client will use the client's scheduled
         * executor. If supplied by the user, this {@link ScheduledExecutorService} must be closed by the caller when it
         * is ready to be shut down.
         *
         * @param scheduledExecutor
         *        the scheduledExecutor to be used
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder scheduledExecutor(ScheduledExecutorService scheduledExecutor);

        /**
         * Builds an instance of {@link BatchManagerTestAsyncBatchManager} based on the configurations supplied to this
         * builder
         *
         * @return An initialized {@link BatchManagerTestAsyncBatchManager}
         */
        BatchManagerTestAsyncBatchManager build();
    }
}
