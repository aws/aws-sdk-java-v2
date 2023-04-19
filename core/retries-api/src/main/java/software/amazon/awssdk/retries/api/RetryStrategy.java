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

import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A strategy used by an SDK to determine when something should be retried.
 *
 * <p>We do not recommend SDK users create their own retry strategies. We recommend refining an
 * existing strategy:
 * <ol>
 *     <li>If you are using the strategy with a service, you can get the existing strategy
 *     from that service via {@code [ServiceName]Client.defaults().retryStrategy()}.
 *     <li>{@code RetryStrategies} from the {@code software.amazon.awssdk:retries} module.
 * </ol>
 *
 * <p>Terminology:
 * <ol>
 *     <li>An <b>attempt</b> is a single invocation of an action.
 *     <li>The <b>attempt count</b> is which attempt (starting with 1) the SDK is attempting to
 *     make.
 * </ol>
 */
@ThreadSafe
@SdkPublicApi
public interface RetryStrategy<
    B extends CopyableBuilder<B, T> & RetryStrategy.Builder<B, T>,
    T extends ToCopyableBuilder<B, T> & RetryStrategy<B, T>>
    extends ToCopyableBuilder<B, T> {
    /**
     * Invoked before the first request attempt.
     *
     * <p>Callers MUST wait for the {@code delay} returned by this call before making the first attempt. Callers that wish to
     * retry a failed attempt MUST call {@link #refreshRetryToken} before doing so.
     *
     * <p>If the attempt was successful, callers MUST call {@link #recordSuccess}.
     *
     * @throws NullPointerException            if a required parameter is not specified
     * @throws TokenAcquisitionFailedException if a token cannot be acquired
     */
    AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request);

    /**
     * Invoked before each subsequent (non-first) request attempt.
     *
     * <p>Callers MUST wait for the {@code delay} returned by this call before making the next attempt. If the next attempt
     * fails, callers MUST re-call {@link #refreshRetryToken} before attempting another retry. This call invalidates the provided
     * token, and returns a new one. Callers MUST use the new token.
     *
     * <p>If the attempt was successful, callers MUST call {@link #recordSuccess}.
     *
     * @throws NullPointerException            if a required parameter is not specified
     * @throws IllegalArgumentException        if the provided token was not issued by this strategy or the provided token was
     *                                         already used for a previous refresh or success call.
     * @throws TokenAcquisitionFailedException if a token cannot be acquired
     */
    RefreshRetryTokenResponse refreshRetryToken(RefreshRetryTokenRequest request);

    /**
     * Invoked after an attempt succeeds.
     *
     * @throws NullPointerException     if a required parameter is not specified
     * @throws IllegalArgumentException if the provided token was not issued by this strategy or the provided token was already
     *                                  used for a previous refresh or success call.
     */
    RecordSuccessResponse recordSuccess(RecordSuccessRequest request);

    /**
     * Create a new {@link Builder} with the current configuration.
     *
     * <p>This is useful for modifying the strategy's behavior, like conditions or max retries.
     */
    @Override
    B toBuilder();

    /**
     * Builder to create immutable instances of {@link RetryStrategy}.
     */
    interface Builder<
        B extends Builder<B, T> & CopyableBuilder<B, T>,
        T extends ToCopyableBuilder<B, T> & RetryStrategy<B, T>>
        extends CopyableBuilder<B, T> {
        /**
         * Configure the strategy to retry when the provided predicate returns true, given a failure exception.
         */
        B retryOnException(Predicate<Throwable> shouldRetry);

        /**
         * Configure the strategy to retry when a failure exception class is equal to the provided class.
         */
        default B retryOnException(Class<? extends Throwable> throwable) {
            return retryOnException(t -> t.getClass() == throwable);
        }

        /**
         * Configure the strategy to retry when a failure exception class is an instance of the provided class (includes
         * subtypes).
         */
        default B retryOnExceptionInstanceOf(Class<? extends Throwable> throwable) {
            return retryOnException(t -> throwable.isAssignableFrom(t.getClass()));
        }

        /**
         * Configure the strategy to retry when a failure exception or one of its cause classes is equal to the provided class.
         */
        default B retryOnExceptionOrCause(Class<? extends Throwable> throwable) {
            return retryOnException(t -> {
                if (t.getClass() == throwable) {
                    return true;
                }
                Throwable cause = t.getCause();
                while (cause != null) {
                    if (cause.getClass() == throwable) {
                        return true;
                    }
                    cause = cause.getCause();
                }
                return false;
            });
        }

        /**
         * Configure the strategy to retry when a failure exception or one of its cause classes is an instance of the provided
         * class (includes subtypes).
         */
        default B retryOnExceptionOrCauseInstanceOf(Class<? extends Throwable> throwable) {
            return retryOnException(t -> {
                if (throwable.isAssignableFrom(t.getClass())) {
                    return true;
                }
                Throwable cause = t.getCause();
                while (cause != null) {
                    if (throwable.isAssignableFrom(cause.getClass())) {
                        return true;
                    }
                    cause = cause.getCause();
                }
                return false;
            });
        }

        /**
         * Configure the strategy to retry the root cause of a failure (the final cause) a failure exception is equal to the
         * provided class.
         */
        default B retryOnRootCause(Class<? extends Throwable> throwable) {
            return retryOnException(t -> {
                boolean shouldRetry = false;
                Throwable cause = t.getCause();
                while (cause != null) {
                    shouldRetry = throwable == cause.getClass();
                    cause = cause.getCause();
                }
                return shouldRetry;
            });
        }

        /**
         * Configure the strategy to retry the root cause of a failure (the final cause) a failure exception is an instance of to
         * the provided class (includes subtypes).
         */
        default B retryOnRootCauseInstanceOf(Class<? extends Throwable> throwable) {
            return retryOnException(t -> {
                boolean shouldRetry = false;
                Throwable cause = t.getCause();
                while (cause != null) {
                    shouldRetry = throwable.isAssignableFrom(cause.getClass());
                    cause = cause.getCause();
                }
                return shouldRetry;
            });
        }

        /**
         * Configure the maximum number of attempts used by this executor.
         *
         * <p>The actual number of attempts made may be less, depending on the retry strategy implementation. For example, the
         * standard and adaptive retry modes both employ short-circuiting which reduces the maximum attempts during outages.
         *
         * <p>The default value for the standard and adaptive retry strategies is 3.
         */
        B maxAttempts(int maxAttempts);

        /**
         * Build a new {@link RetryStrategy} with the current configuration on this builder.
         */
        @Override
        T build();
    }
}
