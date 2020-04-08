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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.meter.NoOpCounter;
import software.amazon.awssdk.metrics.meter.NoOpGauge;
import software.amazon.awssdk.metrics.meter.NoOpTimer;
import software.amazon.awssdk.metrics.meter.Timer;

/**
 * A NoOp implementation of {@link MetricRegistry} interface.
 */
@SdkInternalApi
public final class NoOpMetricRegistry implements MetricRegistry {

    private static final NoOpMetricRegistry INSTANCE = new NoOpMetricRegistry();

    /**
     * @return A singleton instance of the {@link NoOpMetricRegistry}.
     */
    public static NoOpMetricRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<String, Metric> metrics() {
        return Collections.unmodifiableMap(Collections.EMPTY_MAP);
    }

    @Override
    public List<MetricRegistry> apiCallAttemptMetrics() {
        return Collections.unmodifiableList(Collections.EMPTY_LIST);
    }

    @Override
    public MetricRegistry registerApiCallAttemptMetrics() {
        return getInstance();
    }

    @Override
    public Metric register(String name, Metric metric) {
        return metric;
    }

    @Override
    public Optional<Metric> metric(String name) {
        return Optional.empty();
    }

    @Override
    public boolean remove(String name) {
        return false;
    }

    @Override
    public Counter counter(String name, MetricBuilderParams metricBuilderParams) {
        return NoOpCounter.instance();
    }

    @Override
    public Timer timer(String name, MetricBuilderParams metricBuilderParams) {
        return NoOpTimer.instance();
    }

    @Override
    public <T> Gauge<T> gauge(String name, T value, MetricBuilderParams metricBuilderParams) {
        return NoOpGauge.instance();
    }

    @Override
    public void clear() {

    }
}
