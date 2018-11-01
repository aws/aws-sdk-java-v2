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
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.benchmark.utils.ApiCallUtils;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.protocolec2.ProtocolEc2Client;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;
import software.amazon.awssdk.utils.Pair;

@State(Scope.Thread)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class ApiCallProtocolBenchmark {

    private static final String JSON_ERROR_RESPONSE= "{\"__type\": \"SomeUnknownType\"}";
    private WireMockServer wireMockServer = new WireMockServer(options().port(8089)); //No-args constructor will start on port 8080, no HTTPS

    @Param({"xml", "json", "ec2", "query"})
    private String protocol;

//    @Param({"true", "false"})
//    private boolean isSuccessful;

    private SdkClient client;

    private Runnable runnable;

    private Map<String, List<Pair<String, String>>> protocolToResponseMap;

    private static final String XML_ERROR_RESPONSE = "<ErrorResponse><Error><Code>UnmodeledException</Code></Error></ErrorResponse>";

    private static final String XML_BODY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><AllTypesResponse "
                                           + "xmlns=\"https://restxml/\"><stringMember>foo</stringMember><integerMember>123"
                                           + "</integerMember><booleanMember>true</booleanMember><floatMember>123"
                                           + ".0</floatMember><doubleMember>123"
                                           + ".9</doubleMember><longMember>123</longMember><simpleList><member>so "
                                           + "simple</member></simpleList><listOfStructs><member><StringMember>listOfStructs1"
                                           + "</StringMember></member></listOfStructs><timestampMember>2018-10-31T10:51:12"
                                           + ".302183Z</timestampMember><structWithNestedTimestampMember><NestedTimestamp>2018"
                                           + "-10-31T10:51:12.311305Z</NestedTimestamp></structWithNestedTimestampMember"
                                           + "><blobArg>aGVsbG8gd29ybGQ=</blobArg></AllTypesResponse>";

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

        switch (protocol) {
            case "xml":
                client = ProtocolRestXmlClient.builder()
                                              .endpointOverride(uri)
                                              .build();
                //if (isSuccessful) {
                    stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(XML_BODY)));
                //} else {
                    //stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(XML_ERROR_RESPONSE)));
                //}
                runnable = () -> ((ProtocolRestXmlClient) client).allTypes(ApiCallUtils.xmlAllTypeRequest());
                break;
            case "ec2":
                stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(XML_BODY)));
                client = ProtocolEc2Client.builder().endpointOverride(uri).build();
                runnable = () -> ((ProtocolEc2Client) client).allTypes(ApiCallUtils.ec2AllTypeRequest());
                break;
            case "json":
                client = ProtocolRestJsonClient.builder()
                                               .endpointOverride(uri)
                                               .build();
                //if (isSuccessful) {
                    stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(JSON_BODY)));
                //} else {
                //    stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(JSON_ERROR_RESPONSE)));
                //}
                //stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(JSON_BODY)));
                runnable = () -> ((ProtocolRestJsonClient) client).allTypes(ApiCallUtils.jsonAllTypeRequest());
                break;
            case "query":
                client = ProtocolQueryClient.builder()
                                            .endpointOverride(uri)
                                            .build();
                stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(XML_BODY)));
                runnable = () -> ((ProtocolQueryClient) client).allTypes(ApiCallUtils.queryAllTypeRequest());
                break;
                default:
                    throw new IllegalArgumentException("invalid protocol");
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        wireMockServer.stop();
    }

    @Benchmark
    public void apiCall() {
        runnable.run();
    }

    public static void main(String... args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ApiCallProtocolBenchmark.class.getSimpleName())
            //.addProfiler(StackProfiler.class)
            //.addProfiler(GCProfiler.class)
            .build();
        new Runner(opt).run();
//        results.iterator().forEachRemaining(r -> {
//            r.
//        });

    }
}
