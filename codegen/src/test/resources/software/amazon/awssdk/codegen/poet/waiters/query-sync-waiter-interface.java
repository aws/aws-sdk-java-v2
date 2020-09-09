package software.amazon.awssdk.services.query.waiters;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.waiters.PollingStrategy;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.query.QueryClient;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;

/**
 * Waiter utility class that polls a resource until a desired state is reached or until it is determined that the
 * resource will never enter into the desired state. This can be created using the static {@link #builder()} method
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryWaiter {
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
     * Create a builder that can be used to configure and create a {@link QueryWaiter}.
     *
     * @return a builder
     */
    static void builder() {
        throw new UnsupportedOperationException();
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
         * Defines the {@link QueryClient} to use when polling a resource
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
