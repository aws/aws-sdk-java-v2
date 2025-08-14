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

package software.amazon.awssdk.benchmark.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.Logger;

/**
 * Shared S3 operations implementation used by all benchmark classes.
 */
public class S3BenchmarkImpl {
    private static final Logger logger = Logger.loggerFor(S3BenchmarkImpl.class);
    private static final String TEST_KEY_PREFIX = "benchmark-object-";
    private static final int OBJECT_COUNT = 100;

    private final S3Client s3Client;
    private final String bucketName;
    private final byte[] testData;

    public S3BenchmarkImpl(S3Client s3Client, byte[] testData) {
        this.s3Client = s3Client;
        this.bucketName = "benchmark-bucket-" + UUID.randomUUID().toString().substring(0, 8);
        this.testData = testData;
        ThreadLocalRandom.current().nextBytes(testData);
    }

    public void setup() {
        try {
            // Create bucket
            s3Client.createBucket(CreateBucketRequest.builder()
                                                     .bucket(bucketName)
                                                     .build());

            logger.info(() -> "Created bucket: " + bucketName);

            // Wait for bucket to be ready
            s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
                                                                     .bucket(bucketName)
                                                                     .build());

            // Upload test objects
            for (int i = 0; i < OBJECT_COUNT; i++) {
                String key = TEST_KEY_PREFIX + i;
                s3Client.putObject(
                    PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build(),
                    RequestBody.fromBytes(testData)
                );
            }

            logger.info(() -> "Uploaded " + OBJECT_COUNT + " test objects");

        } catch (Exception e) {
            logger.error(() -> "Setup failed: " + e.getMessage(), e);
            throw new RuntimeException("Failed to setup S3 benchmark", e);
        }
    }

    public void executeGet(String size, Blackhole blackhole) throws IOException {
        // Random key to avoid caching effects
        String key = TEST_KEY_PREFIX + ThreadLocalRandom.current().nextInt(OBJECT_COUNT);

        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(key)
                                                   .build();

        ResponseInputStream<GetObjectResponse> response = null;
        try {
            response = s3Client.getObject(request);
            byte[] data = readAllBytes(response);
            blackhole.consume(data);
            blackhole.consume(response.response());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public void executePut(String size, Blackhole blackhole) {
        String key = "put-object-" + UUID.randomUUID();

        PutObjectRequest request = PutObjectRequest.builder()
                                                   .bucket(bucketName)
                                                   .key(key)
                                                   .build();

        PutObjectResponse response = s3Client.putObject(request,
                                                        RequestBody.fromBytes(testData));

        blackhole.consume(response);

        // Clean up immediately to avoid accumulating objects
        s3Client.deleteObject(DeleteObjectRequest.builder()
                                                 .bucket(bucketName)
                                                 .key(key)
                                                 .build());
    }

    public void cleanup() {
        try {
            // Delete all objects (handle pagination)
            ListObjectsV2Request.Builder listRequestBuilder = ListObjectsV2Request.builder()
                                                                                  .bucket(bucketName);
            String continuationToken = null;
            do {
                if (continuationToken != null) {
                    listRequestBuilder.continuationToken(continuationToken);
                }
                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequestBuilder.build());
                for (S3Object object : listResponse.contents()) {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                                                             .bucket(bucketName)
                                                             .key(object.key())
                                                             .build());
                }
                continuationToken = listResponse.nextContinuationToken();
            } while (continuationToken != null);
            // Delete bucket
            s3Client.deleteBucket(DeleteBucketRequest.builder()
                                                     .bucket(bucketName)
                                                     .build());
            logger.info(() -> "Cleaned up bucket: " + bucketName);

        } catch (Exception e) {
            logger.warn(() -> "Cleanup failed: " + e.getMessage(), e);
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
