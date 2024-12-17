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

package software.amazon.awssdk.metrics.publishers.emf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;

@SdkInternalApi
public final class EmfMetricConfiguration {
    private static final String DEFAULT_NAMESPACE = "AwsSdk/JavaSdk2";
    private static final String DEFAULT_LOG_GROUP_NAME = "/aws/emf/metrics";
    private static final Set<SdkMetric<String>> DEFAULT_DIMENSIONS = Collections.emptySet();
    private static final Set<MetricCategory> DEFAULT_METRIC_CATEGORIES = Collections.singleton(MetricCategory.ALL);
    private static final MetricLevel DEFAULT_METRIC_LEVEL = MetricLevel.INFO;

    private final String namespace;
    private final String logGroupName;
    private final Collection<SdkMetric<String>> dimensions;
    private final Collection<MetricCategory> metricCategories;
    private final MetricLevel metricLevel;
    private final List<String> dimensionStrings;

    EmfMetricConfiguration(EmfMetricPublisher.Builder builder) {
        this.namespace = resolveNamespace(builder);
        this.logGroupName = resolveLogGroupName(builder);
        this.dimensions = resolveDimensions(builder);
        this.metricCategories = resolveMetricCategories(builder);
        this.metricLevel = resolveMetricLevel(builder);
        this.dimensionStrings = resolveDimensionStrings(builder);
    }

    // Add getters for all fields
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


    private static String resolveNamespace(EmfMetricPublisher.Builder builder) {
        return builder.namespace == null ? DEFAULT_NAMESPACE : builder.namespace;
    }

    private static Collection<SdkMetric<String>> resolveDimensions(EmfMetricPublisher.Builder builder) {
        return builder.dimensions == null ? DEFAULT_DIMENSIONS : new HashSet<>(builder.dimensions);
    }

    private static Collection<MetricCategory> resolveMetricCategories(EmfMetricPublisher.Builder builder) {
        return builder.metricCategories == null ? DEFAULT_METRIC_CATEGORIES : new HashSet<>(builder.metricCategories);
    }

    private static MetricLevel resolveMetricLevel(EmfMetricPublisher.Builder builder) {
        return builder.metricLevel == null ? DEFAULT_METRIC_LEVEL : builder.metricLevel;
    }

    private static List<String> resolveDimensionStrings(EmfMetricPublisher.Builder builder) {
        List<String> dimensionStrings = new ArrayList<>();
        if (builder.dimensions != null) {
            for (SdkMetric<String> dimension : builder.dimensions) {
                dimensionStrings.add(dimension.name());
            }
        } else {
            for (SdkMetric<String> dimension : DEFAULT_DIMENSIONS) {
                dimensionStrings.add(dimension.name());
            }
        }
        return dimensionStrings;
    }

    private static String resolveLogGroupName(EmfMetricPublisher.Builder builder) {
        return builder.logGroupName == null ? DEFAULT_LOG_GROUP_NAME : builder.logGroupName;
    }
}

