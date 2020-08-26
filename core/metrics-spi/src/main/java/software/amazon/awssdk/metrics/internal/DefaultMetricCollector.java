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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultMetricCollector implements MetricCollector {
    private static final Logger log = Logger.loggerFor(DefaultMetricCollector.class);
    private final String name;
    private final Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = new LinkedHashMap<>();
    private final List<MetricCollector> children = new ArrayList<>();

    public DefaultMetricCollector(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public synchronized <T> void reportMetric(SdkMetric<T> metric, T data) {
        metrics.computeIfAbsent(metric, (m) -> new ArrayList<>())
               .add(new DefaultMetricRecord<>(metric, data));
    }

    @Override
    public synchronized MetricCollector createChild(String name) {
        MetricCollector child = new DefaultMetricCollector(name);
        children.add(child);
        return child;
    }

    @Override
    public synchronized MetricCollection collect() {
        List<MetricCollection> collectedChildren = children.stream()
                .map(MetricCollector::collect)
                .collect(Collectors.toList());

        DefaultMetricCollection metricRecords = new DefaultMetricCollection(name, metrics, collectedChildren);

        log.debug(() -> "Collected metrics records: " + metricRecords);
        return metricRecords;
    }

    public static MetricCollector create(String name) {
        Validate.notEmpty(name, "name");
        return new DefaultMetricCollector(name);
    }

    @Override
    public String toString() {
        return ToString.builder("DefaultMetricCollector")
            .add("metrics", metrics).build();
    }
}
