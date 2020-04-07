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
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCategory;

/**
 * A basic implementation of {@link Gauge} that has ability to set, update and return a single value.
 *
 * @param <TypeT> type of the value stored in gauge
 */
@SdkPublicApi
public final class DefaultGauge<TypeT> implements Gauge<TypeT> {

    private final AtomicReference<TypeT> atomicReference;
    private final Set<MetricCategory> categories;

    private DefaultGauge(Builder<TypeT> builder) {
        this.atomicReference = new AtomicReference<>(builder.value);
        this.categories = Collections.unmodifiableSet(builder.categories);
    }

    /**
     * Update the value stored in the gauge
     * @param value the new value to store in the gauge
     */
    public void value(TypeT value) {
        this.atomicReference.set(value);
    }

    @Override
    public TypeT value() {
        return atomicReference.get();
    }

    @Override
    public Set<MetricCategory> categories() {
        return categories;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param initialValue the value to stored in the gauge instance when its created
     * @param <T> type of the value
     * @return A new instance of {@link DefaultGauge} with the given #initialValue stored in the gauge.
     */
    public static <T> DefaultGauge<T> create(T initialValue) {
        return builder().value(initialValue).build();
    }

    public static final class Builder<BuilderT> {
        private BuilderT value;
        private final Set<MetricCategory> categories = new HashSet<>();

        private Builder() {
        }

        /**
         * @param value the initial value to store in the gauge
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

        public DefaultGauge<BuilderT> build() {
            return new DefaultGauge(this);
        }
    }
}
