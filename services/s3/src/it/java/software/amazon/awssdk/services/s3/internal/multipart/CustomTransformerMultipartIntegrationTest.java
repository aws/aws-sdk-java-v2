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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Integration test verifying that a custom AsyncResponseTransformer receives
 * correct full-object response metadata when used with the multipart client.
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
public class CustomTransformerMultipartIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(CustomTransformerMultipartIntegrationTest.class);
    private static final int MIB = 1024 * 1024;
    private static final int PART_SIZE = 5 * MIB;
    private static final String LARGE_KEY = "large-object.dat";
    private static final String MPU_CHECKSUM_KEY = "mpu-checksum-object.dat";
    private static final long LARGE_OBJECT_SIZE = 3L * PART_SIZE; // 15MB → 3 parts

    private static S3AsyncClient multipartClient;

    @BeforeAll
    static void setup() throws Exception {
        setUp();
        createBucket(BUCKET);

        multipartClient = S3AsyncClient.builder()
            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
            .region(DEFAULT_REGION)
            .multipartEnabled(true)
            .multipartConfiguration(c -> c.minimumPartSizeInBytes((long) PART_SIZE))
            .build();

        // Upload a large object via multipart
        byte[] data = new byte[(int) LARGE_OBJECT_SIZE];
        new Random(42).nextBytes(data);
        multipartClient.putObject(r -> r.bucket(BUCKET).key(LARGE_KEY),
            AsyncRequestBody.fromBytes(data)).join();

        // Upload MPU object with CRC32 checksum
        uploadMpuWithChecksum();
    }

    @AfterAll
    static void tearDown() {
        deleteBucketAndAllContents(BUCKET);
        if (multipartClient != null) {
            multipartClient.close();
        }
    }

    @Test
    void customTransformer_receivesFullObjectMetadata() {
        AsyncResponseTransformer<GetObjectResponse, String> customTransformer =
            new AsyncResponseTransformer<GetObjectResponse, String>() {
                private CompletableFuture<String> future;
                private GetObjectResponse response;

                @Override
                public CompletableFuture<String> prepare() {
                    future = new CompletableFuture<>();
                    return future;
                }

                @Override
                public void onResponse(GetObjectResponse r) {
                    this.response = r;
                }

                @Override
                public void onStream(SdkPublisher<ByteBuffer> publisher) {
                    publisher.subscribe(new Subscriber<ByteBuffer>() {
                        @Override public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }
                        @Override public void onNext(ByteBuffer b) { }
                        @Override public void onError(Throwable t) { future.completeExceptionally(t); }
                        @Override public void onComplete() {
                            future.complete(
                                "contentLength=" + response.contentLength()
                                + "|contentRange=" + response.contentRange());
                        }
                    });
                }

                @Override
                public void exceptionOccurred(Throwable error) {
                    future.completeExceptionally(error);
                }
            };

        String result = multipartClient.getObject(
            GetObjectRequest.builder().bucket(BUCKET).key(LARGE_KEY).build(),
            customTransformer).join();

        assertThat(result).contains("contentLength=" + LARGE_OBJECT_SIZE);
        assertThat(result).contains("contentRange=bytes 0-" + (LARGE_OBJECT_SIZE - 1) + "/" + LARGE_OBJECT_SIZE);
    }

    @Test
    void customTransformer_mpuWithChecksumMode_checksumNulled() {
        AsyncResponseTransformer<GetObjectResponse, String> customTransformer =
            new AsyncResponseTransformer<GetObjectResponse, String>() {
                private CompletableFuture<String> future;
                private GetObjectResponse response;

                @Override public CompletableFuture<String> prepare() { future = new CompletableFuture<>(); return future; }
                @Override public void onResponse(GetObjectResponse r) { this.response = r; }
                @Override public void onStream(SdkPublisher<ByteBuffer> publisher) {
                    publisher.subscribe(new Subscriber<ByteBuffer>() {
                        @Override public void onSubscribe(Subscription s) { s.request(Long.MAX_VALUE); }
                        @Override public void onNext(ByteBuffer b) { }
                        @Override public void onError(Throwable t) { future.completeExceptionally(t); }
                        @Override public void onComplete() {
                            future.complete("contentLength=" + response.contentLength()
                                + "|checksumType=" + response.checksumType()
                                + "|checksumCRC32=" + response.checksumCRC32());
                        }
                    });
                }
                @Override public void exceptionOccurred(Throwable error) { future.completeExceptionally(error); }
            };

        String result = multipartClient.getObject(
            GetObjectRequest.builder().bucket(BUCKET).key(MPU_CHECKSUM_KEY)
                .checksumMode(ChecksumMode.ENABLED).build(),
            customTransformer).join();

        long expectedSize = 2L * PART_SIZE;
        assertThat(result).contains("contentLength=" + expectedSize);
        assertThat(result).contains("checksumType=COMPOSITE");
        assertThat(result).contains("checksumCRC32=null");
    }

    private static void uploadMpuWithChecksum() {
        S3Client syncClient = S3Client.builder()
            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
            .region(DEFAULT_REGION).build();

        CreateMultipartUploadResponse createResp = syncClient.createMultipartUpload(b -> b.bucket(BUCKET)
            .key(MPU_CHECKSUM_KEY).checksumAlgorithm(ChecksumAlgorithm.CRC32));
        String uploadId = createResp.uploadId();
        List<CompletedPart> parts = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            byte[] data = new byte[PART_SIZE];
            new Random(i).nextBytes(data);
            final int partNum = i;
            UploadPartResponse uploadResp = syncClient.uploadPart(
                b -> b.bucket(BUCKET).key(MPU_CHECKSUM_KEY).uploadId(uploadId).partNumber(partNum)
                      .checksumAlgorithm(ChecksumAlgorithm.CRC32),
                RequestBody.fromBytes(data));
            parts.add(CompletedPart.builder()
                .partNumber(partNum).eTag(uploadResp.eTag()).checksumCRC32(uploadResp.checksumCRC32()).build());
        }

        syncClient.completeMultipartUpload(b -> b.bucket(BUCKET).key(MPU_CHECKSUM_KEY).uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build()));
        syncClient.close();
    }
}
