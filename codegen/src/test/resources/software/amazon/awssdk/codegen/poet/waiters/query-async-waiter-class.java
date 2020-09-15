package software.amazon.awssdk.services.query.waiters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.internal.waiters.WaiterAttribute;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.AsyncWaiter;
import software.amazon.awssdk.core.waiters.PollingStrategy;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.core.waiters.WaitersRuntime;
import software.amazon.awssdk.services.query.QueryAsyncClient;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
@ThreadSafe
final class DefaultQueryAsyncWaiter implements QueryAsyncWaiter {
    private static final WaiterAttribute<SdkAutoCloseable> CLIENT_ATTRIBUTE = new WaiterAttribute<>(SdkAutoCloseable.class);

    private static final WaiterAttribute<ScheduledExecutorService> SCHEDULED_EXECUTOR_SERVICE_ATTRIBUTE = new WaiterAttribute<>(
        ScheduledExecutorService.class);

    private final QueryAsyncClient client;

    private final AttributeMap managedResources;

    private final AsyncWaiter<APostOperationResponse> postOperationSuccessWaiter;

    private final ScheduledExecutorService executorService;

    private DefaultQueryAsyncWaiter(DefaultBuilder builder) {
        AttributeMap.Builder attributeMapBuilder = AttributeMap.builder();
        if (builder.client == null) {
            this.client = QueryAsyncClient.builder().build();
            attributeMapBuilder.put(CLIENT_ATTRIBUTE, this.client);
        } else {
            this.client = builder.client;
        }
        if (builder.executorService == null) {
            this.executorService = Executors.newScheduledThreadPool(5,
                                                                    new ThreadFactoryBuilder().threadNamePrefix("waiters-ScheduledExecutor").build());
            attributeMapBuilder.put(SCHEDULED_EXECUTOR_SERVICE_ATTRIBUTE, this.executorService);
        } else {
            this.executorService = builder.executorService;
        }
        managedResources = attributeMapBuilder.build();
        PollingStrategy postOperationSuccessStrategy = builder.pollingStrategy == null ? PollingStrategy.builder()
                                                                                                        .maxAttempts(40).backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1))).build()
                                                                                       : builder.pollingStrategy;
        this.postOperationSuccessWaiter = AsyncWaiter.builder(APostOperationResponse.class)
                                                     .pollingStrategy(postOperationSuccessStrategy).acceptors(postOperationSuccessWaiterAcceptors())
                                                     .scheduledExecutorService(executorService).build();
    }

    private static String errorCode(Throwable error) {
        if (error instanceof AwsServiceException) {
            return ((AwsServiceException) error).awsErrorDetails().errorCode();
        }
        return null;
    }

    @Override
    public CompletableFuture<WaiterResponse<APostOperationResponse>> waitUntilPostOperationSuccess(
        APostOperationRequest aPostOperationRequest) {
        return postOperationSuccessWaiter.runAsync(() -> client.aPostOperation(aPostOperationRequest));
    }

    private static List<WaiterAcceptor<? super APostOperationResponse>> postOperationSuccessWaiterAcceptors() {
        List<WaiterAcceptor<? super APostOperationResponse>> result = new ArrayList<>();
        result.add(new WaitersRuntime.ResponseStatusAcceptor(200, WaiterState.SUCCESS));
        result.add(new WaitersRuntime.ResponseStatusAcceptor(404, WaiterState.RETRY));
        result.add(WaiterAcceptor.successOnResponseAcceptor(response -> {
            WaitersRuntime.Value input = new WaitersRuntime.Value(response);
            List<Object> resultValues = input.field("foo").field("bar").values();
            return !resultValues.isEmpty() && resultValues.stream().anyMatch(v -> Objects.equals(v, "baz"));
        }));
        result.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return result;
    }

    @Override
    public void close() {
        managedResources.close();
    }

    public static QueryAsyncWaiter.Builder builder() {
        return new DefaultBuilder();
    }

    public static final class DefaultBuilder implements QueryAsyncWaiter.Builder {
        private QueryAsyncClient client;

        private PollingStrategy pollingStrategy;

        private ScheduledExecutorService executorService;

        private DefaultBuilder() {
        }

        @Override
        public QueryAsyncWaiter.Builder executorService(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        @Override
        public QueryAsyncWaiter.Builder pollingStrategy(PollingStrategy pollingStrategy) {
            this.pollingStrategy = pollingStrategy;
            return this;
        }

        @Override
        public QueryAsyncWaiter.Builder client(QueryAsyncClient client) {
            this.client = client;
            return this;
        }

        public QueryAsyncWaiter build() {
            return new DefaultQueryAsyncWaiter(this);
        }
    }
}
