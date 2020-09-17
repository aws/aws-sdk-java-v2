package software.amazon.awssdk.services.query.waiters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.internal.waiters.WaiterAttribute;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.PollingStrategy;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.services.query.QueryClient;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.services.query.waiters.internal.WaitersRuntime;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
@ThreadSafe
final class DefaultQueryWaiter implements QueryWaiter {
    private static final WaiterAttribute<SdkAutoCloseable> CLIENT_ATTRIBUTE = new WaiterAttribute<>(SdkAutoCloseable.class);

    private final QueryClient client;

    private final AttributeMap managedResources;

    private final Waiter<APostOperationResponse> postOperationSuccessWaiter;

    private DefaultQueryWaiter(DefaultBuilder builder) {
        AttributeMap.Builder attributeMapBuilder = AttributeMap.builder();
        if (builder.client == null) {
            this.client = QueryClient.builder().build();
            attributeMapBuilder.put(CLIENT_ATTRIBUTE, this.client);
        } else {
            this.client = builder.client;
        }
        managedResources = attributeMapBuilder.build();
        PollingStrategy postOperationSuccessStrategy = builder.pollingStrategy == null ? PollingStrategy.builder()
                                                                                                        .maxAttempts(40).backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1))).build()
                                                                                       : builder.pollingStrategy;
        this.postOperationSuccessWaiter = Waiter.builder(APostOperationResponse.class)
                                                .pollingStrategy(postOperationSuccessStrategy).acceptors(postOperationSuccessWaiterAcceptors()).build();
    }

    private static String errorCode(Throwable error) {
        if (error instanceof AwsServiceException) {
            return ((AwsServiceException) error).awsErrorDetails().errorCode();
        }
        return null;
    }

    @Override
    public WaiterResponse<APostOperationResponse> waitUntilPostOperationSuccess(APostOperationRequest aPostOperationRequest) {
        return postOperationSuccessWaiter.run(() -> client.aPostOperation(aPostOperationRequest));
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

    public static QueryWaiter.Builder builder() {
        return new DefaultBuilder();
    }

    public static final class DefaultBuilder implements QueryWaiter.Builder {
        private QueryClient client;

        private PollingStrategy pollingStrategy;

        private DefaultBuilder() {
        }

        @Override
        public QueryWaiter.Builder pollingStrategy(PollingStrategy pollingStrategy) {
            this.pollingStrategy = pollingStrategy;
            return this;
        }

        @Override
        public QueryWaiter.Builder client(QueryClient client) {
            this.client = client;
            return this;
        }

        public QueryWaiter build() {
            return new DefaultQueryWaiter(this);
        }
    }
}
