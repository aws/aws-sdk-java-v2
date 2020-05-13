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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultMetricCollector implements MetricCollector {
    private final String name;
    private final Function<Collection<MetricRecord<?>>, Collection<MetricRecord<?>>> metricsMapper;
    private Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = new LinkedHashMap<>();
    private final List<MetricCollector> children = new ArrayList<>();

    private boolean closed = false;

    private MetricCollection collection;

    public DefaultMetricCollector(String name, Function<Collection<MetricRecord<?>>, Collection<MetricRecord<?>>> metricsMapper) {
        this.name = name;
        this.metricsMapper = metricsMapper;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public <T> void reportMetric(SdkMetric<T> metric, T data) {
        if (isClosed()) {
            throw new IllegalStateException("This collector has already been closed");
        }
        metrics.compute(metric, (m,l) -> new ArrayList<>())
                .add(new DefaultMetricRecord<>(metric, data));
    }

    @Override
    public MetricCollector createChild(String name) {
        MetricCollector child = new DefaultMetricCollector(name, metricsMapper);
        children.add(child);
        return child;
    }

    @Override
    public MetricCollection collect() {
        if (!isClosed()) {
            close();
        }

        if (collection == null) {
            // TODO: filter through mapper
            List<MetricCollection> collectedChildren = children.stream()
                    .map(MetricCollector::collect)
                    .collect(Collectors.toList());

            collection = new DefaultMetricCollection(name, metrics, collectedChildren);
        }

        return collection;
    }

    @Override
    public void close() {
        if (closed) return;

        children.forEach(MetricCollector::close);

        this.closed = true;
    }

    public static MetricCollector create(String name) {
        Validate.notEmpty(name, "name");
        return new DefaultMetricCollector(name, Function.identity());
    }

    private boolean isClosed() {
        return closed;
    }
}
