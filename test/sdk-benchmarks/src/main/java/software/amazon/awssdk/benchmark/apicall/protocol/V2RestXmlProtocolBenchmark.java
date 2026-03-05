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

import java.util.HashMap;
import java.util.Map;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.StorageClass;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class V2RestXmlProtocolBenchmark {
    private ProtocolBenchmarkServer server;
    private S3Client s3;

    @Setup
    public void setup() throws Exception {
        server = new ProtocolBenchmarkServer();
        server.start();

        System.setProperty("com.amazonaws.services.s3.disableGetObjectMD5Validation", "true");
        System.setProperty("com.amazonaws.services.s3.disablePutObjectMD5Validation", "true");

        s3 = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .endpointOverride(server.getHttpUri())
                .forcePathStyle(true)
                .build();
    }

    @TearDown
    public void tearDown() throws Exception {
        s3.close();
        server.stop();
    }

    @Benchmark
    public void putObject() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");

        s3.putObject(r -> r
                        .bucket("test-bucket")
                        .key("test-key")
                        .contentType("application/octet-stream")
                        .contentEncoding("gzip")
                        .cacheControl("max-age=3600")
                        .acl(ObjectCannedACL.PRIVATE)
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .storageClass(StorageClass.STANDARD)
                        .checksumAlgorithm(ChecksumAlgorithm.SHA256)
                        .metadata(metadata),
                RequestBody.fromString("test-data"));
    }
}
