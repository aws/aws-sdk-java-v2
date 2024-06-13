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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
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
    private static final Function<RetryMode, RetryStrategy> RETRY_MODE_TO_RETRY_STRATEGY = forRetryModeHandler();


    private SdkDefaultRetryStrategy() {
    }

    /**
     * Retrieve the default retry strategy for the configured retry mode.
     *
     * @return the default retry strategy for the configured retry mode.
     */
    public static RetryStrategy defaultRetryStrategy() {
        return forRetryMode(RetryMode.defaultRetryMode());
    }

    /**
     * Retrieve the appropriate retry strategy for the retry mode with AWS-specific conditions added.
     *
     * @param mode The retry mode for which we want the retry strategy
     * @return the appropriate retry strategy for the retry mode with AWS-specific conditions added.
     */
    public static RetryStrategy forRetryMode(RetryMode mode) {
        return RETRY_MODE_TO_RETRY_STRATEGY.apply(mode);
    }

    /**
     * Returns the {@link RetryMode} for the given retry strategy.
     *
     * @param retryStrategy The retry strategy to test for
     * @return The retry mode for the given strategy
     */
    public static RetryMode retryMode(RetryStrategy retryStrategy) {
        if (retryStrategy instanceof StandardRetryStrategy) {
            return RetryMode.STANDARD;
        }
        if (retryStrategy instanceof AdaptiveRetryStrategy) {
            return RetryMode.ADAPTIVE_V2;
        }
        if (retryStrategy instanceof LegacyRetryStrategy) {
            return RetryMode.LEGACY;
        }
        if (retryStrategy instanceof RetryPolicyAdapter) {
            return RetryMode.ADAPTIVE;
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
     * @param <T>     The type of the builder extending {@link RetryStrategy.Builder}
     * @return The given builder
     */

    public static <T extends RetryStrategy.Builder<T, ?>> T configure(T builder) {
        builder.retryOnException(SdkDefaultRetryStrategy::retryOnRetryableException)
               .retryOnException(SdkDefaultRetryStrategy::retryOnStatusCodes)
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

    private static boolean retryOnRetryableException(Throwable ex) {
        if (ex instanceof SdkException) {
            return RetryUtils.isRetryableException((SdkException) ex);
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

    /**
     * Returns a {@link RetryStrategy} that implements the legacy {@link RetryMode#ADAPTIVE} mode.
     *
     * @return a {@link RetryStrategy} that implements the legacy {@link RetryMode#ADAPTIVE} mode.
     */
    private static RetryStrategy legacyAdaptiveRetryStrategy() {
        return RetryPolicyAdapter.builder()
                                 .retryPolicy(RetryPolicy.forRetryMode(RetryMode.ADAPTIVE))
                                 .build();
    }

    /**
     * Creating a retry strategy using retry mode needs to be properly configured for the expected retry conditions. If we are
     * building a retry strategy for an AWS service the SDK retry strategies do not cover all the AWS retryable conditions.
     * Furthermore, this can be called statically in a non-client specific context, as when calling
     * {@link ClientOverrideConfiguration#builder()} and then using
     * {@link ClientOverrideConfiguration.Builder#retryStrategy(RetryMode)}, this means that we need to call an statically defined
     * method, and by default we call {@link #forRetryMode(RetryMode)} in this class.
     * <p>
     * This method attempts to return properly configured retry strategy for AWS services since we cannot adjust it downstream
     * without risking overwriting customer defined ones. We do that by trying to load the {@code AwsRetryStrategy} from the class
     * path and, if found, creating a reflective delegate to its method {@link AwsRetryStrategy#forRetryMode(RetryMode)} which
     * will create proper strategies for AWS services.
     */
    private static Function<RetryMode, RetryStrategy> forRetryModeHandler() {
        try {
            Class<?> awsRetryStrategy = Class.forName("software.amazon.awssdk.awscore.retry.AwsRetryStrategy");
            Method method = awsRetryStrategy.getMethod("forRetryMode", RetryMode.class);
            return new ReflectiveRetryModeToRetryStrategy(method);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // ignored.
        }
        return SdkDefaultRetryStrategy::defaultForRetryMode;
    }

    static RetryStrategy defaultForRetryMode(RetryMode mode) {
        switch (mode) {
            case STANDARD:
                return standardRetryStrategy();
            case ADAPTIVE:
                return legacyAdaptiveRetryStrategy();
            case ADAPTIVE_V2:
                return adaptiveRetryStrategy();
            case LEGACY:
                return legacyRetryStrategy();
            default:
                throw new IllegalStateException("unknown retry mode: " + mode);
        }
    }

    static class ReflectiveRetryModeToRetryStrategy implements Function<RetryMode, RetryStrategy> {
        private final Method method;

        ReflectiveRetryModeToRetryStrategy(Method method) {
            this.method = method;
        }

        @Override
        public RetryStrategy apply(RetryMode retryMode) {
            try {
                return RetryStrategy.class.cast(method.invoke(null, retryMode));
            } catch (ClassCastException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                // ignore and fall back.
            }
            return defaultForRetryMode(retryMode);
        }
    }
}

