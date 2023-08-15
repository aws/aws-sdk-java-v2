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

package software.amazon.awssdk.services.s3.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.s3.multipart.S3ClientMultiPartCopyIntegrationTest.randomBytes;
import static software.amazon.awssdk.services.s3.utils.ChecksumUtils.computeCheckSum;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class S3CrossRegionCrtIntegrationTest extends S3IntegrationTestBase {
    public static final Region CROSS_REGION = Region.EU_CENTRAL_1;
    private static final String BUCKET = temporaryBucketName(S3CrossRegionCrtIntegrationTest.class);
    private static final String KEY = "key";
    private static final String ORIGINAL_OBJ = "test_file.dat";
    private static final String COPIED_OBJ = "test_file_copy.dat";
    private static final long OBJ_SIZE = ThreadLocalRandom.current().nextLong(8 * 1024, 16 * 1024 + 1);
    private static S3AsyncClient crtClient;
    private static File file;
    private static ExecutorService executorService;

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(BUCKET);
        crtClient = S3AsyncClient.crtBuilder()
                                 .region(CROSS_REGION)
                                 .crossRegionAccessEnabled(true)
                                 .credentialsProvider(AwsTestBase.CREDENTIALS_PROVIDER_CHAIN)
                                 .build();
        file = new RandomTempFile(10_000);
        S3IntegrationTestBase.s3.putObject(PutObjectRequest.builder()
                                                           .bucket(BUCKET)
                                                           .key(KEY)
                                                           .build(), file.toPath());
        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterAll
    public static void cleanup() {
        crtClient.close();
        S3IntegrationTestBase.deleteBucketAndAllContents(BUCKET);
        executorService.shutdown();
        CrtResource.waitForNoResources();
    }

    @Test
    void crossRegionClient_getObject() throws IOException {
        byte[] bytes =
            crtClient.getObject(b -> b.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join().asByteArray();
        assertThat(bytes).isEqualTo(Files.readAllBytes(file.toPath()));
    }

    @Test
    void putObjectNoSuchBucket() {
        assertThatThrownBy(() -> crtClient.getObject(GetObjectRequest.builder().bucket("nonExistingTestBucket" + UUID.randomUUID()).key(KEY).build(),
                                                     AsyncResponseTransformer.toBytes()).get())
            .hasCauseInstanceOf(S3Exception.class)
            .satisfies(throwable -> assertThat(throwable.getCause()).satisfies(cause -> assertThat(((S3Exception) cause).statusCode()).isEqualTo(404)));
    }

    @Test
    void copy_copiedObject_hasSameContent() {
        byte[] originalContent = randomBytes(OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ);
        copyObject(ORIGINAL_OBJ, COPIED_OBJ);
        validateCopiedObject(originalContent, ORIGINAL_OBJ);
    }

    private void copyObject(String original, String destination) {
        CompletableFuture<CopyObjectResponse> future = crtClient.copyObject(c -> c
            .sourceBucket(BUCKET)
            .sourceKey(original)
            .destinationBucket(BUCKET)
            .destinationKey(destination));

        CopyObjectResponse copyObjectResponse = future.join();
        assertThat(copyObjectResponse.responseMetadata().requestId()).isNotNull();
        assertThat(copyObjectResponse.sdkHttpResponse()).isNotNull();
    }

    @Test
    void putObject_byteBufferBody_objectSentCorrectly() {
        byte[] data = new byte[16384];
        new Random().nextBytes(data);
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        AsyncRequestBody body = AsyncRequestBody.fromByteBuffer(byteBuffer);

        crtClient.putObject(r -> r.bucket(BUCKET).key(KEY), body).join();

        ResponseBytes<GetObjectResponse> responseBytes = S3IntegrationTestBase.s3.getObject(r -> r.bucket(BUCKET).key(KEY),
                                                                                            ResponseTransformer.toBytes());

        byte[] expectedSum = computeCheckSum(byteBuffer);

        assertThat(computeCheckSum(responseBytes.asByteBuffer())).isEqualTo(expectedSum);
    }

    private void validateCopiedObject(byte[] originalContent, String originalKey) {
        ResponseBytes<GetObjectResponse> copiedObject = s3.getObject(r -> r.bucket(BUCKET)
                                                                           .key(originalKey),
                                                                     ResponseTransformer.toBytes());
        assertThat(computeCheckSum(copiedObject.asByteBuffer())).isEqualTo(computeCheckSum(ByteBuffer.wrap(originalContent)));
    }

    private void createOriginalObject(byte[] originalContent, String originalKey) {
        crtClient.putObject(r -> r.bucket(BUCKET)
                                  .key(originalKey),
                            AsyncRequestBody.fromBytes(originalContent)).join();
    }

}
