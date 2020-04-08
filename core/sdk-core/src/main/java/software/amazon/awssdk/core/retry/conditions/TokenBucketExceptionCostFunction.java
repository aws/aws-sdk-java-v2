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

package software.amazon.awssdk.core.retry.conditions;

import java.util.function.Function;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.retry.DefaultTokenBucketExceptionCostFunction;

/**
 * A function used by {@link TokenBucketRetryCondition} to determine how many tokens should be removed from the bucket when an
 * exception is encountered. This can be implemented directly, or using the helper methods provided by the {@link #builder()}.
 */
@SdkPublicApi
@FunctionalInterface
@ThreadSafe
public interface TokenBucketExceptionCostFunction extends Function<SdkException, Integer> {
    /**
     * Create an exception cost function using exception type matchers built into the SDK. This interface may be implemented
     * directly, or created via a builder.
     */
    static Builder builder() {
        return new DefaultTokenBucketExceptionCostFunction.Builder();
    }

    /**
     * A helper that can be used to assign exception costs to specific exception types, created via {@link #builder()}.
     */
    @NotThreadSafe
    interface Builder {
        /**
         * Specify the number of tokens that should be removed from the token bucket when throttling exceptions (e.g. HTTP status
         * code 429) are encountered.
         */
        Builder throttlingExceptionCost(int cost);

        /**
         * Specify the number of tokens that should be removed from the token bucket when no other exception type in this
         * function is matched. This field is required.
         */
        Builder defaultExceptionCost(int cost);

        /**
         * Create a {@link TokenBucketExceptionCostFunction} using the values configured on this builder.
         */
        TokenBucketExceptionCostFunction build();
    }
}
