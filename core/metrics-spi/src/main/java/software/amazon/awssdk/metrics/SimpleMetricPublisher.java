package software.amazon.awssdk.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import software.amazon.awssdk.metrics.publisher.MetricPublisher;

public class SimpleMetricPublisher implements MetricPublisher {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private SimpleMetricPublisher() {
    }

    @Override
    public void publish(MetricCollection metricCollection) {
        try {
            String reportStr = MAPPER.writeValueAsString(metricsReport(metricCollection));
            System.out.println(reportStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
    }

    public static SimpleMetricPublisher create() {
        return new SimpleMetricPublisher();
    }

    private Map<String, Object> metricsReport(MetricCollection metrics) {
        Map<String, Object> jsonObj = new HashMap<>();
        jsonObj.put("name", metrics.name());
        Map<String, String> metricsObj = new HashMap<>();
//        for (MetricRecord<?> r : metrics) {
//           addMetric(metricsObj, r);
//        }
        addSelectMetrics(metricsObj, metrics);
        jsonObj.put("metrics", metricsObj);
        jsonObj.put("children", metrics.children().stream().map(this::metricsReport).collect(Collectors.toList()));
        return jsonObj;
    }

    private void addSelectMetrics(Map<String, String> metricsObj, MetricCollection metricCollection) {
        addDurationMetric(metricsObj, DefaultMetrics.REQUEST_MARSHALLING_TIME, metricCollection);
        addDurationMetric(metricsObj, DefaultMetrics.REQUEST_SIGNING_TIME, metricCollection);
        addDurationMetric(metricsObj, DefaultMetrics.REQUEST_EXECUTION_TIME, metricCollection);

    }

    private void addDurationMetric(Map<String, String> metricsObj, SdkMetric<Duration> metric, MetricCollection metricCollection) {
        metricCollection.getMetricValue(metric).ifPresent(d -> metricsObj.put(metric.name(), Long.toString(d.toMillis())));
    }

    private void addMetric(Map<String, String> obj, MetricRecord<?> r) {
        final SdkMetric<?> m = r.metric();

        if (m.valueClass().isAssignableFrom(Duration.class)) {
            final Duration durationValue = ((SdkMetric<Duration>) m).convertToType(r.value());
            obj.put(m.name(), Long.toString(durationValue.toMillis()));
        } else {
            obj.put(m.name(), r.value().toString());
        }
    }
}
