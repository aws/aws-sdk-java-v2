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

package software.amazon.awssdk.core.internal.retry;

import java.time.Duration;
import java.util.OptionalDouble;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.utils.Validate;

/**
 * Implements the {@link RetryStrategy} interface by wrapping a {@link RetryPolicy} instance.
 */
@SdkInternalApi
public final class RetryPolicyAdapter implements RetryStrategy<RetryPolicyAdapter.Builder, RetryPolicyAdapter> {

    private final RetryPolicy retryPolicy;
    private final RetryPolicyContext retryPolicyContext;
    private final RateLimitingTokenBucket rateLimitingTokenBucket;

    private RetryPolicyAdapter(Builder builder) {
        this.retryPolicy = Validate.paramNotNull(builder.retryPolicy, "retryPolicy");
        this.retryPolicyContext = Validate.paramNotNull(builder.retryPolicyContext, "retryPolicyContext");
        this.rateLimitingTokenBucket = builder.rateLimitingTokenBucket;
    }

    @Override
    public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
        RetryPolicyAdapterToken token = new RetryPolicyAdapterToken(request.scope());
        return AcquireInitialTokenResponse.create(token, rateLimitingTokenAcquire());
    }

    @Override
    public RefreshRetryTokenResponse refreshRetryToken(RefreshRetryTokenRequest request) {
        RetryPolicyAdapterToken token = getToken(request.token());
        boolean willRetry = retryPolicy.aggregateRetryCondition().shouldRetry(retryPolicyContext);
        if (!willRetry) {
            retryPolicy.aggregateRetryCondition().requestWillNotBeRetried(retryPolicyContext);
            throw new TokenAcquisitionFailedException("Retry policy disallowed retry");
        }
        Duration backoffDelay = backoffDelay();
        return RefreshRetryTokenResponse.create(token, backoffDelay);
    }

    @Override
    public RecordSuccessResponse recordSuccess(RecordSuccessRequest request) {
        RetryPolicyAdapterToken token = getToken(request.token());
        retryPolicy.aggregateRetryCondition().requestSucceeded(retryPolicyContext);
        return RecordSuccessResponse.create(token);
    }

    @Override
    public int maxAttempts() {
        return retryPolicy.numRetries() + 1;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    RetryPolicyAdapterToken getToken(RetryToken token) {
        return Validate.isInstanceOf(RetryPolicyAdapterToken.class, token, "Object of class %s was not created by this retry "
                                                                           + "strategy", token.getClass().getName());
    }

    boolean isFastFailRateLimiting() {
        return Boolean.TRUE.equals(retryPolicy.isFastFailRateLimiting());
    }

    Duration rateLimitingTokenAcquire() {
        if (!isRateLimitingEnabled()) {
            return Duration.ZERO;
        }
        OptionalDouble tokenAcquireTimeSeconds = rateLimitingTokenBucket.acquireNonBlocking(1.0, isFastFailRateLimiting());
        if (!tokenAcquireTimeSeconds.isPresent()) {
            String message = "Unable to acquire a send token immediately without waiting. This indicates that ADAPTIVE "
                             + "retry mode is enabled, fast fail rate limiting is enabled, and that rate limiting is "
                             + "engaged because of prior throttled requests. The request will not be executed.";
            throw new TokenAcquisitionFailedException(message, SdkClientException.create(message));
        }
        long tokenAcquireTimeMillis = (long) (tokenAcquireTimeSeconds.getAsDouble() * 1_000);
        return Duration.ofMillis(tokenAcquireTimeMillis);
    }

    boolean isRateLimitingEnabled() {
        return retryPolicy.retryMode() == RetryMode.ADAPTIVE;
    }

    boolean isLastExceptionThrottlingException() {
        SdkException lastException = retryPolicyContext.exception();
        if (lastException == null) {
            return false;
        }

        return RetryUtils.isThrottlingException(lastException);
    }

    Duration backoffDelay() {
        Duration backoffDelay;
        if (RetryUtils.isThrottlingException(retryPolicyContext.exception())) {
            backoffDelay = retryPolicy.throttlingBackoffStrategy().computeDelayBeforeNextRetry(retryPolicyContext);
        } else {
            backoffDelay = retryPolicy.backoffStrategy().computeDelayBeforeNextRetry(retryPolicyContext);
        }
        Duration rateLimitingDelay = rateLimitingTokenAcquire();
        return backoffDelay.plus(rateLimitingDelay);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements RetryStrategy.Builder<RetryPolicyAdapter.Builder, RetryPolicyAdapter> {
        private RetryPolicy retryPolicy;
        private RetryPolicyContext retryPolicyContext;
        private RateLimitingTokenBucket rateLimitingTokenBucket;

        private Builder() {
            rateLimitingTokenBucket = new RateLimitingTokenBucket();
        }

        private Builder(RetryPolicyAdapter adapter) {
            this.retryPolicy = adapter.retryPolicy;
            this.retryPolicyContext = adapter.retryPolicyContext;
            this.rateLimitingTokenBucket = adapter.rateLimitingTokenBucket;
        }

        @Override
        public Builder retryOnException(Predicate<Throwable> shouldRetry) {
            throw new UnsupportedOperationException("RetryPolicyAdapter does not support calling retryOnException");
        }

        @Override
        public Builder maxAttempts(int maxAttempts) {
            throw new UnsupportedOperationException("RetryPolicyAdapter does not support calling retryOnException");
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder rateLimitingTokenBucket(RateLimitingTokenBucket rateLimitingTokenBucket) {
            this.rateLimitingTokenBucket = rateLimitingTokenBucket;
            return this;
        }

        public Builder retryPolicyContext(RetryPolicyContext retryPolicyContext) {
            this.retryPolicyContext = retryPolicyContext;
            return this;
        }

        @Override
        public RetryPolicyAdapter build() {
            return new RetryPolicyAdapter(this);
        }
    }

    static class RetryPolicyAdapterToken implements RetryToken {
        private final String scope;

        RetryPolicyAdapterToken(String scope) {
            this.scope = Validate.paramNotNull(scope, "scope");
        }
    }
}
