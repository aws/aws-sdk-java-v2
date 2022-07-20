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

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.PROTOCOL;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.trustAllTlsAttributeMapBuilder;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.benchmark.utils.MockH2Server;
import software.amazon.awssdk.benchmark.utils.MockServer;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;

/**
 * Using aws-crt-client to test against local mock https server.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class AwsCrtClientH2Benchmark extends BaseCrtBenchmark {

    private MockH2Server mockServer;
    private SdkAsyncHttpClient sdkHttpClient;
    private ProtocolRestJsonAsyncClient client;

    @Setup(Level.Trial)
    @Override
    public void setup() throws Exception {
        mockServer = new MockH2Server(false);
        mockServer.start();

        sdkHttpClient = AwsCrtAsyncHttpClient.builder()
                                               .buildWithDefaults(trustAllTlsAttributeMapBuilder()
                                                                      .put(PROTOCOL, Protocol.HTTP2)
                                                                      .build());
        client = ProtocolRestJsonAsyncClient.builder()
                                            .endpointOverride(mockServer.getHttpsUri())
                                            .httpClient(sdkHttpClient)
                                            .build();

        // Making sure the request actually succeeds
        client.allTypes().join();
    }

    @Override
    protected URI getEndpointOverride(MockServer mock) {
        return mock.getHttpsUri();
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(AwsCrtClientBenchmark.class.getSimpleName())
                .addProfiler(StackProfiler.class)
                .build();
        new Runner(opt).run();
    }
}
