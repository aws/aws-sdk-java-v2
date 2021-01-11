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

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.DEFAULT_JDK_SSL_PROVIDER;
import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.OPEN_SSL_PROVIDER;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.getSslProvider;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.trustAllTlsAttributeMapBuilder;

import io.netty.handler.ssl.SslProvider;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.utils.MockServer;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;

/**
 * Using netty client to test against local mock https server.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class NettyHttpClientH1Benchmark extends BaseNettyBenchmark {

    private MockServer mockServer;
    private SdkAsyncHttpClient sdkHttpClient;

    @Param({DEFAULT_JDK_SSL_PROVIDER, OPEN_SSL_PROVIDER})
    private String sslProviderValue;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        mockServer = new MockServer();
        mockServer.start();

        SslProvider sslProvider = getSslProvider(sslProviderValue);

        sdkHttpClient = NettyNioAsyncHttpClient.builder()
                                               .sslProvider(sslProvider)
                                               .buildWithDefaults(trustAllTlsAttributeMapBuilder().build());
        client = ProtocolRestJsonAsyncClient.builder()
                                            .endpointOverride(mockServer.getHttpsUri())
                                            .httpClient(sdkHttpClient)
                                            .build();

        // Making sure the request actually succeeds
        client.allTypes().join();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        mockServer.stop();
        sdkHttpClient.close();
        client.close();
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(NettyHttpClientH1Benchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
