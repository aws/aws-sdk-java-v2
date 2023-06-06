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

package software.amazon.awssdk.awscore.retry;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsErrorCode;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Retry strategies used by clients when communicating with AWS services.
 */
@SdkPublicApi
public final class AwsRetryStrategy {

    private AwsRetryStrategy() {
    }

    /**
     * Retrieve the {@link SdkDefaultRetryStrategy#defaultRetryStrategy()} with AWS-specific conditions added.
     *
     * @return The default retry strategy.
     */
    public static RetryStrategy<?, ?> defaultRetryStrategy() {
        return forRetryMode(RetryMode.defaultRetryMode());
    }

    /**
     * Retrieve the appropriate retry strategy for the retry mode with AWS-specific conditions added.
     *
     * @param mode The retry mode for which we want to create a retry strategy.
     * @return A retry strategy for the given retry mode.
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
                throw new IllegalArgumentException("unknown retry mode: " + mode);
        }
    }

    /**
     * Update the provided {@link RetryStrategy} to add AWS-specific conditions.
     *
     * @param strategy The strategy to update
     * @return The updated strategy
     */
    public static RetryStrategy<?, ?> addRetryConditions(RetryStrategy<?, ?> strategy) {
        return strategy.toBuilder()
                       .retryOnException(AwsRetryStrategy::retryOnAwsRetryableErrors)
                       .build();
    }

    /**
     * Returns a retry strategy that does not retry.
     *
     * @return A retry strategy that does not retry.
     */
    public static RetryStrategy<?, ?> none() {
        return DefaultRetryStrategy.none();
    }


    /**
     * Returns a {@link StandardRetryStrategy} with AWS-specific conditions added.
     *
     * @return A {@link StandardRetryStrategy} with AWS-specific conditions added.
     */
    public static StandardRetryStrategy standardRetryStrategy() {
        StandardRetryStrategy.Builder builder = SdkDefaultRetryStrategy.standardRetryStrategyBuilder();
        return configure(builder).build();
    }

    /**
     * Returns a {@link LegacyRetryStrategy} with AWS-specific conditions added.
     *
     * @return A {@link LegacyRetryStrategy} with AWS-specific conditions added.
     */
    public static LegacyRetryStrategy legacyRetryStrategy() {
        LegacyRetryStrategy.Builder builder = SdkDefaultRetryStrategy.legacyRetryStrategyBuilder();
        return configure(builder)
            .build();
    }

    /**
     * Returns an {@link AdaptiveRetryStrategy} with AWS-specific conditions added.
     *
     * @return An {@link AdaptiveRetryStrategy} with AWS-specific conditions added.
     */
    public static AdaptiveRetryStrategy adaptiveRetryStrategy() {
        AdaptiveRetryStrategy.Builder builder = SdkDefaultRetryStrategy.adaptiveRetryStrategyBuilder();
        return configure(builder)
            .build();
    }

    /**
     * Configures a retry strategy using its builder to add AWS-specific retry exceptions.
     *
     * @param builder The builder to add the AWS-specific retry exceptions
     * @return The given builder
     * @param <T> The type of the builder extending {@link RetryStrategy.Builder}
     */
    public static <T extends RetryStrategy.Builder<T, ?>> T configure(T builder) {
        return builder.retryOnException(AwsRetryStrategy::retryOnAwsRetryableErrors);
    }

    private static boolean retryOnAwsRetryableErrors(Throwable ex) {
        if (ex instanceof AwsServiceException) {
            AwsServiceException exception = (AwsServiceException) ex;
            return AwsErrorCode.RETRYABLE_ERROR_CODES.contains(exception.awsErrorDetails().errorCode());
        }
        return false;
    }
}
