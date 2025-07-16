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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.apache4.Apache4Benchmark;
import software.amazon.awssdk.benchmark.apache5.Apache5Benchmark;
import software.amazon.awssdk.benchmark.apache5.Apache5VirtualBenchmark;
import software.amazon.awssdk.benchmark.core.BenchmarkResult;
import software.amazon.awssdk.benchmark.metrics.CloudWatchMetricsPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.JavaSystemSetting;
import software.amazon.awssdk.utils.Logger;

public final class UnifiedBenchmarkRunner {
    private static final Logger logger = Logger.loggerFor(UnifiedBenchmarkRunner.class);

    private UnifiedBenchmarkRunner() {
    }

    private static void printToConsole(String message) {
        // CHECKSTYLE:OFF - We want the Benchmark results to be printed at the end of the run
        System.out.println(message);
        // CHECKSTYLE:ON
    }

    private static boolean isJava21OrHigher() {

        String version = JavaSystemSetting.JAVA_VERSION.getStringValueOrThrow();
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dotPos = version.indexOf('.');
        int majorVersion;
        if (dotPos != -1) {
            majorVersion = Integer.parseInt(version.substring(0, dotPos));
        } else {
            majorVersion = Integer.parseInt(version);
        }
        return majorVersion >= 21;
    }

    public static void main(String[] args) throws Exception {
        // Update logging calls to use Supplier<String> pattern
        logger.info(() -> "Starting unified benchmark comparison");

        String runId = Instant.now().toString();
        CloudWatchMetricsPublisher publisher = new CloudWatchMetricsPublisher(
            Region.US_WEST_2,
            "S3-HTTP-Client-Comparison"
        );

        List<BenchmarkResult> allResults = new ArrayList<>();

        try {
            // Run Apache4 benchmark
            logger.info(() -> "Running Apache4 benchmark...");
            allResults.addAll(runBenchmark("Apache4", Apache4Benchmark.class));

            // Run Apache5 with platform threads
            logger.info(() -> "Running Apache5...");
            allResults.addAll(runBenchmark("Apache5", Apache5Benchmark.class));

            // Only run virtual threads benchmark if Java 21+
            if (isJava21OrHigher()) {
                logger.info(() -> "Running Apache5 with virtual threads...");
                allResults.addAll(runBenchmark("Apache5-Virtual", Apache5VirtualBenchmark.class));
            } else {
                logger.info(() -> "Skipping virtual threads benchmark - requires Java 21 or higher (current: " +
                                  JavaSystemSetting.JAVA_VERSION.getStringValueOrThrow() + ")");
            }

            // Debug: Print all results to understand the structure
            logger.info(() -> "All benchmark results:");
            for (BenchmarkResult result : allResults) {
                logger.info(() -> String.format("Client: %s, Benchmark: %s, Throughput: %.2f",
                                                result.getClientType(), result.getBenchmarkName(), result.getThroughput()));
            }

            // Publish results to CloudWatch
            logger.info(() -> "Publishing results to CloudWatch...");
            for (BenchmarkResult result : allResults) {
                publisher.publishBenchmarkResult(result, runId);
            }
            logger.info(() -> "\nBenchmark complete! CloudWatch metrics published with run ID: " + runId);

            // Print benchmark results summary
            printBenchmarkSummary(allResults);

        } finally {
            publisher.shutdown();
        }
    }

    private static List<BenchmarkResult> runBenchmark(String clientType,
                                                      Class<?> benchmarkClass) throws RunnerException {
        ChainedOptionsBuilder optBuilder = new OptionsBuilder()
            .include(benchmarkClass.getSimpleName())
            .forks(1)
            .warmupIterations(2)
            .measurementIterations(3);

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

        // Log for debugging - update to use Supplier pattern
        logger.info(() -> String.format("Converting: %s -> %s %s", fullBenchmarkName, benchmarkName, parameters));

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
        if (fullLabel == null) {
            return "unknown";
        }

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
            logger.warn(() -> "No benchmark results to display");
            return;
        }

        printToConsole("\n" + repeatString("=", 140));
        printToConsole("BENCHMARK RESULTS SUMMARY");
        printToConsole(repeatString("=", 140));

        // Print header
        printToConsole(String.format("%-20s | %-50s | %-15s | %-15s | %-15s | %-10s",
                                     "Client Type", "Benchmark", "Throughput", "Avg Latency", "P99 Latency", "Threads"));
        printToConsole(repeatString("-", 140));

        // Sort results for better readability
        List<BenchmarkResult> sortedResults = results.stream()
                                                     .filter(r -> r != null && r.getClientType() != null
                                                                  && r.getBenchmarkName() != null)
                                                     .sorted((a, b) -> {
                                                         int clientCompare = a.getClientType().compareTo(b.getClientType());
                                                         if (clientCompare != 0) {
                                                             return clientCompare;
                                                         }
                                                         return a.getBenchmarkName().compareTo(b.getBenchmarkName());
                                                     })
                                                     .collect(Collectors.toList());

        // Print all results (including parameter variations)
        for (BenchmarkResult result : sortedResults) {
            printToConsole(String.format("%-20s | %-50s | %,13.2f/s | %13.2f ms | %13.2f ms | %10d",
                                         result.getClientType(),
                                         result.getBenchmarkName(),
                                         result.getThroughput(),
                                         result.getAvgLatency(),
                                         result.getP99Latency(),
                                         result.getThreadCount()));
        }

        printToConsole(repeatString("=", 140));
        printToConsole(String.format("Total benchmark results: %d", sortedResults.size()));
        printToConsole(repeatString("=", 140));
        // Print performance comparison in between Apache clients for now
        printApachePerformanceComparison(results);

    }

    private static void printApachePerformanceComparison(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        printToConsole("\nPERFORMANCE COMPARISON (Apache5 vs Apache4):");
        printToConsole(repeatString("=", 80));

        Map<String, List<BenchmarkResult>> groupedResults = results.stream()
                                                                   .filter(r -> r != null && r.getBenchmarkName() != null)
                                                                   .collect(Collectors
                                                                                .groupingBy(BenchmarkResult::getBenchmarkName));

        for (Map.Entry<String, List<BenchmarkResult>> entry : groupedResults.entrySet()) {
            String benchmarkName = entry.getKey();
            List<BenchmarkResult> benchmarkResults = entry.getValue();

            // Find Apache4 baseline
            BenchmarkResult apache4 = benchmarkResults.stream()
                                                      .filter(r -> r.getClientType() != null
                                                                   && r.getClientType().equals("Apache4"))
                                                      .findFirst()
                                                      .orElse(null);

            if (apache4 == null) {
                continue;
            }

            printToConsole(String.format("\n%s:", benchmarkName));
            printToConsole(repeatString("-", 80));

            for (BenchmarkResult result : benchmarkResults) {
                if (result.getClientType() != null && !result.getClientType().equals("Apache4")) {
                    double throughputImprovement = ((result.getThroughput() - apache4.getThroughput())
                                                    / apache4.getThroughput()) * 100;
                    double latencyImprovement = ((apache4.getAvgLatency() - result.getAvgLatency())
                                                 / apache4.getAvgLatency()) * 100;

                    printToConsole(String.format("  %-20s: %+.1f%% throughput, %+.1f%% latency improvement",
                                                 result.getClientType(),
                                                 throughputImprovement,
                                                 latencyImprovement));
                }
            }
        }

        printToConsole("\n" + repeatString("=", 80));
    }

    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
