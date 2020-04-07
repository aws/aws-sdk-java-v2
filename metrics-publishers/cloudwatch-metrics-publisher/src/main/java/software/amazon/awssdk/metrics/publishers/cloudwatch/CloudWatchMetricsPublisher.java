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

package software.amazon.awssdk.metrics.publishers.cloudwatch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.metrics.publisher.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.MetricConsumer;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.MetricProducer;
import software.amazon.awssdk.metrics.registry.MetricRegistry;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of the {@link MetricPublisher} that uploads metrics to Amazon CloudWatch.
 */
@SdkPublicApi
public final class CloudWatchMetricsPublisher implements MetricPublisher {

    private static final Logger log = Logger.loggerFor(CloudWatchMetricsPublisher.class);

    private final CloudWatchAsyncClient client;

    private final String namespace;

    private final int metricQueueSize;
    private final BlockingQueue<MetricDatum> queue;

    private final MetricProducer producer;
    private final ExecutorService producerExecutorService;

    private final MetricConsumer consumer;
    private final ScheduledExecutorService consumerExecutorService;

    private final AtomicBoolean publishStarted = new AtomicBoolean(false);
    private final Duration publishFrequency;

    private CloudWatchMetricsPublisher(Builder builder) {
        this.client = resolveClient(builder.client, builder.awsCredentialsProvider, builder.region);
        this.publishFrequency = Validate.notNull(builder.publishFrequency, "Publish frequency cannot be null.");
        this.namespace = Validate.notEmpty(builder.namespace, "Namespace cannot be null or empty.");
        this.metricQueueSize = Validate.isPositive(builder.metricQueueSize, "Metric queue size should be positive.");
        this.queue = new LinkedBlockingQueue<>(this.metricQueueSize);

        this.producer = MetricProducer.builder().queue(queue).build();
        this.producerExecutorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                                                                             .threadNamePrefix("sdk-metrics-cw-producer")
                                                                             .build());

        this.consumer = MetricConsumer.builder()
                                      .cloudWatchClient(client)
                                      .queue(queue)
                                      .namespace(namespace)
                                      .build();
        this.consumerExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                                                                                      .threadNamePrefix("sdk-metrics-cw-consumer")
                                                                                      .build());
    }

    /**
     * CloudWatch publisher converts the given metrics into {@link MetricDatum} instances and add them
     * to a queue for publishing.
     *
     * @param metricsRegistry registry containing the collected metrics
     */
    @Override
    public void registerMetrics(MetricRegistry metricsRegistry) {
        try {
            if (publishStarted.compareAndSet(false, true)) {
                consumerExecutorService.scheduleAtFixedRate(this::publish,
                                                            0L,
                                                            publishFrequency.toMillis(),
                                                            TimeUnit.MILLISECONDS);
            }

            producerExecutorService.execute(() -> producer.addMetrics(metricsRegistry));
        } catch (Throwable throwable) {
            log.warn(() -> "An error occurred when registering metrics in the publisher", throwable);
        }
    }

    @Override
    public CompletableFuture<Void> publish() {
        List<CompletableFuture<PutMetricDataResponse>> futures = new ArrayList<>();

        try {
            futures = consumer.call();
        } catch (Throwable throwable) {
            log.warn(() -> "An error occurred when uploading metrics to CloudWatch.", throwable);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

    @Override
    public void close() {
        try {
            if (producerExecutorService != null) {
                producerExecutorService.shutdown();
            }
        } catch (Throwable t) {
            log.warn(() -> "An error occurred when closing the CloudWatch metrics publisher producer", t);
        }

        try {
            if (consumerExecutorService != null) {
                consumerExecutorService.shutdown();
            }
        } catch (Throwable t) {
            log.warn(() -> "An error occurred when closing the CloudWatch metrics publisher consumer", t);
        }

        try {
            if (client != null) {
                client.close();
            }
        } catch (Throwable t) {
            log.warn(() -> "An error occurred when closing the CloudWatch metrics publisher client", t);
        }
    }

    private CloudWatchAsyncClient resolveClient(CloudWatchAsyncClient builderClient,
                                                AwsCredentialsProvider awsCredentialsProvider,
                                                Region region) {
        return builderClient != null ? builderClient : CloudWatchAsyncClient.builder()
                                                                            .credentialsProvider(awsCredentialsProvider)
                                                                            .region(region)
                                                                            .build();
    }

    /**
     * @return A {@link Builder} object to build {@link CloudWatchMetricsPublisher}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return A new instance of {@link CloudWatchMetricsPublisher} with all defaults.
     */
    public static CloudWatchMetricsPublisher create() {
        return builder().build();
    }

    /**
     * Builder class to construct {@link CloudWatchMetricsPublisher} instances.
     */
    public static final class Builder {
        private static final String DEFAULT_NAMESPACE = "AwsSdk/JavaSdk2x";
        private static final int QUEUE_SIZE = 1000;
        private static final Duration DEFAULT_PUBLISH_FREQUENCY = Duration.ofMinutes(1);

        private CloudWatchAsyncClient client;
        private Region region;
        private AwsCredentialsProvider awsCredentialsProvider;
        private Duration publishFrequency = DEFAULT_PUBLISH_FREQUENCY;
        private String namespace = DEFAULT_NAMESPACE;
        private int metricQueueSize = QUEUE_SIZE;

        private Builder() {
        }

        /**
         * @param client async client to use for uploads metrics to Amazon CloudWatch
         * @return This object for method chaining
         */
        public Builder cloudWatchClient(CloudWatchAsyncClient client) {
            this.client = client;
            return this;
        }

        /**
         * Specifies the {@link Region} to use for publishing metrics to CloudWatch if
         * {@link #cloudWatchClient(CloudWatchAsyncClient)} was not called.
         *
         * @param region The {@link Region} to use for CloudWatch.
         * @return This object for method chaining.
         */
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        /**
         * Specifies the {@link AwsCredentialsProvider} for publishing metrics to CloudWatch if
         * {@link #cloudWatchClient(CloudWatchAsyncClient)} was not called.
         *
         * @param awsCredentialsProvider The {@link AwsCredentialsProvider} to use for CloudWatch.
         * @return This object for method chaining.
         */
        public Builder credentialsProvider(AwsCredentialsProvider awsCredentialsProvider) {
            this.awsCredentialsProvider = awsCredentialsProvider;
            return this;
        }

        /**
         * @param publishFrequency the timeout between consecutive {@link CloudWatchMetricsPublisher#publish()} calls
         * @return This object for method chaining
         */
        public Builder publishFrequency(Duration publishFrequency) {
            this.publishFrequency = publishFrequency;
            return this;
        }

        /**
         * @param metricQueueSize max number of metrics to store in queue. If the queue is full, new metrics are dropped
         * @return This object for method chaining
         */
        public Builder metricQueueSize(int metricQueueSize) {
            this.metricQueueSize = metricQueueSize;
            return this;
        }

        /**
         * @param namespace The CloudWatch namespace for the metric data
         * @return This object for method chaining
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * @return an instance of {@link CloudWatchMetricsPublisher}
         */
        public CloudWatchMetricsPublisher build() {
            return new CloudWatchMetricsPublisher(this);
        }
    }
}
