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
import java.util.concurrent.atomic.LongAdder;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.utils.Validate;

/**
 * A {@link Counter} implementation that stores {@link Long} values.
 */
@SdkPublicApi
public final class LongCounter implements Counter<Long> {

    private final LongAdder count;
    private final Set<MetricCategory> categories;

    private LongCounter(Builder builder) {
        this.count = new LongAdder();
        this.categories = Collections.unmodifiableSet(builder.categories);
    }

    @Override
    public void increment() {
        increment(1L);
    }

    @Override
    public void increment(Long value) {
        Validate.isNotNegative(value, "Value should not be negative. Use decrement(...) method to reduce count.");
        count.add(value);
    }

    @Override
    public void decrement() {
        decrement(1L);
    }

    @Override
    public void decrement(Long value) {
        Validate.isNotNegative(value, "Value should be positive. Use increment(...) method to increase count.");
        count.add(-value);
    }

    @Override
    public Long count() {
        return count.sum();
    }

    @Override
    public Set<MetricCategory> categories() {
        return categories;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LongCounter create() {
        return builder().build();
    }

    /**
     * Builder class to create instances of {@link LongCounter}
     */
    public static final class Builder {
        private final Set<MetricCategory> categories = new HashSet<>();

        private Builder() {
        }

        /**
         * Register the given categories in this metric
         * @param categories the set of {@link MetricCategory} this metric belongs to
         * @return This object for method chaining
         */
        public Builder categories(Set<MetricCategory> categories) {
            this.categories.addAll(categories);
            return this;
        }

        /**
         * Register the given {@link MetricCategory} in this metric
         * @param category the {@link MetricCategory} to tag the metric with
         * @return This object for method chaining
         */
        public Builder addCategory(MetricCategory category) {
            this.categories.add(category);
            return this;
        }

        public LongCounter build() {
            return new LongCounter(this);
        }
    }
}
