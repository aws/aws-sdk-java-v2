package software.amazon.awssdk.metrics.internal.cloudwatch;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.ServiceMetricCollector;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

public class LoggingMetricCollector extends MetricCollector {
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public RequestMetricCollector getRequestMetricCollector() {
        return new RequestMetricCollector() {
            @Override
            public void collectMetrics(Request<?> request, Object response) {
              AwsRequestMetrics metrics = request.getAwsRequestMetrics();
              metrics.log();
            }
        };
    }

    @Override
    public ServiceMetricCollector getServiceMetricCollector() {
        return ServiceMetricCollector.NONE;
    }
}
