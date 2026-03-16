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

import java.time.Instant;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.protocols.rpcv2.internal.SdkStructuredRpcV2CborFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;

/**
 * Roundtrip benchmark for SmithyRpcV2 CBOR protocol using CloudWatch GetMetricData via HTTP servlet.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class V2CborRoundtripBenchmark {

    private ProtocolRoundtripServer server;
    private CloudWatchClient client;
    private GetMetricDataRequest request;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] response = createCborResponseFixture();

        ProtocolRoundtripServlet servlet = new ProtocolRoundtripServlet()
            .defaultRoute("application/cbor", response);

        server = new ProtocolRoundtripServer(servlet);
        server.start();

        client = CloudWatchClient.builder()
            .endpointOverride(server.getHttpUri())
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .httpClient(Apache5HttpClient.create())
            .build();

        Instant end = Instant.parse("2026-03-09T00:00:00Z");
        Instant start = end.minusSeconds(3600);
        request = GetMetricDataRequest.builder()
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

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.close();
        server.stop();
    }

    @Benchmark
    public void getMetricData(Blackhole bh) {
        bh.consume(client.getMetricData(request));
    }

    private static byte[] createCborResponseFixture() {
        StructuredJsonGenerator gen =
            SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY.createWriter("application/cbor");
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
