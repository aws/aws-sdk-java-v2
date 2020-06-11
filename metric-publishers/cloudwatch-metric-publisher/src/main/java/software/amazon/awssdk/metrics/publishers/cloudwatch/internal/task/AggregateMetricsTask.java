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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal.task;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform.MetricCollectionAggregator;

/**
 * A task that is executed on the {@link CloudWatchMetricPublisher}'s executor to add a {@link MetricCollection} to a
 * {@link MetricCollectionAggregator}.
 */
@SdkInternalApi
public class AggregateMetricsTask implements Runnable {
    private final MetricCollectionAggregator collectionAggregator;
    private final MetricCollection metricCollection;

    public AggregateMetricsTask(MetricCollectionAggregator collectionAggregator,
                                MetricCollection metricCollection) {
        this.collectionAggregator = collectionAggregator;
        this.metricCollection = metricCollection;
    }

    @Override
    public void run() {
        collectionAggregator.addCollection(metricCollection);
    }
}
