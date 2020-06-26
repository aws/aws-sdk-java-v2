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

import static software.amazon.awssdk.core.client.config.SdkClientOption.METRIC_PUBLISHERS;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.Pair;

/**
 * Utility methods for working with metrics.
 */
@SdkInternalApi
public final class MetricUtils {

    private MetricUtils() {
    }

    /**
     * Resolve the correct list of metric publishers to use. The publishers set on the request always takes precedence.
     *
     * @param clientConfig The client configuration.
     * @param request The request.
     * @return The list of metric publishers to use.
     */
    //TODO: remove this and use the overload instead
    public static List<MetricPublisher> resolvePublishers(SdkClientConfiguration clientConfig,
                                                          SdkRequest request) {
        Optional<? extends RequestOverrideConfiguration> requestConfig = request.overrideConfiguration();
        if (requestConfig.isPresent()) {
            List<MetricPublisher> requestOverridePublishers = requestConfig.get().metricPublishers();
            if (!requestOverridePublishers.isEmpty()) {
                return requestOverridePublishers;
            }
        }

        if (clientConfig != null) {
            return clientConfig.option(METRIC_PUBLISHERS);
        }

        // Else if both of the two configurations are null, return an empty list
        return new ArrayList<>();
    }

    /**
     * Resolve the correct list of metric publishers to use. The publishers set on the request always takes precedence.
     *
     * @param clientConfig The client configuration.
     * @param requestConfig The request override configuration.
     * @return The list of metric publishers to use.
     */
    public static List<MetricPublisher> resolvePublishers(SdkClientConfiguration clientConfig,
                                                          RequestOverrideConfiguration requestConfig) {
        if (requestConfig != null) {
            List<MetricPublisher> requestOverridePublishers = requestConfig.metricPublishers();
            if (!requestOverridePublishers.isEmpty()) {
                return requestOverridePublishers;
            }
        }

        if (clientConfig != null) {
            return clientConfig.option(METRIC_PUBLISHERS);
        }

        // Else if both of the two configurations are null, return an empty list
        return new ArrayList<>();
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
     * Measure the duration of the given callable.
     *
     * @param c The callable to measure.
     * @return A {@code Pair} containing the result of {@code c} and the duration.
     */
    public static <T> Pair<T, Duration> measureDurationUnsafe(Callable<T> c) throws Exception {
        long start = System.nanoTime();
        T result = c.call();
        Duration d = Duration.ofNanos(System.nanoTime() - start);
        return Pair.of(result, d);
    }

    public static void collectHttpMetrics(MetricCollector metricCollector, SdkHttpFullResponse httpResponse) {
        metricCollector.reportMetric(CoreMetric.HTTP_STATUS_CODE, httpResponse.statusCode());
        httpResponse.firstMatchingHeader("x-amz-request-id")
                    .ifPresent(v -> metricCollector.reportMetric(CoreMetric.AWS_REQUEST_ID, v));
        httpResponse.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                    .ifPresent(v -> metricCollector.reportMetric(CoreMetric.AWS_REQUEST_ID, v));
        httpResponse.firstMatchingHeader("x-amz-id-2")
                    .ifPresent(v -> metricCollector.reportMetric(CoreMetric.AWS_EXTENDED_REQUEST_ID, v));
    }

    public static MetricCollector createAttemptMetricsCollector(RequestExecutionContext context) {
        MetricCollector parentCollector = context.executionContext().metricCollector();
        if (parentCollector != null) {
            return parentCollector.createChild("ApiCallAttempt");
        }
        return NoOpMetricCollector.create();
    }

    public static MetricCollector createHttpMetricsCollector(RequestExecutionContext context) {
        MetricCollector parentCollector = context.metricCollector();
        if (parentCollector != null) {
            return parentCollector.createChild("HttpClient");
        }
        return NoOpMetricCollector.create();
    }
}
