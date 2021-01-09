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

import static software.amazon.awssdk.metrics.publishers.cloudwatch.internal.CloudWatchMetricLogger.METRIC_LOGGER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.MetricUploader;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.task.AggregateMetricsTask;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.task.UploadMetricsTasks;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform.MetricCollectionAggregator;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * An implementation of {@link MetricPublisher} that aggregates and uploads metrics to Amazon CloudWatch on a periodic basis.
 *
 * <p>This simplifies the process of uploading custom metrics to CloudWatch, and can also be configured on the AWS
 * SDK clients directly to upload AWS SDK-specific metrics (e.g. request latencies, failure rates) to CloudWatch.
 *
 * <p><b>Overview</b>
 *
 * <p>This publisher aggregates metric data in memory, and periodically uploads it to CloudWatch in a background thread. This
 * minimizes the work necessary to upload metrics, allowing the caller to focus on collecting the data.
 *
 * <p>The default settings of the metrics publisher are meant to minimize memory usage and CloudWatch cost, while still
 * providing a useful amount of insight into the metric data. Care should be taken when overriding the default values on the
 * publisher, because they can result in an associated increased in memory usage and CloudWatch cost.
 *
 * <p>By default, all metrics are uploaded using summary statistics. This means that only count, maximum, minimum, sum and
 * average data is available in CloudWatch. Metric details (e.g. p90, p99) can be enabled on a per-metric basis using
 * {@link Builder#detailedMetrics(Collection)}.
 *
 * <p>See {@link Builder} for the configuration values that are available for the publisher, and how they can be used to
 * increase the functionality or decrease the cost the publisher.
 *
 * <p><b>Logging</b>
 *
 * The CloudWatchMetricPublisher logs all aggregation and upload-related logs to the
 * {@code software.amazon.awssdk.metrics.publishers.cloudwatch} namespace. To determine how many metrics are being uploaded
 * successfully without checking the CloudWatch console, you can check for a "success" message at the DEBUG level. At the TRACE
 * level, you can see exactly which metrics are being uploaded.
 *
 * <p><b>Configuring AWS SDK clients to upload client metrics</b>
 *
 * TODO
 *
 * <p><b>Uploading your own custom metrics</b>
 *
 * <i>Step 1: Define which metrics you wish to collect</i>
 *
 * <p>Metrics are described using the {@link SdkMetric#create} method. When you describe your metric, you specify
 * the name that will appear in CloudWatch and the Java data-type of the metric. The metric should be described once for your
 * entire application.
 *
 * <p>Supported types: (1) {@link Number} types (e.g. {@link Integer}, {@link Double}, etc.), (2) {@link Duration}.
 *
 * <pre>
 *     // In this and the following examples, we want to collect metrics about calls to a method we have defined: "myMethod"
 *     public static final class MyMethodMetrics {
 *         // The number of times "myMethod" has been called.
 *         private static final SdkMetric&lt;Integer&gt; MY_METHOD_CALL_COUNT =
 *                 SdkMetric.create("MyMethodCallCount", Integer.class, MetricLevel.INFO, MetricCategory.CUSTOM);
 *
 *         // The amount of time that "myMethod" took to execute.
 *         private static final SdkMetric&lt;Duration&gt; MY_METHOD_LATENCY =
 *                 SdkMetric.create("MyMethodLatency", Duration.class, MetricLevel.INFO, MetricCategory.CUSTOM);
 *     }
 * </pre>
 *
 * <p><i>Step 2: Create a {@code CloudWatchMetricPublisher}</i>
 *
 * <p>A {@code CloudWatchMetricPublisher} should be created once for your entire application, and be reused wherever it is
 * needed. {@code CloudWatchMetricPublisher}s are thread-safe, so there should be no need to create multiple instances. Most
 * people create and manage the publisher in their inversion-of-control (IoC) container (e.g. Spring/Dagger/Guice).
 *
 * <p>Note: When your application is finished with the {@code CloudWatchMetricPublisher}, make sure to {@link #close()} it. Your
 * inversion-of-control container may handle this for you on JVM shutdown.
 *
 * <p>See {@link CloudWatchMetricPublisher.Builder} for all available configuration options.
 *
 * <pre>
 *     // Create a CloudWatchMetricPublisher using a custom namespace.
 *     MetricPublisher metricPublisher = CloudWatchMetricPublisher.builder()
 *                                                                .namespace("MyApplication")
 *                                                                .build();
 * </pre>
 *
 * <p><i>Step 3: Collect and Publish Metrics</i>
 *
 * <p>Create and use a {@link MetricCollector} to collect data about your configured metrics.
 *
 * <pre>
 *     // Call "myMethod" and collect metrics about the call.
 *     Instant methodCallStartTime = Instant.now();
 *     myMethod();
 *     Duration methodCallDuration = Duration.between(methodCallStartTime, Instant.now());
 *
 *     // Write the metrics to the CloudWatchMetricPublisher.
 *     MetricCollector metricCollector = MetricCollector.create("MyMethodCall");
 *     metricCollector.reportMetric(MyCustomMetrics.MY_METHOD_CALL_COUNT, 1);
 *     metricCollector.reportMetric(MyCustomMetrics.MY_METHOD_LATENCY, methodCallDuration);
 *     MetricCollection metricCollection = metricCollector.collect();
 *
 *     metricPublisher.publish(metricCollection);
 * </pre>
 *
 * <p><b>Warning:</b> Make sure the {@link #close()} this publisher when it is done being used to release all resources it
 * consumes. Failure to do so will result in possible thread or file descriptor leaks.
 */
@ThreadSafe
@Immutable
@SdkPublicApi
public final class CloudWatchMetricPublisher implements MetricPublisher {
    /**
     * The maximum queue size for the internal {@link #executor} that is used to aggregate metric data and upload it to
     * CloudWatch. If this value is too high, memory is wasted. If this value is too low, metrics could be dropped.
     *
     * This value is not currently configurable, because it's unlikely that this is a value that customers should need to modify.
     * If customers really need control over this value, we might consider letting them instead configure the
     * {@link BlockingQueue} used on the executor. The value here depends on the type of {@code BlockingQueue} in use, and
     * we should probably not indirectly couple people to the type of blocking queue we're using.
     */
    private static final int MAXIMUM_TASK_QUEUE_SIZE = 128;

    private static final String DEFAULT_NAMESPACE = "AwsSdk/JavaSdk2";
    private static final int DEFAULT_MAXIMUM_CALLS_PER_UPLOAD = 10;
    private static final Duration DEFAULT_UPLOAD_FREQUENCY = Duration.ofMinutes(1);
    private static final Set<SdkMetric<String>> DEFAULT_DIMENSIONS = Stream.of(CoreMetric.SERVICE_ID,
                                                                               CoreMetric.OPERATION_NAME)
                                                                           .collect(Collectors.toSet());
    private static final Set<MetricCategory> DEFAULT_METRIC_CATEGORIES = Collections.singleton(MetricCategory.ALL);
    private static final MetricLevel DEFAULT_METRIC_LEVEL = MetricLevel.INFO;
    private static final Set<SdkMetric<?>> DEFAULT_DETAILED_METRICS = Collections.emptySet();

    /**
     * Whether {@link #close()} should call {@link CloudWatchAsyncClient#close()}. This is false when
     * {@link Builder#cloudWatchClient(CloudWatchAsyncClient)} was specified, meaning the customer has to close the client
     * themselves.
     */
    private final boolean closeClientWithPublisher;

    /**
     * The aggregator that takes {@link MetricCollection}s and converts them into {@link PutMetricDataRequest}s. This aggregator
     * is *not* thread safe, so it should only ever be accessed from the {@link #executor}'s thread.
     */
    private final MetricCollectionAggregator metricAggregator;

    /**
     * The uploader that takes {@link PutMetricDataRequest}s and sends them to a {@link CloudWatchAsyncClient}.
     */
    private final MetricUploader metricUploader;

    /**
     * The executor that executes {@link AggregateMetricsTask}s and {@link UploadMetricsTasks}s.
     */
    private final ExecutorService executor;

    /**
     * A scheduled executor that periodically schedules a {@link UploadMetricsTasks} on the {@link #executor} thread. Note: this
     * executor should never execute the flush task itself, because that needs access to the {@link #metricAggregator}, and the
     * {@code metricAggregator} should only ever be accessed from the {@link #executor} thread.
     */
    private final ScheduledExecutorService scheduledExecutor;

    /**
     * The maximum number of {@link PutMetricDataRequest}s that should ever be executed as part of a single
     * {@link UploadMetricsTasks}.
     */
    private final int maximumCallsPerUpload;

    private CloudWatchMetricPublisher(Builder builder) {
        this.closeClientWithPublisher = resolveCloseClientWithPublisher(builder);
        this.metricAggregator = new MetricCollectionAggregator(resolveNamespace(builder),
                                                               resolveDimensions(builder),
                                                               resolveMetricCategories(builder),
                                                               resolveMetricLevel(builder),
                                                               resolveDetailedMetrics(builder));
        this.metricUploader = new MetricUploader(resolveClient(builder));
        this.maximumCallsPerUpload = resolveMaximumCallsPerUpload(builder);

        ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("cloud-watch-metric-publisher").build();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);

        // Do not increase above 1 thread: access to MetricCollectionAggregator is not thread safe.
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                                               new ArrayBlockingQueue<>(MAXIMUM_TASK_QUEUE_SIZE),
                                               threadFactory);

        long flushFrequencyInMillis = resolveUploadFrequency(builder).toMillis();
        this.scheduledExecutor.scheduleAtFixedRate(this::flushMetricsQuietly,
                                                   flushFrequencyInMillis, flushFrequencyInMillis, TimeUnit.MILLISECONDS);
    }

    private Set<MetricCategory> resolveMetricCategories(Builder builder) {
        return builder.metricCategories == null ? DEFAULT_METRIC_CATEGORIES : new HashSet<>(builder.metricCategories);
    }

    private MetricLevel resolveMetricLevel(Builder builder) {
        return builder.metricLevel == null ? DEFAULT_METRIC_LEVEL : builder.metricLevel;
    }

    private Set<SdkMetric<?>> resolveDetailedMetrics(Builder builder) {
        return builder.detailedMetrics == null ? DEFAULT_DETAILED_METRICS : new HashSet<>(builder.detailedMetrics);
    }

    private Set<SdkMetric<String>> resolveDimensions(Builder builder) {
        return builder.dimensions == null ? DEFAULT_DIMENSIONS : new HashSet<>(builder.dimensions);
    }

    private boolean resolveCloseClientWithPublisher(Builder builder) {
        return builder.client == null;
    }

    private CloudWatchAsyncClient resolveClient(Builder builder) {
        return builder.client == null ? CloudWatchAsyncClient.create() : builder.client;
    }

    private Duration resolveUploadFrequency(Builder builder) {
        return builder.uploadFrequency == null ? DEFAULT_UPLOAD_FREQUENCY : builder.uploadFrequency;
    }

    private String resolveNamespace(Builder builder) {
        return builder.namespace == null ? DEFAULT_NAMESPACE : builder.namespace;
    }

    private int resolveMaximumCallsPerUpload(Builder builder) {
        return builder.maximumCallsPerUpload == null ? DEFAULT_MAXIMUM_CALLS_PER_UPLOAD : builder.maximumCallsPerUpload;
    }

    @Override
    public void publish(MetricCollection metricCollection) {
        try {
            executor.submit(new AggregateMetricsTask(metricAggregator, metricCollection));
        } catch (RejectedExecutionException e) {
            METRIC_LOGGER.warn(() -> "Some AWS SDK client-side metrics have been dropped because an internal executor did not "
                                     + "accept them. This usually occurs because your publisher has been shut down or you have "
                                     + "generated too many requests for the publisher to handle in a timely fashion.", e);
        }
    }

    /**
     * Flush the metrics (via a {@link UploadMetricsTasks}). In the event that the {@link #executor} task queue is full, this
     * this will retry automatically.
     *
     * This returns when the {@code UploadMetricsTask} has been submitted to the executor. The returned future is completed
     * when the metrics upload to cloudwatch has started. The inner-most future is finally completed when the upload to cloudwatch
     * has finished.
     */
    private Future<CompletableFuture<?>> flushMetrics() throws InterruptedException {
        while (!executor.isShutdown()) {
            try {
                return executor.submit(new UploadMetricsTasks(metricAggregator, metricUploader, maximumCallsPerUpload));
            } catch (RejectedExecutionException e) {
                Thread.sleep(100);
            }
        }

        return CompletableFuture.completedFuture(CompletableFuture.completedFuture(null));
    }

    private void flushMetricsQuietly() {
        try {
            flushMetrics();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            METRIC_LOGGER.error(() -> "Interrupted during metric flushing.", e);
        }
    }

    @Override
    public void close() {
        try {
            scheduledExecutor.shutdownNow();

            Future<CompletableFuture<?>> flushFuture = flushMetrics();
            executor.shutdown();

            flushFuture.get(60, TimeUnit.SECONDS) // Wait for flush to start
                       .get(60, TimeUnit.SECONDS); // Wait for flush to finish

            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                throw new TimeoutException("Internal executor did not shut down in 60 seconds.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            METRIC_LOGGER.error(() -> "Interrupted during graceful metric publisher shutdown.", e);
        } catch (ExecutionException e) {
            METRIC_LOGGER.error(() -> "Failed during graceful metric publisher shutdown.", e);
        } catch (TimeoutException e) {
            METRIC_LOGGER.error(() -> "Timed out during graceful metric publisher shutdown.", e);
        } finally {
            runQuietly(scheduledExecutor::shutdownNow, "shutting down scheduled executor");
            runQuietly(executor::shutdownNow, "shutting down executor");
            runQuietly(() -> metricUploader.close(closeClientWithPublisher), "closing metric uploader");
        }
    }

    private void runQuietly(Runnable runnable, String taskName) {
        try {
            runnable.run();
        } catch (Exception e) {
            METRIC_LOGGER.warn(() -> "Failed while " + taskName + ".", e);
        }
    }

    /**
     * Create a new {@link Builder} that can be used to create {@link CloudWatchMetricPublisher}s.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a {@link CloudWatchMetricPublisher} using all default values.
     */
    public static CloudWatchMetricPublisher create() {
        return builder().build();
    }

    /**
     * Returns {@code true} when the internal executors for this publisher are shut down.
     */
    boolean isShutdown() {
        return scheduledExecutor.isShutdown() && executor.isShutdown();
    }

    /**
     * Builder class to construct {@link CloudWatchMetricPublisher} instances. See the individual properties for which
     * configuration settings are available.
     */
    public static final class Builder {
        private CloudWatchAsyncClient client;
        private Duration uploadFrequency;
        private String namespace;
        private Integer maximumCallsPerUpload;
        private Collection<SdkMetric<String>> dimensions;
        private Collection<MetricCategory> metricCategories;
        private MetricLevel metricLevel;
        private Collection<SdkMetric<?>> detailedMetrics;

        private Builder() {
        }

        /**
         * Configure the {@link PutMetricDataRequest#namespace()} used for all put-metric-data calls from this publisher.
         *
         * <p>If this is not specified, {@code AwsSdk/JavaSdk2} will be used.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Configure the {@link CloudWatchAsyncClient} instance that should be used to communicate with CloudWatch.
         *
         * <p>If this is not specified, the {@code CloudWatchAsyncClient} will be created via
         * {@link CloudWatchAsyncClient#create()} (and will be closed when {@link #close()} is invoked).
         *
         * <p>If you specify a {@code CloudWatchAsyncClient} via this method, it <i>will not</i> be closed when this publisher
         * is closed. You will need to need to manage the lifecycle of the client yourself.
         */
        public Builder cloudWatchClient(CloudWatchAsyncClient client) {
            this.client = client;
            return this;
        }

        /**
         * Configure the frequency at which aggregated metrics are uploaded to CloudWatch and released from memory.
         *
         * <p>If this is not specified, metrics will be uploaded once per minute.
         *
         * <p>Smaller values will: (1) reduce the amount of memory used by the library (particularly when
         * {@link #detailedMetrics(Collection)} are enabled), (2) increase the number of CloudWatch calls (and therefore
         * increase CloudWatch usage cost).
         *
         * <p>Larger values will: (1) increase the amount of memory used by the library (particularly when
         * {@code detailedMetrics} are enabled), (2) increase the time it takes for metric data to appear in
         * CloudWatch, (3) reduce the number of CloudWatch calls (and therefore decrease CloudWatch usage cost).
         *
         * <p><b>Warning:</b> When {@code detailedMetrics} are enabled, all unique metric values are stored in memory until they
         * can be published to CloudWatch. A high {@code uploadFrequency} with multiple {@code detailedMetrics} enabled can
         * quickly consume heap memory while the values wait to be published to CloudWatch. In memory constrained environments, it
         * is recommended to minimize the number of {@code detailedMetrics} configured on the publisher, or to upload metric data
         * more frequently. As with all performance and resource concerns, profiling in a production-like environment is
         * encouraged.
         */
        public Builder uploadFrequency(Duration uploadFrequency) {
            this.uploadFrequency = uploadFrequency;
            return this;
        }

        /**
         * Configure the maximum number of {@link CloudWatchAsyncClient#putMetricData(PutMetricDataRequest)} calls that an
         * individual "upload" event can make to CloudWatch. Any metrics that would exceed this limit are dropped during the
         * upload, logging a warning on the {@code software.amazon.awssdk.metrics.publishers.cloudwatch} namespace.
         *
         * <p>The SDK will always attempt to maximize the number of metrics per put-metric-data call, but uploads will be split
         * into multiple put-metric-data calls if they include a lot of different metrics or if there are a lot of high-value-
         * distribution {@link #detailedMetrics(Collection)} being monitored.
         *
         * <p>This value combined with the {@link #uploadFrequency(Duration)} effectively provide a "hard cap" on the number of
         * put-metric-data calls, to prevent unbounded cost in the event that too many metrics are enabled by the user.
         *
         * <p>If this is not specified, put-metric-data calls will be capped at 10 per upload.
         */
        public Builder maximumCallsPerUpload(Integer maximumCallsPerUpload) {
            this.maximumCallsPerUpload = maximumCallsPerUpload;
            return this;
        }

        /**
         * Configure the {@link SdkMetric}s that are used to define the {@link Dimension}s metrics are aggregated under.
         *
         * <p>If this is not specified, {@link CoreMetric#SERVICE_ID} and {@link CoreMetric#OPERATION_NAME} are used, allowing
         * you to compare metrics for different services and operations.
         *
         * <p><b>Warning:</b> Configuring the dimensions incorrectly can result in a large increase in the number of unique
         * metrics and put-metric-data calls to cloudwatch, which have an associated monetary cost. Be sure you're choosing your
         * metric dimensions wisely, and that you always evaluate the cost of modifying these values on your monthly usage costs.
         *
         * <p><b>Example useful settings:</b>
         * <ul>
         * <li>{@code CoreMetric.SERVICE_ID} and {@code CoreMetric.OPERATION_NAME} (default): Separate metrics by service and
         * operation, so that you can compare latencies between AWS services and operations.</li>
         * <li>{@code CoreMetric.SERVICE_ID}, {@code CoreMetric.OPERATION_NAME} and {@code CoreMetric.HOST_NAME}: Separate
         * metrics by service, operation and host so that you can compare latencies across hosts in your fleet. Note: This should
         * only be used when your fleet is relatively small. Large fleets result in a large number of unique metrics being
         * generated.</li>
         * <li>{@code CoreMetric.SERVICE_ID}, {@code CoreMetric.OPERATION_NAME} and {@code HttpMetric.HTTP_CLIENT_NAME}: Separate
         * metrics by service, operation and HTTP client type so that you can compare latencies between different HTTP client
         * implementations.</li>
         * </ul>
         */
        public Builder dimensions(Collection<SdkMetric<String>> dimensions) {
            this.dimensions = new ArrayList<>(dimensions);
            return this;
        }

        /**
         * @see #dimensions(SdkMetric[])
         */
        @SafeVarargs
        public final Builder dimensions(SdkMetric<String>... dimensions) {
            return dimensions(Arrays.asList(dimensions));
        }

        /**
         * Configure the {@link MetricCategory}s that should be uploaded to CloudWatch.
         *
         * <p>If this is not specified, {@link MetricCategory#ALL} is used.
         *
         * <p>All {@link SdkMetric}s are associated with at least one {@code MetricCategory}. This setting determines which
         * category of metrics uploaded to CloudWatch. Any metrics {@link #publish(MetricCollection)}ed that do not fall under
         * these configured categories are ignored.
         *
         * <p>Note: If there are {@link #dimensions(Collection)} configured that do not fall under these {@code MetricCategory}
         * values, the dimensions will NOT be ignored. In other words, the metric category configuration only affects which
         * metrics are uploaded to CloudWatch, not which values can be used for {@code dimensions}.
         */
        public Builder metricCategories(Collection<MetricCategory> metricCategories) {
            this.metricCategories = new ArrayList<>(metricCategories);
            return this;
        }

        /**
         * @see #metricCategories(Collection)
         */
        public Builder metricCategories(MetricCategory... metricCategories) {
            return metricCategories(Arrays.asList(metricCategories));
        }

        /**
         * Configure the {@link MetricLevel} that should be uploaded to CloudWatch.
         *
         * <p>If this is not specified, {@link MetricLevel#INFO} is used.
         *
         * <p>All {@link SdkMetric}s are associated with one {@code MetricLevel}. This setting determines which level of metrics
         * uploaded to CloudWatch. Any metrics {@link #publish(MetricCollection)}ed that do not fall under these configured
         * categories are ignored.
         *
         * <p>Note: If there are {@link #dimensions(Collection)} configured that do not fall under this {@code MetricLevel}
         * values, the dimensions will NOT be ignored. In other words, the metric category configuration only affects which
         * metrics are uploaded to CloudWatch, not which values can be used for {@code dimensions}.
         */
        public Builder metricLevel(MetricLevel metricLevel) {
            this.metricLevel = metricLevel;
            return this;
        }

        /**
         * Configure the set of metrics for which detailed values and counts are uploaded to CloudWatch, instead of summaries.
         *
         * <p>By default, all metrics published to this publisher are summarized using {@link StatisticSet}s. This saves memory,
         * because it allows the publisher to store a fixed amount of information in memory, no matter how many different metric
         * values are published. The drawback is that metrics other than count, sum, average, maximum and minimum are not made
         * available in CloudWatch. The {@code detailedMetrics} setting instructs the publisher to store and publish itemized
         * {@link MetricDatum#values()} and {@link MetricDatum#counts()}, which enables other metrics like p90 and p99 to be
         * queried in CloudWatch.
         *
         * <p><b>Warning:</b> When {@code detailedMetrics} are enabled, all unique metric values are stored in memory until they
         * can be published to CloudWatch. A high {@code uploadFrequency} with multiple {@code detailedMetrics} enabled can
         * quickly consume heap memory while the values wait to be published to CloudWatch. In memory constrained environments, it
         * is recommended to minimize the number of {@code detailedMetrics} configured on the publisher, or to upload metric data
         * more frequently. As with all performance and resource concerns, profiling in a production-like environment is
         * encouraged.
         *
         * <p>In addition to additional heap memory usage, detailed metrics can result in more requests being sent to CloudWatch,
         * which can also introduce additional usage cost. The {@link #maximumCallsPerUpload(Integer)} acts as a safeguard against
         * too many calls being made, but if you configure multiple {@code detailedMetrics}, you may need to increase the
         * {@code maximumCallsPerUpload} limit.
         */
        public Builder detailedMetrics(Collection<SdkMetric<?>> detailedMetrics) {
            this.detailedMetrics = new ArrayList<>(detailedMetrics);
            return this;
        }

        /**
         * @see #detailedMetrics(Collection)
         */
        public Builder detailedMetrics(SdkMetric<?>... detailedMetrics) {
            return detailedMetrics(Arrays.asList(detailedMetrics));
        }

        /**
         * Build a {@link CloudWatchMetricPublisher} using the configuration currently configured on this publisher.
         */
        public CloudWatchMetricPublisher build() {
            return new CloudWatchMetricPublisher(this);
        }
    }
}
