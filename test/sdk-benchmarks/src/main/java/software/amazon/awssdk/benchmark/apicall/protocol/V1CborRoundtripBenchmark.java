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

package software.amazon.awssdk.benchmark.apicall.protocol;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.protocol.rpcv2cbor.SdkStructuredCborFactory;
import com.amazonaws.protocol.rpcv2cbor.StructuredRpcV2CborGenerator;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import com.amazonaws.services.cloudwatch.model.MetricStat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * V1 roundtrip benchmark for SmithyRpcV2 CBOR protocol using CloudWatch GetMetricData via HTTP servlet.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class V1CborRoundtripBenchmark {

    private ProtocolRoundtripServer server;
    private AmazonCloudWatch client;
    private GetMetricDataRequest request;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] response = createCborResponseFixture();

        ProtocolRoundtripServlet servlet = new ProtocolRoundtripServlet()
            .defaultRoute("application/cbor", response);

        server = new ProtocolRoundtripServer(servlet);
        server.start();

        client = AmazonCloudWatchClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                server.getHttpUri().toString(), "us-east-1"))
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("test", "test")))
            .build();

        Date end = Date.from(java.time.Instant.parse("2026-03-09T00:00:00Z"));
        Date start = Date.from(java.time.Instant.parse("2026-03-09T00:00:00Z").minusSeconds(3600));
        request = new GetMetricDataRequest()
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

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.shutdown();
        server.stop();
    }

    @Benchmark
    public void getMetricData(Blackhole bh) {
        bh.consume(client.getMetricData(request));
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
