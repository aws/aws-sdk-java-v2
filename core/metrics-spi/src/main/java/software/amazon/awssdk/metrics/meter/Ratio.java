/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.meter;

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A ratio of one quantity to another.
 */
@SdkPublicApi
public final class Ratio implements ToCopyableBuilder<Ratio.Builder, Ratio> {

    private final double numerator;
    private final double denominator;

    private Ratio(Builder builder) {
        this.numerator = builder.numerator;
        this.denominator = builder.denominator;
    }

    /**
     * Returns the ratio, which is either a {@code double} between 0 and 1 (inclusive) or
     * {@code NaN}.
     *
     * @return the ratio
     */
    public double value() {
        if (isNaN(denominator) || isInfinite(denominator) || denominator == 0) {
            return Double.NaN;
        }
        return numerator / denominator;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().numerator(numerator)
                        .denominator(denominator);
    }

    @Override
    public String toString() {
        return numerator + ":" + denominator;
    }

    public static final class Builder implements CopyableBuilder<Builder, Ratio> {
        private double numerator;
        private double denominator;

        private Builder() {
        }

        /**
         * @param numerator the value to use for numerator in the ratio
         * @return This object for method chaining
         */
        public Builder numerator(double numerator) {
            this.numerator = numerator;
            return this;
        }

        /**
         * @param denominator the value to use for denominator in the ratio
         * @return This object for method chaining
         */
        public Builder denominator(double denominator) {
            this.denominator = denominator;
            return this;
        }

        @Override
        public Ratio build() {
            return new Ratio(this);
        }
    }
}
