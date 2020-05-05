package software.amazon.awssdk.metrics;

/**
 * A container associating an event with its data.
 */
public interface MetricEventRecord<T> {
    /**
     * @return The event.
     */
    MetricEvent<T> getEvent();

    /**
     * @return The data associated with this event.
     */
    T getData();
}
