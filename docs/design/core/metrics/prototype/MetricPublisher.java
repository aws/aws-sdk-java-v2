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

package software.amazon.awssdk.metrics.publisher;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

/**
 * Interface to report and publish the collected SDK metrics to external sources.
 *
 * Publisher implementations create and maintain resources (like clients, thread pool etc) that are used for publishing.
 * They should be closed in the close() method to avoid resource leakage.
 *
 * <p>
 *     As metrics are not part of the business logic, failures caused by metrics features should not fail the application.
 *     So SDK publisher implementations suppress all errors during the metrics publishing and log them.
 * </p>
 *
 * <p>
 *     In certain situations (high throttling errors, metrics are reported faster than publishing etc), storing all the metrics
 *     might take up lot of memory and can crash the application. In these cases, it is recommended to have a max limit on
 *     number of metrics stored or memory used for metrics and drop the metrics when the limit is breached.
 * </p>
 */
@SdkPublicApi
public interface MetricPublisher extends AutoCloseable {

    /**
     * Registers the metric information supplied in MetricsRegistry. The reported metrics can be transformed and
     * stored in a format the publisher uses to publish the metrics.
     *
     * This method is called at the end of each request execution to report all the metrics collected
     * for that request (including retry attempt metrics)
     */
    void registerMetrics(MetricRegistry metricsRegistry);

    /**
     * Publish all metrics stored in the publisher. If all available metrics cannot be published in a single call,
     * multiple calls will be made to publish the metrics.
     *
     * It is recommended to publish the metrics in a non-blocking way. As it is common to publish metrics to an external
     * source which involves network calls, the method is intended to be implemented in a non-blocking way and thus
     * returns a {@link CompletableFuture}.
     *
     * Depending on the implementation, the metrics are published to the external source periodically like:
     * a) after a certain time period
     * b) after n metrics are registered
     * c) after the buffer is full
     *
     * Implementations can also call publish method for every reported metric. But this can be expensive and
     * is not recommended.
     */
    CompletableFuture<Void> publish();
}
