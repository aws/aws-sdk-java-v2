/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.internal.util;

import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADERS;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZ_ID_2_HEADER;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.utils.Pair;

/**
 * Utility methods for working with metrics.
 * <p>
 * Implementation notes: this class should've been outside internal package,
 * but we can't fix it due to backwards compatibility reasons.
 */
@SdkProtectedApi
public final class MetricUtils {
    private static final long ONE_SEC_IN_NS = 1_000_000_000L;

    private MetricUtils() {
    }

    /**
     * Measure the duration of the given callable.
     *
     * @param c The callable to measure.
     * @return A {@code Pair} containing the result of {@code c} and the duration.
     */
    public static <T> Pair<T, Duration> measureDuration(Supplier<T> c) {
        long start = System.nanoTime();
        T result = c.get();
        Duration d = Duration.ofNanos(System.nanoTime() - start);
        return Pair.of(result, d);
    }

    /**
     * Report a duration metric of the given {@link CompletableFuture} supplier.
     *
     * @param c The callable to measure.
     * @param metricCollector The MetricCollector where the metric is to be reported.
     * @param metric The metric to be reported.
     * @return A {@code Pair} containing the result of {@code c} and the duration.
     */
    public static <T> CompletableFuture<T> reportDuration(Supplier<CompletableFuture<T>> c,
                                                          MetricCollector metricCollector,
                                                          SdkMetric<Duration> metric) {
        long start = System.nanoTime();
        CompletableFuture<T> result = c.get();
        result.whenComplete((r, t) -> {
            Duration d = Duration.ofNanos(System.nanoTime() - start);
            metricCollector.reportMetric(metric, d);
        });
        return result;
    }

    /**
     * Measure the duration of the given callable.
     *
     * @param c The callable to measure.
     * @return A {@code Pair} containing the result of {@code c} and the duration.
     */
    public static <T> Pair<T, Duration> measureDurationUnsafe(Callable<T> c) throws Exception {
        return measureDurationUnsafe(c, System.nanoTime());
    }

    /**
     * Measure the duration of the given callable, using the provided time as the basis.
     */
    public static <T> Pair<T, Duration> measureDurationUnsafe(Callable<T> c, long startTime) throws Exception {
        T result = c.call();
        Duration d = Duration.ofNanos(System.nanoTime() - startTime);
        return Pair.of(result, d);
    }

    /**
     * Collect the SERVICE_ENDPOINT metric for this request.
     */
    public static void collectServiceEndpointMetrics(MetricCollector metricCollector, SdkHttpFullRequest httpRequest) {
        if (metricCollector != null && !(metricCollector instanceof NoOpMetricCollector) && httpRequest != null) {
            // Only interested in the service endpoint so don't include any path, query, or fragment component
            URI requestUri = httpRequest.getUri();
            try {
                URI serviceEndpoint = new URI(requestUri.getScheme(), requestUri.getAuthority(), null, null, null);
                metricCollector.reportMetric(CoreMetric.SERVICE_ENDPOINT, serviceEndpoint);
            } catch (URISyntaxException e) {
                // This should not happen since getUri() should return a valid URI
                throw SdkClientException.create("Unable to collect SERVICE_ENDPOINT metric", e);
            }
        }
    }

    public static void collectHttpMetrics(MetricCollector metricCollector, SdkHttpFullResponse httpResponse) {
        if (metricCollector != null && !(metricCollector instanceof NoOpMetricCollector) && httpResponse != null) {
            metricCollector.reportMetric(HttpMetric.HTTP_STATUS_CODE, httpResponse.statusCode());
            X_AMZN_REQUEST_ID_HEADERS.forEach(h -> {
                httpResponse.firstMatchingHeader(h).ifPresent(v -> metricCollector.reportMetric(CoreMetric.AWS_REQUEST_ID, v));
            });
            httpResponse.firstMatchingHeader(X_AMZ_ID_2_HEADER)
                        .ifPresent(v -> metricCollector.reportMetric(CoreMetric.AWS_EXTENDED_REQUEST_ID, v));
        }
    }

    public static MetricCollector createAttemptMetricsCollector(RequestExecutionContext context) {
        MetricCollector parentCollector = context.executionContext().metricCollector();
        if (parentCollector != null) {
            return parentCollector.createChild("ApiCallAttempt");
        }
        return NoOpMetricCollector.create();
    }

    public static MetricCollector createHttpMetricsCollector(RequestExecutionContext context) {
        MetricCollector parentCollector = context.attemptMetricCollector();
        if (parentCollector != null) {
            return parentCollector.createChild("HttpClient");
        }
        return NoOpMetricCollector.create();
    }

    public static OptionalLong apiCallAttemptStartNanoTime(RequestExecutionContext context) {
        Long t = context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME);
        if (t == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(t);
    }

    public static long resetApiCallAttemptStartNanoTime(RequestExecutionContext context) {
        long now = System.nanoTime();
        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.API_CALL_ATTEMPT_START_NANO_TIME, now);
        return now;
    }

    public static OptionalLong apiCallAttemptResponseBytesRead(RequestExecutionContext context) {
        AtomicLong read = context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ);
        if (read == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(read.get());
    }

    public static OptionalLong responseHeadersReadEndNanoTime(RequestExecutionContext context) {
        Long startTime = context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.HEADERS_READ_END_NANO_TIME);
        if (startTime == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(startTime);
    }

    public static double bytesPerSec(long totalBytes, long nanoStart, long nanoEnd) {
        long duration = nanoEnd - nanoStart;
        double bytesPerNs = (double) totalBytes / duration;
        return bytesPerNs * ONE_SEC_IN_NS;
    }
}
