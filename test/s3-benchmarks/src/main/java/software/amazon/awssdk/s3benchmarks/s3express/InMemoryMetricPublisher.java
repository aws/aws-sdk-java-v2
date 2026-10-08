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

package software.amazon.awssdk.s3benchmarks.s3express;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;

/**
 * Collects SDK per-request metrics in memory for later aggregation.
 * Captures API_CALL_DURATION per operation and READ_THROUGHPUT for GetObject.
 */
public class InMemoryMetricPublisher implements MetricPublisher {
    private final ConcurrentHashMap<String, List<Duration>> apiCallDurations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Double>> readThroughputs = new ConcurrentHashMap<>();

    @Override
    public void publish(MetricCollection metricCollection) {
        String operationName = metricCollection.metricValues(CoreMetric.OPERATION_NAME)
                                               .stream().findFirst().orElse("Unknown");

        metricCollection.metricValues(CoreMetric.API_CALL_DURATION)
                        .forEach(d -> apiCallDurations
                            .computeIfAbsent(operationName, k -> new CopyOnWriteArrayList<>())
                            .add(d));

        metricCollection.childrenWithName("ApiCallAttempt")
                        .forEach(attempt -> attempt.metricValues(CoreMetric.READ_THROUGHPUT)
                            .forEach(t -> readThroughputs
                                .computeIfAbsent(operationName, k -> new CopyOnWriteArrayList<>())
                                .add(t)));
    }

    @Override
    public void close() {
    }

    public double avgDurationMs(String operationName) {
        List<Duration> durations = apiCallDurations.getOrDefault(operationName, Collections.emptyList());
        if (durations.isEmpty()) {
            throw new IllegalStateException("No API_CALL_DURATION samples collected for " + operationName);
        }
        return durations.stream()
                        .mapToDouble(d -> d.toNanos() / 1_000_000.0)
                        .average()
                        .orElse(0.0);
    }

    public double avgReadThroughput(String operationName) {
        List<Double> throughputs = readThroughputs.getOrDefault(operationName, Collections.emptyList());
        if (throughputs.isEmpty()) {
            throw new IllegalStateException("No READ_THROUGHPUT samples collected for " + operationName);
        }
        return throughputs.stream()
                          .mapToDouble(Double::doubleValue)
                          .average()
                          .orElse(0.0);
    }

    public void reset() {
        apiCallDurations.clear();
        readThroughputs.clear();
    }
}
