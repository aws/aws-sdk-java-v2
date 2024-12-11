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
 * The set of retry predicates that are by default added to a retry strategy.
 *
 * <p>
 * Implementation notes: this class should've been outside internal package,
 * but we can't fix it due to backwards compatibility reasons.
 */
@SdkProtectedApi
public interface RetryStrategyDefaults {

    /**
     * @return The unique name that identifies this set of predicates
     */
    String name();

    /**
     * Apply this set of defaults to the provided retry strategy builder.
     * @param retryStrategyBuilder the retry strategy to apply the defaults to
     */
    void applyDefaults(RetryStrategy.Builder<?, ?> retryStrategyBuilder);
}
