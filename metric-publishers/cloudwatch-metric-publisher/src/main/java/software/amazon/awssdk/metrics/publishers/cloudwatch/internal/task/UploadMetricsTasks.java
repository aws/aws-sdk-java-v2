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

import static software.amazon.awssdk.metrics.publishers.cloudwatch.internal.CloudWatchMetricLogger.METRIC_LOGGER;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.MetricUploader;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform.MetricCollectionAggregator;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * A task that is executed on the {@link CloudWatchMetricPublisher}'s executor to collect requests from a
 * {@link MetricCollectionAggregator} and write them to a {@link MetricUploader}.
 */
@SdkInternalApi
public class UploadMetricsTasks implements Callable<CompletableFuture<?>> {
    private final MetricCollectionAggregator collectionAggregator;
    private final MetricUploader uploader;
    private int maximumRequestsPerFlush;

    public UploadMetricsTasks(MetricCollectionAggregator collectionAggregator,
                              MetricUploader uploader,
                              int maximumRequestsPerFlush) {
        this.collectionAggregator = collectionAggregator;
        this.uploader = uploader;
        this.maximumRequestsPerFlush = maximumRequestsPerFlush;
    }

    @Override
    public CompletableFuture<?> call() {
        try {
            List<PutMetricDataRequest> allRequests = collectionAggregator.getRequests();
            List<PutMetricDataRequest> requests = allRequests;
            if (requests.size() > maximumRequestsPerFlush) {
                METRIC_LOGGER.warn(() -> "Maximum AWS SDK client-side metric call count exceeded: " + allRequests.size() +
                                         " > " + maximumRequestsPerFlush + ". Some metric requests will be dropped. This occurs "
                                         + "when the caller has configured too many metrics or too unique of dimensions without "
                                         + "an associated increase in the maximum-calls-per-upload configured on the publisher.");
                requests = requests.subList(0, maximumRequestsPerFlush);
            }
            return uploader.upload(requests);
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }
}
