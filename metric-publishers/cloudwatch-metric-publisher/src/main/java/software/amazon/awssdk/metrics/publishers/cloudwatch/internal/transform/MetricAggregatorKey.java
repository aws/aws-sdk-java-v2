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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;

/**
 * A pairing of {@link SdkMetric} and {@link Dimension}s that can be used as a key in a map. This uniquely identifies a specific
 * {@link MetricAggregator}.
 */
@SdkInternalApi
class MetricAggregatorKey {
    private final SdkMetric<?> metric;
    private final List<Dimension> dimensions;

    MetricAggregatorKey(SdkMetric<?> metric, List<Dimension> dimensions) {
        this.metric = metric;
        this.dimensions = dimensions;
    }

    public final SdkMetric<?> metric() {
        return this.metric;
    }

    public final List<Dimension> dimensions() {
        return this.dimensions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MetricAggregatorKey that = (MetricAggregatorKey) o;

        if (!metric.equals(that.metric)) {
            return false;
        }
        return dimensions.equals(that.dimensions);
    }

    @Override
    public int hashCode() {
        int result = metric.hashCode();
        result = 31 * result + dimensions.hashCode();
        return result;
    }
}
