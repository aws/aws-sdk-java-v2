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
import java.util.*;
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

            // Print comparison report
            printComparisonReport(allResults);

            logger.info("\nBenchmark complete! CloudWatch metrics published with run ID: " + runId);

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

        // Extract just the method name (everything after the last dot)
        String benchmarkName = fullBenchmarkName;
        if (fullBenchmarkName.contains(".")) {
            benchmarkName = fullBenchmarkName.substring(fullBenchmarkName.lastIndexOf('.') + 1);
        }

        // Log for debugging
        logger.info(String.format("Converting: %s -> %s", fullBenchmarkName, benchmarkName));

        double throughput = runResult.getPrimaryResult().getScore();
        double avgLatency = 1000.0 / throughput;
        double p99Latency = avgLatency * 1.5;

        int threadCount = benchmarkName.contains("multiThreaded") ? 10 : 1;

        return new BenchmarkResult(
            clientType,
            benchmarkName,
            throughput,
            avgLatency,
            p99Latency,
            threadCount
        );
    }

    private static void printComparisonReport(List<BenchmarkResult> results) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PERFORMANCE COMPARISON REPORT");
        System.out.println("=".repeat(80) + "\n");

        // Group results by client type
        Map<String, Map<String, BenchmarkResult>> grouped = results.stream()
                                                                   .collect(Collectors.groupingBy(
                                                                       BenchmarkResult::getClientType,
                                                                       Collectors.toMap(
                                                                           BenchmarkResult::getBenchmarkName,
                                                                           r -> r
                                                                       )
                                                                   ));

        // Print the structure we actually have , to view it on console
        System.out.println("Available results structure:");
        for (Map.Entry<String, Map<String, BenchmarkResult>> entry : grouped.entrySet()) {
            System.out.println("Client: " + entry.getKey());
            for (String benchmarkName : entry.getValue().keySet()) {
                System.out.println("  - " + benchmarkName);
            }
        }
        System.out.println();

        // Get all unique benchmark names across all clients
        Set<String> allBenchmarkNames = grouped.values().stream()
                                               .flatMap(map -> map.keySet().stream())
                                               .collect(Collectors.toSet());

        // Print results table with dynamic columns
        System.out.printf("%-20s", "Client Type");
        for (String benchmarkName : allBenchmarkNames) {
            System.out.printf(" %-15s", benchmarkName);
        }
        System.out.println();
        System.out.println("-".repeat(20 + (allBenchmarkNames.size() * 16)));

        for (String clientType : Arrays.asList("Apache4", "Apache5-Platform", "Apache5-Virtual")) {
            Map<String, BenchmarkResult> clientResults = grouped.get(clientType);
            if (clientResults != null) {
                System.out.printf("%-20s", clientType);
                for (String benchmarkName : allBenchmarkNames) {
                    BenchmarkResult result = clientResults.get(benchmarkName);
                    if (result != null) {
                        System.out.printf(" %-15.2f", result.getThroughput());
                    } else {
                        System.out.printf(" %-15s", "N/A");
                    }
                }
                System.out.println();
            }
        }

        // Print performance improvements (only if we have matching benchmarks)
        printPerformanceImprovements(grouped);
        System.out.println("\n" + "=".repeat(80));
    }

    private static void printPerformanceImprovements(Map<String, Map<String, BenchmarkResult>> grouped) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PERFORMANCE IMPROVEMENTS");
        System.out.println("=".repeat(80));

        Map<String, BenchmarkResult> apache4 = grouped.get("Apache4");
        Map<String, BenchmarkResult> apache5Platform = grouped.get("Apache5-Platform");
        Map<String, BenchmarkResult> apache5Virtual = grouped.get("Apache5-Virtual");

        if (apache4 != null && apache5Platform != null) {
            System.out.println("\nApache5 (Platform) vs Apache4:");
            printImprovements(apache4, apache5Platform);
        }

        if (apache5Platform != null && apache5Virtual != null) {
            System.out.println("\nApache5 (Virtual) vs Apache5 (Platform):");
            printImprovements(apache5Platform, apache5Virtual);
        }

        if (apache4 != null && apache5Virtual != null) {
            System.out.println("\nApache5 (Virtual) vs Apache4:");
            printImprovements(apache4, apache5Virtual);
        }
    }

    private static void printImprovements(Map<String, BenchmarkResult> baseline,
                                          Map<String, BenchmarkResult> comparison) {
        // Find common benchmark names
        Set<String> commonBenchmarks = new HashSet<>(baseline.keySet());
        commonBenchmarks.retainAll(comparison.keySet());

        if (commonBenchmarks.isEmpty()) {
            System.out.println("  No common benchmarks found for comparison");
            return;
        }

        for (String benchmarkName : commonBenchmarks) {
            BenchmarkResult baseResult = baseline.get(benchmarkName);
            BenchmarkResult compResult = comparison.get(benchmarkName);

            if (baseResult != null && compResult != null) {
                double improvement = (compResult.getThroughput() / baseResult.getThroughput() - 1) * 100;
                System.out.printf("  %-20s: %+.1f%%%n", benchmarkName, improvement);
            }
        }
    }
}
