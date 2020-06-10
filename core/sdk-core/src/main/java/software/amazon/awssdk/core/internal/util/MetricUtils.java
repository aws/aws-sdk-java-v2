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

import static software.amazon.awssdk.core.client.config.SdkClientOption.METRIC_PUBLISHER;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * Utility methods for working with metrics.
 */
@SdkInternalApi
public final class MetricUtils {

    private MetricUtils() {
    }

    /**
     * Resolve the correct metric publisher to use. The publisher set on the request always takes precedence.
     *
     * @param clientConfig The client configuration.
     * @param requestConfig The request override configuration.
     * @return The metric publisher to use.
     */
    //TODO: remove this and use the overload instead
    public static Optional<MetricPublisher> resolvePublisher(SdkClientConfiguration clientConfig,
                                                             SdkRequest requestConfig) {
        Optional<MetricPublisher> requestOverride = requestConfig.overrideConfiguration()
                .flatMap(RequestOverrideConfiguration::metricPublisher);
        if (requestOverride.isPresent()) {
            return requestOverride;
        }
        return Optional.ofNullable(clientConfig.option(METRIC_PUBLISHER));
    }

    /**
     * Resolve the correct metric publisher to use. The publisher set on the request always takes precedence.
     *
     * @param clientConfig The client configuration.
     * @param requestConfig The request override configuration.
     * @return The metric publisher to use.
     */
    public static Optional<MetricPublisher> resolvePublisher(SdkClientConfiguration clientConfig,
                                                             RequestOverrideConfiguration requestConfig) {
        if (requestConfig != null) {
            return OptionalUtils.firstPresent(requestConfig.metricPublisher(), () -> clientConfig.option(METRIC_PUBLISHER));
        }

        return Optional.ofNullable(clientConfig.option(METRIC_PUBLISHER));
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
}
