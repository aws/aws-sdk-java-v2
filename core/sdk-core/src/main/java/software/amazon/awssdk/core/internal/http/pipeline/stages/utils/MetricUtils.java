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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.internal.SdkMetric;
import software.amazon.awssdk.metrics.meter.ConstantGauge;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Timer;
import software.amazon.awssdk.metrics.registry.MetricBuilderParams;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

/**
 * Helper class to register metrics in the {@link MetricRegistry} instances.
 */
@SdkInternalApi
public final class MetricUtils {

    private MetricUtils() {
    }

    /**
     * Register a {@link Timer} in the #metricRegistry using the information in given {@link SdkMetric}.
     * If there is already a metric registered with {@link SdkMetric#name()}, the existing {@link Timer} instance is returned.
     */
    public static Timer timer(MetricRegistry metricRegistry, SdkMetric metric) {
        return metricRegistry.timer(metric.name(), metricBuilderParams(metric));
    }

    /**
     * Register a {@link Counter} in the #metricRegistry using the information in given {@link SdkMetric}.
     * If there is already a metric registered with {@link SdkMetric#name()}, the existing {@link Counter} instance is returned.
     */
    public static Counter counter(MetricRegistry metricRegistry, SdkMetric metric) {
        return metricRegistry.counter(metric.name(), metricBuilderParams(metric));
    }

    /**
     * Register a {@link ConstantGauge} in the #metricRegistry. Throws an error if there is already a metric registered with
     * same {@link SdkMetric#name()}.
     */
    public static <T> ConstantGauge<T> registerConstantGauge(T value, MetricRegistry metricRegistry, SdkMetric metric) {
        return (ConstantGauge<T>) metricRegistry.register(metric.name(), ConstantGauge.builder()
                                                                                      .value(value)
                                                                                      .categories(metric.categories())
                                                                                      .build());
    }

    private static MetricBuilderParams metricBuilderParams(SdkMetric metric) {
        return MetricBuilderParams.builder()
                                  .categories(metric.categories())
                                  .build();
    }
}
