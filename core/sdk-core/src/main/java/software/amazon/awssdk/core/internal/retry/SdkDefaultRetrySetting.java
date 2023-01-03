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

import static java.util.Collections.unmodifiableSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.conditions.TokenBucketExceptionCostFunction;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class SdkDefaultRetrySetting {
    public static final class Legacy {
        private static final int MAX_ATTEMPTS = 4;
        private static final Duration BASE_DELAY = Duration.ofMillis(100);
        private static final Duration THROTTLED_BASE_DELAY = Duration.ofMillis(500);
        private static final int THROTTLE_EXCEPTION_TOKEN_COST = 0;
        private static final int DEFAULT_EXCEPTION_TOKEN_COST = 5;

        public static final TokenBucketExceptionCostFunction COST_FUNCTION =
            TokenBucketExceptionCostFunction.builder()
                                            .throttlingExceptionCost(THROTTLE_EXCEPTION_TOKEN_COST)
                                            .defaultExceptionCost(DEFAULT_EXCEPTION_TOKEN_COST)
                                            .build();
    }

    public static final class Standard {
        private static final int MAX_ATTEMPTS = 3;
        private static final Duration BASE_DELAY = Duration.ofMillis(100);
        private static final Duration THROTTLED_BASE_DELAY = Duration.ofSeconds(1);
        private static final int THROTTLE_EXCEPTION_TOKEN_COST = 5;
        private static final int DEFAULT_EXCEPTION_TOKEN_COST = 5;

        public static final TokenBucketExceptionCostFunction COST_FUNCTION =
            TokenBucketExceptionCostFunction.builder()
                                            .throttlingExceptionCost(THROTTLE_EXCEPTION_TOKEN_COST)
                                            .defaultExceptionCost(DEFAULT_EXCEPTION_TOKEN_COST)
                                            .build();
    }

    public static final int TOKEN_BUCKET_SIZE = 500;

    public static final Duration MAX_BACKOFF = Duration.ofSeconds(20);

    public static final Set<Integer> RETRYABLE_STATUS_CODES;
    public static final Set<Class<? extends Exception>> RETRYABLE_EXCEPTIONS;

    static {
        Set<Integer> retryableStatusCodes = new HashSet<>();
        retryableStatusCodes.add(HttpStatusCode.INTERNAL_SERVER_ERROR);
        retryableStatusCodes.add(HttpStatusCode.BAD_GATEWAY);
        retryableStatusCodes.add(HttpStatusCode.SERVICE_UNAVAILABLE);
        retryableStatusCodes.add(HttpStatusCode.GATEWAY_TIMEOUT);
        RETRYABLE_STATUS_CODES = unmodifiableSet(retryableStatusCodes);

        Set<Class<? extends Exception>> retryableExceptions = new HashSet<>();
        retryableExceptions.add(RetryableException.class);
        retryableExceptions.add(IOException.class);
        retryableExceptions.add(UncheckedIOException.class);
        retryableExceptions.add(ApiCallAttemptTimeoutException.class);
        RETRYABLE_EXCEPTIONS = unmodifiableSet(retryableExceptions);
    }

    private SdkDefaultRetrySetting() {
    }

    public static Integer maxAttempts(RetryMode retryMode) {
        Integer maxAttempts = SdkSystemSetting.AWS_MAX_ATTEMPTS.getIntegerValue().orElse(null);

        if (maxAttempts == null) {
            switch (retryMode) {
                case LEGACY:
                    maxAttempts = Legacy.MAX_ATTEMPTS;
                    break;
                case ADAPTIVE:
                case STANDARD:
                    maxAttempts = Standard.MAX_ATTEMPTS;
                    break;
                default:
                    throw new IllegalStateException("Unsupported RetryMode: " + retryMode);
            }
        }

        Validate.isPositive(maxAttempts, "Maximum attempts must be positive, but was " + maxAttempts);

        return maxAttempts;
    }

    public static TokenBucketExceptionCostFunction tokenCostFunction(RetryMode retryMode) {
        switch (retryMode) {
            case LEGACY:
                return Legacy.COST_FUNCTION;
            case ADAPTIVE:
            case STANDARD:
                return Standard.COST_FUNCTION;
            default:
                throw new IllegalStateException("Unsupported RetryMode: " + retryMode);
        }
    }

    public static Integer defaultMaxAttempts() {
        return maxAttempts(RetryMode.defaultRetryMode());
    }

    public static Duration baseDelay(RetryMode retryMode) {
        switch (retryMode) {
            case LEGACY:
                return Legacy.BASE_DELAY;
            case ADAPTIVE:
            case STANDARD:
                return Standard.BASE_DELAY;
            default:
                throw new IllegalStateException("Unsupported RetryMode: " + retryMode);
        }
    }

    public static Duration throttledBaseDelay(RetryMode retryMode) {
        switch (retryMode) {
            case LEGACY:
                return Legacy.THROTTLED_BASE_DELAY;
            case ADAPTIVE:
            case STANDARD:
                return Standard.THROTTLED_BASE_DELAY;
            default:
                throw new IllegalStateException("Unsupported RetryMode: " + retryMode);
        }
    }
}
