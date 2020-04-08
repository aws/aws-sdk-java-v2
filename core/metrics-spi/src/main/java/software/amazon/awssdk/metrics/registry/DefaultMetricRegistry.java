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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.DefaultGauge;
import software.amazon.awssdk.metrics.meter.DefaultTimer;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.LongCounter;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.meter.Timer;

/**
 * Default implementation of {@link MetricRegistry} used by the SDK
 */
@SdkProtectedApi
public final class DefaultMetricRegistry implements MetricRegistry {

    private final ConcurrentMap<String, Metric> metrics;
    private final List<MetricRegistry> attemptMetrics;

    private DefaultMetricRegistry() {
        this.metrics = new ConcurrentHashMap<>();
        this.attemptMetrics = new ArrayList<>();
    }

    /**
     * @return a new instance of the {@link DefaultMetricRegistry}
     */
    public static DefaultMetricRegistry create() {
        return new DefaultMetricRegistry();
    }

    @Override
    public Map<String, Metric> metrics() {
        return Collections.unmodifiableMap(metrics);
    }

    @Override
    public List<MetricRegistry> apiCallAttemptMetrics() {
        return Collections.unmodifiableList(attemptMetrics);
    }

    @Override
    public MetricRegistry registerApiCallAttemptMetrics() {
        MetricRegistry registry = create();
        this.attemptMetrics.add(registry);
        return registry;
    }

    @Override
    public Metric register(String name, Metric metric) {
        Metric existing = metrics.putIfAbsent(name, metric);

        if (existing != null) {
            throw new IllegalArgumentException("A metric with name " + name + " already exists in the registry");
        }

        return metric;
    }

    @Override
    public Optional<Metric> metric(String name) {
        return Optional.ofNullable(metrics.getOrDefault(name, null));
    }

    @Override
    public boolean remove(String name) {
        return metrics.remove(name) != null;
    }

    @Override
    public Counter counter(String name, MetricBuilderParams metricBuilderParams) {
        return getOrAdd(name, MetricBuilder.COUNTERS, metricBuilderParams);
    }

    @Override
    public Timer timer(String name, MetricBuilderParams metricBuilderParams) {
        return getOrAdd(name, MetricBuilder.TIMERS, metricBuilderParams);
    }

    @Override
    public <T> Gauge<T> gauge(String name, T value, MetricBuilderParams metricBuilderParams) {
        DefaultGauge<T> gauge = (DefaultGauge) getOrAdd(name, MetricBuilder.GAUGES, metricBuilderParams);
        gauge.value(value);
        return gauge;
    }

    @Override
    public void clear() {
        if (metrics != null) {
            metrics.clear();
        }

        for (MetricRegistry mr : attemptMetrics) {
            mr.clear();
        }
    }

    private <T extends Metric> T getOrAdd(String name, MetricBuilder<T> builder, MetricBuilderParams metricBuilderParams) {
        Metric metric = metrics.get(name);
        if (builder.isInstance(metric)) {
            return (T) metric;
        } else if (metric == null) {
            try {
                return (T) register(name, builder.newMetric(metricBuilderParams));
            } catch (IllegalArgumentException e) {
                Metric added = metrics.get(name);
                if (builder.isInstance(added)) {
                    return (T) added;
                }
            }
        }

        throw new IllegalArgumentException(name + " is already used for a different type of metric");
    }


    private interface MetricBuilder<T extends Metric> {
        MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric(MetricBuilderParams metricBuilderParams) {
                return LongCounter.builder()
                                  .categories(metricBuilderParams.categories())
                                  .build();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        };

        MetricBuilder<Timer> TIMERS = new MetricBuilder<Timer>() {
            @Override
            public Timer newMetric(MetricBuilderParams metricBuilderParams) {
                return DefaultTimer.builder()
                                   .categories(metricBuilderParams.categories())
                                   .build();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Timer.class.isInstance(metric);
            }
        };

        MetricBuilder<Gauge> GAUGES = new MetricBuilder<Gauge>() {
            @Override
            public Gauge newMetric(MetricBuilderParams metricBuilderParams) {
                return DefaultGauge.builder()
                                   .categories(metricBuilderParams.categories())
                                   .build();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Gauge.class.isInstance(metric);
            }
        };

        /**
         * @return a new metric instance of type T
         * @param metricBuilderParams Optional parameters that can be used for constructing a new metric
         */
        T newMetric(MetricBuilderParams metricBuilderParams);

        /**
         * @return true if given #metric is instance of type T. Otherwise false.
         */
        boolean isInstance(Metric metric);
    }
}
