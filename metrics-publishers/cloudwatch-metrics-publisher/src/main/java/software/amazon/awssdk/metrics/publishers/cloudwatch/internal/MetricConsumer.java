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
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Retrieves {@link MetricDatum}s from the shared blocking queue and upload them to Amazon CloudWatch.
 */
@SdkInternalApi
public class MetricConsumer implements Callable<List<CompletableFuture<PutMetricDataResponse>>> {

    private static final Logger log = Logger.loggerFor(MetricConsumer.class);

    /**
     * CloudWatch limits number of {@link MetricDatum} to 20 per {@link PutMetricDataRequest}.
     */
    private static final int MAX_METRIC_PER_REQUEST = 20;

    /**
     * Limit max number of service calls to CloudWatch in a single metric publish.
     */
    private static final int MAX_SERVICE_CALLS_PER_PUBLISH = 10;

    private final CloudWatchAsyncClient client;
    private final BlockingQueue<MetricDatum> queue;
    private final String namespace;

    private MetricConsumer(Builder builder) {
        this.client = Validate.paramNotNull(builder.cloudWatchClient, "cloudWatchClient");
        this.queue = Validate.paramNotNull(builder.queue, "queue");
        this.namespace = Validate.paramNotBlank(builder.namespace, "namespace");
    }

    /**
     * If metrics are present in {@link #queue}, uploads them to Amazon CloudWatch.
     */
    @Override
    public List<CompletableFuture<PutMetricDataResponse>> call() {
        List<CompletableFuture<PutMetricDataResponse>> futures = new ArrayList<>();

        int count = 0;
        while (queue.peek() != null && ++count <= MAX_SERVICE_CALLS_PER_PUBLISH) {
            List<MetricDatum> metricDatums = metricDatums();
            if (metricDatums.isEmpty()) {
                futures.add(CompletableFuture.completedFuture(PutMetricDataResponse.builder().build()));
            }

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                                                               .namespace(namespace)
                                                               .metricData(metricDatums)
                                                               .build();

            log.debug(() -> "Call putMetricData API to upload metrics to CloudWatch");
            futures.add(client.putMetricData(request));
        }

        return futures;
    }

    private List<MetricDatum> metricDatums() {
        List<MetricDatum> metricDatums = new ArrayList<>();
        while (metricDatums.size() <= MAX_METRIC_PER_REQUEST) {
            MetricDatum datum = queue.poll();
            if (datum == null) {
                break; // queue is empty
            } else {
                metricDatums.add(datum);
            }
        }

        return metricDatums;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to create instances of {@link MetricConsumer}.
     */
    public static final class Builder {

        private CloudWatchAsyncClient cloudWatchClient;
        private BlockingQueue<MetricDatum> queue;
        private String namespace;

        private Builder() {
        }

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

        public MetricConsumer build() {
            return new MetricConsumer(this);
        }
    }
}
