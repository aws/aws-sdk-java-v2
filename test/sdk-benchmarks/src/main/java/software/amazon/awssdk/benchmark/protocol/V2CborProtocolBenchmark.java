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

package software.amazon.awssdk.benchmark.protocol;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;
import software.amazon.awssdk.protocols.rpcv2.internal.SdkStructuredRpcV2CborFactory;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;

/**
 * Isolated ser/de benchmark for V2 CloudWatch (smithy-rpc-v2-cbor protocol).
 * Measures only CBOR parsing + object construction -- no HTTP, signing, or retries.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V2CborProtocolBenchmark {

    private static final String CONTENT_TYPE = "application/cbor";
    private static final URI ENDPOINT = URI.create("http://localhost/");

    private static final AwsJsonProtocolMetadata METADATA = AwsJsonProtocolMetadata.builder()
        .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR)
        .contentType(CONTENT_TYPE)
        .build();

    private static final OperationInfo OP_INFO = OperationInfo.builder()
        .requestUri("/service/GraniteServiceVersion20100801/operation/GetMetricData")
        .httpMethod(SdkHttpMethod.POST)
        .hasExplicitPayloadMember(false)
        .hasImplicitPayloadMembers(true)
        .hasPayloadMembers(true)
        .build();

    private JsonProtocolUnmarshaller unmarshaller;
    private byte[] responseBytes;
    private GetMetricDataRequest request;

    @Setup
    public void setup() throws Exception {
        unmarshaller = JsonProtocolUnmarshaller.builder()
            .enableFastUnmarshalling(true)
            .protocolUnmarshallDependencies(SmithyRpcV2CborProtocolFactory.defaultProtocolUnmarshallDependencies())
            .build();

        responseBytes = createCborResponseFixture();
        request = createRequest();
    }

    @Benchmark
    public void getMetricDataDeser(Blackhole bh) throws Exception {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
            .statusCode(200)
            .putHeader("Content-Type", CONTENT_TYPE)
            .content(AbortableInputStream.create(new ByteArrayInputStream(responseBytes)))
            .build();
        bh.consume(unmarshaller.unmarshall(GetMetricDataResponse.builder(), response));
    }

    @Benchmark
    public void getMetricDataSer(Blackhole bh) {
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = JsonProtocolMarshallerBuilder.create()
            .endpoint(ENDPOINT)
            .jsonGenerator(SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY.createWriter(CONTENT_TYPE))
            .contentType(CONTENT_TYPE)
            .operationInfo(OP_INFO)
            .sendExplicitNullForPayload(false)
            .protocolMetadata(METADATA)
            .build();
        bh.consume(marshaller.marshall(request));
    }

    private static GetMetricDataRequest createRequest() {
        Instant end = Instant.parse("2026-03-09T00:00:00Z");
        Instant start = end.minusSeconds(3600);
        return GetMetricDataRequest.builder()
            .startTime(start)
            .endTime(end)
            .maxDatapoints(1000)
            .metricDataQueries(
                MetricDataQuery.builder()
                    .id("cpu")
                    .metricStat(MetricStat.builder()
                        .metric(Metric.builder()
                            .namespace("AWS/EC2")
                            .metricName("CPUUtilization")
                            .build())
                        .period(300)
                        .stat("Average")
                        .build())
                    .returnData(true)
                    .build())
            .build();
    }

    private static byte[] createCborResponseFixture() {
        StructuredJsonGenerator gen =
            SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY.createWriter(CONTENT_TYPE);
        gen.writeStartObject();
        gen.writeFieldName("MetricDataResults");
        gen.writeStartArray();
        gen.writeStartObject();
        gen.writeFieldName("Id");
        gen.writeValue("cpu");
        gen.writeFieldName("Label");
        gen.writeValue("CPUUtilization");
        gen.writeFieldName("StatusCode");
        gen.writeValue("Complete");
        gen.writeFieldName("Timestamps");
        gen.writeStartArray();
        long base = 1772611200L;
        for (int i = 0; i < 12; i++) {
            gen.writeValue((double) ((base + i * 300) * 1000));
        }
        gen.writeEndArray();
        gen.writeFieldName("Values");
        gen.writeStartArray();
        for (int i = 0; i < 12; i++) {
            gen.writeValue(45.2 + i * 1.1);
        }
        gen.writeEndArray();
        gen.writeFieldName("Messages");
        gen.writeStartArray();
        gen.writeEndArray();
        gen.writeEndObject();
        gen.writeEndArray();
        gen.writeFieldName("Messages");
        gen.writeStartArray();
        gen.writeEndArray();
        gen.writeEndObject();
        return gen.getBytes();
    }
}
