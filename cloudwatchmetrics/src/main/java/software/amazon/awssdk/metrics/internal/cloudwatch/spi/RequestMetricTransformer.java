/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.internal.cloudwatch.spi;

import java.util.Date;
import java.util.List;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.metrics.spi.MetricType;
import software.amazon.awssdk.metrics.spi.TimingInfo;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;

/**
 * Internal SPI used to provide custom request metric transformer that can be
 * added to or override the default AWS SDK implementation. Implementation of
 * this interface should ensure the {@link Object#equals(Object)} and
 * {@link Object#hashCode()} methods are overridden as necessary.
 */
public interface RequestMetricTransformer {

    /**
     * A convenient instance of a no-op request metric transformer.
     */
    RequestMetricTransformer NONE = (requestMetric, request, response) -> null;

    /**
     * Returns a list of metric datum for the metrics collected for the given
     * request/response, or null if this transformer does not recognize the
     * specific input metric type.
     * <p>
     * Note returning an empty list means the transformer recognized the metric
     * type but concluded there is no metrics to be generated for it.
     *
     * @param metricType the predefined metric type
     */
    List<MetricDatum> toMetricData(MetricType metricType, Request<?> request, Object response);

    /**
     * Common utilities for implementing this SPI.
     */
    enum Utils {
        ;

        public static long endTimeMilli(TimingInfo ti) {
            Long endTimeMilli = ti.getEndEpochTimeMilliIfKnown();
            return endTimeMilli == null ? System.currentTimeMillis() : endTimeMilli;
        }

        public static Date endTimestamp(TimingInfo ti) {
            return new Date(endTimeMilli(ti));
        }
    }
}
