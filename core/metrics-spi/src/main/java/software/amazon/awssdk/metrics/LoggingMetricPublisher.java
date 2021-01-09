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
import software.amazon.awssdk.utils.Logger;

/**
 * An implementation of {@link MetricPublisher} that writes all published metrics to the logs at the INFO level under the
 * {@code software.amazon.awssdk.metrics.LoggingMetricPublisher} namespace.
 */
@SdkPublicApi
public final class LoggingMetricPublisher implements MetricPublisher {
    private static final Logger LOGGER = Logger.loggerFor(LoggingMetricPublisher.class);

    private LoggingMetricPublisher() {
    }

    public static LoggingMetricPublisher create() {
        return new LoggingMetricPublisher();
    }

    @Override
    public void publish(MetricCollection metricCollection) {
        LOGGER.info(() -> "Metrics published: " + metricCollection);
    }

    @Override
    public void close() {
    }
}
