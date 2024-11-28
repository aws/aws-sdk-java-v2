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
 */
@SdkProtectedApi
public interface DefaultAwareRetryStrategy {

    /**
     * Check if the specified set of predicates, identified by their name, has been added to this retry strategy
     * @param defaultPredicatesName the name of the set of predicates to check
     * @return true if the named set of predicate
     */
    default boolean shouldAddDefaults(String defaultPredicatesName) {
        return false;
    }

    interface Builder {

        /**
         * Identify the Builder has having the specified set of predicate added to it.
         * @param defaultPredicatesName the name of the set of predicate
         */
        default void markDefaultAdded(String defaultPredicatesName) {

        }
    }
}
