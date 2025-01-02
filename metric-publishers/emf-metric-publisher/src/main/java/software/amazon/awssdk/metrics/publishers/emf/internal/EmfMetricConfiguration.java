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

package software.amazon.awssdk.metrics.publishers.emf.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class EmfMetricConfiguration {
    private static final String DEFAULT_NAMESPACE = "AwsSdk/JavaSdk2";
    private static final Set<SdkMetric<String>> DEFAULT_DIMENSIONS = Stream.of(CoreMetric.SERVICE_ID,
                                                                               CoreMetric.OPERATION_NAME)
                                                                           .collect(Collectors.toSet());
    private static final Set<MetricCategory> DEFAULT_CATEGORIES = Collections.singleton(MetricCategory.ALL);
    private static final MetricLevel DEFAULT_METRIC_LEVEL = MetricLevel.INFO;

    private final String namespace;
    private final String logGroupName;
    private final Collection<SdkMetric<String>> dimensions;
    private final Collection<MetricCategory> metricCategories;
    private final MetricLevel metricLevel;
    private final List<String> dimensionStrings;

    private EmfMetricConfiguration(Builder builder) {
        this.namespace = builder.namespace == null ? DEFAULT_NAMESPACE : builder.namespace;
        this.logGroupName = builder.logGroupName; // This is a required field
        this.dimensions = builder.dimensions == null ? DEFAULT_DIMENSIONS : new HashSet<>(builder.dimensions);
        this.metricCategories = builder.metricCategories == null ? DEFAULT_CATEGORIES : new HashSet<>(builder.metricCategories);
        this.metricLevel = builder.metricLevel == null ? DEFAULT_METRIC_LEVEL : builder.metricLevel;
        this.dimensionStrings = resolveDimensionStrings(builder.dimensions);
    }

    private static List<String> resolveDimensionStrings(Collection<SdkMetric<String>> dimensions) {
        List<String> dimensionStrings = new ArrayList<>();
        if (dimensions != null) {
            for (SdkMetric<String> dimension : dimensions) {
                dimensionStrings.add(dimension.name());
            }
        } else {
            for (SdkMetric<String> dimension : DEFAULT_DIMENSIONS) {
                dimensionStrings.add(dimension.name());
            }
        }
        return dimensionStrings;
    }

    public static class Builder {
        private String namespace;
        private String logGroupName;
        private Collection<SdkMetric<String>> dimensions;
        private Collection<MetricCategory> metricCategories;
        private MetricLevel metricLevel;

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder logGroupName(String logGroupName) {
            this.logGroupName = logGroupName;
            return this;
        }

        public Builder dimensions(Collection<SdkMetric<String>> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder metricCategories(Collection<MetricCategory> metricCategories) {
            this.metricCategories = metricCategories;
            return this;
        }

        public Builder metricLevel(MetricLevel metricLevel) {
            this.metricLevel = metricLevel;
            return this;
        }

        public EmfMetricConfiguration build() {
            Validate.notNull(logGroupName, "logGroupName must be configured for publishing emf format log");
            return new EmfMetricConfiguration(this);
        }
    }

    // Existing getter methods remain the same
    public String getNamespace() {
        return namespace;
    }

    public String getLogGroupName() {
        return logGroupName;
    }

    public Collection<SdkMetric<String>> getDimensions() {
        return dimensions;
    }

    public Collection<MetricCategory> getMetricCategories() {
        return metricCategories;
    }

    public MetricLevel getMetricLevel() {
        return metricLevel;
    }

    public List<String> getDimensionStrings() {
        return dimensionStrings;
    }
}

