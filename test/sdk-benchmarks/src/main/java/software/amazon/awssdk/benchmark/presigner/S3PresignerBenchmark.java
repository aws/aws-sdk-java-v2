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

package software.amazon.awssdk.benchmark.presigner;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@State(Scope.Thread)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
public class S3PresignerBenchmark {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";

    private S3Presigner presigner;
    private GetObjectPresignRequest request;

    @Setup(Level.Trial)
    public void setup() {
        presigner = S3Presigner.builder()
                               .region(Region.US_EAST_1)
                               .credentialsProvider(StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create("dummykey", "dummysecret")))
                               .build();

        GetObjectRequest getObjectRequest =
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(KEY)
                            .build();

        request = GetObjectPresignRequest.builder()
                                         .signatureDuration(Duration.ofMinutes(15))
                                         .getObjectRequest(getObjectRequest)
                                         .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        presigner.close();
    }

    @Benchmark
    public PresignedGetObjectRequest presignGetObject() {
        return presigner.presignGetObject(request);
    }
}
