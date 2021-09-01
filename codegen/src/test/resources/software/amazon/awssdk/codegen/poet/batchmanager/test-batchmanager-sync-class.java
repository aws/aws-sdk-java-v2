package software.amazon.awssdk.services.batchmanagertest.batchmanager.internal;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.batchmanager.BatchManager;
import software.amazon.awssdk.core.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.batchmanagertest.BatchManagerTestClient;
import software.amazon.awssdk.services.batchmanagertest.batchmanager.BatchManagerTestBatchManager;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.DeleteRequestResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestBatchResponse;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestRequest;
import software.amazon.awssdk.services.batchmanagertest.model.SendRequestResponse;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultBatchManagerTestBatchManager implements BatchManagerTestBatchManager {
    private boolean createdExecutor;

    private final Executor executor;

    private final BatchManagerTestClient client;

    private final BatchManager<SendRequestRequest, SendRequestResponse, SendRequestBatchResponse> sendRequestBatchManager;

    private final BatchManager<DeleteRequestRequest, DeleteRequestResponse, DeleteRequestBatchResponse> deleteRequestBatchManager;

    private DefaultBatchManagerTestBatchManager(DefaultBuilder builder) {
        this.client = builder.client;
        ScheduledExecutorService scheduledExecutor = builder.scheduledExecutor;
        if (builder.executor == null) {
            this.executor = createDefaultExecutor();
            this.createdExecutor = true;
        } else {
            this.executor = builder.executor;
            this.createdExecutor = false;
        }
        this.sendRequestBatchManager = BatchManager
            .builder(SendRequestRequest.class, SendRequestResponse.class, SendRequestBatchResponse.class)
            .batchFunction(BatchManagerTestBatchFunctions.sendRequestBatchFunction(client, executor))
            .responseMapper(BatchManagerTestBatchFunctions.sendRequestResponseMapper())
            .batchKeyMapper(BatchManagerTestBatchFunctions.sendRequestBatchKeyMapper())
            .overrideConfiguration(sendRequestConfig(builder.overrideConfiguration)).scheduledExecutor(scheduledExecutor)
            .build();
        this.deleteRequestBatchManager = BatchManager
            .builder(DeleteRequestRequest.class, DeleteRequestResponse.class, DeleteRequestBatchResponse.class)
            .batchFunction(BatchManagerTestBatchFunctions.deleteRequestBatchFunction(client, executor))
            .responseMapper(BatchManagerTestBatchFunctions.deleteRequestResponseMapper())
            .batchKeyMapper(BatchManagerTestBatchFunctions.deleteRequestBatchKeyMapper())
            .overrideConfiguration(deleteRequestConfig(builder.overrideConfiguration)).scheduledExecutor(scheduledExecutor)
            .build();
    }

    @SdkInternalApi
    DefaultBatchManagerTestBatchManager(BatchManagerTestClient client,
                                        BatchManager<SendRequestRequest, SendRequestResponse, SendRequestBatchResponse> sendRequestBatchManager,
                                        BatchManager<DeleteRequestRequest, DeleteRequestResponse, DeleteRequestBatchResponse> deleteRequestBatchManager,
                                        Executor executor, boolean createdExecutor) {
        this.sendRequestBatchManager = sendRequestBatchManager;
        this.deleteRequestBatchManager = deleteRequestBatchManager;
        this.client = client;
        this.executor = executor;
        this.createdExecutor = createdExecutor;
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
        if (createdExecutor && executor instanceof ExecutorService) {
            ExecutorService executorService = (ExecutorService) executor;
            executorService.shutdownNow();
        }
    }

    public static BatchManagerTestBatchManager.Builder builder() {
        return new DefaultBuilder();
    }

    private Executor createDefaultExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("sdk-BatchManagerTest-batchmanager").build();
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(8, processors);
        int maxPoolSize = Math.max(64, processors * 2);
        ThreadPoolExecutor defaultExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 10, TimeUnit.SECONDS,
                                                                    new LinkedBlockingQueue<>(1_000), threadFactory);
        // Allow idle core threads to time out
        defaultExecutor.allowCoreThreadTimeOut(true);
        return defaultExecutor;
    }

    public static final class DefaultBuilder implements BatchManagerTestBatchManager.Builder {
        private BatchManagerTestClient client;

        private BatchOverrideConfiguration overrideConfiguration;

        private ScheduledExecutorService scheduledExecutor;

        Executor executor;

        private DefaultBuilder() {
        }

        @Override
        public BatchManagerTestBatchManager.Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public BatchManagerTestBatchManager.Builder overrideConfiguration(BatchOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public BatchManagerTestBatchManager.Builder client(BatchManagerTestClient client) {
            this.client = client;
            return this;
        }

        @Override
        public BatchManagerTestBatchManager.Builder scheduledExecutor(ScheduledExecutorService scheduledExecutor) {
            this.scheduledExecutor = scheduledExecutor;
            return this;
        }

        public BatchManagerTestBatchManager build() {
            return new DefaultBatchManagerTestBatchManager(this);
        }
    }
}
