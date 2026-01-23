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

package software.amazon.awssdk.retries.api;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.retries.api.internal.backoff.ExponentialDelayWithHalfJitter;
import software.amazon.awssdk.retries.api.internal.backoff.ExponentialDelayWithJitter;
import software.amazon.awssdk.retries.api.internal.backoff.ExponentialDelayWithoutJitter;
import software.amazon.awssdk.retries.api.internal.backoff.FixedDelayWithJitter;
import software.amazon.awssdk.retries.api.internal.backoff.FixedDelayWithoutJitter;
import software.amazon.awssdk.retries.api.internal.backoff.Immediately;

/**
 * Determines how long to wait before each execution attempt.
 */
@SdkPublicApi
@ThreadSafe
public interface BackoffStrategy {

    /**
     * Compute the amount of time to wait before the provided attempt number is executed.
     *
     * @param attempt The attempt to compute the delay for, starting at one.
     * @throws IllegalArgumentException If the given attempt is less or equal to zero.
     */
    Duration computeDelay(int attempt);

    /**
     * Do not back off: retry immediately.
     */
    static BackoffStrategy retryImmediately() {
        return new Immediately();
    }

    /**
     * Wait for a random period of time between 0ms and the provided delay.
     */
    static BackoffStrategy fixedDelay(Duration delay) {
        return new FixedDelayWithJitter(ThreadLocalRandom::current, delay);
    }

    /**
     * Wait for a period of time equal to the provided delay.
     */
    static BackoffStrategy fixedDelayWithoutJitter(Duration delay) {
        return new FixedDelayWithoutJitter(delay);
    }

    /**
     * Wait for a random period of time between 0ms and an exponentially increasing amount of time between each subsequent attempt
     * of the same call.
     *
     * <p>Specifically, the first attempt waits 0ms, and each subsequent attempt waits between
     * 0ms and {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}.
     */
    static BackoffStrategy exponentialDelay(Duration baseDelay, Duration maxDelay) {
        return new ExponentialDelayWithJitter(ThreadLocalRandom::current, baseDelay, maxDelay);
    }

    /**
     * Wait for a random period of time with an upper bound of exponentially increasing amount of time between each subsequent
     * attempt of the same call and a lower bound of half the amount of the computed exponential delay.
     *
     * <p>Specifically, the first attempt waits 0ms, and each subsequent attempt waits between
     * {@code min(maxDelay, baseDelay * (1 << (attempt - 2))) / 2} and {@code min(maxDelay, baseDelay * (1 << (attempt - 2))) +
     * 1}.
     */
    static BackoffStrategy exponentialDelayHalfJitter(Duration baseDelay, Duration maxDelay) {
        return new ExponentialDelayWithHalfJitter(ThreadLocalRandom::current, baseDelay, maxDelay);
    }

    /**
     * Wait for an exponentially increasing amount of time between each subsequent attempt of the same call.
     *
     * <p>Specifically, the first attempt waits 0ms, and each subsequent attempt waits for
     * {@code min(maxDelay, baseDelay * (1 << (attempt - 2)))}.
     */
    static BackoffStrategy exponentialDelayWithoutJitter(Duration baseDelay, Duration maxDelay) {
        return new ExponentialDelayWithoutJitter(baseDelay, maxDelay);
    }

}
