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

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.OBJECT_MAPPER;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.apicall.MetricsEnabledBenchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.async.AwsCrtClientBenchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.async.NettyHttpClientH1Benchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.async.NettyHttpClientH2Benchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.sync.ApacheHttpClientBenchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.sync.CrtHttpClientBenchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.sync.UrlConnectionHttpClientBenchmark;
import software.amazon.awssdk.benchmark.apicall.protocol.Ec2ProtocolBenchmark;
import software.amazon.awssdk.benchmark.apicall.protocol.JsonProtocolBenchmark;
import software.amazon.awssdk.benchmark.apicall.protocol.QueryProtocolBenchmark;
import software.amazon.awssdk.benchmark.apicall.protocol.XmlProtocolBenchmark;
import software.amazon.awssdk.benchmark.coldstart.V2DefaultClientCreationBenchmark;
import software.amazon.awssdk.benchmark.coldstart.V2OptimizedClientCreationBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientDeleteV1MapperComparisonBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientGetOverheadBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientGetV1MapperComparisonBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientPutOverheadBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientPutV1MapperComparisonBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientQueryV1MapperComparisonBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientScanV1MapperComparisonBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientUpdateV1MapperComparisonBenchmark;
import software.amazon.awssdk.benchmark.stats.SdkBenchmarkResult;
import software.amazon.awssdk.benchmark.utils.BenchmarkProcessorOutput;
import software.amazon.awssdk.utils.Logger;


public class BenchmarkRunner {

    private static final List<String> PROTOCOL_BENCHMARKS = Arrays.asList(
        Ec2ProtocolBenchmark.class.getSimpleName(), JsonProtocolBenchmark.class.getSimpleName(),
        QueryProtocolBenchmark.class.getSimpleName(), XmlProtocolBenchmark.class.getSimpleName());

    private static final List<String> ASYNC_BENCHMARKS = Arrays.asList(
        NettyHttpClientH2Benchmark.class.getSimpleName(),
        NettyHttpClientH1Benchmark.class.getSimpleName(),
        AwsCrtClientBenchmark.class.getSimpleName());

    private static final List<String> SYNC_BENCHMARKS = Arrays.asList(
        ApacheHttpClientBenchmark.class.getSimpleName(),
        UrlConnectionHttpClientBenchmark.class.getSimpleName(),
        CrtHttpClientBenchmark.class.getSimpleName());

    private static final List<String> COLD_START_BENCHMARKS = Arrays.asList(
        V2OptimizedClientCreationBenchmark.class.getSimpleName(),
        V2DefaultClientCreationBenchmark.class.getSimpleName());

    private static final List<String> MAPPER_BENCHMARKS = Arrays.asList(
            EnhancedClientGetOverheadBenchmark.class.getSimpleName(),
            EnhancedClientPutOverheadBenchmark.class.getSimpleName(),
            EnhancedClientGetV1MapperComparisonBenchmark.class.getSimpleName(),
            EnhancedClientPutV1MapperComparisonBenchmark.class.getSimpleName(),
            EnhancedClientUpdateV1MapperComparisonBenchmark.class.getSimpleName(),
            EnhancedClientDeleteV1MapperComparisonBenchmark.class.getSimpleName(),
            EnhancedClientScanV1MapperComparisonBenchmark.class.getSimpleName(),
            EnhancedClientQueryV1MapperComparisonBenchmark.class.getSimpleName()
    );

    private static final List<String> METRIC_BENCHMARKS = Arrays.asList(MetricsEnabledBenchmark.class.getSimpleName());

    private static final Logger log = Logger.loggerFor(BenchmarkRunner.class);

    private final List<String> benchmarksToRun;
    private final BenchmarkResultProcessor resultProcessor;
    private final BenchmarkRunnerOptions options;

    private BenchmarkRunner(List<String> benchmarksToRun, BenchmarkRunnerOptions options) {
        this.benchmarksToRun = benchmarksToRun;
        this.resultProcessor = new BenchmarkResultProcessor();
        this.options = options;
    }

    public static void main(String... args) throws Exception {
        List<String> benchmarksToRun = new ArrayList<>();
        benchmarksToRun.addAll(SYNC_BENCHMARKS);
        benchmarksToRun.addAll(ASYNC_BENCHMARKS);
        benchmarksToRun.addAll(PROTOCOL_BENCHMARKS);
        benchmarksToRun.addAll(COLD_START_BENCHMARKS);

        log.info(() -> "Skipping tests, to reduce benchmark times: \n" + MAPPER_BENCHMARKS + "\n" + METRIC_BENCHMARKS);

        BenchmarkRunner runner = new BenchmarkRunner(benchmarksToRun, parseOptions(args));

        runner.runBenchmark();
    }

    private void runBenchmark() throws RunnerException {
        log.info(() -> "Running with options: " + options);

        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder();

        benchmarksToRun.forEach(optionsBuilder::include);

        log.info(() -> "Starting to run: " + benchmarksToRun);

        Collection<RunResult> results = new Runner(optionsBuilder.build()).run();

        BenchmarkProcessorOutput processedResults = resultProcessor.processBenchmarkResult(results);
        List<String> failedResults = processedResults.getFailedBenchmarks();

        if (options.outputPath != null) {
            log.info(() -> "Writing results to " + options.outputPath);
            writeResults(processedResults, options.outputPath);
        }

        if (options.check && !failedResults.isEmpty()) {
            log.info(() -> "Failed perf regression tests: " + failedResults);
            throw new RuntimeException("Perf regression tests failed: " + failedResults);
        }
    }

    private static BenchmarkRunnerOptions parseOptions(String[] args) throws ParseException {
        Options cliOptions = new Options();
        cliOptions.addOption("o", "output", true,
                                     "The path to write the benchmark results to.");
        cliOptions.addOption("c", "check", false,
                             "If specified, exit with error code 1 if the results are not within the baseline.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(cliOptions, args);

        BenchmarkRunnerOptions options = new BenchmarkRunnerOptions()
            .check(cmdLine.hasOption("c"));

        if (cmdLine.hasOption("o")) {
            options.outputPath(Paths.get(cmdLine.getOptionValue("o")));
        }

        return options;
    }

    private static void writeResults(BenchmarkProcessorOutput output, Path outputPath) {
        List<SdkBenchmarkResult> results = output.getBenchmarkResults().values().stream().collect(Collectors.toList());
        try (OutputStream os = Files.newOutputStream(outputPath)) {
            OBJECT_MAPPER.writeValue(os, results);
        } catch (IOException e) {
            log.error(() -> "Failed to write the results to " + outputPath, e);
            throw new RuntimeException(e);
        }
    }

    private static class BenchmarkRunnerOptions {
        private Path outputPath;
        private boolean check;

        public BenchmarkRunnerOptions outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public BenchmarkRunnerOptions check(boolean check) {
            this.check = check;
            return this;
        }

        @Override
        public String toString() {
            return "BenchmarkRunnerOptions{" +
                   "outputPath=" + outputPath +
                   ", check=" + check +
                   '}';
        }
    }
}
