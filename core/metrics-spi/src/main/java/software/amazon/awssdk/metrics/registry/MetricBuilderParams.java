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
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCategory;

/**
 * Optional common parameters that can be applied to all metric types. These parameters are used to build a metric instance.
 */
@SdkPublicApi
public final class MetricBuilderParams {

    private final Set<MetricCategory> categories;

    private MetricBuilderParams(Builder builder) {
        this.categories = Collections.unmodifiableSet(builder.categories);
    }

    public Set<MetricCategory> categories() {
        return categories;
    }

    /**
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to create instances of {@link MetricBuilderParams}
     */
    public static final class Builder {

        private final Set<MetricCategory> categories = new HashSet<>();

        private Builder() {
        }

        /**
         * Set the set of {@link MetricCategory} to add to the metric
         */
        public Builder categories(Set<MetricCategory> categories) {
            this.categories.addAll(categories);
            return this;
        }

        /**
         * Set the {@link MetricCategory} to add to the metric
         */
        public Builder addCategory(MetricCategory category) {
            this.categories.add(category);
            return this;
        }

        public MetricBuilderParams build() {
            return new MetricBuilderParams(this);
        }
    }
}
