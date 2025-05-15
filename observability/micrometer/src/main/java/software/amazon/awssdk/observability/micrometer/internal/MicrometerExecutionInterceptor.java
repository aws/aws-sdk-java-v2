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

package software.amazon.awssdk.observability.micrometer.internal;


import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public class MicrometerExecutionInterceptor implements ExecutionInterceptor {

    private static final ExecutionAttribute<Observation> OBSERVATION = new ExecutionAttribute<>("Observation");
    private final ObservationRegistry observationRegistry;

    private final Set<String> requestHeadersToCapture;
    private final Set<String> responseHeadersToCapture;

    public MicrometerExecutionInterceptor(ObservationRegistry observationRegistry,
                                          Set<String> requestHeadersToCapture,
                                          Set<String> responseHeadersToCapture) {
        this.observationRegistry = observationRegistry;
        // Convert to lowercase for case-insensitive matching
        this.requestHeadersToCapture = requestHeadersToCapture.stream()
                                                              .map(String::toLowerCase)
                                                              .collect(Collectors.toSet());
        this.responseHeadersToCapture = responseHeadersToCapture.stream()
                                                                .map(String::toLowerCase)
                                                                .collect(Collectors.toSet());
    }


    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        Observation observation = Observation.start("aws.sdk.request", observationRegistry);
        executionAttributes.putAttribute(OBSERVATION, observation);
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        Observation observation = executionAttributes.getAttribute(OBSERVATION);
        if (observation == null) {
            return;
        }
        processMetrics(executionAttributes, observation, SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);
        processMetrics(executionAttributes, observation, SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR);
        captureHeaders(context.httpRequest().headers(), observation, requestHeadersToCapture, "req");
        captureHeaders(context.httpResponse().headers(), observation, responseHeadersToCapture, "resp");
        observation.stop();
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        Observation observation = executionAttributes.getAttribute(OBSERVATION);
        if (observation != null) {
            observation.error(context.exception());
            processMetrics(executionAttributes, observation, SdkExecutionAttribute.API_CALL_METRIC_COLLECTOR);
            processMetrics(executionAttributes, observation, SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR);
            observation.stop();
        }
    }

    private void processMetrics(ExecutionAttributes executionAttributes, Observation observation,
                                ExecutionAttribute<MetricCollector> metricCollectorAttribute) {
        MetricCollector collector = executionAttributes.getAttribute(metricCollectorAttribute);
        if (collector != null) {
            MetricCollection metrics = collector.collect();
            metrics.forEach(metric ->
                                observation.lowCardinalityKeyValue(metric.metric().name(), metric.value().toString())
            );
        }
    }

    private void captureHeaders(Map<String, List<String>> headers,
                                Observation observation,
                                Set<String> headersToCapture,
                                String prefix) {
        if (headersToCapture.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            String headerName = header.getKey().toLowerCase();
            if (headersToCapture.contains(headerName)) {
                observation.lowCardinalityKeyValue(prefix + "." + headerName,
                                                   joinHeaderValues(header.getValue()));
            }
        }
    }

    private String joinHeaderValues(List<String> values) {
        return String.join(",", values);
    }


    /**
     * Builder for creating MicrometerExecutionInterceptor with custom configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ObservationRegistry observationRegistry;
        private final Set<String> requestHeadersToCapture = new HashSet<>();
        private final Set<String> responseHeadersToCapture = new HashSet<>();

        public Builder observationRegistry(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
            return this;
        }

        public Builder captureRequestHeader(String headerName) {
            this.requestHeadersToCapture.add(headerName);
            return this;
        }

        public Builder captureRequestHeaders(Collection<String> headerNames) {
            this.requestHeadersToCapture.addAll(headerNames);
            return this;
        }

        public Builder captureResponseHeader(String headerName) {
            this.responseHeadersToCapture.add(headerName);
            return this;
        }

        public Builder captureResponseHeaders(Collection<String> headerNames) {
            this.responseHeadersToCapture.addAll(headerNames);
            return this;
        }

        public MicrometerExecutionInterceptor build() {
            if (observationRegistry == null) {
                throw new IllegalStateException("ObservationRegistry must be provided");
            }
            return new MicrometerExecutionInterceptor(
                observationRegistry, requestHeadersToCapture, responseHeadersToCapture);
        }
    }


}
