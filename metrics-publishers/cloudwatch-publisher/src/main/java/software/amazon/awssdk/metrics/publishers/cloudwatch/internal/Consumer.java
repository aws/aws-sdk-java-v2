/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * Retrieves {@link MetricDatum}s from the shared blocking queue and upload them to Amazon CloudWatch.
 */
@SdkInternalApi
public class Consumer implements Callable<CompletableFuture<PutMetricDataResponse>> {
    /**
     * CloudWatch limits number of {@link MetricDatum} to 20 per {@link PutMetricDataRequest}.
     */
    private static final int MAX_METRIC_PER_REQUEST = 20;

    private final CloudWatchAsyncClient client;
    private final BlockingQueue<MetricDatum> queue;
    private final String namespace;

    private Consumer(Builder builder) {
        this.client = Validate.notNull(builder.cloudWatchClient, "CloudWatch client cannot be null");
        this.queue = Validate.notNull(builder.queue, "Queue cannot be null");
        this.namespace = Validate.notNull(builder.namespace, "Namespace cannot be null or empty.");
    }

    /**
     * If metrics are present in {@link #queue}, uploads them to Amazon CloudWatch.
     */
    @Override
    public CompletableFuture<PutMetricDataResponse> call() throws Exception {
        PutMetricDataRequest request = putMetricDataRequest();
        if (request.metricData().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return client.putMetricData(putMetricDataRequest());
    }

    private PutMetricDataRequest putMetricDataRequest() {
        List<MetricDatum> metricDatums = new ArrayList<>();
        while (metricDatums.size() <= MAX_METRIC_PER_REQUEST) {
            MetricDatum datum = queue.poll();
            if (datum == null) {
                break; // queue is empty
            } else {
                metricDatums.add(datum);
            }
        }

        return PutMetricDataRequest.builder()
                                    .namespace(namespace)
                                    .metricData(metricDatums)
                                    .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to create instances of {@link Consumer}.
     */
    public static final class Builder {

        private CloudWatchAsyncClient cloudWatchClient;
        private BlockingQueue<MetricDatum> queue;
        private String namespace;

        /**
         * Sets the async CloudWatch client to interact with CloudWatch service.
         */
        public Builder cloudWatchClient(CloudWatchAsyncClient cloudWatchClient) {
            this.cloudWatchClient = cloudWatchClient;
            return this;
        }

        /**
         * Sets the blocking queue to store the metrics.
         */
        public Builder queue(BlockingQueue<MetricDatum> queue) {
            this.queue = queue;
            return this;
        }

        /**
         * Sets the CloudWatch namespace for the metric data
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Consumer build() {
            return new Consumer(this);
        }
    }
}
