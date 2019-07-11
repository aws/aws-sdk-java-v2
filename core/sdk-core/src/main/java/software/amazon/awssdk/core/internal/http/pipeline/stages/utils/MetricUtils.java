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
import software.amazon.awssdk.metrics.SdkMetrics;
import software.amazon.awssdk.metrics.meter.ConstantGauge;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Timer;
import software.amazon.awssdk.metrics.registry.MetricBuilderParams;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

@SdkInternalApi
public final class MetricUtils {

    private MetricUtils() {
    }

    public static Timer timer(MetricRegistry metricRegistry, SdkMetrics metric) {
        return metricRegistry
                      .timer(metric.name(), MetricBuilderParams.builder()
                                                               .categories(metric.categories())
                                                               .build());
    }

    public static Counter counter(MetricRegistry metricRegistry, SdkMetrics metric) {
        return metricRegistry.counter(metric.name(), MetricBuilderParams.builder()
                                                                        .categories(metric.categories())
                                                                        .build());
    }

    public static <T> ConstantGauge<T> registerConstantGauge(T value, MetricRegistry metricRegistry, SdkMetrics metric) {
        return (ConstantGauge<T>) metricRegistry.register(metric.name(), ConstantGauge.builder()
                                                                                      .value(value)
                                                                                      .categories(metric.categories())
                                                                                      .build());
    }
}
