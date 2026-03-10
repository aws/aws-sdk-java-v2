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
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.utils.Pair;

/**
 * Isolated ser/de benchmark for V2 EC2 (EC2 Query protocol).
 * Measures only XML parsing + form encoding -- no HTTP, signing, or retries.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class V2Ec2ProtocolBenchmark {

    private static final URI ENDPOINT = URI.create("http://localhost/");
    private static final OperationInfo OP_INFO = OperationInfo.builder()
        .requestUri("/")
        .httpMethod(SdkHttpMethod.POST)
        .hasExplicitPayloadMember(false)
        .hasPayloadMembers(true)
        .operationIdentifier("DescribeInstances")
        .apiVersion("2016-11-15")
        .build();

    private QueryProtocolUnmarshaller unmarshaller;
    private byte[] responseBytes;
    private DescribeInstancesRequest request;

    @Setup
    public void setup() throws Exception {
        unmarshaller = QueryProtocolUnmarshaller.builder().hasResultWrapper(false).build();
        responseBytes = loadFixture("fixtures/ec2-protocol/describe-instances-response.xml");
        request = createRequest();
    }

    @Benchmark
    public void describeInstancesDeser(Blackhole bh) {
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
            .statusCode(200)
            .content(AbortableInputStream.create(new ByteArrayInputStream(responseBytes)))
            .build();

        Pair<DescribeInstancesResponse, ?> result = unmarshaller.unmarshall(DescribeInstancesResponse.builder(), response);
        bh.consume(result.left());
    }

    @Benchmark
    public void describeInstancesSer(Blackhole bh) {
        ProtocolMarshaller<SdkHttpFullRequest> marshaller = QueryProtocolMarshaller.builder()
                                                                                   .endpoint(ENDPOINT)
                                                                                   .operationInfo(OP_INFO)
                                                                                   .isEc2(true)
                                                                                   .build();
        bh.consume(marshaller.marshall(request));
    }

    private static DescribeInstancesRequest createRequest() {
        return DescribeInstancesRequest.builder()
            .instanceIds("i-0abcdef1234567890")
            .filters(
                Filter.builder().name("instance-state-name")
                    .values("running").build(),
                Filter.builder().name("instance-type")
                    .values("m5.xlarge").build())
            .maxResults(100)
            .build();
    }

    private static byte[] loadFixture(String path) throws IOException {
        return software.amazon.awssdk.utils.IoUtils.toByteArray(
            V2Ec2ProtocolBenchmark.class.getClassLoader()
                .getResourceAsStream(path));
    }
}
