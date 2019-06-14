/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * Default implemenation of {@link MetricRegistry} used by the SDk
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
    public Map<String, Metric> getMetrics() {
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

        return existing;
    }

    @Override
    public Optional<Metric> metric(String name) {
        return Optional.ofNullable(metrics.getOrDefault(name, null));
    }

    @Override
    public boolean remove(String name) {
        Metric metric = metrics.remove(name);
        return metric != null;
    }

    @Override
    public Counter counter(String name) {
        return getOrAdd(name, MetricBuilder.COUNTERS);
    }

    @Override
    public Timer timer(String name) {
        return getOrAdd(name, MetricBuilder.TIMERS);
    }

    @Override
    public <T> Gauge<T> gauge(String name, T value) {
        DefaultGauge<T> gauge = (DefaultGauge) getOrAdd(name, MetricBuilder.GAUGES);
        gauge.value(value);
        return gauge;
    }

    private <T extends Metric> T getOrAdd(String name, MetricBuilder<T> builder) {
        Metric metric = metrics.get(name);
        if (builder.isInstance(metric)) {
            return (T) metric;
        } else if (metric == null) {
            try {
                return (T) register(name, builder.newMetric());
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
        /**
         * @return a new metric instance of type T
         */
        T newMetric();

        /**
         * @return true if given #metric is instance of type T. Otherwise false.
         */
        boolean isInstance(Metric metric);

        MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric() {
                return LongCounter.create();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        };

        MetricBuilder<Timer> TIMERS = new MetricBuilder<Timer>() {
            @Override
            public Timer newMetric() {
                return DefaultTimer.builder().build();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Timer.class.isInstance(metric);
            }
        };

        MetricBuilder<Gauge> GAUGES = new MetricBuilder<Gauge>() {
            @Override
            public Gauge newMetric() {
                return DefaultGauge.create(null);
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Gauge.class.isInstance(metric);
            }
        };
    }
}
