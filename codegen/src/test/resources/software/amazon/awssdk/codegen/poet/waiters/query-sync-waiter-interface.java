package software.amazon.awssdk.services.query.waiters;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.waiters.PollingStrategy;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.query.QueryClient;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Waiter utility class that polls a resource until a desired state is reached or until it is determined that the
 * resource will never enter into the desired state. This can be created using the static {@link #builder()} method
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryWaiter extends SdkAutoCloseable {
    /**
     * Polls {@link QueryClient#aPostOperation} API until the desired condition {@code PostOperationSuccess} is met, or
     * until it is determined that the resource will never enter into the desired state
     *
     * @param aPostOperationRequest
     *        the request to be used for polling
     * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
     *         condition
     */
    default WaiterResponse<APostOperationResponse> waitUntilPostOperationSuccess(APostOperationRequest aPostOperationRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * Polls {@link QueryClient#aPostOperation} API until the desired condition {@code PostOperationSuccess} is met, or
     * until it is determined that the resource will never enter into the desired state.
     * <p>
     * This is a convenience method to create an instance of the request builder without the need to create one manually
     * using {@link APostOperationRequest.builder()}
     *
     * @param aPostOperationRequest
     *        the request to be used for polling
     * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
     *         condition
     */
    default WaiterResponse<APostOperationResponse> waitUntilPostOperationSuccess(
        Consumer<APostOperationRequest.Builder> aPostOperationRequest) {
        return waitUntilPostOperationSuccess(APostOperationRequest.builder().applyMutation(aPostOperationRequest).build());
    }

    /**
     * Create a builder that can be used to configure and create a {@link QueryWaiter}.
     *
     * @return a builder
     */
    static Builder builder() {
        return DefaultQueryWaiter.builder();
    }

    interface Builder {
        /**
         * Defines a {@link PollingStrategy} to use when polling a resource
         *
         * @param pollingStrategy
         *        the polling strategy to set
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder pollingStrategy(PollingStrategy pollingStrategy);

        /**
         * This is a convenient method to pass the configuration of the {@link PollingStrategy} without the need to
         * create an instance manually via {@link PollingStrategy.builder()}
         *
         * @param pollingStrategy
         *        the polling strategy to set
         * @return a reference to this object so that method calls can be chained together.
         * @see #pollingStrategy(PollingStrategy)
         */
        default Builder pollingStrategy(Consumer<PollingStrategy.Builder> pollingStrategy) {
            PollingStrategy.Builder builder = PollingStrategy.builder();
            pollingStrategy.accept(builder);
            return pollingStrategy(builder.build());
        }

        /**
         * Sets a custom {@link QueryClient} that will be used to pool the resource
         * <p>
         * This SDK client must be closed by the caller when it is ready to be disposed. The SDK will not close the
         * client when the waiter is closed
         *
         * @param client
         *        the client to send the request
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder client(QueryClient client);

        /**
         * Builds an instance of {@link QueryWaiter} based on the configurations supplied to this builder
         *
         * @return An initialized {@link QueryWaiter}
         */
        QueryWaiter build();
    }
}
