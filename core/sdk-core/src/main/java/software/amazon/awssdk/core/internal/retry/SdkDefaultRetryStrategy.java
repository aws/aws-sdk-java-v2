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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Retry strategies used by any SDK client.
 */
@SdkPublicApi
public final class SdkDefaultRetryStrategy {

    private SdkDefaultRetryStrategy() {
    }

    /**
     * Retrieve the default retry strategy for the configured retry mode.
     *
     * @return the default retry strategy for the configured retry mode.
     */
    public static RetryStrategy<?, ?> defaultRetryStrategy() {
        return forRetryMode(RetryMode.defaultRetryMode());
    }

    /**
     * Retrieve the appropriate retry strategy for the retry mode with AWS-specific conditions added.
     *
     * @param mode The retry mode for which we want the retry strategy
     * @return the appropriate retry strategy for the retry mode with AWS-specific conditions added.
     */
    public static RetryStrategy<?, ?> forRetryMode(RetryMode mode) {
        switch (mode) {
            case STANDARD:
                return standardRetryStrategy();
            case ADAPTIVE:
                return adaptiveRetryStrategy();
            case LEGACY:
                return legacyRetryStrategy();
            default:
                throw new IllegalStateException("unknown retry mode: " + mode);
        }
    }

    /**
     * Returns the {@link RetryMode} for the given retry strategy.
     *
     * @param retryStrategy The retry strategy to test for
     * @return The retry mode for the given strategy
     */
    public static RetryMode retryMode(RetryStrategy<?, ?> retryStrategy) {
        if (retryStrategy instanceof StandardRetryStrategy) {
            return RetryMode.STANDARD;
        }
        if (retryStrategy instanceof AdaptiveRetryStrategy) {
            return RetryMode.ADAPTIVE;
        }
        if (retryStrategy instanceof LegacyRetryStrategy) {
            return RetryMode.LEGACY;
        }
        throw new IllegalArgumentException("unknown retry strategy class: " + retryStrategy.getClass().getName());
    }

    /**
     * Returns a {@link StandardRetryStrategy} with generic SDK retry conditions.
     *
     * @return a {@link StandardRetryStrategy} with generic SDK retry conditions.
     */
    public static StandardRetryStrategy standardRetryStrategy() {
        return standardRetryStrategyBuilder().build();
    }

    /**
     * Returns a {@link LegacyRetryStrategy} with generic SDK retry conditions.
     *
     * @return a {@link LegacyRetryStrategy} with generic SDK retry conditions.
     */
    public static LegacyRetryStrategy legacyRetryStrategy() {
        return legacyRetryStrategyBuilder().build();
    }

    /**
     * Returns an {@link AdaptiveRetryStrategy} with generic SDK retry conditions.
     *
     * @return an {@link AdaptiveRetryStrategy} with generic SDK retry conditions.
     */
    public static AdaptiveRetryStrategy adaptiveRetryStrategy() {
        return adaptiveRetryStrategyBuilder().build();
    }

    /**
     * Returns a {@link StandardRetryStrategy.Builder} with preconfigured generic SDK retry conditions.
     *
     * @return a {@link StandardRetryStrategy.Builder} with preconfigured generic SDK retry conditions.
     */
    public static StandardRetryStrategy.Builder standardRetryStrategyBuilder() {
        StandardRetryStrategy.Builder builder = DefaultRetryStrategy.standardStrategyBuilder();
        return configure(builder);
    }

    /**
     * Returns a {@link LegacyRetryStrategy.Builder} with preconfigured generic SDK retry conditions.
     *
     * @return a {@link LegacyRetryStrategy.Builder} with preconfigured generic SDK retry conditions.
     */
    public static LegacyRetryStrategy.Builder legacyRetryStrategyBuilder() {
        LegacyRetryStrategy.Builder builder = DefaultRetryStrategy.legacyStrategyBuilder();
        return configure(builder)
            .treatAsThrottling(SdkDefaultRetryStrategy::treatAsThrottling);
    }

    /**
     * Returns an {@link AdaptiveRetryStrategy.Builder} with preconfigured generic SDK retry conditions.
     *
     * @return an {@link AdaptiveRetryStrategy.Builder} with preconfigured generic SDK retry conditions.
     */
    public static AdaptiveRetryStrategy.Builder adaptiveRetryStrategyBuilder() {
        AdaptiveRetryStrategy.Builder builder = DefaultRetryStrategy.adaptiveStrategyBuilder();
        return configure(builder)
            .treatAsThrottling(SdkDefaultRetryStrategy::treatAsThrottling);
    }

    /**
     * Configures a retry strategy using its builder to add SDK-generic retry exceptions.
     *
     * @param builder The builder to add the SDK-generic retry exceptions
     * @return The given builder
     * @param <T> The type of the builder extending {@link RetryStrategy.Builder}
     */

    public static <T extends RetryStrategy.Builder<T, ?>> T configure(T builder) {
        builder.retryOnException(SdkDefaultRetryStrategy::retryOnStatusCodes)
               .retryOnException(SdkDefaultRetryStrategy::retryOnClockSkewException)
               .retryOnException(SdkDefaultRetryStrategy::retryOnThrottlingCondition);
        SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS.forEach(builder::retryOnExceptionOrCauseInstanceOf);
        return builder;
    }

    private static boolean treatAsThrottling(Throwable t) {
        if (t instanceof SdkException) {
            return RetryUtils.isThrottlingException((SdkException) t);
        }
        return false;
    }

    private static boolean retryOnStatusCodes(Throwable ex) {
        if (ex instanceof SdkServiceException) {
            SdkServiceException failure = (SdkServiceException) ex;
            return SdkDefaultRetrySetting.RETRYABLE_STATUS_CODES.contains(failure.statusCode());
        }
        return false;
    }

    private static boolean retryOnClockSkewException(Throwable ex) {
        if (ex instanceof SdkException) {
            return RetryUtils.isClockSkewException((SdkException) ex);
        }
        return false;
    }

    private static boolean retryOnThrottlingCondition(Throwable ex) {
        if (ex instanceof SdkException) {
            return RetryUtils.isThrottlingException((SdkException) ex);
        }
        return false;
    }
}
