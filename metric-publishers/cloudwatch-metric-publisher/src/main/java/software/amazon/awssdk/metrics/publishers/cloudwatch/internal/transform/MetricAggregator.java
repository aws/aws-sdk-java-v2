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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Used by {@link MetricCollectionAggregator} to aggregate metrics in memory until they are ready to be added to a
 * {@link MetricDatum}.
 *
 * <p>This is either a {@link SummaryMetricAggregator} or a {@link DetailedMetricAggregator}, depending on the configured
 * {@link CloudWatchMetricPublisher.Builder#detailedMetrics(Collection)} setting.
 */
@SdkInternalApi
interface MetricAggregator {
    /**
     * The metric that this aggregator is aggregating. For example, this may be aggregating {@link CoreMetric#API_CALL_DURATION}
     * metric values. There may be multiple aggregators for a single type of metric, when their {@link #dimensions()} differ.
     */
    SdkMetric<?> metric();

    /**
     * The dimensions associated with the metric values that this aggregator is aggregating. For example, this may be aggregating
     * "S3's putObject" metrics or "DynamoDb's listTables" metrics. The exact metric being aggregated is available via
     * {@link #metric()}.
     */
    List<Dimension> dimensions();

    /**
     * Get the unit of the {@link #metric()} when it is published to CloudWatch.
     */
    StandardUnit unit();

    /**
     * Add the provided metric value to this aggregator.
     */
    void addMetricValue(double value);

    /**
     * Execute the provided consumer if this {@code MetricAggregator} is a {@link SummaryMetricAggregator}.
     */
    default void ifSummary(Consumer<SummaryMetricAggregator> summaryConsumer) {
        if (this instanceof SummaryMetricAggregator) {
            summaryConsumer.accept((SummaryMetricAggregator) this);
        }
    }

    /**
     * Execute the provided consumer if this {@code MetricAggregator} is a {@link DetailedMetricAggregator}.
     */
    default void ifDetailed(Consumer<DetailedMetricAggregator> detailsConsumer) {
        if (this instanceof DetailedMetricAggregator) {
            detailsConsumer.accept((DetailedMetricAggregator) this);
        }
    }
}
