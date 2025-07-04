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
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.awaitCountdownLatchUninterruptibly;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.countDownUponCompletion;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.trustAllTlsAttributeMapBuilder;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.apicall.httpclient.SdkHttpClientBenchmark;
import software.amazon.awssdk.benchmark.utils.MockServer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;

/**
 * Benchmarking for running with different Apache HTTP clients.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class ApacheHttpClientBenchmark implements SdkHttpClientBenchmark {

    private static final int STREAM_SIZE = 1024 * 1024; // 1MB
    private static final byte[] STREAM_DATA = new byte[STREAM_SIZE];

    static {
        // Initialize stream data
        for (int i = 0; i < STREAM_SIZE; i++) {
            STREAM_DATA[i] = (byte) (i % 256);
        }
    }

    @Param({"apache4", "apache5"})
    private String clientType;

    private MockServer mockServer;
    private SdkHttpClient sdkHttpClient;
    private ProtocolRestJsonClient client;
    private ExecutorService executorService;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        mockServer = new MockServer();
        mockServer.start();

        // Create HTTP client based on parameter
        switch (clientType) {
            case "apache4":
                sdkHttpClient = ApacheHttpClient.builder()
                                                .buildWithDefaults(trustAllTlsAttributeMapBuilder().build());
                break;
            case "apache5":
                sdkHttpClient = Apache5HttpClient.builder()
                                                 .buildWithDefaults(trustAllTlsAttributeMapBuilder().build());
                break;
            default:
                throw new IllegalArgumentException("Unknown client type: " + clientType);
        }

        client = ProtocolRestJsonClient.builder()
                                       .endpointOverride(mockServer.getHttpsUri())
                                       .httpClient(sdkHttpClient)
                                       .region(Region.US_EAST_1)
                                       .build();
        executorService = Executors.newFixedThreadPool(CONCURRENT_CALLS);

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

    @Benchmark
    @Override
    @OperationsPerInvocation(CONCURRENT_CALLS)
    public void concurrentApiCall(Blackhole blackhole) {
        CountDownLatch countDownLatch = new CountDownLatch(CONCURRENT_CALLS);
        for (int i = 0; i < CONCURRENT_CALLS; i++) {
            countDownUponCompletion(blackhole,
                                    CompletableFuture.runAsync(() -> client.allTypes(), executorService), countDownLatch);
        }

        awaitCountdownLatchUninterruptibly(countDownLatch, 10, TimeUnit.SECONDS);
    }

    @Benchmark
    @Override
    public void streamingPutOperation(Blackhole blackhole) {
        StreamingInputOperationRequest request = StreamingInputOperationRequest.builder()
                                                                               .build();
        RequestBody requestBody = RequestBody.fromBytes(STREAM_DATA);

        blackhole.consume(client.streamingInputOperation(request, requestBody));
    }

    @Benchmark
    @Override
    @OperationsPerInvocation(CONCURRENT_CALLS)
    public void concurrentStreamingPutOperation(Blackhole blackhole) {
        CountDownLatch countDownLatch = new CountDownLatch(CONCURRENT_CALLS);

        for (int i = 0; i < CONCURRENT_CALLS; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                StreamingInputOperationRequest request = StreamingInputOperationRequest.builder()
                                                                                       .build();
                RequestBody requestBody = RequestBody.fromBytes(STREAM_DATA);
                client.streamingInputOperation(request, requestBody);
            }, executorService);

            countDownUponCompletion(blackhole, future, countDownLatch);
        }

        awaitCountdownLatchUninterruptibly(countDownLatch, 10, TimeUnit.SECONDS);
    }

    @Benchmark
    @Override
    public void streamingOutputOperation(Blackhole blackhole) {
        StreamingOutputOperationRequest request = StreamingOutputOperationRequest.builder()
                                                                                 .build();

        ResponseBytes<StreamingOutputOperationResponse> responseBytes =
            client.streamingOutputOperation(request, ResponseTransformer.toBytes());

        blackhole.consume(responseBytes.asByteArray());
    }

    @Benchmark
    @Override
    @OperationsPerInvocation(CONCURRENT_CALLS)
    public void concurrentStreamingOutputOperation(Blackhole blackhole) {
        CountDownLatch countDownLatch = new CountDownLatch(CONCURRENT_CALLS);

        for (int i = 0; i < CONCURRENT_CALLS; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                StreamingOutputOperationRequest request = StreamingOutputOperationRequest.builder()
                                                                                         .build();

                ResponseBytes<StreamingOutputOperationResponse> responseBytes =
                    client.streamingOutputOperation(request, ResponseTransformer.toBytes());

                blackhole.consume(responseBytes.asByteArray());
            }, executorService);

            countDownUponCompletion(blackhole, future, countDownLatch);
        }

        awaitCountdownLatchUninterruptibly(countDownLatch, 10, TimeUnit.SECONDS);
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(ApacheHttpClientBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .addProfiler(GCProfiler.class)
            .build();

        Collection<RunResult> run = new Runner(opt).run();
    }
}
