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

/*
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

package software.amazon.awssdk.metrics.internal.cloudwatch;

import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

/**
 * Configuration for the default AWS SDK collection implementation. This class
 * is not intended to be used directly by client code except for cases where the
 * default behavior of the internal Amazon CloudWatch collector implementation
 * needs to be customized.
 * <p>
 * Example:
 *
 * <pre>
 * /**
 *  * My custom Request Metric Collector by extending from the internal Amazon CloudWatch
 *  * implementation.
 *  &#42;/
 * static class MyCloudWatchMetricCollector extends
 *         CloudWatchRequestMetricCollector {
 *     MyCloudWatchMetricCollector(CloudWatchMetricConfig config) {
 *         super(config);
 *     }
 * }
 * MyCloudWatchMetricCollector myCollector = new MyCloudWatchMetricCollector(
 *         new CloudWatchMetricConfig()
 *                 .withQueuePollTimeoutMilli(60000)
 *                 .withMetricQueueSize(1000)
 *                 .withCredentialsProvider(
 *                         new DefaultAWSCredentialsProviderChain())
 *                 .withCloudWatchEndPoint(&quot;monitoring.us-west-2.amazonaws.com&quot;)
 *                 .withPredefinedMetrics(
 *                         new HashSet&lt;Field&gt;(Arrays.asList(Field.HttpRequestTime,
 *                                 Field.ResponseProcessingTime))));
 * myCollector.start();
 * // Enable the AWS SDK level request metric collection with a custom collector
 * AwsSdkMetrics.setRequestMetricCollector(myCollector);
 * </pre>
 *
 * @see AwsSdkMetrics
 */
@NotThreadSafe
public class CloudWatchMetricConfig {
    /**
     * Default metrics queue size. If the queue size
     * exceeds this value, then excessive metrics will be dropped to prevent
     * resource exhaustion.
     */
    public static final int DEFAULT_METRICS_QSIZE = 1000;
    /**
     * Default timeout in millisecond for queue polling.  Set to one-minute
     * which is the finest granularity of Amazon CloudWatch.
     */
    public static final int DEFAULT_QUEUE_POLL_TIMEOUT_MILLI = (int) TimeUnit.MINUTES.toMillis(1);
    static final String NAMESPACE_DELIMITER = "/";
    /**
     * Maximum number of metric data that Amazon CloudWatch can
     * accept in a single request
     */
    static final int MAX_METRICS_DATUM_SIZE = 20;

    /**
     * Number of milliseconds to wait before the polling of the metrics queue
     * times out.
     */
    private long queuePollTimeoutMilli = DEFAULT_QUEUE_POLL_TIMEOUT_MILLI;

    private int metricQueueSize = DEFAULT_METRICS_QSIZE;
    private CloudWatchClient cloudWatchClient;

    /**
     * Returns the metrics queue polling timeout in millisecond.
     */
    public long getQueuePollTimeoutMilli() {
        return queuePollTimeoutMilli;
    }

    /**
     * Sets the metric queue polling timeout in millisecond. The default set
     * set to one-minute per the finest granularity of Amazon CloudWatch
     */
    public void setQueuePollTimeoutMilli(long queuePollTimeoutMilli) {
        this.queuePollTimeoutMilli = queuePollTimeoutMilli;
    }

    public CloudWatchMetricConfig withQueuePollTimeoutMilli(long queuePollTimeoutMilli) {
        setQueuePollTimeoutMilli(queuePollTimeoutMilli);
        return this;
    }

    public int getMetricQueueSize() {
        return metricQueueSize;
    }

    /**
     * Configure the metric queue size, overriding the default. Must be at
     * least 1.
     *
     * @see #DEFAULT_METRICS_QSIZE
     */
    public void setMetricQueueSize(int metricQueueSize) {
        if (metricQueueSize < 1) {
            throw new IllegalArgumentException();
        }
        this.metricQueueSize = metricQueueSize;
    }

    public CloudWatchMetricConfig withMetricQueueSize(int metricQueueSize) {
        setMetricQueueSize(metricQueueSize);
        return this;
    }

    public CloudWatchClient getCloudWatchClient() {
        return cloudWatchClient;
    }

    public void setCloudWatchClient(CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    public CloudWatchMetricConfig withCloudWatchClient(CloudWatchClient cloudWatchClient) {
        setCloudWatchClient(cloudWatchClient);
        return this;
    }
}
