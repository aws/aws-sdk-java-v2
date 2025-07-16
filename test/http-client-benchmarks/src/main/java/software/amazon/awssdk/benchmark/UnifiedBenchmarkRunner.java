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
import software.amazon.awssdk.benchmark.util.SystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Logger;

public final class UnifiedBenchmarkRunner {
    private static final Logger logger = Logger.loggerFor(UnifiedBenchmarkRunner.class);

    private UnifiedBenchmarkRunner() {
    }

    private static boolean isJava21OrHigher() {
        String version = SystemSetting.getJavaVersion();
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

    public static void main(String[] args) {
        try {

            // Run Apache4 benchmarks
            logger.info(() -> "Running Apache4 benchmarks...");
            List<BenchmarkResult> results = new ArrayList<>(runBenchmark("Apache4",
                                                                         Apache4Benchmark.class, null));

            // Run Apache5 benchmarks
            logger.info(() -> "Running Apache5 benchmarks...");
            results.addAll(runBenchmark("Apache5", Apache5Benchmark.class, null));

            // Only run virtual threads benchmark if Java 21+
            if (isJava21OrHigher()) {
                logger.info(() -> "Running Apache5 with virtual threads...");
                results.addAll(runBenchmark("Apache5-Virtual", Apache5VirtualBenchmark.class, null));
            } else {
                logger.info(() -> "Skipping virtual threads benchmark - requires Java 21 or higher (current: " +
                            SystemSetting.getJavaVersion() + ")");
            }

            // Print results summary
            printBenchmarkSummary(results);

            // Optionally publish to CloudWatch
            if (SystemSetting.isPublishToCloudWatchEnabled()) {
                publishResultsToCloudWatch(results);
            }

        } catch (Exception e) {
            logger.error(() -> "Benchmark execution failed: " + e.getMessage(), e);
            System.exit(1);
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
            .measurementIterations(3)
            .threads(1);

        if (executorType != null) {
            optBuilder.param("executorType", executorType);
        }

        Options opt = optBuilder.build();
        Collection<RunResult> runResults = new Runner(opt).run();

        return runResults.stream()
                         .map(result -> {
                             String benchmarkName = extractBenchmarkName(result.getPrimaryResult().getLabel());
                             String paramInfo = extractParamInfo(result.getPrimaryResult().getLabel());
                             if (!paramInfo.isEmpty()) {
                                 benchmarkName = benchmarkName + " (" + paramInfo + ")";
                             }

                             double avgLatency = 0.0;
                             double p99Latency = 0.0;
                             
                             // Safely get secondary metrics if they exist
                             if (result.getSecondaryResults().containsKey("avgLatency")) {
                                 avgLatency = result.getSecondaryResults().get("avgLatency").getScore();
                             }
                             
                             if (result.getSecondaryResults().containsKey("p99Latency")) {
                                 p99Latency = result.getSecondaryResults().get("p99Latency").getScore();
                             }
                             
                             return new BenchmarkResult(
                                 clientType,
                                 benchmarkName,
                                 result.getPrimaryResult().getScore(),
                                 avgLatency,
                                 p99Latency,
                                 result.getParams().getThreads()
                             );
                         })
                         .collect(Collectors.toList());
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

    private static String extractParamInfo(String fullLabel) {
        if (fullLabel == null || !fullLabel.contains(":")) {
            return "";
        }

        String params = fullLabel.substring(fullLabel.indexOf(':') + 1);
        if (params.contains("(") && params.contains(")")) {
            params = params.substring(params.indexOf('(') + 1, params.lastIndexOf(')'));
        }
        return params;
    }

    private static void printBenchmarkSummary(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) {
            logger.warn(() -> "No benchmark results to display");
            return;
        }

        logger.info(() -> "\n" + repeatString("=", 130));
        logger.info(() -> "BENCHMARK RESULTS SUMMARY");
        logger.info(() -> repeatString("=", 130));

        // Print header
        logger.info(() -> String.format("%-20s | %-50s | %-15s | %-15s | %-15s | %-10s",
                          "Client Type", "Benchmark", "Throughput", "Avg Latency", "P99 Latency", "Threads"));
        logger.info(() -> repeatString("-", 130));

        // Sort results for better readability
        List<BenchmarkResult> sortedResults = results.stream()
                                                     .filter(r -> r != null 
                                                                  && r.getClientType() != null 
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
            logger.info(() -> String.format("%-20s | %-50s | %,13.2f/s | %13.2f ms | %13.2f ms | %10d",
                              result.getClientType(),
                              result.getBenchmarkName(),
                              result.getThroughput(),
                              result.getAvgLatency(),
                              result.getP99Latency(),
                              result.getThreadCount()));
        }

        logger.info(() -> repeatString("=", 130));
        logger.info(() -> String.format("Total benchmark results: %d", sortedResults.size()));
        logger.info(() -> repeatString("=", 130));
        // Print performance comparison in between Apache clients
        printApachePerformanceComparison(results);
    }

    private static void printApachePerformanceComparison(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        logger.info(() -> "\nPERFORMANCE COMPARISON (Apache5 vs Apache4):");
        logger.info(() -> repeatString("=", 80));

        Map<String, List<BenchmarkResult>> groupedResults = 
            results.stream()
                   .collect(Collectors.groupingBy(BenchmarkResult::getBenchmarkName));

        for (Map.Entry<String, List<BenchmarkResult>> entry : groupedResults.entrySet()) {
            String benchmarkName = entry.getKey();
            List<BenchmarkResult> benchmarkResults = entry.getValue();

            BenchmarkResult apache4 = benchmarkResults.stream()
                                                      .filter(r -> r.getClientType() != null 
                                                             && r.getClientType().equals("Apache4"))
                                                      .findFirst()
                                                      .orElse(null);

            if (apache4 == null) {
                continue;
            }

            logger.info(() -> String.format("\n%s:", benchmarkName));
            logger.info(() -> repeatString("-", 80));

            for (BenchmarkResult result : benchmarkResults) {
                if (result.getClientType() != null && !result.getClientType().equals("Apache4")) {
                    double throughputImprovement = ((result.getThroughput() - apache4.getThroughput())
                                                    / apache4.getThroughput()) * 100;
                    double latencyImprovement = ((apache4.getAvgLatency() - result.getAvgLatency())
                                                 / apache4.getAvgLatency()) * 100;

                    logger.info(() -> String.format("  %-20s: %+.1f%% throughput, %+.1f%% latency improvement",
                                      result.getClientType(),
                                      throughputImprovement,
                                      latencyImprovement));
                }
            }
        }

        logger.info(() -> "\n" + repeatString("=", 80));
    }

    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder(count * str.length());
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private static void publishResultsToCloudWatch(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) {
            logger.warn(() -> "No benchmark results to publish to CloudWatch");
            return;
        }

        logger.info(() -> "Publishing benchmark results to CloudWatch...");

        CloudWatchMetricsPublisher publisher = new CloudWatchMetricsPublisher(
            Region.US_WEST_2,
            "AWS-SDK-Java/Http-Client-Benchmarks"
        );

        String runId = Instant.now().toString();
        for (BenchmarkResult result : results) {
            publisher.publishBenchmarkResult(result, runId);
        }

        publisher.shutdown();
        logger.info(() -> "Finished publishing benchmark results to CloudWatch");
    }
}
