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

        // Generate unique run ID with timestamp
        String runId = Instant.now().toString();

        // Initialize CloudWatch publisher
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

            // Publish all results to CloudWatch with synchronized timestamp
            logger.info("Publishing results to CloudWatch...");
            for (BenchmarkResult result : allResults) {
                publisher.publishBenchmarkResult(result, runId);
            }

            // Print comparison report to console
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

        // Add executor type parameter for Apache5
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
        String benchmarkName = fullBenchmarkName.substring(fullBenchmarkName.lastIndexOf('.') + 1);

        double throughput = runResult.getPrimaryResult().getScore();
        double avgLatency = 1000.0 / throughput; // Convert to ms
        double p99Latency = avgLatency * 1.5; // Estimate

        // Determine thread count from benchmark name
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

        // Print results table
        System.out.printf("%-20s %-15s %-15s %-15s %-15s%n",
                          "Client Type", "Simple GET", "Simple PUT", "MT GET (10t)", "MT PUT (10t)");
        System.out.println("-".repeat(80));

        for (String clientType : Arrays.asList("Apache4", "Apache5-Platform", "Apache5-Virtual")) {
            Map<String, BenchmarkResult> clientResults = grouped.get(clientType);
            if (clientResults != null) {
                System.out.printf("%-20s %-15.2f %-15.2f %-15.2f %-15.2f%n",
                                  clientType,
                                  clientResults.get("simpleGet").getThroughput(),
                                  clientResults.get("simplePut").getThroughput(),
                                  clientResults.get("multiThreadedGet").getThroughput(),
                                  clientResults.get("multiThreadedPut").getThroughput()
                );
            }
        }

        // Print performance improvements
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

        System.out.println("\n" + "=".repeat(80));
    }

    private static void printImprovements(Map<String, BenchmarkResult> baseline,
                                          Map<String, BenchmarkResult> comparison) {
        for (String op : Arrays.asList("simpleGet", "simplePut",
                                       "multiThreadedGet", "multiThreadedPut")) {
            double improvement = (comparison.get(op).getThroughput() /
                                  baseline.get(op).getThroughput() - 1) * 100;
            System.out.printf("  %-20s: %+.1f%%%n", op, improvement);
        }
    }
}
