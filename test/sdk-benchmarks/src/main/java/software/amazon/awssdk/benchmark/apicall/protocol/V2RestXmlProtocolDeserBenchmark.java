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
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V2RestXmlProtocolDeserBenchmark {

    private S3Client s3;
    private PutObjectRequest putObjectRequest;
    private RequestBody requestBody;

    @Setup
    public void setup() {
        PutObjectResponse putObjectResponse = PutObjectResponse.builder()
                .eTag("24346e1b50066607059af36e3b684b24")
                .build();

        s3 = new S3Client() {
            @Override
            public String serviceName() {
                return "S3";
            }

            @Override
            public void close() {
            }

            @Override
            public PutObjectResponse putObject(PutObjectRequest request, RequestBody body) {
                return putObjectResponse;
            }
        };

        putObjectRequest = PutObjectRequest.builder()
                .bucket("test-bucket")
                .key("test-key")
                .build();

        requestBody = RequestBody.fromString("test-data");
    }

    @Benchmark
    public PutObjectResponse putObjectDeserialization(Blackhole bh) {
        PutObjectResponse result = s3.putObject(putObjectRequest, requestBody);
        bh.consume(result.eTag());
        return result;
    }
}
