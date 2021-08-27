package software.amazon.awssdk.services.batchmanagertest.batchmanager.internal;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.batchmanagertest.BatchManagerTestAsyncClient;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestAsyncBatchManager;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultBatchManagerTestAsyncBatchManager implements BatchManagerTestAsyncBatchManager {
    private final BatchManagerTestAsyncClient client;

    private final BatchManager<SendRequestRequest, SendRequestResponse, SendRequestBatchResponse> sendRequestBatchManager;

    private final BatchManager<DeleteRequestRequest, DeleteRequestResponse, DeleteRequestBatchResponse> deleteRequestBatchManager;

    private DefaultBatchManagerTestAsyncBatchManager(DefaultBuilder builder) {
        this.client = builder.client;
        ScheduledExecutorService scheduledExecutor = builder.scheduledExecutor;
        this.sendRequestBatchManager = BatchManager
            .builder(SendRequestRequest.class, SendRequestResponse.class, SendRequestBatchResponse.class)
            .batchFunction(BatchManagerTestBatchFunctions.sendRequestBatchAsyncFunction(client))
            .responseMapper(BatchManagerTestBatchFunctions.sendRequestResponseMapper())
            .batchKeyMapper(BatchManagerTestBatchFunctions.sendRequestBatchKeyMapper())
            .overrideConfiguration(sendRequestConfig(builder.overrideConfiguration)).scheduledExecutor(scheduledExecutor)
            .build();
        this.deleteRequestBatchManager = BatchManager
            .builder(DeleteRequestRequest.class, DeleteRequestResponse.class, DeleteRequestBatchResponse.class)
            .batchFunction(BatchManagerTestBatchFunctions.deleteRequestBatchAsyncFunction(client))
            .responseMapper(BatchManagerTestBatchFunctions.deleteRequestResponseMapper())
            .batchKeyMapper(BatchManagerTestBatchFunctions.deleteRequestBatchKeyMapper())
            .overrideConfiguration(deleteRequestConfig(builder.overrideConfiguration)).scheduledExecutor(scheduledExecutor)
            .build();
    }

    @SdkInternalApi
    public DefaultBatchManagerTestAsyncBatchManager(BatchManagerTestAsyncClient client,
                                                    BatchManager<SendRequestRequest, SendRequestResponse, SendRequestBatchResponse> sendRequestBatchManager,
                                                    BatchManager<DeleteRequestRequest, DeleteRequestResponse, DeleteRequestBatchResponse> deleteRequestBatchManager) {
        this.sendRequestBatchManager = sendRequestBatchManager;
        this.deleteRequestBatchManager = deleteRequestBatchManager;
        this.client = client;
    }

    @Override
    public CompletableFuture<SendRequestResponse> sendRequest(SendRequestRequest request) {
        return sendRequestBatchManager.sendRequest(request);
    }

    @Override
    public CompletableFuture<DeleteRequestResponse> deleteRequest(DeleteRequestRequest request) {
        return deleteRequestBatchManager.sendRequest(request);
    }

    private BatchOverrideConfiguration sendRequestConfig(BatchOverrideConfiguration overrideConfiguration) {
        BatchOverrideConfiguration.Builder config = BatchOverrideConfiguration.builder();
        if (overrideConfiguration == null) {
            config.maxBatchItems(15);
            config.maxBatchOpenInMs(Duration.ofMillis(200));
        } else {
            config.maxBatchItems(overrideConfiguration.maxBatchItems().orElse(15));
            config.maxBatchOpenInMs(overrideConfiguration.maxBatchOpenInMs().orElse(Duration.ofMillis(200)));
        }
        return config.build();
    }

    private BatchOverrideConfiguration deleteRequestConfig(BatchOverrideConfiguration overrideConfiguration) {
        BatchOverrideConfiguration.Builder config = BatchOverrideConfiguration.builder();
        if (overrideConfiguration == null) {
            config.maxBatchItems(10);
            config.maxBatchOpenInMs(Duration.ofMillis(100));
        } else {
            config.maxBatchItems(overrideConfiguration.maxBatchItems().orElse(10));
            config.maxBatchOpenInMs(overrideConfiguration.maxBatchOpenInMs().orElse(Duration.ofMillis(100)));
        }
        return config.build();
    }

    @Override
    public void close() {
        sendRequestBatchManager.close();
        deleteRequestBatchManager.close();
    }

    public static BatchManagerTestAsyncBatchManager.Builder builder() {
        return new DefaultBuilder();
    }

    public static final class DefaultBuilder implements BatchManagerTestAsyncBatchManager.Builder {
        private BatchManagerTestAsyncClient client;

        private BatchOverrideConfiguration overrideConfiguration;

        private ScheduledExecutorService scheduledExecutor;

        private DefaultBuilder() {
        }

        @Override
        public BatchManagerTestAsyncBatchManager.Builder overrideConfiguration(BatchOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public BatchManagerTestAsyncBatchManager.Builder client(BatchManagerTestAsyncClient client) {
            this.client = client;
            return this;
        }

        @Override
        public BatchManagerTestAsyncBatchManager.Builder scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        public BatchManagerTestAsyncBatchManager build() {
            return new DefaultBatchManagerTestAsyncBatchManager(this);
        }
    }
}
