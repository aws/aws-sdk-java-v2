package software.amazon.awssdk.metrics;

import java.time.Duration;

/**
 * The set of metrics collected by default by the SDK.
 */
public final class DefaultMetrics {
    /**
     * The duration of time it took to marshall an SDK request object to an HTTP request object.
     */
    public static final SdkMetric<Duration> REQUEST_MARSHALLING_TIME = SdkMetric.of("RequestMarshallingTime", Duration.class, MetricCategory.DEFAULT);

    /**
     * The duration of time it took to sign a request.
     * <p>
     * Note: this metric is only present if the SDK client is configured to sign requests.
     */
    public static final SdkMetric<Duration> REQUEST_SIGNING_TIME = SdkMetric.of("RequestSigningTime", Duration.class, MetricCategory.DEFAULT);

    /**
     * The duration of time between sending the HTTP request and when the HTTP response is received.
     */
    public static final SdkMetric<Duration> REQUEST_EXECUTION_TIME = SdkMetric.of("RequestExecutionTime", Duration.class, MetricCategory.DEFAULT);

    private DefaultMetrics() {
    }
}
