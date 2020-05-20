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

/**
 * Used to collect metrics collected by the SDK.
 * <p>
 * Collectors are allowed to nest, allowing metrics to be collected within the
 * context of other metrics.
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
    <T> void reportMetric(SdkMetric<T> metric, T value);

    /**
     *
     * @param name The name of the child collector.
     * @return The child collector.
     */
    MetricCollector createChild(String name);

    /**
     * Return the collected metrics. The returned {@code MetricCollection} must
     * preserve the children of this collector; in other words the tree formed
     * by this collector and its children should be identical to the tree formed
     * by the returned {@code MetricCollection} and its child collections.
     * <p>
     * Calling {@code collect()} prevents further invocations of {@link
     * #reportMetric(SdkMetric, Object)}.
     *
     * @return The collected metrics.
     */
    MetricCollection collect();

    static MetricCollector create(String name) {
        return DefaultMetricCollector.create(name);
    }
}
