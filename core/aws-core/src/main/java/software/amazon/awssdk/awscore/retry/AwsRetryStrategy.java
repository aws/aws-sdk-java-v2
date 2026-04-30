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
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.retry.RetryPolicyAdapter;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.internal.DefaultAwareRetryStrategy;
import software.amazon.awssdk.retries.internal.RetryStrategyDefaults;

/**
 * Retry strategies used by clients when communicating with AWS services.
 */
@SdkPublicApi
public final class AwsRetryStrategy {

    private static final String DEFAULTS_NAME = "aws";

    private static final RetryStrategyDefaults DEFAULTS_PREDICATES = new RetryStrategyDefaults() {
        @Override
        public String name() {
            return DEFAULTS_NAME;
        }

        @Override
        public void applyDefaults(RetryStrategy.Builder<?, ?> retryStrategyBuilder) {
            configureStrategy(retryStrategyBuilder);
            markDefaultsAdded(retryStrategyBuilder);
        }
    };

    private AwsRetryStrategy() {
    }

    /**
     * Retrieve the {@link SdkDefaultRetryStrategy#defaultRetryStrategy()} with AWS-specific conditions added.
     *
     * @return The default retry strategy.
     */
    public static RetryStrategy defaultRetryStrategy() {
        return forRetryMode(RetryMode.defaultRetryMode());
    }

    /**
     * Retrieve the appropriate retry strategy for the retry mode with AWS-specific conditions added. This is equivalent to
     * {@code forRetryMode(mode, false)}.
     *
     * @param mode The retry mode for which we want to create a retry strategy.
     * @return A retry strategy for the given retry mode.
     */
    public static RetryStrategy forRetryMode(RetryMode mode) {
        return forRetryMode(mode, false);
    }

    /**
     * Retrieve the appropriate retry strategy for the retry mode with AWS-specific conditions added.
     *
     * @param mode The retry mode for which we want to create a retry strategy.
     * @param newRetries2026Enabled Whether retries 2.1 behavior is enabled.
     * @return A retry strategy for the given retry mode.
     */
    public static RetryStrategy forRetryMode(RetryMode mode, Boolean newRetries2026Enabled) {
        switch (mode) {
            case STANDARD:
                return standardRetryStrategy(newRetries2026Enabled);
            case ADAPTIVE_V2:
                return adaptiveRetryStrategy(newRetries2026Enabled);
            case LEGACY:
                return legacyRetryStrategy();
            case ADAPTIVE:
                return legacyAdaptiveRetryStrategy();
            default:
                throw new IllegalArgumentException("unknown retry mode: " + mode);
        }
    }


    /**
     * Update the provided {@link RetryStrategy} to add AWS-specific conditions.
     *
     * @param strategy The strategy to update
     * @return The updated strategy.
     */
    public static RetryStrategy addRetryConditions(RetryStrategy strategy) {
        return strategy.toBuilder()
                       .retryOnException(AwsRetryStrategy::retryOnAwsRetryableErrors)
                       .build();
    }

    /**
     * Returns a retry strategy that does not retry.
     *
     * @return A retry strategy that do not retry.
     */
    public static RetryStrategy doNotRetry() {
        return DefaultRetryStrategy.doNotRetry();
    }

    /**
     * Returns a {@link StandardRetryStrategy} with AWS-specific conditions added. This is equivalent to {@code
     * standardRetryStrategy(false)}.
     *
     * @return A {@link StandardRetryStrategy} with AWS-specific conditions added.
     */
    public static StandardRetryStrategy standardRetryStrategy() {
        return standardRetryStrategy(false);
    }

    /**
     * Returns a {@link StandardRetryStrategy} with AWS-specific conditions added.
     *
     * @param newRetries2026Enabled Whether retries 2.1 behavior is enabled.
     * @return A {@link StandardRetryStrategy} with AWS-specific conditions added.
     */
    public static StandardRetryStrategy standardRetryStrategy(Boolean newRetries2026Enabled) {
        StandardRetryStrategy.Builder builder = SdkDefaultRetryStrategy.standardRetryStrategyBuilder(newRetries2026Enabled);
        return configure(builder, newRetries2026Enabled).build();
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
     * Returns an {@link AdaptiveRetryStrategy} with AWS-specific conditions added. This is equivalent to {@code
     * adaptiveRetryStrategy(false)}.
     *
     * @return An {@link AdaptiveRetryStrategy} with AWS-specific conditions added.
     */
    public static AdaptiveRetryStrategy adaptiveRetryStrategy() {
        return adaptiveRetryStrategy(false);
    }

    /**
     * Returns an {@link AdaptiveRetryStrategy} with AWS-specific conditions added.
     *
     * @param newRetries2026Enabled Whether retries 2.1 behavior is enabled.
     * @return An {@link AdaptiveRetryStrategy} with AWS-specific conditions added.
     */
    public static AdaptiveRetryStrategy adaptiveRetryStrategy(Boolean newRetries2026Enabled) {
        AdaptiveRetryStrategy.Builder builder = SdkDefaultRetryStrategy.adaptiveRetryStrategyBuilder(newRetries2026Enabled);
        return configure(builder, newRetries2026Enabled)
            .build();
    }

    /**
     * Configures a retry strategy using its builder to add AWS-specific retry exceptions.
     *
     * @param builder The builder to add the AWS-specific retry exceptions
     * @param <T>     The type of the builder extending {@link RetryStrategy.Builder}
     * @return The given builder
     */
    public static <T extends RetryStrategy.Builder<T, ?>> T configure(T builder) {
        return configure(builder, false);
    }

    /**
     * Configures a retry strategy using its builder to add AWS-specific retry exceptions.
     *
     * @param builder The builder to add the AWS-specific retry exceptions
     * @param <T>     The type of the builder extending {@link RetryStrategy.Builder}
     * @return The given builder
     */
    private static <T extends RetryStrategy.Builder<T, ?>> T configure(T builder, Boolean newRetries2026Enabled) {
        builder.retryOnException(AwsRetryStrategy::retryOnAwsRetryableErrors);
        if (Boolean.TRUE.equals(newRetries2026Enabled)) {
            builder.retryOnException(AwsRetryStrategy::isLimitExceededErrorCode);
            builder.treatAsThrottling(AwsRetryStrategy::treatAsThrottlingV21);
        }
        markDefaultsAdded(builder);
        return builder;
    }

    /**
     * Configures any retry strategy using its builder to add AWS-specific retry exceptions.
     *
     * @param builder The builder to add the AWS-specific retry exceptions
     * @return The given builder
     */
    public static RetryStrategy.Builder<?, ?> configureStrategy(RetryStrategy.Builder<?, ?> builder) {
        if (builder instanceof RetryPolicyAdapter.Builder) {
            return builder;
        }
        return builder.retryOnException(AwsRetryStrategy::retryOnAwsRetryableErrors);
    }

    private static boolean retryOnAwsRetryableErrors(Throwable ex) {
        if (ex instanceof AwsServiceException) {
            AwsServiceException exception = (AwsServiceException) ex;
            return AwsErrorCode.RETRYABLE_ERROR_CODES.contains(exception.awsErrorDetails().errorCode());
        }
        return false;
    }

    /**
     * Additionally, check for LimitExceededException as it was not previously treated as a throttling exception.
     */
    private static boolean treatAsThrottlingV21(Throwable ex) {
        if (!(ex instanceof SdkException)) {
            return false;
        }

        SdkException sdkException = (SdkException) ex;

        return RetryUtils.isThrottlingException(sdkException)
               || isLimitExceededErrorCode(sdkException);
    }

    private static boolean isLimitExceededErrorCode(Throwable ex) {
        return ex instanceof AwsServiceException
               && "LimitExceededException".equals(((AwsServiceException) ex).awsErrorDetails().errorCode());
    }

    /**
     * Returns a {@link RetryStrategy} that implements the legacy {@link RetryMode#ADAPTIVE} mode.
     *
     * @return a {@link RetryStrategy} that implements the legacy {@link RetryMode#ADAPTIVE} mode.
     */
    private static RetryStrategy legacyAdaptiveRetryStrategy() {
        return RetryPolicyAdapter.builder()
                                 .retryPolicy(AwsRetryPolicy.forRetryMode(RetryMode.ADAPTIVE))
                                 .build();
    }

    public static RetryStrategyDefaults retryStrategyDefaults() {
        return DEFAULTS_PREDICATES;
    }

    private static void markDefaultsAdded(RetryStrategy.Builder<?, ?> builder) {
        if (builder instanceof DefaultAwareRetryStrategy.Builder) {
            DefaultAwareRetryStrategy.Builder b = (DefaultAwareRetryStrategy.Builder) builder;
            b.markDefaultAdded(DEFAULTS_NAME);
        }
    }

}
