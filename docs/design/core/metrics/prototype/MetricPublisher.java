/**
 * Interface to report and publish the collected SDK metric events to external
 * sources.
 * <p>
 * Conceptually, a publisher receives a stream of {@link MetricCollection}
 * objects overs its lifetime through its {@link #publish(MetricCollection)} )}
 * method.  Implementations are then free further aggregate these events into
 * sets of metrics that are then published to some external system for further
 * use.  As long as a publisher is not closed, then it can receive {@code
 * MetricCollection} objects at any time. In addition, as the SDK makes use of
 * multithreading, it's possible that the publisher is shared concurrently by
 * multiple threads, and necessitates that all implementations are threadsafe.
 */
@ThreadSafe
@SdkPublicApi
public interface MetricPublisher extends SdkAutoCloseable {
    /**
     * Notify the publisher of new metric data. After this call returns, the
     * caller can safely discard the given {@code metricCollection} instance if
     * it no longer needs it. Implementations are strongly encouraged to
     * complete any further aggregation and publishing of metrics in an
     * asynchronous manner to avoid blocking the calling thread.
     * <p>
     * With the exception of a {@code null} {@code metricCollection}, all
     * invocations of this method must return normally. This is to ensure that
     * callers of the publisher can safely assume that even in situations where
     * an error happens during publishing that it will not interrupt the calling
     * thread.
     *
     * @param metricCollection The collection of metrics.
     * @throws IllegalArgumentException If {@code metricCollection} is {@code
     * null}.
     */
    void publish(MetricCollection metricCollection);

    /**
     * {@inheritDoc}
     * <p>
     * <b>Important:</b> Implementations must block the calling thread until all
     * pending metrics are published and any resources acquired have been freed.
     */
    @Override
    void close();
}

