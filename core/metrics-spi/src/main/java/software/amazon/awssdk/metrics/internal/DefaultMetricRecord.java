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

package software.amazon.awssdk.metrics.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultMetricRecord<T> implements MetricRecord<T> {
    private final SdkMetric<T> metric;
    private final T value;

    public DefaultMetricRecord(SdkMetric<T> metric, T value) {
        this.metric = metric;
        this.value = value;
    }

    @Override
    public SdkMetric<T> metric() {
        return metric;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public String toString() {
        return ToString.builder("MetricRecord")
                       .add("metric", metric.name())
                       .add("value", value)
                       .build();
    }
}
