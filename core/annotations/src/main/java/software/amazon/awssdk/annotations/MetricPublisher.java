package software.amazon.awssdk.annotations;

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
import java.util.concurrent.CompletableFuture;

/**
 * Interface to report and publish the collected SDK metric events to external
 * sources.
 * <p>
 * Conceptually, a publisher receives a stream of {@link MetricEvents} objects
 * overs its lifetime through its {@link #consume(MetricEvents)} method.
 * Implementations are then free further aggregate these events into sets of
 * metrics that are then published to some external system for further use.
 * As long as a publisher is not closed, then it can receive {@code
 * MetricEvents} objects at any time. In addition, as the SDK makes use of
 * multithreading, it's possible that the publisher is shared concurrently by
 * multiple threads, and necessitates that all implementations are threadsafe.
 * <p>
 * <b>Example:</b>
 * <p>
 * At {@code t0}:
 * <pre>
 *     metricEventsBuilder.putMetricEvent(Events.MARSHALLING_START, Instant.now());
 * </pre>
 * <p>
 * At {@code t1}, after mashalling is complete:
 * <pre>
 *     metricEventsBuilder.putMetricEvent(Events.MARSHALLING_END, Instant.now());
 * }
 * </pre>
 * <p>
 * At {@code t2} after the SDK operation is complete:
 * <pre>
 * {@code
 *     MetricEvents metricEvents = metricEventsBuilder.build();
 *     metricPublisher.consume(metricEvents)
 *         .whenComplete((r,t) -> {
 *            if (t == null) {
 *                log.debug("Publishing completed successfully.");
 *            } else {
 *                log.error("Publishing of " + metricEvents + " was unsuccessful", e);
 *            }
 *         });
 * }
 * </pre>
 * At some later {@code tN}, the publisher can then choose to aggregate all of
 * the {@code metricEvents} it has received and publish them.
 * <p>
 * The SDK may invoke methods on the interface from multiple threads
 * concurrently so implementations must be threadsafe.
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
     * invocations of this method must return normally. The only legal way to
     * report an error is by completing the returned future exceptionally. This
     * is to ensure that callers of the publisher can safely assume that even
     * in situations where an error happens during publishing that it will not
     * interrupt the calling thread.
     * <p>
     * The future is completed when the metrics calculated or otherwise derived
     * from the given {@code metricEvents} have been published.
     *
     * @param metricEvents The metric events.
     * @return A future representing the publishing of the given metric events.
     * @throws IllegalArgumentException If {@code metricEvents} is {@code null}.
     */
    CompletableFuture<Void> consume(MetricEvents metricEvents);

    /**
     * Close this publisher, allowing it to free any resources it holds and
     * prevents further use.
     * <p>
     * Implementations <b>must</b> block until all pending metrics are
     * published and all held resources are freed.
     */
    @Override
    void close();

    class MetricEvents {
    }
}
