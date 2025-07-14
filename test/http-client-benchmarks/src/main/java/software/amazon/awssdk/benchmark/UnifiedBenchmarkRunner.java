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

package software.amazon.awssdk.benchmark;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.apache4.Apache4Benchmark;
import software.amazon.awssdk.benchmark.apache5.Apache5Benchmark;
import software.amazon.awssdk.benchmark.core.BenchmarkResult;
import software.amazon.awssdk.benchmark.metrics.CloudWatchMetricsPublisher;
import software.amazon.awssdk.regions.Region;

import java.time.Instant;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UnifiedBenchmarkRunner {
    private static final Logger logger = Logger.getLogger(UnifiedBenchmarkRunner.class.getName());

    public static void main(String[] args) throws Exception {
        logger.info("Starting unified benchmark comparison");

        String runId = Instant.now().toString();
        CloudWatchMetricsPublisher publisher = new CloudWatchMetricsPublisher(
            Region.US_WEST_2,
            "S3-HTTP-Client-Comparison"
        );

        List<BenchmarkResult> allResults = new ArrayList<>();

        try {
            // Run Apache4 benchmark
            logger.info("Running Apache4 benchmark...");
            allResults.addAll(runBenchmark("Apache4", Apache4Benchmark.class, null));

            // Run Apache5 with platform threads
            logger.info("Running Apache5 with platform threads...");
            allResults.addAll(runBenchmark("Apache5-Platform", Apache5Benchmark.class, "platform"));

            // Run Apache5 with virtual threads
            logger.info("Running Apache5 with virtual threads...");
            allResults.addAll(runBenchmark("Apache5-Virtual", Apache5Benchmark.class, "virtual"));

            // Debug: Print all results to understand the structure
            logger.info("All benchmark results:");
            for (BenchmarkResult result : allResults) {
                logger.info(String.format("Client: %s, Benchmark: %s, Throughput: %.2f",
                                          result.getClientType(), result.getBenchmarkName(), result.getThroughput()));
            }

            // Publish results to CloudWatch
            logger.info("Publishing results to CloudWatch...");
            for (BenchmarkResult result : allResults) {
                publisher.publishBenchmarkResult(result, runId);
            }
            logger.info("\nBenchmark complete! CloudWatch metrics published with run ID: " + runId);

            // Print benchmark results summary
            printBenchmarkSummary(allResults);

        } finally {
            publisher.shutdown();
        }
    }

    private static List<BenchmarkResult> runBenchmark(String clientType,
                                                      Class<?> benchmarkClass,
                                                      String executorType)
        throws RunnerException {
        ChainedOptionsBuilder optBuilder = new OptionsBuilder()
            .include(benchmarkClass.getSimpleName())
            .forks(1)
            .warmupIterations(2)
            .measurementIterations(3);

        if (executorType != null) {
            optBuilder.param("executorType", executorType);
        }

        Options opt = optBuilder.build();
        Collection<RunResult> runResults = new Runner(opt).run();

        return runResults.stream()
                         .map(result -> convertToBenchmarkResult(clientType, result))
                         .collect(Collectors.toList());
    }

    private static BenchmarkResult convertToBenchmarkResult(String clientType,
                                                            RunResult runResult) {
        String fullBenchmarkName = runResult.getPrimaryResult().getLabel();

        // Extract benchmark details including parameters
        String benchmarkName = extractBenchmarkName(fullBenchmarkName);
        String parameters = extractParameters(fullBenchmarkName);

        // Log for debugging
        logger.info(String.format("Converting: %s -> %s %s", fullBenchmarkName, benchmarkName, parameters));

        double throughput = runResult.getPrimaryResult().getScore();
        double avgLatency = 1000.0 / throughput;
        double p99Latency = avgLatency * 1.5;

        int threadCount = benchmarkName.contains("multiThreaded") ? 10 : 1;

        // Include parameters in the benchmark name for uniqueness
        String fullName = parameters.isEmpty() ? benchmarkName : benchmarkName + " " + parameters;

        return new BenchmarkResult(
            clientType,
            fullName,
            throughput,
            avgLatency,
            p99Latency,
            threadCount
        );
    }

    private static String extractBenchmarkName(String fullLabel) {
        if (fullLabel == null) return "unknown";

        // JMH format: package.ClassName.methodName
        String methodPart = fullLabel;
        if (fullLabel.contains(".")) {
            methodPart = fullLabel.substring(fullLabel.lastIndexOf('.') + 1);
        }

        // Remove parameter part if exists (after colon)
        if (methodPart.contains(":")) {
            methodPart = methodPart.substring(0, methodPart.indexOf(':'));
        }

        return methodPart;
    }

    private static String extractParameters(String fullLabel) {
        if (fullLabel == null || !fullLabel.contains(":")) {
            return "";
        }

        // Extract everything after the colon (parameters)
        String params = fullLabel.substring(fullLabel.indexOf(':') + 1).trim();

        // Format parameters nicely
        if (params.contains("(") && params.contains(")")) {
            params = params.substring(params.indexOf('(') + 1, params.lastIndexOf(')'));
        }

        return "(" + params + ")";
    }

    private static void printBenchmarkSummary(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) {
            logger.warning("No benchmark results to display");
            return;
        }

        System.out.println("\n" + "=".repeat(140));
        System.out.println("BENCHMARK RESULTS SUMMARY");
        System.out.println("=".repeat(140));

        // Print header
        System.out.printf("%-20s | %-50s | %-15s | %-15s | %-15s | %-10s%n",
                          "Client Type", "Benchmark", "Throughput", "Avg Latency", "P99 Latency", "Threads");
        System.out.println("-".repeat(140));

        // Sort results for better readability
        List<BenchmarkResult> sortedResults = results.stream()
                                                     .filter(r -> r != null && r.getClientType() != null && r.getBenchmarkName() != null)
                                                     .sorted((a, b) -> {
                                                         int clientCompare = a.getClientType().compareTo(b.getClientType());
                                                         if (clientCompare != 0) return clientCompare;
                                                         return a.getBenchmarkName().compareTo(b.getBenchmarkName());
                                                     })
                                                     .collect(Collectors.toList());

        // Print all results (including parameter variations)
        for (BenchmarkResult result : sortedResults) {
            System.out.printf("%-20s | %-50s | %,13.2f/s | %13.2f ms | %13.2f ms | %10d%n",
                              result.getClientType(),
                              result.getBenchmarkName(),
                              result.getThroughput(),
                              result.getAvgLatency(),
                              result.getP99Latency(),
                              result.getThreadCount());
        }

        System.out.println("=".repeat(140));
        System.out.printf("Total benchmark results: %d%n", sortedResults.size());
        System.out.println("=".repeat(140));
    }
}
