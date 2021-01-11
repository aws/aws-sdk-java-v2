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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal;

import static software.amazon.awssdk.metrics.publishers.cloudwatch.internal.CloudWatchMetricLogger.METRIC_LOGGER;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

/**
 * Uploads {@link PutMetricDataRequest}s to a {@link CloudWatchAsyncClient}, logging whether it was successful or a failure to
 * the {@link CloudWatchMetricLogger#METRIC_LOGGER}.
 */
@SdkInternalApi
public class MetricUploader {
    private final CloudWatchAsyncClient cloudWatchClient;

    public MetricUploader(CloudWatchAsyncClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    /**
     * Upload the provided list of requests to CloudWatch, completing the returned future when the uploads complete. Note: This
     * will log a message if one of the provided requests fails.
     */
    public CompletableFuture<Void> upload(List<PutMetricDataRequest> requests) {
        CompletableFuture<?>[] publishResults = startCalls(requests);
        return CompletableFuture.allOf(publishResults).whenComplete((r, t) -> {
            int numRequests = publishResults.length;
            if (t != null) {
                METRIC_LOGGER.warn(() -> "Failed while publishing some or all AWS SDK client-side metrics to CloudWatch.", t);
            } else {
                METRIC_LOGGER.debug(() -> "Successfully published " + numRequests +
                                          " AWS SDK client-side metric requests to CloudWatch.");
            }
        });
    }

    private CompletableFuture<?>[] startCalls(List<PutMetricDataRequest> requests) {
        return requests.stream()
                       .peek(this::logRequest)
                       .map(cloudWatchClient::putMetricData)
                       .toArray(CompletableFuture[]::new);
    }

    private void logRequest(PutMetricDataRequest putMetricDataRequest) {
        METRIC_LOGGER.trace(() -> "Sending request to CloudWatch: " + putMetricDataRequest);
    }

    public void close(boolean closeClient) {
        if (closeClient) {
            this.cloudWatchClient.close();
        }
    }
}
