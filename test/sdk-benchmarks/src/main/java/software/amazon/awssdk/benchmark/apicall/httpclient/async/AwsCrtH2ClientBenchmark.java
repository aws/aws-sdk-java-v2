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
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.awaitCountdownLatchUninterruptibly;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.countDownUponCompletion;

import java.util.concurrent.CountDownLatch;
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
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.benchmark.apicall.httpclient.SdkHttpClientBenchmark;
import software.amazon.awssdk.benchmark.utils.MockH2Server;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Benchmark for CRT HTTP client with HTTP/2 over TLS.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
public class AwsCrtH2ClientBenchmark implements SdkHttpClientBenchmark {

    private MockH2Server mockServer;
    private SdkAsyncHttpClient sdkHttpClient;
    private ProtocolRestJsonAsyncClient client;

    static {
        //System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Error);
    }

    @Setup(Level.Trial)
    public void setup() throws Exception {
        mockServer = new MockH2Server(true);
        mockServer.start();

        sdkHttpClient = AwsCrtAsyncHttpClient.builder()
                                             .buildWithDefaults(
                                                 AttributeMap.builder()
                                                             .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                                                             .put(SdkHttpConfigurationOption.PROTOCOL,
                                                                  Protocol.HTTP2)
                                                             .build());

        client = ProtocolRestJsonAsyncClient.builder()
                                            .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                            .endpointOverride(mockServer.getHttpsUri())
                                            .httpClient(sdkHttpClient)
                                            .region(Region.US_EAST_1)
                                            .build();

        client.allTypes().join();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        mockServer.stop();
        client.close();
        sdkHttpClient.close();
    }

    @Override
    @Benchmark
    @OperationsPerInvocation(CONCURRENT_CALLS)
    public void concurrentApiCall(Blackhole blackhole) {
        CountDownLatch countDownLatch = new CountDownLatch(CONCURRENT_CALLS);
        for (int i = 0; i < CONCURRENT_CALLS; i++) {
            countDownUponCompletion(blackhole, client.allTypes(), countDownLatch);
        }
        awaitCountdownLatchUninterruptibly(countDownLatch, 10, TimeUnit.SECONDS);
    }

    @Override
    @Benchmark
    public void sequentialApiCall(Blackhole blackhole) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownUponCompletion(blackhole, client.allTypes(), countDownLatch);
        awaitCountdownLatchUninterruptibly(countDownLatch, 1, TimeUnit.SECONDS);
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(AwsCrtH2ClientBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        new Runner(opt).run();
    }
}
