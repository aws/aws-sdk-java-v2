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
import java.io.IOException;
import java.net.URI;
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
import software.amazon.awssdk.protocols.query.internal.marshall.QueryProtocolMarshaller;
import software.amazon.awssdk.protocols.query.internal.unmarshall.QueryProtocolUnmarshaller;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.utils.Pair;

/**
 * Isolated ser/de benchmark for V2 STS (Query protocol).
 * Measures only XML parsing + form encoding -- no HTTP, signing, or retries.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V2QueryProtocolBenchmark {

    private static final URI ENDPOINT = URI.create("http://localhost/");
    private static final OperationInfo OP_INFO = OperationInfo.builder()
        .requestUri("/")
        .httpMethod(SdkHttpMethod.POST)
        .hasExplicitPayloadMember(false)
        .hasPayloadMembers(true)
        .operationIdentifier("AssumeRole")
        .apiVersion("2011-06-15")
        .build();

    private QueryProtocolUnmarshaller unmarshaller;
    private byte[] responseBytes;
    private AssumeRoleRequest request;

    @Setup
    public void setup() throws Exception {
        unmarshaller = QueryProtocolUnmarshaller.builder()
            .hasResultWrapper(true)
            .build();

        responseBytes = loadFixture("fixtures/query-protocol/assumerole-response.xml");
        request = createRequest();
    }

    @Benchmark
    public void assumeRoleDeser(Blackhole bh) {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
            .statusCode(200)
            .content(AbortableInputStream.create(new ByteArrayInputStream(responseBytes)))
            .build();

        Pair<AssumeRoleResponse, ?> result = unmarshaller.unmarshall(AssumeRoleResponse.builder(), response);
        bh.consume(result.left());
    }

    @Benchmark
    public void assumeRoleSer(Blackhole bh) {
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = QueryProtocolMarshaller.builder()
                                                                                   .endpoint(ENDPOINT)
                                                                                   .operationInfo(OP_INFO)
                                                                                   .isEc2(false)
                                                                                   .build();
        bh.consume(marshaller.marshall(request));
    }

    private static AssumeRoleRequest createRequest() {
        return AssumeRoleRequest.builder()
            .roleArn("arn:aws:iam::123456789012:role/benchmark-role")
            .roleSessionName("benchmark-session")
            .durationSeconds(3600)
            .externalId("benchmark-external-id")
            .policy("{\"Version\":\"2012-10-17\"}")
            .build();
    }

    private static byte[] loadFixture(String path) throws IOException {
        return software.amazon.awssdk.utils.IoUtils.toByteArray(
            V2QueryProtocolBenchmark.class.getClassLoader()
                .getResourceAsStream(path));
    }
}
