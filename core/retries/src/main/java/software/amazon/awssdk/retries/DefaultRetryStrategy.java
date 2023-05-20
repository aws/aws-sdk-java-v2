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

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Built-in implementations of the {@link RetryStrategy} interface.
 */
@SdkPublicApi
public final class DefaultRetryStrategy {

    private DefaultRetryStrategy() {
    }

    /**
     * Create a new builder for a {@link StandardRetryStrategy}.
     *
     * <p>Example Usage
     * {@snippet
     * StandardRetryStrategy retryStrategy =
     *     DefaultRetryStrategy.standardStrategyBuilder()
     *                    .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                    .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                    .build();
     * }
     */
    public static StandardRetryStrategy.Builder standardStrategyBuilder() {
        return StandardRetryStrategy.builder()
                                    .maxAttempts(Standard.MAX_ATTEMPTS)
                                    .backoffStrategy(BackoffStrategy.exponentialDelay(Standard.BASE_DELAY, Standard.MAX_BACKOFF));
    }

    /**
     * Create a new builder for a {@link LegacyRetryStrategy}.
     *
     * <p>Example Usage
     * {@snippet
     * LegacyRetryStrategy retryStrategy =
     *     DefaultRetryStrategy.legacyStrategyBuilder()
     *                    .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                    .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                    .build();
     * }
     */
    public static LegacyRetryStrategy.Builder legacyStrategyBuilder() {
        return LegacyRetryStrategy.builder()
                                  .maxAttempts(Legacy.MAX_ATTEMPTS)
                                  .backoffStrategy(BackoffStrategy.exponentialDelay(Legacy.BASE_DELAY, Legacy.MAX_BACKOFF))
                                  .throttlingBackoffStrategy(BackoffStrategy.exponentialDelay(Legacy.THROTTLED_BASE_DELAY,
                                                                                              Legacy.MAX_BACKOFF));
    }

    /**
     * Create a new builder for a {@link AdaptiveRetryStrategy}.
     *
     * <p>Example Usage
     * {@snippet
     * AdaptiveRetryStrategy retryStrategy =
     *     DefaultRetryStrategy.adaptiveStrategyBuilder()
     *                    .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                    .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                    .build();
     * }
     */
    public static AdaptiveRetryStrategy.Builder adaptiveStrategyBuilder() {
        return AdaptiveRetryStrategy.builder()
                                    .maxAttempts(Adaptive.MAX_ATTEMPTS);
    }

    static final class Standard {
        static final int MAX_ATTEMPTS = 3;
        static final Duration BASE_DELAY = Duration.ofSeconds(1);
        static final Duration MAX_BACKOFF = Duration.ofSeconds(20);
        static final int TOKEN_BUCKET_SIZE = 500;
        static final int DEFAULT_EXCEPTION_TOKEN_COST = 5;

        private Standard() {
        }
    }

    static final class Adaptive {
        static final int MAX_ATTEMPTS = 3;

        private Adaptive() {
        }
    }

    static final class Legacy {
        static final int MAX_ATTEMPTS = 4;
        static final Duration BASE_DELAY = Duration.ofMillis(100);
        static final Duration THROTTLED_BASE_DELAY = Duration.ofMillis(500);

        static final Duration MAX_BACKOFF = Duration.ofSeconds(20);
        static final int TOKEN_BUCKET_SIZE = 500;
        static final int DEFAULT_EXCEPTION_TOKEN_COST = 5;
        static final int THROTTLE_EXCEPTION_TOKEN_COST = 0;

        private Legacy() {
        }
    }
}
