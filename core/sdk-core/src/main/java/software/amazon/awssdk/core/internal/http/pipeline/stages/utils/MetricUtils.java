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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import static software.amazon.awssdk.core.interceptor.MetricExecutionAttribute.ATTEMPT_METRIC_REGISTRY;
import static software.amazon.awssdk.core.interceptor.MetricExecutionAttribute.METRIC_REGISTRY;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.metrics.meter.ConstantGauge;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.NoOpCounter;
import software.amazon.awssdk.metrics.meter.NoOpGauge;
import software.amazon.awssdk.metrics.meter.NoOpTimer;
import software.amazon.awssdk.metrics.meter.Timer;
import software.amazon.awssdk.metrics.metrics.SdkDefaultMetric;
import software.amazon.awssdk.metrics.registry.MetricBuilderParams;
import software.amazon.awssdk.metrics.MetricEvents;
import software.amazon.awssdk.metrics.registry.NoOpMetricEvents;

/**
 * Helper class to register metrics in the {@link MetricEvents} instances.
 */
@SdkInternalApi
public final class MetricUtils {

    private MetricUtils() {
    }

    /**
     * Register a {@link Timer} in the #metricRegistry using the information in given {@link SdkDefaultMetric}.
     * If there is already a metric registered with {@link SdkDefaultMetric#name()}, the existing {@link Timer} instance is
     * returned.
     */
    public static Timer timer(MetricEvents metricEvents, SdkDefaultMetric metric) {
        if (metricEvents instanceof NoOpMetricEvents) {
            return NoOpTimer.instance();
        }

        return metricEvents.timer(metric.name(), metricBuilderParams(metric));
    }

    public static Timer startTimer(MetricEvents metricEvents, SdkDefaultMetric metric) {
        if (metricEvents instanceof NoOpMetricEvents) {
            return NoOpTimer.instance();
        }

        Timer timer = metricEvents.timer(metric.name(), metricBuilderParams(metric));
        timer.start();

        return timer;
    }

    /**
     * Register a {@link Counter} in the #metricRegistry using the information in given {@link SdkDefaultMetric}.
     * If there is already a metric registered with {@link SdkDefaultMetric#name()}, the existing {@link Counter} instance is
     * returned.
     */
    public static Counter counter(MetricEvents metricEvents, SdkDefaultMetric metric) {
        if (metricEvents instanceof NoOpMetricEvents) {
            return NoOpCounter.instance();
        }

        return metricEvents.counter(metric.name(), metricBuilderParams(metric));
    }

    /**
     * Register a {@link ConstantGauge} in the #metricRegistry. Throws an error if there is already a metric registered with
     * same {@link SdkDefaultMetric#name()}.
     */
    public static <T> Gauge<T> registerConstantGauge(T value, MetricEvents metricEvents, SdkDefaultMetric metric) {
        if (metricEvents instanceof NoOpMetricEvents) {
            return NoOpGauge.instance();
        }

        return (ConstantGauge<T>) metricEvents.register(metric.name(), ConstantGauge.builder()
                                                                                          .value(value)
                                                                                          .categories(metric.categories())
                                                                                          .build());
    }

    public static MetricEvents newRegistry(ExecutionAttributes executionAttributes) {
        MetricEvents apiCallMR = executionAttributes.getAttribute(METRIC_REGISTRY);
        MetricUtils.counter(apiCallMR, SdkDefaultMetric.API_CALL_ATTEMPT_COUNT)
                   .increment();

        // From now on, downstream calls should use this attempt metric registry to record metrics
        MetricEvents attemptMR = apiCallMR.registerApiCallAttemptMetrics();
        executionAttributes.putAttribute(ATTEMPT_METRIC_REGISTRY, attemptMR);
        return attemptMR;
    }

    private static MetricBuilderParams metricBuilderParams(SdkDefaultMetric metric) {
        return MetricBuilderParams.builder()
                                  .categories(metric.categories())
                                  .build();
    }
}
