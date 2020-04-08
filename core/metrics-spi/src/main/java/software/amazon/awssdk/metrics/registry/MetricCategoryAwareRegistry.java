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

package software.amazon.awssdk.metrics.registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.meter.NoOpCounter;
import software.amazon.awssdk.metrics.meter.NoOpGauge;
import software.amazon.awssdk.metrics.meter.NoOpTimer;
import software.amazon.awssdk.metrics.meter.Timer;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link MetricCategory} that registers a set of {@link MetricCategory} and
 * delegates calls to the underlying registry only if the given metric belongs to one of these categories.
 */
@SdkProtectedApi
public class MetricCategoryAwareRegistry implements MetricRegistry {

    private final MetricRegistry delegate;
    private final Set<MetricCategory> whitelisted;

    private MetricCategoryAwareRegistry(Builder builder) {
        this.delegate = Validate.notNull(builder.delegate, "MetricRegistry cannot be null");
        this.whitelisted = Collections.unmodifiableSet(builder.whitelisted);
    }

    @Override
    public Map<String, Metric> metrics() {
        return delegate.metrics();
    }

    @Override
    public List<MetricRegistry> apiCallAttemptMetrics() {
        return delegate.apiCallAttemptMetrics();
    }

    @Override
    public MetricRegistry registerApiCallAttemptMetrics() {
        return delegate.registerApiCallAttemptMetrics();
    }

    @Override
    public Metric register(String name, Metric metric) {
        if (shouldDelegate(metric)) {
            return delegate.register(name, metric);
        }
        return metric;
    }

    @Override
    public Optional<Metric> metric(String name) {
        return delegate.metric(name);
    }

    @Override
    public boolean remove(String name) {
        return delegate.remove(name);
    }

    @Override
    public Counter counter(String name, MetricBuilderParams metricBuilderParams) {
        if (shouldDelegate(metricBuilderParams)) {
            return delegate.counter(name, metricBuilderParams);
        }

        return NoOpCounter.instance();
    }

    @Override
    public Timer timer(String name, MetricBuilderParams metricBuilderParams) {
        if (shouldDelegate(metricBuilderParams)) {
            return delegate.timer(name, metricBuilderParams);
        }

        return NoOpTimer.instance();
    }

    @Override
    public <T> Gauge<T> gauge(String name, T value, MetricBuilderParams metricBuilderParams) {
        if (shouldDelegate(metricBuilderParams)) {
            return delegate.gauge(name, value, metricBuilderParams);
        }

        return NoOpGauge.instance();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if metric belongs to one of the {@link MetricCategory} in the whitelisted set.
     * Otherwise false.
     */
    private boolean shouldDelegate(Metric metric) {
        return shouldDelegate(metric.categories());
    }

    /**
     * Returns true if metric created by #metricBuilderParams belongs to one of the {@link MetricCategory} in the
     * whitelisted set. Otherwise false.
     */
    private boolean shouldDelegate(MetricBuilderParams metricBuilderParams) {
        return shouldDelegate(metricBuilderParams.categories());
    }

    private boolean shouldDelegate(Set<MetricCategory> categories) {
        return categories.stream().anyMatch(whitelisted::contains);
    }

    /**
     * Builder class to build instance of {@link MetricCategoryAwareRegistry}.
     */
    public static final class Builder {
        private MetricRegistry delegate;
        private final Set<MetricCategory> whitelisted = new HashSet<>();

        private Builder() {
        }

        /**
         * @param delegate the {@link MetricRegistry} to delegate the method calls
         * @return This object for method chaining
         */
        public Builder metricRegistry(MetricRegistry delegate) {
            this.delegate = delegate;
            return this;
        }

        public Builder categories(Set<MetricCategory> whitelisted) {
            this.whitelisted.addAll(whitelisted);
            return this;
        }

        public Builder addCategory(MetricCategory category) {
            this.whitelisted.add(category);
            return this;
        }

        public MetricCategoryAwareRegistry build() {
            return new MetricCategoryAwareRegistry(this);
        }
    }
}
