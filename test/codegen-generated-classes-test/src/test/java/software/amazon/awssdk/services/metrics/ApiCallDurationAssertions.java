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

package software.amazon.awssdk.services.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * Assertions on the window that {@link CoreMetric#API_CALL_DURATION} is supposed to cover.
 */
public final class ApiCallDurationAssertions {

    private ApiCallDurationAssertions() {
    }

    /**
     * Assert that {@code ApiCallDuration} individually encloses each duration reported inside its window.
     *
     * <p>Every one of these is a necessary condition on its own, so this is safe to apply to any API call, including
     * calls that failed or were retried.
     *
     * <p>{@code MARSHALLING_DURATION} and {@code ENDPOINT_RESOLVE_DURATION} are additionally required to be present. An
     * enclosure check over an absent metric passes vacuously, which would let a regression that stopped reporting one of
     * them through unnoticed — and those two are exactly the ones this PR's defect hid.
     */
    public static void assertEnclosesComponents(MetricCollection apiCall) {
        Duration apiCallDuration = single(apiCall, CoreMetric.API_CALL_DURATION);

        // Required: every API call marshals a request and resolves an endpoint before it can be sent, so a regression
        // that stops reporting either of these must fail rather than pass vacuously.
        assertRequiredAndEnclosed(apiCallDuration, apiCall, CoreMetric.MARSHALLING_DURATION);
        assertRequiredAndEnclosed(apiCallDuration, apiCall, CoreMetric.ENDPOINT_RESOLVE_DURATION);

        // Optional: absent when the endpoint is not signed, when an identity is already cached in a form that reports no
        // duration, or when the call failed before the phase ran.
        assertEnclosedIfPresent(apiCallDuration, apiCall, CoreMetric.CREDENTIALS_FETCH_DURATION);

        for (MetricCollection attempt : apiCall.children()) {
            assertEnclosedIfPresent(apiCallDuration, attempt, CoreMetric.SIGNING_DURATION);
            assertEnclosedIfPresent(apiCallDuration, attempt, CoreMetric.SERVICE_CALL_DURATION);
            assertEnclosedIfPresent(apiCallDuration, attempt, CoreMetric.UNMARSHALLING_DURATION);
        }
    }

    /**
     * Assert the javadoc additivity formula: {@code ApiCallDuration} is at least the sum of the components it is
     * documented to be composed of.
     *
     * <p>The relation is an inequality rather than an equality because several steps inside the window have no metric of
     * its own.
     *
     * <p>Only apply this where the phases are genuinely disjoint: a single-attempt call on a synchronous client. It does
     * not hold on asynchronous clients, where {@code SERVICE_CALL_DURATION} does not stop at time to first byte (see
     * {@code CoreMetric.TIME_TO_FIRST_BYTE}) and so overlaps {@code UNMARSHALLING_DURATION} for streaming operations.
     * Use {@link #assertEnclosesComponents(MetricCollection)} there, which holds unconditionally.
     */
    public static void assertEnclosesComponentSum(MetricCollection apiCall) {
        Duration apiCallDuration = single(apiCall, CoreMetric.API_CALL_DURATION);

        Duration componentSum = sum(apiCall, CoreMetric.MARSHALLING_DURATION)
            .plus(sum(apiCall, CoreMetric.ENDPOINT_RESOLVE_DURATION))
            .plus(sum(apiCall, CoreMetric.CREDENTIALS_FETCH_DURATION));

        for (MetricCollection attempt : apiCall.children()) {
            componentSum = componentSum.plus(sum(attempt, CoreMetric.BACKOFF_DELAY_DURATION))
                                       .plus(sum(attempt, CoreMetric.SIGNING_DURATION))
                                       .plus(sum(attempt, CoreMetric.SERVICE_CALL_DURATION))
                                       .plus(sum(attempt, CoreMetric.UNMARSHALLING_DURATION));
        }

        assertThat(apiCallDuration)
            .as("ApiCallDuration must be at least the sum of the components")
            .isGreaterThanOrEqualTo(componentSum);
    }

    private static void assertRequiredAndEnclosed(Duration apiCallDuration,
                                                  MetricCollection collection,
                                                  SdkMetric<Duration> metric) {
        assertThat(collection.metricValues(metric))
            .as("%s must be reported for every API call", metric.name())
            .isNotEmpty();
        assertEnclosedIfPresent(apiCallDuration, collection, metric);
    }

    private static void assertEnclosedIfPresent(Duration apiCallDuration,
                                                MetricCollection collection,
                                                SdkMetric<Duration> metric) {
        for (Duration value : collection.metricValues(metric)) {
            assertThat(apiCallDuration)
                .as("ApiCallDuration must enclose %s", metric.name())
                .isGreaterThanOrEqualTo(value);
        }
    }

    private static Duration single(MetricCollection collection, SdkMetric<Duration> metric) {
        List<Duration> values = collection.metricValues(metric);
        assertThat(values).as("%s must be reported", metric.name()).hasSize(1);
        return values.get(0);
    }

    private static Duration sum(MetricCollection collection, SdkMetric<Duration> metric) {
        Duration total = Duration.ZERO;
        for (Duration value : collection.metricValues(metric)) {
            total = total.plus(value);
        }
        return total;
    }
}
