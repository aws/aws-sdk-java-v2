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

package software.amazon.awssdk.metrics.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.meter.Timer;

/**
 * Registry to store the collected metrics data. The interface can be used to store metrics for ApiCall and ApiCallAttempt.
 * For a ApiCall, there can be multiple attempts and so a MetricRegistry has the option to store other MetricRegistry instances.
 */
@SdkPublicApi
public interface MetricRegistry {

    /**
     * Return the ApiCall level metrics registered in this metric registry as a map of metric name to metric instance.
     * Only metrics that can be recorded once for entire request lifecycle are recorded here.
     *
     * The method does not return the Api Call Attempt metrics. For metrics recorded separately for each attempt,
     * see {@link #apiCallAttemptMetrics()}.
     */
    Map<String, Metric> getMetrics();


    /**
     * Return an ordered list of {@link MetricRegistry} instances recorded for each Api Call Attempt in the request execution.
     * Each Api call attempt metrics are recorded as a separate {@link MetricRegistry} instance in the given list.
     *
     * For example,
     * If the Api finishes (succeed or fail) in the first attempt, the returned list size will be 1.
     *
     * If the Api finishes after 4 attempts (1 initial attempt + 3 retries), the returned list size will be 4. In this case,
     * The 0th entry in the list has the metrics for the initial attempt,
     * The 1st entry in the list has the metrics for the second attempt (1st retry) and so on.
     *
     * @return an ordered list of {@link MetricRegistry} instances, one for each Api Call Attempt in the request execution
     */
    List<MetricRegistry> apiCallAttemptMetrics();

    /**
     * Create and return a new instance of {@link MetricRegistry} for the current ApiCall Attempt.
     * Records the registry instance within the class. The instance for the current attempt can be accessed by calling
     * the {@link #apiCallAttemptMetrics()} method and getting the last element in the output list.
     *
     * If the Api Call finishes in the first attempt, this method is only called once.
     * If the Api Call finishes after n retry attmpts, this method is called n + 1 times
     * (1 time for initial attempt, n times for n retries)
     *
     * @return a instance of {@link MetricRegistry} to record metrics for a ApiCall Attempt
     */
    MetricRegistry registerApiCallAttemptMetrics();

    /**
     * Given a {@link Metric}, registers it under the given name.
     * If a metric with given name is already present, method throws {@link IllegalArgumentException}.
     *
     * @param name   the name of the metric
     * @param metric the metric
     * @return the given metric
     */
    Metric register(String name, Metric metric);

    /**
     * Returns an optional representing the metric registered with the given name. If no metric is registered
     * with the given name, an empty optional will be returned.
     *
     * @param name the name of the metric
     * @return an optional representing the metric registered with the given name.
     */
    Optional<Metric> metric(String name);

    /**
     * Removes the metric with the given name.
     *
     * @param name the name of the metric
     * @return True if the metric was removed. False is the metric doesn't exist or cannot be removed
     */
    boolean remove(String name);

    /**
     * Return the {@link Counter} registered under this name.
     * If there is none registered already, create and register a new {@link Counter}.
     *
     * @param name name of the metric
     * @return a new or pre-existing {@link Counter}
     */
    Counter counter(String name);

    /**
     * Return the {@link Timer} registered under this name.
     * If there is none registered already, create and register a new {@link Timer}.
     *
     * @param name name of the metric
     * @return a new or pre-existing {@link Timer}
     */
    Timer timer(String name);

    /**
     * Return a {@link Gauge} registered under this name and updates its value with #value.
     * If there is none registered already, create and register a new {@link Gauge} with the given initial #value.
     *
     * @param name name of the metric
     * @param value initial value of the guage
     * @param <T> type of the value
     * @return a new or pre-existing {@link Gauge} with updated value
     */
    <T> Gauge<T> gauge(String name, T value);
}
