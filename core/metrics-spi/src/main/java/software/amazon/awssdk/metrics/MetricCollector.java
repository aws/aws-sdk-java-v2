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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.internal.DefaultMetricCollector;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Used to collect metrics collected by the SDK.
 */
@SdkPublicApi
public interface MetricCollector extends SdkAutoCloseable {
    /**
     * @return The name of this collector.
     */
    String name();

    /**
     * Report a metric.
     */
    <T> void reportMetric(SdkMetric<T> metric, T data);

    /**
     *
     * @param name
     * @return
     */
    MetricCollector createChild(String name);

    /**
     * Return the collected metrics.
     * <p>
     * Calling this method implicitly closes this collector.
     *
     * @return The collected metrics.
     * @see #close()
     */
    MetricCollection collect();

    /**
     * {@inheritDoc}
     * <p>
     * Closing this collector prevents users from calling {@link
     * #reportMetric(SdkMetric, Object)}, and will also close all child
     * collectors.
     */
    @Override
    void close();

    static MetricCollector create(String name) {
        return DefaultMetricCollector.create(name);
    }
}
