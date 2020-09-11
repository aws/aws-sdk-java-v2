package software.amazon.awssdk.services.query.waiters;

import java.time.Duration;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.internal.waiters.WaiterAttribute;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.PollingStrategy;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.query.QueryClient;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
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
                                                .pollingStrategy(postOperationSuccessStrategy)
                                                .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(ignore -> true)).build();
    }

    @Override
    public WaiterResponse<APostOperationResponse> waitUntilPostOperationSuccess(APostOperationRequest aPostOperationRequest) {
        return postOperationSuccessWaiter.run(() -> client.aPostOperation(aPostOperationRequest));
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
