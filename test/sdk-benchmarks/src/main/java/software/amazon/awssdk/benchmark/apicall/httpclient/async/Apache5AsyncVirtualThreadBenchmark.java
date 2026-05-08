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

package software.amazon.awssdk.benchmark.apicall.httpclient.async;

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.CONCURRENT_CALLS;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.trustAllTlsAttributeMapBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.apicall.httpclient.SdkHttpClientBenchmark;
import software.amazon.awssdk.benchmark.utils.MockServer;
import software.amazon.awssdk.http.apache5.Apache5NioAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;

/**
 * Apache5 NIO async client benchmark with virtual threads dispatching concurrent calls.
 * Requires Java 21+.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgsAppend = "-Djdk.tracePinnedThreads=full")
@BenchmarkMode(Mode.Throughput)
public class Apache5AsyncVirtualThreadBenchmark implements SdkHttpClientBenchmark {

    private MockServer mockServer;
    private SdkAsyncHttpClient sdkHttpClient;
    private ProtocolRestJsonAsyncClient client;
    private ExecutorService virtualThreadExecutor;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        mockServer = new MockServer();
        mockServer.start();

        sdkHttpClient = Apache5NioAsyncHttpClient.builder()
                                                 .buildWithDefaults(trustAllTlsAttributeMapBuilder().build());

        client = ProtocolRestJsonAsyncClient.builder()
                                            .endpointOverride(mockServer.getHttpsUri())
                                            .httpClient(sdkHttpClient)
                                            .build();

        virtualThreadExecutor = createVirtualThreadExecutor();

        // Verify request succeeds
        client.allTypes().join();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        virtualThreadExecutor.shutdown();
        virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS);
        mockServer.stop();
        client.close();
        sdkHttpClient.close();
    }

    @Override
    @Benchmark
    @OperationsPerInvocation(CONCURRENT_CALLS)
    public void concurrentApiCall(Blackhole blackhole) {
        List<Future<?>> futures = new ArrayList<>(CONCURRENT_CALLS);
        for (int i = 0; i < CONCURRENT_CALLS; i++) {
            futures.add(virtualThreadExecutor.submit(() -> {
                blackhole.consume(client.allTypes().join());
            }));
        }
        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @Benchmark
    public void sequentialApiCall(Blackhole blackhole) {
        try {
            blackhole.consume(virtualThreadExecutor.submit(() -> client.allTypes().join()).get(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ExecutorService createVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(Apache5AsyncVirtualThreadBenchmark.class.getSimpleName())
            .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
