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

import com.amazonaws.services.s3.AbstractAmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import java.io.ByteArrayInputStream;
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

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15)
@Measurement(iterations = 3, time = 15)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class V1RestXmlProtocolDeserBenchmark {

    private AbstractAmazonS3 s3;
    private PutObjectRequest putObjectRequest;

    @Setup
    public void setup() {
        PutObjectResult putObjectResult = new PutObjectResult();
        putObjectResult.setETag("24346e1b50066607059af36e3b684b24");

        s3 = new AbstractAmazonS3() {
            @Override
            public PutObjectResult putObject(PutObjectRequest request) {
                return putObjectResult;
            }
        };

        putObjectRequest = new PutObjectRequest("test-bucket", "test-key",
                new ByteArrayInputStream("test-data".getBytes()), null);
    }

    @Benchmark
    public PutObjectResult putObjectDeserialization(Blackhole bh) {
        PutObjectResult result = s3.putObject(putObjectRequest);
        bh.consume(result.getETag());
        return result;
    }
}
