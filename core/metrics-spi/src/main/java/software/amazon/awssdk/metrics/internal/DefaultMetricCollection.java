package software.amazon.awssdk.metrics.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;

public class DefaultMetricCollection implements MetricCollection {
    private final String name;
    private final Map<SdkMetric<?>, List<MetricRecord<?>>> metrics;
    private final Collection<MetricCollection> children;


    public DefaultMetricCollection(String name, Map<SdkMetric<?>, List<MetricRecord<?>>> metrics, Collection<MetricCollection> children) {
        this.name = name;
        this.metrics = metrics;
        this.children = children != null ? Collections.unmodifiableCollection(children) : Collections.emptyList();
    }

    @Override
    public String name() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getAllMetricValues(SdkMetric<T> metric) {
        if (metrics.containsKey(metric)) {
            List<MetricRecord<?>> metricRecords = metrics.get(metric);
            List<?> values = metricRecords.stream()
                    .map(MetricRecord::value)
                    .collect(Collectors.toList());
            return (List<T>) Collections.unmodifiableList(values);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getMetricValue(SdkMetric<T> metric) {
        if (metrics.containsKey(metric)) {
            return (Optional<T>) Optional.of(metrics.get(metric).stream()
                    .iterator()
                    .next()
                    .value());
        }
        return Optional.empty();
    }

    @Override
    public Collection<MetricCollection> children() {
        return children;
    }

    @Override
    public Iterator<MetricRecord<?>> iterator() {
        return metrics.values().stream()
                .flatMap(List::stream)
                .iterator();
    }
}
