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

package software.amazon.awssdk.metrics.meter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.utils.Validate;

/**
 * A {@link Gauge} implementation that stores a constant value for a metric.
 * The value stored cannot be changed after object creation
 *
 * @param <TypeT> the type of the value recorded in the Gauge
 */
@SdkPublicApi
public final class ConstantGauge<TypeT> implements Gauge<TypeT> {

    private final TypeT value;
    private final Set<MetricCategory> categories;

    private ConstantGauge(Builder<TypeT> builder) {
        this.value = Validate.notNull(builder.value, "Value cannot be null");
        this.categories = Collections.unmodifiableSet(builder.categories);
    }

    @Override
    public TypeT value() {
        return value;
    }

    @Override
    public Set<MetricCategory> categories() {
        return categories;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param value the value to store in the guage
     * @param <T> type of the value
     * @return An instance of {@link ConstantGauge} with the given {@link #value} stored in the gauge.
     */
    public static <T> ConstantGauge<T> create(T value) {
        return builder().value(value).build();
    }

    public static final class Builder<BuilderT> {
        private BuilderT value;
        private final Set<MetricCategory> categories = new HashSet<>();

        private Builder() {
        }

        /**
         * @param value the value to store in the gauge
         * @return This object for method chaining
         */
        public Builder<BuilderT> value(BuilderT value) {
            this.value = value;
            return this;
        }

        /**
         * Register the given categories in this metric
         * @param categories the set of {@link MetricCategory} this metric belongs to
         * @return This object for method chaining
         */
        public Builder<BuilderT> categories(Set<MetricCategory> categories) {
            this.categories.addAll(categories);
            return this;
        }

        /**
         * Register the given {@link MetricCategory} in this metric
         * @param category the {@link MetricCategory} to tag the metric with
         * @return This object for method chaining
         */
        public Builder<BuilderT> addCategory(MetricCategory category) {
            this.categories.add(category);
            return this;
        }

        public ConstantGauge<BuilderT> build() {
            return new ConstantGauge(this);
        }
    }
}
