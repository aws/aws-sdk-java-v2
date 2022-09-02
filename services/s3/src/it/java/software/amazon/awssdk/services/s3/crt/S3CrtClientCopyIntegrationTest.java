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
import static software.amazon.awssdk.services.s3.utils.ChecksumUtils.computeCheckSum;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class S3CrtClientCopyIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3CrtClientCopyIntegrationTest.class);
    private static final String ORIGINAL_OBJ = "test_file.dat";
    private static final String COPIED_OBJ = "test_file_copy.dat";
    private static final String ORIGINAL_OBJ_SPECIAL_CHARACTER = "original-special-chars-@$%";
    private static final String COPIED_OBJ_SPECIAL_CHARACTER = "special-special-chars-@$%";
    private static final long OBJ_SIZE = ThreadLocalRandom.current().nextLong(8 * 1024 * 1024, 16 * 1024 * 1024 + 1);
    private static final long SMALL_OBJ_SIZE = 1024 * 1024;
    private static S3AsyncClient s3CrtAsyncClient;
    @BeforeAll
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET);
        s3CrtAsyncClient = S3CrtAsyncClient.builder()
                                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                           .region(DEFAULT_REGION)
                                           .build();
    }

    @AfterAll
    public static void teardown() throws Exception {
        s3CrtAsyncClient.close();
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    void copy_singlePart_hasSameContent() {
        byte[] originalContent = randomBytes(SMALL_OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ);
        copyObject(ORIGINAL_OBJ, COPIED_OBJ);
        validateCopiedObject(originalContent, ORIGINAL_OBJ);
    }

    @Test
    void copy_copiedObject_hasSameContent() {
        byte[] originalContent = randomBytes(OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ);
        copyObject(ORIGINAL_OBJ, COPIED_OBJ);
        validateCopiedObject(originalContent, ORIGINAL_OBJ);
    }

    @Test
    void copy_specialCharacters_hasSameContent() {
        byte[] originalContent = randomBytes(OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ_SPECIAL_CHARACTER);
        copyObject(ORIGINAL_OBJ_SPECIAL_CHARACTER, COPIED_OBJ_SPECIAL_CHARACTER);
        validateCopiedObject(originalContent, COPIED_OBJ_SPECIAL_CHARACTER);
    }

    private void createOriginalObject(byte[] originalContent, String originalKey) {
        s3CrtAsyncClient.putObject(r -> r.bucket(BUCKET)
                           .key(originalKey),
                                   AsyncRequestBody.fromBytes(originalContent)).join();
    }

    private void copyObject(String original, String destination) {
        CompletableFuture<CopyObjectResponse> future = s3CrtAsyncClient.copyObject(c -> c
            .sourceBucket(BUCKET)
            .sourceKey(original)
            .destinationBucket(BUCKET)
            .destinationKey(destination));

        CopyObjectResponse copyObjectResponse = future.join();
        assertThat(copyObjectResponse.responseMetadata().requestId()).isNotNull();
        assertThat(copyObjectResponse.sdkHttpResponse()).isNotNull();
    }

    private void validateCopiedObject(byte[] originalContent, String originalKey) {
        ResponseBytes<GetObjectResponse> copiedObject = s3.getObject(r -> r.bucket(BUCKET)
                                                                           .key(originalKey),
                                                                     ResponseTransformer.toBytes());
        assertThat(computeCheckSum(copiedObject.asByteBuffer())).isEqualTo(computeCheckSum(ByteBuffer.wrap(originalContent)));
    }

    private static byte[] randomBytes(long size) {
        byte[] bytes = new byte[Math.toIntExact(size)];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}
