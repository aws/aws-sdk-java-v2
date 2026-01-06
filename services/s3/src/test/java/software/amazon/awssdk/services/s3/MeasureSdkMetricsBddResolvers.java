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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.internal.BaselineRulesResolver;
import software.amazon.awssdk.services.s3.endpoints.internal.BddCostOpt5Runtime6;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MeasureSdkMetricsBddResolvers {

    public static void main(String[] args) throws Exception {
        String bucket = "alexwoo-us-east-1";
        String keyPrefix = "metrics-test/object-";

        InMemoryMetricPublisher metricPublisher = new InMemoryMetricPublisher();

        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .addMetricPublisher(metricPublisher)
                                       .apiCallTimeout(Duration.ofSeconds(30))
                                       .build();

        try (S3AsyncClient s3 = S3AsyncClient.builder()
                                             .region(Region.US_EAST_1)
                                             .endpointProvider(new BaselineRulesResolver())
                                             //.endpointProvider(new BddCostOpt5Runtime6())
                                             .overrideConfiguration(overrideConfig)
                                             .build()) {

            List<CompletableFuture<?>> futures =
                IntStream.range(0, 200)
                         .mapToObj(i -> {
                             String key = keyPrefix + i;
                             return putThenGet(s3, bucket, key, i);
                         })
                         .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        System.out.println("\n===== SDK METRICS =====");
        metricPublisher.printSummary();
    }

    private static CompletableFuture<Void> putThenGet(
        S3AsyncClient s3, String bucket, String key, int i) {

        PutObjectRequest putReq = PutObjectRequest.builder()
                                                  .bucket(bucket)
                                                  .key(key)
                                                  .build();

        GetObjectRequest getReq = GetObjectRequest.builder()
                                                  .bucket(bucket)
                                                  .key(key)
                                                  .build();

        byte[] payload = ("payload-" + i).getBytes(StandardCharsets.UTF_8);

        return s3.putObject(putReq, AsyncRequestBody.fromBytes(payload))
                 .thenCompose(r ->
                                  s3.getObject(getReq, AsyncResponseTransformer.toBytes()))
                 .thenAccept(r -> {
                     // no-op
                 });
    }

    /**
     * Simple in-memory MetricPublisher that aggregates counts and durations.
     */
    static final class InMemoryMetricPublisher implements MetricPublisher {

        private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();
        private final Map<String, LongAdder> totals = new ConcurrentHashMap<>();

        @Override
        public void publish(MetricCollection metricCollection) {
            record(metricCollection);
        }

        private void record(MetricCollection metrics) {
            metrics.forEach(metric -> {
                String name = metric.metric().name();
                Object value = metric.value();


                if (value instanceof Duration) {
                    counters.computeIfAbsent(name, k -> new LongAdder()).increment();
                    totals.computeIfAbsent(name, k -> new LongAdder())
                          .add(((Duration) value).toNanos());
                }
            });

            metrics.children().forEach(this::record);
        }

        @Override
        public void close() {
            // nothing to clean up
        }

        public void printSummary() {
            counters.forEach((name, count) -> {
                long total = totals.getOrDefault(name, new LongAdder()).sum();
                double avg = (double) total / (double) count.sum();
                System.out.printf(
                    "%-40s avg=%.2f ns\tcount=%6d\ttotal=%d%n",
                    name, avg, count.sum(), total);
            });
        }
    }
}
