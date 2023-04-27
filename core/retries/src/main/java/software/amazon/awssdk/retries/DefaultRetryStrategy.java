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
     * Create a new builder for a {@code StandardRetryStrategy}.
     *
     * <p>Example Usage
     * <pre>
     * StandardRetryStrategy retryStrategy =
     *     RetryStrategies.adaptiveStrategyBuilder()
     *                    .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                    .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                    .build();
     * </pre>
     */
    public static StandardRetryStrategy.Builder standardStrategyBuilder() {
        return StandardRetryStrategy.builder()
                                    .maxAttempts(Standard.MAX_ATTEMPTS)
                                    .backoffStrategy(BackoffStrategy.exponentialDelay(Standard.BASE_DELAY, Standard.MAX_BACKOFF))
                                    .circuitBreakerEnabled(true);
    }

    static final class Standard {
        static final int MAX_ATTEMPTS = 3;
        static final Duration BASE_DELAY = Duration.ofSeconds(1);
        static final Duration MAX_BACKOFF = Duration.ofSeconds(20);
    }
}
