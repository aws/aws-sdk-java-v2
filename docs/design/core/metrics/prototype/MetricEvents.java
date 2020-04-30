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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.meter.Timer;

/**
 * Object used to store metric events collected by the SDK.
 */
@SdkPublicApi
public interface MetricEvents {
    /**
     * Add the given metric with associated data.
     * @throws IllegalStateException If the given event is already present.
     * @throws IllegalArgumentException If {@code eventData} is {@code null}.
     */
    <T> void putMetricEvent(MetricEvent<T> event, T eventData);

    /**
     * Return the metric data associated with the given event. Returns {@code
     * null} if no event is found.
     */
    <T> T getMetric(MetricEvent<T> event);

    /**
     * Return the contained metric events.
     */
    Map<MetricEvent<?>, ?> getMetricEvents();
}
