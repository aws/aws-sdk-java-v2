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

import com.amazonaws.protocol.rpcv2cbor.RpcV2CborClientMetadata;
import com.amazonaws.protocol.rpcv2cbor.SdkRpcV2CborProtocolFactory;
import com.amazonaws.protocol.rpcv2cbor.SdkStructuredCborFactory;
import com.amazonaws.protocol.rpcv2cbor.StructuredRpcV2CborGenerator;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import com.amazonaws.services.cloudwatch.model.MetricStat;
import com.amazonaws.services.cloudwatch.model.transform.GetMetricDataRequestProtocolMarshaller;
import com.amazonaws.services.cloudwatch.model.transform.GetMetricDataResultRpcV2CborUnmarshaller;
import com.amazonaws.transform.rpcv2cbor.RpcV2CborUnmarshallerContextImpl;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import java.util.Date;
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

/**
 * Isolated ser/de benchmark for V1 CloudWatch (smithy-rpc-v2-cbor protocol).
 * Measures only CBOR parsing + object construction -- no HTTP, signing, or retries.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V1CborProtocolBenchmark {

    private static final CBORFactory CBOR_FACTORY = new CBORFactory();

    private SdkRpcV2CborProtocolFactory protocolFactory;
    private byte[] responseBytes;
    private GetMetricDataRequest request;

    @Setup
    public void setup() throws Exception {
        protocolFactory = new SdkRpcV2CborProtocolFactory(new RpcV2CborClientMetadata());
        responseBytes = createCborResponseFixture();
        request = createRequest();
    }

    @Benchmark
    public void getMetricDataDeser(Blackhole bh) throws Exception {
        CBORParser parser = CBOR_FACTORY.createParser(responseBytes);
        RpcV2CborUnmarshallerContextImpl ctx = new RpcV2CborUnmarshallerContextImpl(
            parser, SdkStructuredCborFactory.CBOR_SCALAR_UNMARSHALLERS, null);
        ctx.nextToken();
        bh.consume(GetMetricDataResultRpcV2CborUnmarshaller.getInstance().unmarshall(ctx));
    }

    @Benchmark
    public void getMetricDataSer(Blackhole bh) {
        bh.consume(new GetMetricDataRequestProtocolMarshaller(protocolFactory).marshall(request));
    }

    private static GetMetricDataRequest createRequest() {
        Date end = Date.from(java.time.Instant.parse("2026-03-09T00:00:00Z"));
        Date start = Date.from(java.time.Instant.parse("2026-03-09T00:00:00Z").minusSeconds(3600));
        return new GetMetricDataRequest()
            .withStartTime(start)
            .withEndTime(end)
            .withMaxDatapoints(1000)
            .withMetricDataQueries(
                new MetricDataQuery()
                    .withId("cpu")
                    .withMetricStat(new MetricStat()
                        .withMetric(new Metric()
                            .withNamespace("AWS/EC2")
                            .withMetricName("CPUUtilization"))
                        .withPeriod(300)
                        .withStat("Average"))
                    .withReturnData(true));
    }

    private static byte[] createCborResponseFixture() {
        StructuredRpcV2CborGenerator gen =
            SdkStructuredCborFactory.SDK_CBOR_FACTORY.createWriter("application/cbor");
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
