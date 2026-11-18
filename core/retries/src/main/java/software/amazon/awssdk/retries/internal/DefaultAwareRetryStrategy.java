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

package software.amazon.awssdk.retries.internal;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Identify a {@link RetryStrategy} that has the capacity to work with sets of default retry predicates.
 *
 * <p>
 * Implementation notes: this class should've been outside internal package,
 * but we can't fix it due to backwards compatibility reasons.
 */
@SdkProtectedApi
public interface DefaultAwareRetryStrategy extends RetryStrategy {

    /**
     * Add the specified defaults to this retry strategy
     * @param retryStrategyDefaults the defaults to add to this strategy
     * @return a new retry strategy containing the specified defaults.
     */
    DefaultAwareRetryStrategy addDefaults(RetryStrategyDefaults retryStrategyDefaults);

    /**
     * Whether this retry strategy follows retry behavior 2.1, which includes taking its suggested delay from the
     * {@code x-amz-retry-after} response header.
     */
    default boolean usesRetryBehavior21() {
        return false;
    }

    interface Builder {

        /**
         * Identify the Builder as having the specified defaults to it.
         * @param defaultPredicatesName the name the defaults to mark as added
         */
        void markDefaultAdded(String defaultPredicatesName);

        /**
         * Whether the retry strategy built by this builder follows retry behavior 2.1.
         */
        default boolean usesRetryBehavior21() {
            return false;
        }
    }
}
