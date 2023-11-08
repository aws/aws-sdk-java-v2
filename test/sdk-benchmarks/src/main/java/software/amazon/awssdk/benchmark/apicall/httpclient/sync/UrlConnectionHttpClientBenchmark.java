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

package software.amazon.awssdk.benchmark.apicall.httpclient.sync;

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.CONCURRENT_CALLS;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.trustAllTlsAttributeMapBuilder;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.apicall.httpclient.SdkHttpClientBenchmark;
import software.amazon.awssdk.benchmark.utils.MockServer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;

/**
 * Benchmarking for running with different http clients.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class UrlConnectionHttpClientBenchmark implements SdkHttpClientBenchmark {

    private MockServer mockServer;
    private SdkHttpClient sdkHttpClient;
    private ProtocolRestJsonClient client;
    private ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_CALLS);

    @Setup(Level.Trial)
    public void setup() throws Exception {
        mockServer = new MockServer();
        mockServer.start();
        sdkHttpClient = UrlConnectionHttpClient.builder()
                                               .buildWithDefaults(trustAllTlsAttributeMapBuilder().build());
        client = ProtocolRestJsonClient.builder()
                                       .endpointOverride(mockServer.getHttpsUri())
                                       .region(Region.US_EAST_1)
                                       .httpClient(sdkHttpClient)
                                       .build();
        client.allTypes();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        executorService.shutdown();
        mockServer.stop();
        sdkHttpClient.close();
        client.close();
    }

    @Benchmark
    @Override
    public void sequentialApiCall(Blackhole blackhole) {
        blackhole.consume(client.allTypes());
    }

    public static void main(String... args) throws Exception {

        Options opt = new OptionsBuilder()
            .include(UrlConnectionHttpClientBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
