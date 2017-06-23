package software.amazon.awssdk.metrics.internal.cloudwatch;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;


/**
 * A customized {@link DefaultMetricsCollector} that is only interested in
 * collecting DynamoDB's PutItemRequest.
 */
public class CustomMetricCollector extends MetricCollectorSupport {
    protected CustomMetricCollector(CloudWatchMetricConfig config) {
        super(config);
    }
    @Override
    public RequestMetricCollector getRequestMetricCollector() {
        final RequestMetricCollector orig = super.getRequestMetricCollector();
        return new RequestMetricCollector() {
            @Override
            public void collectMetrics(Request<?> request, Object response) {
                if (request.getOriginalRequest() instanceof PutItemRequest) {
                    orig.collectMetrics(request, response);
                }
            }
            @Override
            public boolean isEnabled() {
                return orig.isEnabled();
            }
        };
    }
}
