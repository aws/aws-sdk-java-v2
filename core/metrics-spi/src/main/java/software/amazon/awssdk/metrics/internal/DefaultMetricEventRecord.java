package software.amazon.awssdk.metrics.internal;

import software.amazon.awssdk.metrics.MetricEvent;
import software.amazon.awssdk.metrics.MetricEventRecord;

public class DefaultMetricEventRecord<T> implements MetricEventRecord<T> {
    private final MetricEvent<T> event;
    private final T data;

    public DefaultMetricEventRecord(MetricEvent<T> event, T data) {
        this.event = event;
        this.data = data;
    }

    @Override
    public MetricEvent<T> getEvent() {
        return event;
    }

    @Override
    public T getData() {
        return data;
    }
}
