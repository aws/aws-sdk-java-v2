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

package software.amazon.awssdk.retries.internal.ratelimiter;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class RateLimiterUpdateResponse {
    private double measuredTxRate;
    private double fillRate;

    private RateLimiterUpdateResponse(Builder builder) {
        this.measuredTxRate = Validate.paramNotNull(builder.measuredTxRate, "measuredTxRate");
        this.fillRate = Validate.paramNotNull(builder.fillRate, "fillRate");
    }

    public double measuredTxRate() {
        return measuredTxRate;
    }

    public double fillRate() {
        return fillRate;
    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private Double measuredTxRate;
        private Double fillRate;

        Builder() {
        }

        public Builder measuredTxRate(double measuredTxRate) {
            this.measuredTxRate = measuredTxRate;
            return this;
        }

        public Builder fillRate(double fillRate) {
            this.fillRate = fillRate;
            return this;
        }

        public RateLimiterUpdateResponse build() {
            return new RateLimiterUpdateResponse(this);
        }
    }
}
