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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.core.retry.conditions.TokenBucketExceptionCostFunction;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class DefaultTokenBucketExceptionCostFunction implements TokenBucketExceptionCostFunction {
    private final Integer throttlingExceptionCost;
    private final int defaultExceptionCost;

    private DefaultTokenBucketExceptionCostFunction(Builder builder) {
        this.throttlingExceptionCost = builder.throttlingExceptionCost;
        this.defaultExceptionCost = Validate.paramNotNull(builder.defaultExceptionCost, "defaultExceptionCost");
    }

    @Override
    public Integer apply(SdkException e) {
        if (throttlingExceptionCost != null && RetryUtils.isThrottlingException(e)) {
            return throttlingExceptionCost;
        }

        return defaultExceptionCost;
    }

    @Override
    public String toString() {
        return ToString.builder("TokenBucketExceptionCostCalculator")
                       .add("throttlingExceptionCost", throttlingExceptionCost)
                       .add("defaultExceptionCost", defaultExceptionCost)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultTokenBucketExceptionCostFunction that = (DefaultTokenBucketExceptionCostFunction) o;

        if (defaultExceptionCost != that.defaultExceptionCost) {
            return false;
        }

        return throttlingExceptionCost != null ?
               throttlingExceptionCost.equals(that.throttlingExceptionCost) :
               that.throttlingExceptionCost == null;
    }

    @Override
    public int hashCode() {
        int result = throttlingExceptionCost != null ? throttlingExceptionCost.hashCode() : 0;
        result = 31 * result + defaultExceptionCost;
        return result;
    }

    public static final class Builder implements TokenBucketExceptionCostFunction.Builder {
        private Integer throttlingExceptionCost;
        private Integer defaultExceptionCost;

        public TokenBucketExceptionCostFunction.Builder throttlingExceptionCost(int cost) {
            this.throttlingExceptionCost = cost;
            return this;
        }

        public TokenBucketExceptionCostFunction.Builder defaultExceptionCost(int cost) {
            this.defaultExceptionCost = cost;
            return this;
        }

        public TokenBucketExceptionCostFunction build() {
            return new DefaultTokenBucketExceptionCostFunction(this);
        }

    }
}
