/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.benchmark.apicall;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.benchmark.utils.ApiCallUtils;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;

@State(Scope.Thread)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class ApiCallHttpClientBenchmark {

    private WireMockServer wireMockServer = new WireMockServer(options().port(8089)); //No-args constructor will start on port
    // 8080, no HTTPS

    @Param( {"UrlConnectionHttpClient", "ApacheHttpClient"})
    private String clientType;

    private SdkHttpClient sdkHttpClient;

    private ProtocolRestJsonClient client;

    private Runnable runnable;

    private static final String JSON_BODY = "{\"StringMember\":\"foo\",\"IntegerMember\":123,\"BooleanMember\":true,"
                                            + "\"FloatMember\":123.0,\"DoubleMember\":123.9,\"LongMember\":123,"
                                            + "\"SimpleList\":[\"so simple\"],"
                                            + "\"ListOfStructs\":[{\"StringMember\":\"listOfStructs1\"}],"
                                            + "\"TimestampMember\":1540982918.887,"
                                            + "\"StructWithNestedTimestampMember\":{\"NestedTimestamp\":1540982918.908},"
                                            + "\"BlobArg\":\"aGVsbG8gd29ybGQ=\"}";


    @Setup(Level.Trial)
    public void setup() {
        wireMockServer.start();

        configureFor("localhost", 8089);
        URI uri = URI.create("http://localhost:8089");

        if (clientType.equalsIgnoreCase("UrlConnectionHttpClient")) {
            sdkHttpClient = UrlConnectionHttpClient.builder().build();
        } else {
            sdkHttpClient = ApacheHttpClient.builder().build();
        }

        client = ProtocolRestJsonClient.builder()
                                       .endpointOverride(uri)
                                       .httpClient(sdkHttpClient)
                                       .build();
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(JSON_BODY)));
        runnable = () -> client.allTypes(ApiCallUtils.jsonAllTypeRequest());
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        wireMockServer.stop();
    }

    @Benchmark
    public void apiCall() {
        try {
            runnable.run();
        } catch (AwsServiceException exception) {
            // Ignore
        }
    }

    public static void main(String... args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ApiCallHttpClientBenchmark.class.getSimpleName())
            //.addProfiler(StackProfiler.class)
            //.addProfiler(GCProfiler.class)
            .build();
        new Runner(opt).run();
        //        results.iterator().forEachRemaining(r -> {
        //            r.
        //        });

    }
}
