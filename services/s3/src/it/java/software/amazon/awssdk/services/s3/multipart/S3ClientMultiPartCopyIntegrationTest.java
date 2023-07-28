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

package software.amazon.awssdk.services.s3.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AES256;
import static software.amazon.awssdk.services.s3.utils.ChecksumUtils.computeCheckSum;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.crypto.KeyGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.utils.Md5Utils;

@Timeout(value = 3, unit = TimeUnit.MINUTES)
public class S3ClientMultiPartCopyIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3ClientMultiPartCopyIntegrationTest.class);
    private static final String ORIGINAL_OBJ = "test_file.dat";
    private static final String COPIED_OBJ = "test_file_copy.dat";
    private static final String ORIGINAL_OBJ_SPECIAL_CHARACTER = "original-special-chars-@$%";
    private static final String COPIED_OBJ_SPECIAL_CHARACTER = "special-special-chars-@$%";
    private static final long OBJ_SIZE = ThreadLocalRandom.current().nextLong(8 * 1024 * 1024, 16 * 1024 * 1024 + 1);
    private static final long SMALL_OBJ_SIZE = 1024 * 1024;
    private static S3AsyncClient s3CrtAsyncClient;
    private static S3AsyncClient s3MpuClient;

    @BeforeAll
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(BUCKET);
        s3CrtAsyncClient = S3CrtAsyncClient.builder()
                                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                           .region(DEFAULT_REGION)
                                           .build();
        s3MpuClient = S3AsyncClient.builder()
                                   .region(DEFAULT_REGION)
                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                   .overrideConfiguration(o -> o.addExecutionInterceptor(
                                       new UserAgentVerifyingExecutionInterceptor("NettyNio", ClientType.ASYNC)))
                                   .multipartEnabled(true)
                                   .build();
    }

    @AfterAll
    public static void teardown() throws Exception {
        s3CrtAsyncClient.close();
        s3MpuClient.close();
        deleteBucketAndAllContents(BUCKET);
    }

    public static Stream<S3AsyncClient> s3AsyncClient() {
        return Stream.of(s3MpuClient, s3CrtAsyncClient);
    }

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("s3AsyncClient")
    void copy_singlePart_hasSameContent(S3AsyncClient s3AsyncClient) {
        byte[] originalContent = randomBytes(SMALL_OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ);
        copyObject(ORIGINAL_OBJ, COPIED_OBJ, s3AsyncClient);
        validateCopiedObject(originalContent, ORIGINAL_OBJ);
    }

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("s3AsyncClient")
    void copy_copiedObject_hasSameContent(S3AsyncClient s3AsyncClient) {
        byte[] originalContent = randomBytes(OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ);
        copyObject(ORIGINAL_OBJ, COPIED_OBJ, s3AsyncClient);
        validateCopiedObject(originalContent, ORIGINAL_OBJ);
    }

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("s3AsyncClient")
    void copy_specialCharacters_hasSameContent(S3AsyncClient s3AsyncClient) {
        byte[] originalContent = randomBytes(OBJ_SIZE);
        createOriginalObject(originalContent, ORIGINAL_OBJ_SPECIAL_CHARACTER);
        copyObject(ORIGINAL_OBJ_SPECIAL_CHARACTER, COPIED_OBJ_SPECIAL_CHARACTER, s3AsyncClient);
        validateCopiedObject(originalContent, COPIED_OBJ_SPECIAL_CHARACTER);
    }

    @ParameterizedTest(autoCloseArguments = false)
    @MethodSource("s3AsyncClient")
    void copy_ssecServerSideEncryption_shouldSucceed(S3AsyncClient s3AsyncClient) {
        byte[] originalContent = randomBytes(OBJ_SIZE);
        byte[] secretKey = generateSecretKey();
        String b64Key = Base64.getEncoder().encodeToString(secretKey);
        String b64KeyMd5 = Md5Utils.md5AsBase64(secretKey);

        byte[] newSecretKey = generateSecretKey();
        String newB64Key = Base64.getEncoder().encodeToString(newSecretKey);
        String newB64KeyMd5 = Md5Utils.md5AsBase64(newSecretKey);

        s3AsyncClient.putObject(r -> r.bucket(BUCKET)
                                      .key(ORIGINAL_OBJ)
                                      .sseCustomerKey(b64Key)
                                      .sseCustomerAlgorithm(AES256.name())
                                      .sseCustomerKeyMD5(b64KeyMd5),
                                AsyncRequestBody.fromBytes(originalContent)).join();

        CompletableFuture<CopyObjectResponse> future = s3AsyncClient.copyObject(c -> c
            .sourceBucket(BUCKET)
            .sourceKey(ORIGINAL_OBJ)
            .metadataDirective(MetadataDirective.REPLACE)
            .sseCustomerAlgorithm(AES256.name())
            .sseCustomerKey(newB64Key)
            .sseCustomerKeyMD5(newB64KeyMd5)
            .copySourceSSECustomerAlgorithm(AES256.name())
            .copySourceSSECustomerKey(b64Key)
            .copySourceSSECustomerKeyMD5(b64KeyMd5)
            .destinationBucket(BUCKET)
            .destinationKey(COPIED_OBJ));

        CopyObjectResponse copyObjectResponse = future.join();
        assertThat(copyObjectResponse.responseMetadata().requestId()).isNotNull();
        assertThat(copyObjectResponse.sdkHttpResponse()).isNotNull();
    }

    private static byte[] generateSecretKey() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
            generator.init(256, new SecureRandom());
            return generator.generateKey().getEncoded();
        } catch (Exception e) {
            fail("Unable to generate symmetric key: " + e.getMessage());
            return null;
        }
    }

    private void createOriginalObject(byte[] originalContent, String originalKey) {
        s3CrtAsyncClient.putObject(r -> r.bucket(BUCKET)
                                         .key(originalKey),
                                   AsyncRequestBody.fromBytes(originalContent)).join();
    }

    private void copyObject(String original, String destination, S3AsyncClient s3AsyncClient) {
        CompletableFuture<CopyObjectResponse> future = s3AsyncClient.copyObject(c -> c
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

    public static byte[] randomBytes(long size) {
        byte[] bytes = new byte[Math.toIntExact(size)];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}
