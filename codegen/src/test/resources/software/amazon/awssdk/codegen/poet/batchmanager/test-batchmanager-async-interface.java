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
 * Batch manager class that implements automatic batching features for a BatchManagerTest sync client. This can be
 * created using the static {@link #builder()} method.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface BatchManagerTestAsyncBatchManager extends SdkAutoCloseable {
    /**
     * Buffers outgoing SendRequestResponses on the client and sends them as a SendRequestBatchRequest to SQS. Requests
     * are batched together according to a batchKey and are sent periodically to BatchManagerTest. If the number of
     * requests for a batchKey reaches or exceeds the configured max items, then the requests are immediately flushed
     * and the timeout on the periodic flush is reset.
     *
     * @param request
     *        the outgoing SendRequestResponse
     * @return CompletableFuture of the corresponding
     *         software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse
     */
    default CompletableFuture<SendRequestResponse> sendRequest(SendRequestRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Buffers outgoing DeleteRequestResponses on the client and sends them as a DeleteRequestBatchRequest to SQS.
     * Requests are batched together according to a batchKey and are sent periodically to BatchManagerTest. If the
     * number of requests for a batchKey reaches or exceeds the configured max items, then the requests are immediately
     * flushed and the timeout on the periodic flush is reset.
     *
     * @param request
     *        the outgoing DeleteRequestResponse
     * @return CompletableFuture of the corresponding
     *         software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse
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
         * This SDK client must be closed by the caller when it is ready to be disposed. The SDK will not close the
         * client when the BatchManager is closed.
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
         * executor. If supplied by the user, this ScheduledExecutorService must be closed by the caller when it is
         * ready to be shut down.
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
