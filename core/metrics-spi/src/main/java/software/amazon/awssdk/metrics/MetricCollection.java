package software.amazon.awssdk.metrics;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * An immutable collection of metrics.
 */
public interface MetricCollection extends Iterable<MetricRecord<?>> {
    /**
     * @return The name of this metric collection.
     */
    String name();

    /**
     * Return all the values of the given metric.
     *
     * @param metric The metric.
     * @param <T> The type of the value.
     * @return All of the values of this metric.
     */
    <T> List<T> getAllMetricValues(SdkMetric<T> metric);

    /**
     * Return the values of the given metric.
     *
     * @param metric The metric.
     * @param <T> The type of the value.
     * @return The value of this metric.
     */
    <T> Optional<T> getMetricValue(SdkMetric<T> metric);

    /**
     * @return The child metric collections.
     */
    Collection<MetricCollection> children();
}
