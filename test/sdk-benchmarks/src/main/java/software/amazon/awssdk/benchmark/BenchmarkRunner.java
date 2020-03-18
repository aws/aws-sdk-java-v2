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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.apicall.httpclient.async.NettyClientH1NonTlsBenchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.async.NettyHttpClientH1Benchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.async.NettyHttpClientH2Benchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.sync.ApacheHttpClientBenchmark;
import software.amazon.awssdk.benchmark.apicall.httpclient.sync.UrlConnectionHttpClientBenchmark;
import software.amazon.awssdk.benchmark.apicall.protocol.Ec2ProtocolBenchmark;
import software.amazon.awssdk.benchmark.apicall.protocol.JsonProtocolBenchmark;
import software.amazon.awssdk.benchmark.apicall.protocol.QueryProtocolBenchmark;
import software.amazon.awssdk.benchmark.apicall.protocol.XmlProtocolBenchmark;
import software.amazon.awssdk.benchmark.coldstart.V2DefaultClientCreationBenchmark;
import software.amazon.awssdk.benchmark.coldstart.V2OptimizedClientCreationBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientGetOverheadBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.EnhancedClientPutOverheadBenchmark;
import software.amazon.awssdk.benchmark.enhanced.dynamodb.V1MapperComparisonBenchmark;
import software.amazon.awssdk.utils.Logger;


public class BenchmarkRunner {

    private static final List<String> PROTOCOL_BENCHMARKS = Arrays.asList(
        Ec2ProtocolBenchmark.class.getSimpleName(), JsonProtocolBenchmark.class.getSimpleName(),
        QueryProtocolBenchmark.class.getSimpleName(), XmlProtocolBenchmark.class.getSimpleName());

    private static final List<String> ASYNC_BENCHMARKS = Arrays.asList(
        NettyHttpClientH2Benchmark.class.getSimpleName(),
        NettyHttpClientH1Benchmark.class.getSimpleName(),
        NettyClientH1NonTlsBenchmark.class.getSimpleName());

    private static final List<String> SYNC_BENCHMARKS = Arrays.asList(
        ApacheHttpClientBenchmark.class.getSimpleName(),
        UrlConnectionHttpClientBenchmark.class.getSimpleName());

    private static final List<String> COLD_START_BENCHMARKS = Arrays.asList(
        V2OptimizedClientCreationBenchmark.class.getSimpleName(),
        V2DefaultClientCreationBenchmark.class.getSimpleName());

    private static final List<String> MAPPER_BENCHMARKS = Arrays.asList(
            EnhancedClientGetOverheadBenchmark.class.getSimpleName(),
            EnhancedClientPutOverheadBenchmark.class.getSimpleName(),
            V1MapperComparisonBenchmark.class.getSimpleName()
    );

    private static final Logger log = Logger.loggerFor(BenchmarkRunner.class);

    private final List<String> benchmarksToRun;
    private final BenchmarkResultProcessor resultProcessor;

    private BenchmarkRunner(List<String> benchmarksToRun) {
        this.benchmarksToRun = benchmarksToRun;
        this.resultProcessor = new BenchmarkResultProcessor();
    }

    public static void main(String... args) throws RunnerException, JsonProcessingException {
        List<String> benchmarksToRun = new ArrayList<>();
        benchmarksToRun.addAll(SYNC_BENCHMARKS);
        benchmarksToRun.addAll(ASYNC_BENCHMARKS);
        benchmarksToRun.addAll(PROTOCOL_BENCHMARKS);
        benchmarksToRun.addAll(COLD_START_BENCHMARKS);
        benchmarksToRun.addAll(MAPPER_BENCHMARKS);

        BenchmarkRunner runner = new BenchmarkRunner(benchmarksToRun);

        runner.runBenchmark();
    }

    private void runBenchmark() throws RunnerException {
        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder();

        benchmarksToRun.forEach(optionsBuilder::include);

        log.info(() -> "Starting to run: " + benchmarksToRun);

        Collection<RunResult> results = new Runner(optionsBuilder.build()).run();

        List<String> failedResult = resultProcessor.processBenchmarkResult(results);

        if (!failedResult.isEmpty()) {
            log.info(() -> "Failed perf regression tests: " + failedResult);
            throw new RuntimeException("Perf regression tests failed: " + failedResult);
        }
    }
}
