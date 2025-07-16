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

package software.amazon.awssdk.benchmark.metrics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.benchmark.core.BenchmarkResult;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.utils.Logger;

/**
 * Publishes benchmark results to CloudWatch for visualization and analysis.
 */
public class CloudWatchMetricsPublisher {
    private static final Logger logger = Logger.loggerFor(CloudWatchMetricsPublisher.class);

    private final CloudWatchClient cloudWatch;
    private final String namespace;

    public CloudWatchMetricsPublisher(Region region, String namespace) {
        this.cloudWatch = CloudWatchClient.builder()
                                          .region(region)
                                          .credentialsProvider(DefaultCredentialsProvider.create())
                                          .build();
        this.namespace = namespace;
    }

    public void publishBenchmarkResult(BenchmarkResult result, String comparisonRunId) {
        try {
            List<MetricDatum> metrics = new ArrayList<>();

            // Use the provided timestamp for all metrics to ensure synchronization
            Instant metricTime = Instant.parse(comparisonRunId);

            // Common dimensions for all metrics
            List<Dimension> dimensions = Arrays.asList(
                Dimension.builder()
                         .name("ClientType")
                         .value(result.getClientType())
                         .build(),
                Dimension.builder()
                         .name("Operation")
                         .value(result.getBenchmarkName())
                         .build(),
                Dimension.builder()
                         .name("ThreadCount")
                         .value(String.valueOf(result.getThreadCount()))
                         .build(),
                Dimension.builder()
                         .name("ComparisonRun")
                         .value(comparisonRunId)
                         .build()
            );

            // Throughput metric
            metrics.add(MetricDatum.builder()
                                   .metricName("Throughput")
                                   .value(result.getThroughput())
                                   .unit(StandardUnit.COUNT_SECOND)
                                   .timestamp(metricTime)
                                   .dimensions(dimensions)
                                   .build());

            // Average latency metric
            metrics.add(MetricDatum.builder()
                                   .metricName("AverageLatency")
                                   .value(result.getAvgLatency())
                                   .unit(StandardUnit.MILLISECONDS)
                                   .timestamp(metricTime)
                                   .dimensions(dimensions)
                                   .build());

            // P99 latency metric
            metrics.add(MetricDatum.builder()
                                   .metricName("P99Latency")
                                   .value(result.getP99Latency())
                                   .unit(StandardUnit.MILLISECONDS)
                                   .timestamp(metricTime)
                                   .dimensions(dimensions)
                                   .build());

            // Publish metrics in batches (CloudWatch limit is 1000 metrics per request)
            publishMetrics(metrics);

            logger.info(() -> "Published metrics for " + result.getClientType() +
                        "." + result.getBenchmarkName());

        } catch (Exception e) {
            logger.error(() -> "Failed to publish metrics: " + e.getMessage(), e);
            throw new RuntimeException("CloudWatch publication failed", e);
        }
    }

    private void publishMetrics(List<MetricDatum> metrics) {
        if (metrics.isEmpty()) {
            return;
        }

        // CloudWatch has a limit of 1000 metrics per request
        List<List<MetricDatum>> batches = partition(metrics, 1000);

        for (List<MetricDatum> batch : batches) {
            PutMetricDataRequest request = PutMetricDataRequest.builder()
                                                               .namespace(namespace)
                                                               .metricData(batch)
                                                               .build();

            cloudWatch.putMetricData(request);
        }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    public void shutdown() {
        if (cloudWatch != null) {
            cloudWatch.close();
        }
    }
}
