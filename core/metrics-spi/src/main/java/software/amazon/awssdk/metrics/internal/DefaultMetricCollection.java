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

import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultMetricCollection implements MetricCollection {
    private final String name;
    private final Map<SdkMetric<?>, List<MetricRecord<?>>> metrics;
    private final List<MetricCollection> children;
    private final Instant creationTime;

    public DefaultMetricCollection(String name, Map<SdkMetric<?>,
        List<MetricRecord<?>>> metrics, List<MetricCollection> children) {
        this.name = name;
        this.metrics = new HashMap<>(metrics);
        this.children = children != null ? Collections.unmodifiableList(new ArrayList<>(children)) : Collections.emptyList();
        this.creationTime = Instant.now();
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
                    .collect(toList());
            return (List<T>) Collections.unmodifiableList(values);
        }
        return Collections.emptyList();
    }

    @Override
    public List<MetricCollection> children() {
        return children;
    }

    @Override
    public Instant creationTime() {
        return creationTime;
    }

    @Override
    public Iterator<MetricRecord<?>> iterator() {
        return metrics.values().stream()
                      .flatMap(List::stream)
                      .iterator();
    }

    @Override
    public String toString() {
        return ToString.builder("MetricCollection")
                       .add("name", name)
                       .add("metrics", metrics.values().stream().flatMap(List::stream).collect(toList()))
                       .add("children", children)
                       .build();
    }
}
