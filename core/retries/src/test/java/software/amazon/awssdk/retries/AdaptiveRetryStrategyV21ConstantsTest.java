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

package software.amazon.awssdk.retries;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.internal.AcquireInitialTokenRequestImpl;
import software.amazon.awssdk.retries.internal.DefaultRetryToken;

/**
 * Tests that {@code AdaptiveRetryStrategy.builder(boolean retries2026Enabled)} selects the correct
 * v2.0 or v2.1 constants for base delay, exception token cost, and throttling token cost.
 */
class AdaptiveRetryStrategyV21ConstantsTest {

    private static final int BUCKET_CAPACITY = 500;

    @Test
    void v21Enabled_nonThrottlingRetry_deducts14Tokens() {
        RetryStrategy strategy = AdaptiveRetryStrategy.builder(true)
            .retryOnException(t -> true)
            .treatAsThrottling(t -> false)
            .build();

        DefaultRetryToken token = retryOnceBeforeSuccess(strategy, new RuntimeException("transient"));
        assertThat(token.capacityRemaining()).isEqualTo(BUCKET_CAPACITY - 14);
    }

    @Test
    void v21Enabled_throttlingRetry_deducts5Tokens() {
        RetryStrategy strategy = AdaptiveRetryStrategy.builder(true)
            .retryOnException(t -> true)
            .treatAsThrottling(t -> true)
            .build();

        DefaultRetryToken token = retryOnceBeforeSuccess(strategy, new RuntimeException("throttled"));
        assertThat(token.capacityRemaining()).isEqualTo(BUCKET_CAPACITY - 5);
    }

    @Test
    void v20_nonThrottlingRetry_deducts5Tokens() {
        RetryStrategy strategy = AdaptiveRetryStrategy.builder(false)
            .retryOnException(t -> true)
            .treatAsThrottling(t -> false)
            .build();

        DefaultRetryToken token = retryOnceBeforeSuccess(strategy, new RuntimeException("transient"));
        assertThat(token.capacityRemaining()).isEqualTo(BUCKET_CAPACITY - 5);
    }

    @Test
    void v20_throttlingRetry_deducts5Tokens() {
        RetryStrategy strategy = AdaptiveRetryStrategy.builder(false)
            .retryOnException(t -> true)
            .treatAsThrottling(t -> true)
            .build();

        DefaultRetryToken token = retryOnceBeforeSuccess(strategy, new RuntimeException("throttled"));
        assertThat(token.capacityRemaining()).isEqualTo(BUCKET_CAPACITY - 5);
    }

    @Test
    void v21Enabled_backoffUses50msBaseDelay() {
        RetryStrategy strategy = AdaptiveRetryStrategy.builder(true)
            .retryOnException(t -> true)
            .build();

        RefreshRetryTokenResponse response = refreshToken(strategy, new RuntimeException("err"));
        // First retry delay should include exponential backoff component in [0, 50ms]
        assertThat(response.delay()).isBetween(Duration.ZERO, Duration.ofMillis(50));
    }

    @Test
    void v20_backoffUses100msBaseDelay() {
        RetryStrategy strategy = AdaptiveRetryStrategy.builder(false)
            .retryOnException(t -> true)
            .build();

        RefreshRetryTokenResponse response = refreshToken(strategy, new RuntimeException("err"));
        // First retry delay should include exponential backoff component in [0, 100ms]
        assertThat(response.delay()).isBetween(Duration.ZERO, Duration.ofMillis(100));
    }

    @Test
    void noArgBuilder_usesV20Constants() {
        RetryStrategy strategy = AdaptiveRetryStrategy.builder()
            .retryOnException(t -> true)
            .treatAsThrottling(t -> false)
            .build();

        DefaultRetryToken token = retryOnceBeforeSuccess(strategy, new RuntimeException("transient"));
        // v2.0: exception cost is 5
        assertThat(token.capacityRemaining()).isEqualTo(BUCKET_CAPACITY - 5);
    }

    /**
     * Acquires an initial token, triggers one retry. Returns the token after the retry (before success).
     */
    private DefaultRetryToken retryOnceBeforeSuccess(RetryStrategy strategy, Exception failure) {
        AcquireInitialTokenResponse initial = strategy.acquireInitialToken(AcquireInitialTokenRequestImpl.create("test"));
        RetryToken token = initial.token();

        RefreshRetryTokenResponse refreshResponse = strategy.refreshRetryToken(
            RefreshRetryTokenRequest.builder().token(token).failure(failure).build());

        return (DefaultRetryToken) refreshResponse.token();
    }

    /**
     * Acquires an initial token and triggers one refresh to get the backoff delay.
     */
    private RefreshRetryTokenResponse refreshToken(RetryStrategy strategy, Exception failure) {
        AcquireInitialTokenResponse initial = strategy.acquireInitialToken(AcquireInitialTokenRequestImpl.create("test"));
        return strategy.refreshRetryToken(
            RefreshRetryTokenRequest.builder().token(initial.token()).failure(failure).build());
    }
}
