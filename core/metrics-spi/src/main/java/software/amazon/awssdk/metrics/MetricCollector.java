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

package software.amazon.awssdk.metrics;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.internal.DefaultMetricCollector;

/**
 * Used to collect metrics reported by the SDK.
 */
@NotThreadSafe
@SdkPublicApi
public interface MetricCollector {
    /**
     * @return The name of this collector.
     */
    String name();

    /**
     * Report a metric.
     */
    <T> void reportMetric(SdkMetric<T> metric, T data);

    /**
     * Create a child of this metric collector.
     *
     * @param name The name of the child collector.
     * @return The child collector.
     */
    MetricCollector createChild(String name);

    /**
     * Return the collected metrics.
     * <p>
     * Calling {@code collect()} prevents further invocations of {@link #reportMetric(SdkMetric, Object)}.
     * @return The collected metrics.
     */
    MetricCollection collect();

    static MetricCollector create(String name) {
        return DefaultMetricCollector.create(name);
    }
}
