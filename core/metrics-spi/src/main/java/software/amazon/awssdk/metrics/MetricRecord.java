package software.amazon.awssdk.metrics;

/**
 * A container associating a metric and its value.
 */
public interface MetricRecord<T> {
    /**
     * @return The metric.
     */
    SdkMetric<T> metric();

    /**
     * @return The value of this metric.
     */
    T value();
}
