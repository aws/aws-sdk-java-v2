/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.waiters;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration values for the {@link Waiter}. All values are optional, and the default values will be used
 * if they are not specified.
 */
@SdkPublicApi
public final class WaiterOverrideConfiguration implements ToCopyableBuilder<WaiterOverrideConfiguration.Builder,
    WaiterOverrideConfiguration> {

    private final Integer maxAttempts;
    private final BackoffStrategy backoffStrategy;
    private final Duration waitTimeout;

    public WaiterOverrideConfiguration(Builder builder) {
        this.maxAttempts = Validate.isPositiveOrNull(builder.maxAttempts, "maxAttempts");
        this.backoffStrategy = builder.backoffStrategy;
        this.waitTimeout = Validate.isPositiveOrNull(builder.waitTimeout, "waitTimeout");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the optional maximum number of attempts that should be used when polling the resource
     */
    public Optional<Integer> maxAttempts() {
        return Optional.ofNullable(maxAttempts);
    }

    /**
     * @return the optional {@link software.amazon.awssdk.core.retry.backoff.BackoffStrategy} that should be used when polling
     * the resource
     * @deprecated Use instead {@link #backoffStrategy2()}
     */
    public Optional<software.amazon.awssdk.core.retry.backoff.BackoffStrategy> backoffStrategy() {
        if (backoffStrategy == null) {
            return Optional.empty();
        }
        if (backoffStrategy instanceof LegacyToNonLegacyAdapter) {
            return Optional.of(((LegacyToNonLegacyAdapter) backoffStrategy).adaptee);
        }
        return Optional.of(new NonLegacyToLegacyAdapter(backoffStrategy));
    }

    /**
     * @return the optional {@link BackoffStrategy} that should be used when polling the
     * resource
     */
    public Optional<BackoffStrategy> backoffStrategy2() {
        return Optional.ofNullable(backoffStrategy);
    }

    /**
     * @return the optional amount of time to wait that should be used when polling the resource
     *
     */
    public Optional<Duration> waitTimeout() {
        return Optional.ofNullable(waitTimeout);
    }

    @Override
    public Builder toBuilder() {
        return new Builder().maxAttempts(maxAttempts)
                            .backoffStrategy(backoffStrategy)
                            .waitTimeout(waitTimeout);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WaiterOverrideConfiguration that = (WaiterOverrideConfiguration) o;

        if (!Objects.equals(maxAttempts, that.maxAttempts)) {
            return false;
        }
        if (!Objects.equals(backoffStrategy, that.backoffStrategy)) {
            return false;
        }
        return Objects.equals(waitTimeout, that.waitTimeout);
    }

    @Override
    public int hashCode() {
        int result = maxAttempts != null ? maxAttempts.hashCode() : 0;
        result = 31 * result + (backoffStrategy != null ? backoffStrategy.hashCode() : 0);
        result = 31 * result + (waitTimeout != null ? waitTimeout.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("WaiterOverrideConfiguration")
                       .add("maxAttempts", maxAttempts)
                       .add("waitTimeout", waitTimeout)
                       .add("backoffStrategy", backoffStrategy)
                       .build();
    }

    public static final class Builder implements CopyableBuilder<WaiterOverrideConfiguration.Builder,
        WaiterOverrideConfiguration> {
        private BackoffStrategy backoffStrategy;
        private Integer maxAttempts;
        private Duration waitTimeout;

        private Builder() {
        }

        /**
         * Define the {@link software.amazon.awssdk.core.retry.backoff.BackoffStrategy} that computes the delay before the next
         * retry request.
         *
         * @param backoffStrategy The new backoffStrategy value.
         * @return This object for method chaining.
         * @deprecated Use instead {@link #backoffStrategy(BackoffStrategy)}
         */
        public Builder backoffStrategy(software.amazon.awssdk.core.retry.backoff.BackoffStrategy backoffStrategy) {
            if (backoffStrategy != null) {
                this.backoffStrategy = new LegacyToNonLegacyAdapter(backoffStrategy);
            }
            return this;
        }

        /**
         * Define the {@link BackoffStrategy} that computes the delay before the next retry
         * request.
         *
         * @param backoffStrategy The new backoffStrategy value.
         * @return This object for method chaining.
         */
        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        /**
         * Define the maximum number of attempts to try before transitioning the waiter to a failure state.
         *
         * @param maxAttempts The new maxAttempts value.
         * @return This object for method chaining.
         */
        public Builder maxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Define the amount of time to wait for the resource to transition to the desired state before
         * timing out. This wait timeout doesn't have strict guarantees on how quickly a request is aborted
         * when the timeout is breached. The request can timeout early if it is determined that the next
         * retry will breach the max wait time. It's disabled by default.
         *
         * @param waitTimeout The new waitTimeout value.
         * @return This object for method chaining.
         */
        public Builder waitTimeout(Duration waitTimeout) {
            this.waitTimeout = waitTimeout;
            return this;
        }

        @Override
        public WaiterOverrideConfiguration build() {
            return new WaiterOverrideConfiguration(this);
        }
    }

    @SdkInternalApi
    static class LegacyToNonLegacyAdapter implements BackoffStrategy {
        private final software.amazon.awssdk.core.retry.backoff.BackoffStrategy adaptee;

        LegacyToNonLegacyAdapter(software.amazon.awssdk.core.retry.backoff.BackoffStrategy adaptee) {
            this.adaptee = Objects.requireNonNull(adaptee);
        }


        @Override
        public Duration computeDelay(int attempt) {
            return adaptee.computeDelayBeforeNextRetry(RetryPolicyContext.builder()
                                                                         .retriesAttempted(attempt - 2)
                                                                         .build());
        }
    }

    @SdkInternalApi
    static class NonLegacyToLegacyAdapter implements software.amazon.awssdk.core.retry.backoff.BackoffStrategy {
        private final BackoffStrategy adaptee;

        NonLegacyToLegacyAdapter(BackoffStrategy adaptee) {
            this.adaptee = Objects.requireNonNull(adaptee);
        }


        @Override
        public Duration computeDelayBeforeNextRetry(RetryPolicyContext context) {
            return adaptee.computeDelay(context.retriesAttempted());
        }
    }
}
