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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

/**
 * Roundtrip benchmark for Query protocol using STS AssumeRole via HTTP servlet.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class V2QueryRoundtripBenchmark {

    private ProtocolRoundtripServer server;
    private StsClient client;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] response = ProtocolRoundtripServer.loadFixture("query-protocol/assumerole-response.xml");

        ProtocolRoundtripServlet servlet = new ProtocolRoundtripServlet(response, "text/xml");

        server = new ProtocolRoundtripServer(servlet);
        server.start();

        client = StsClient.builder()
            .endpointOverride(server.getHttpUri())
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .httpClient(Apache5HttpClient.create())
            .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.close();
        server.stop();
    }

    @Benchmark
    public void assumeRole(Blackhole bh) {
        AssumeRoleRequest request = AssumeRoleRequest.builder()
                                   .roleArn("arn:aws:iam::123456789012:role/benchmark-role")
                                   .roleSessionName("benchmark-session")
                                   .durationSeconds(3600)
                                   .externalId("benchmark-external-id")
                                   .policy("{\"Version\":\"2012-10-17\"}")
                                   .build();

        bh.consume(client.assumeRole(request));
    }
}
