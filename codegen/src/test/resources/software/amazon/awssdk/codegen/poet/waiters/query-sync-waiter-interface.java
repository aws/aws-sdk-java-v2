package software.amazon.awssdk.services.query.waiters;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
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
     * using {@link APostOperationRequest#builder()}
     *
     * @param aPostOperationRequest
     *        The consumer that will configure the request to be used for polling
     * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
     *         condition
     */
    default WaiterResponse<APostOperationResponse> waitUntilPostOperationSuccess(
        Consumer<APostOperationRequest.Builder> aPostOperationRequest) {
        return waitUntilPostOperationSuccess(APostOperationRequest.builder().applyMutation(aPostOperationRequest).build());
    }

    /**
     * Polls {@link QueryClient#aPostOperation} API until the desired condition {@code PostOperationSuccess} is met, or
     * until it is determined that the resource will never enter into the desired state
     *
     * @param aPostOperationRequest
     *        The request to be used for polling
     * @param overrideConfig
     *        Per request override configuration for waiters
     * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
     *         condition
     */
    default WaiterResponse<APostOperationResponse> waitUntilPostOperationSuccess(APostOperationRequest aPostOperationRequest,
                                                                                 WaiterOverrideConfiguration overrideConfig) {
        throw new UnsupportedOperationException();
    }

    /**
     * Polls {@link QueryClient#aPostOperation} API until the desired condition {@code PostOperationSuccess} is met, or
     * until it is determined that the resource will never enter into the desired state.
     * <p>
     * This is a convenience method to create an instance of the request builder and instance of the override config
     * builder
     *
     * @param aPostOperationRequest
     *        The consumer that will configure the request to be used for polling
     * @param overrideConfig
     *        The consumer that will configure the per request override configuration for waiters
     * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
     *         condition
     */
    default WaiterResponse<APostOperationResponse> waitUntilPostOperationSuccess(
        Consumer<APostOperationRequest.Builder> aPostOperationRequest,
        Consumer<WaiterOverrideConfiguration.Builder> overrideConfig) {
        return waitUntilPostOperationSuccess(APostOperationRequest.builder().applyMutation(aPostOperationRequest).build(),
                                             WaiterOverrideConfiguration.builder().applyMutation(overrideConfig).build());
    }

    /**
     * Create a builder that can be used to configure and create a {@link QueryWaiter}.
     *
     * @return a builder
     */
    static Builder builder() {
        return DefaultQueryWaiter.builder();
    }

    /**
     * Create an instance of {@link QueryWaiter} with the default configuration.
     * <p>
     * <b>A default {@link QueryClient} will be created to poll resources. It is recommended to share a single instance
     * of the waiter created via this method. If it is not desirable to share a waiter instance, invoke {@link #close()}
     * to release the resources once the waiter is not needed.</b>
     *
     * @return an instance of {@link QueryWaiter}
     */
    static QueryWaiter create() {
        return DefaultQueryWaiter.builder().build();
    }

    interface Builder {
        /**
         * Defines overrides to the default SDK waiter configuration that should be used for waiters created from this
         * builder
         *
         * @param overrideConfiguration
         *        the override configuration to set
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);

        /**
         * This is a convenient method to pass the override configuration without the need to create an instance
         * manually via {@link WaiterOverrideConfiguration#builder()}
         *
         * @param overrideConfiguration
         *        The consumer that will configure the overrideConfiguration
         * @return a reference to this object so that method calls can be chained together.
         * @see #overrideConfiguration(WaiterOverrideConfiguration)
         */
        default Builder overrideConfiguration(Consumer<WaiterOverrideConfiguration.Builder> overrideConfiguration) {
            WaiterOverrideConfiguration.Builder builder = WaiterOverrideConfiguration.builder();
            overrideConfiguration.accept(builder);
            return overrideConfiguration(builder.build());
        }

        /**
         * Sets a custom {@link QueryClient} that will be used to poll the resource
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
