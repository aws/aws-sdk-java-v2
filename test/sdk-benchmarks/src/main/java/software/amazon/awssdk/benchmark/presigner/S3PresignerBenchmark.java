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

import java.net.URI;
import java.time.Duration;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@State(Scope.Thread)
public class S3PresignerBenchmark {
    private static final String ENDPOINT = "s3.us-east-1.amazonaws.com";
    private static final String BUCKET = "java-sdk-presigner-benchmark";
    private static final String KEY = "reports/2026/representative object+name.txt";

    private S3Presigner presigner;
    private GetObjectPresignRequest request;

    @Setup(Level.Trial)
    public void setup() {
        presigner = S3Presigner.builder()
                               .region(Region.US_EAST_1)
                               .credentialsProvider(StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create("access-key", "secret-key")))
                               .endpointOverride(URI.create("https://" + ENDPOINT))
                               .serviceConfiguration(S3Configuration.builder()
                                                                    .pathStyleAccessEnabled(true)
                                                                    .build())
                               .build();

        GetObjectRequest getObjectRequest =
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(KEY)
                            .responseContentDisposition("attachment; filename=report.txt")
                            .versionId("benchmark-version")
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
