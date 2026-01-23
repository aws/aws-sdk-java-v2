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

package software.amazon.awssdk.retries.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A data rich {@link RetryToken} implementation. The data contained in this class is not part of the interface but is needed for
 * the calling code to test and generate meaningful logs using its current state.
 */
@SdkInternalApi
public final class DefaultRetryToken implements RetryToken, ToCopyableBuilder<DefaultRetryToken.Builder, DefaultRetryToken> {
    private final String scope;
    private final TokenState state;
    private final int attempt;
    private final int capacityAcquired;
    private final int capacityRemaining;
    private final List<Throwable> failures;

    private DefaultRetryToken(Builder builder) {
        this.scope = Validate.paramNotNull(builder.scope, "scope");
        this.state = Validate.paramNotNull(builder.state, "status");
        this.attempt = Validate.isPositive(builder.attempt, "attempt");
        this.capacityAcquired = Validate.isNotNegative(builder.capacityAcquired, "capacityAcquired");
        this.capacityRemaining = Validate.isNotNegative(builder.capacityRemaining, "capacityRemaining");
        this.failures = Collections.unmodifiableList(Validate.paramNotNull(builder.failures, "failures"));
    }

    /**
     * Returns the latest attempt count.
     */
    public int attempt() {
        return attempt;
    }

    /**
     * Returns the token scope.
     */
    public String scope() {
        return scope;
    }

    /**
     * Returns the latest capacity acquired from the token bucket.
     */
    public int capacityAcquired() {
        return capacityAcquired;
    }

    /**
     * Returns the capacity remaining in the token bucket when the last acquire request was done.
     */
    public int capacityRemaining() {
        return capacityRemaining;
    }

    /**
     * Returns the state of the token.
     */
    public TokenState state() {
        return state;
    }

    /**
     * Creates a new builder to mutate the current instance.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return ToString.builder("StandardRetryToken")
                       .add("scope", scope)
                       .add("status", state)
                       .add("attempt", attempt)
                       .add("capacityAcquired", capacityAcquired)
                       .add("capacityRemaining", capacityRemaining)
                       .add("failures", failures)
                       .build();
    }

    /**
     * Returns a new builder to create new instances of the {@link DefaultRetryToken} class.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Set of possibles states on which the RetryToken can be, in-progress, succeeded and all the possible failure modes.
     */
    public enum TokenState {
        /**
         * The request operation is in-progress.
         */
        IN_PROGRESS,
        /**
         * The request operation concluded successfully.
         */
        SUCCEEDED,
        /**
         * The request operation failed with token acquisition failure.
         */
        TOKEN_ACQUISITION_FAILED,
        /**
         * The request operation failed with max retries reached.
         */
        MAX_RETRIES_REACHED,
        /**
         * The request operation failed with non-retryable exception caught.
         */
        NON_RETRYABLE_EXCEPTION
    }

    /**
     * A builder class to create {@link DefaultRetryToken} instances or to mutate them.
     */
    public static class Builder implements CopyableBuilder<Builder, DefaultRetryToken> {
        private TokenState state = TokenState.IN_PROGRESS;
        private String scope;
        private int attempt = 1;
        private int capacityAcquired = 0;
        private int capacityRemaining = 0;
        private List<Throwable> failures;

        Builder() {
            this.failures = new ArrayList<>();
        }

        Builder(DefaultRetryToken token) {
            this.scope = token.scope;
            this.attempt = token.attempt;
            this.capacityAcquired = token.capacityAcquired;
            this.capacityRemaining = token.capacityRemaining;
            this.failures = new ArrayList<>(token.failures);
        }

        /**
         * Sets the scope of the retry token.
         */
        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Sets the state of the retry token.
         */
        public Builder state(TokenState state) {
            this.state = state;
            return this;
        }

        /**
         * Increments the current attempt count.
         */
        public Builder increaseAttempt() {
            ++this.attempt;
            return this;
        }

        /**
         * Sets the capacity acquired from the token bucket.
         */
        public Builder capacityAcquired(int capacityAcquired) {
            this.capacityAcquired = capacityAcquired;
            return this;
        }

        /**
         * Sets the capacity remaining in the token bucket after the last acquire.
         */
        public Builder capacityRemaining(int capacityRemaining) {
            this.capacityRemaining = capacityRemaining;
            return this;
        }

        /**
         * Adds a {@link Throwable} to the retry-token.
         */
        public Builder addFailure(Throwable failure) {
            this.failures.add(Validate.paramNotNull(failure, "failure"));
            return this;
        }

        /**
         * Creates a new {@link DefaultRetryToken} with the configured values.
         */
        public DefaultRetryToken build() {
            return new DefaultRetryToken(this);
        }
    }
}
