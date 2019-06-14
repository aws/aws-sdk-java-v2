package software.amazon.awssdk.metrics.publishers.cloudwatch;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.metrics.publisher.MetricPublisher;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

public class CloudWatchPublisher implements MetricPublisher {

    @Override
    public void registerMetrics(MetricRegistry metricsRegistry) {

    }

    @Override
    public CompletableFuture<Void> publish() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
