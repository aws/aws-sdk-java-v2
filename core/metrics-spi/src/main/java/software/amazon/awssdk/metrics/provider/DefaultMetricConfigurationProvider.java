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

package software.amazon.awssdk.metrics.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * The default implementation of {@link MetricConfigurationProvider} interface.
 */
@SdkPublicApi
public final class DefaultMetricConfigurationProvider implements MetricConfigurationProvider {

    private final boolean enabled;
    private final Set<MetricCategory> metricCategories;

    private DefaultMetricConfigurationProvider(Builder builder) {
        this.enabled = builder.enabled;
        this.metricCategories = Collections.unmodifiableSet(resolveCategories(builder.categories));
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public Set<MetricCategory> metricCategories() {
        return metricCategories;
    }

    /**
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    private Set<MetricCategory> resolveCategories(Set<MetricCategory> categories) {
        if (CollectionUtils.isNullOrEmpty(categories)) {
            return new HashSet<>(Arrays.asList(MetricCategory.DEFAULT));
        }

        return categories;
    }

    public static final class Builder {
        // Metrics are disabled by default
        private boolean enabled = false;

        private final Set<MetricCategory> categories = new HashSet<>();

        private Builder() {
        }

        /**
         * @param enabled set true to enable metrics. Set to false by default
         * @return
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
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

        public DefaultMetricConfigurationProvider build() {
            return new DefaultMetricConfigurationProvider(this);
        }
    }
}
