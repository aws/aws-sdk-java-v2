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

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Random;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.Logger;

/**
 * Shared S3 operations implementation used by all benchmark classes.
 */
public class S3BenchmarkHelper {
    private static final Logger logger = Logger.loggerFor(S3BenchmarkHelper.class);
    private static final String TEST_KEY_PREFIX = "benchmark-object-";

    private final S3Wrapper s3Wrapper;
    private final String name;
    private String bucketName;

    private final EnumMap<ObjectSize, Path> testFiles = new EnumMap<>(ObjectSize.class);

    public S3BenchmarkHelper(String name, S3Client s3Client) {
        this.name = name;
        this.s3Wrapper = new SyncS3Wrapper(s3Client);
    }

    public S3BenchmarkHelper(String name, S3AsyncClient s3AsyncClient) {
        this.name = name;
        this.s3Wrapper = new AsyncS3Wrapper(s3AsyncClient);
    }

    public void setup() {
        try {
            this.bucketName = name.toLowerCase(Locale.ENGLISH)+ "-bucket-" + System.currentTimeMillis();
            // Create bucket
            s3Wrapper.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

            // Wait for bucket to be ready
            s3Wrapper.waitUntilBucketExists(bucketName);

            byte[] testData = new byte[1024 * 1024];
            new Random().nextBytes(testData);

            for (ObjectSize size : ObjectSize.values()) {
                Path p = Files.createTempFile(name + "-test-file", null);
                OutputStream os = Files.newOutputStream(p);

                long chunks = size.sizeInBytes() / testData.length;
                for (long i = 0; i < chunks; i++) {
                    os.write(testData, 0, testData.length);
                }

                os.close();
                testFiles.put(size, p);

                s3Wrapper.putObject(PutObjectRequest.builder().bucket(bucketName).key(objKey(size)).build(), p);
            }
        } catch (Exception e) {
            logger.error(() -> "Setup failed: " + e.getMessage(), e);
            throw new RuntimeException("Failed to setup S3 benchmark", e);
        }
    }

    public String bucketName() {
        return bucketName;
    }

    public String objKey(ObjectSize objectSize) {
        return TEST_KEY_PREFIX + objectSize.name();
    }

    public RequestBody requestBody(ObjectSize objectSize) {
        Path p = testFiles.get(objectSize);
        if (p == null) {
            throw new RuntimeException("Test file not found: " + objectSize.name());
        }

        return RequestBody.fromFile(p);
    }

    public AsyncRequestBody asyncRequestBody(ObjectSize objectSize) {
        Path p = testFiles.get(objectSize);
        if (p == null) {
            throw new RuntimeException("Test file not found: " + objectSize.name());
        }
        return AsyncRequestBody.fromFile(p);
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
                ListObjectsV2Response listResponse = s3Wrapper.listObjectsV2(listRequestBuilder.build());
                for (S3Object object : listResponse.contents()) {
                    s3Wrapper.deleteObject(DeleteObjectRequest.builder()
                                                             .bucket(bucketName)
                                                             .key(object.key())
                                                             .build());
                }
                continuationToken = listResponse.nextContinuationToken();
            } while (continuationToken != null);
            // Delete bucket
            s3Wrapper.deleteBucket(DeleteBucketRequest.builder()
                                                     .bucket(bucketName)
                                                     .build());
            logger.info(() -> "Cleaned up bucket: " + bucketName);
        } catch (Exception e) {
            logger.warn(() -> "Cleanup failed: " + e.getMessage(), e);
        }
    }
}
