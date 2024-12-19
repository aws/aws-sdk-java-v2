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

    public EmfMetricConfiguration(
        String namespace,
        String logGroupName,
        Collection<SdkMetric<String>> dimensions,
        Collection<MetricCategory> metricCategories,
        MetricLevel metricLevel
    ) {
        this.namespace = namespace == null ? DEFAULT_NAMESPACE : namespace;
        this.logGroupName = logGroupName == null ? DEFAULT_LOG_GROUP_NAME : logGroupName;
        this.dimensions = dimensions == null ? DEFAULT_DIMENSIONS : new HashSet<>(dimensions);
        this.metricCategories = metricCategories == null ? DEFAULT_METRIC_CATEGORIES : new HashSet<>(metricCategories);
        this.metricLevel = metricLevel == null ? DEFAULT_METRIC_LEVEL : metricLevel;
        this.dimensionStrings = resolveDimensionStrings(dimensions);
    }

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

}

