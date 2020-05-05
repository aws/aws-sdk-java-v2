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

package software.amazon.awssdk.metrics;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

/**
 * Interface to report and publish the collected SDK metric events to external
 * sources.
 * <p>
 * Conceptually, a publisher receives a stream of {@link MetricEvents} objects
 * overs its lifetime through its {@link #consume{MetricEvents)} method.
 * Implementations are then free further aggregate these events into sets of
 * metrics that are then published to some external system for further use.
 * As long as a publisher is not closed, then it can receive {@code
 * MetricEvents} objects at any time. In addition, as the SDK makes use of
 * multithreading, it's possible that the publisher is shared concurrently by
 * multiple threads, and necessitates that all implementations are threadsafe.
 * <p>
 * <b>Example:</b>
 * At {@code t0}:
 * {@code
 *     metricEventsBuilder.putMetricEvent(Events.MARSHALLING_START, Instant.now());
 * }
 * <p>
 * At {@code t1}, after mashalling is complete:
 * {@code
 *     metricEventsBuilder.putMetricEvent(Events.MARSHALLING_END, Instant.now());
 * }
 * <p>
 * At {@code t2} after the SDK operation is complete:
 * <p>
 * {@code
 *     metricPublisher.consume(metricEventsBuilder.build());
 * }
 * At some later {@code tN}, the publisher can then choose to aggregate all of
 * the {@code metricEvents} it has received and publish them.
 *
 *
 * Implementations must be threadsafe.
 */
@SdkPublicApi
@ThreadSafe
public interface MetricPublisher extends AutoCloseable {
    /**
     * Notify the publisher of new metric data. After this call returns, the
     * caller can safely discard the given {@code metricEvents} instance if it
     * no longer needs it. Implementations are strongly encouraged to complete
     * the aggregation and publishing of metrics in an asynchronous manner to
     * avoid blocking the calling thread.
     * <p>
     * With the exception of a {@code null} {@code metricEvents}, all
     * invocations of this method must return normally. This is to ensure that
     * callers of the publisher can safely assume that even in situations where
     * an error happens during publishing that it will not interrupt the calling
     * thread.
     *
     * @throws IllegalArgumentException If {@code metricEvents} is {@code null}.
     */
    void consume(MetricEvents metricEvents);

    /**
     * Close this publisher, allowing it to free any resources it holds and
     * prevents further use.
     * <p>
     * Implementations <b>must</b> block until all pending metrics are
     * published and all held resources are freed.
     */
    @Override
    void close();
}
