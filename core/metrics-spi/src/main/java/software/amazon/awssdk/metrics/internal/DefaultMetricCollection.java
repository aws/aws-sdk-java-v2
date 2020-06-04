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

package software.amazon.awssdk.metrics.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;

@SdkInternalApi
public final class DefaultMetricCollection implements MetricCollection {
    private final String name;
    private final Map<SdkMetric<?>, List<MetricRecord<?>>> metrics;
    private final List<MetricCollection> children;


    public DefaultMetricCollection(String name, Map<SdkMetric<?>,
                                   List<MetricRecord<?>>> metrics,
                                   List<MetricCollection> children) {
        this.name = name;
        this.metrics = metrics;
        this.children = children != null ? Collections.unmodifiableList(children) : Collections.emptyList();
    }

    @Override
    public String name() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> metricValues(SdkMetric<T> metric) {
        if (metrics.containsKey(metric)) {
            List<MetricRecord<?>> metricRecords = metrics.get(metric);
            List<?> values = metricRecords.stream()
                    .map(MetricRecord::value)
                    .collect(Collectors.toList());
            return (List<T>) Collections.unmodifiableList(values);
        }
        return Collections.emptyList();
    }

    @Override
    public List<MetricCollection> children() {
        return children;
    }

    @Override
    public Iterator<MetricRecord<?>> iterator() {
        return metrics.values().stream()
                .flatMap(List::stream)
                .iterator();
    }
}
