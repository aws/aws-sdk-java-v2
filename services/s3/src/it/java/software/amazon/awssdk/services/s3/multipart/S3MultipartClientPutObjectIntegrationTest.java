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

import static java.util.Base64.getEncoder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AES256;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CheckedInputStream;
import javax.crypto.KeyGenerator;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.utils.ChecksumUtils;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Md5Utils;

@Timeout(value = 60, unit = SECONDS)
public class S3MultipartClientPutObjectIntegrationTest extends S3IntegrationTestBase {

    private static final String TEST_BUCKET = temporaryBucketName(S3MultipartClientPutObjectIntegrationTest.class);
    private static final String TEST_KEY = "testfile.dat";
    private static final int OBJ_SIZE = 1024 * 1024 * 30;
    private static final CapturingInterceptor CAPTURING_INTERCEPTOR = new CapturingInterceptor();
    private static File testFile;
    private static S3AsyncClient mpuS3Client;
    private static ExecutorService executorService = Executors.newFixedThreadPool(2);

    @BeforeAll
    public static void setup() throws Exception {
        setUp();
        createBucket(TEST_BUCKET);

        testFile = new RandomTempFile(OBJ_SIZE);
        mpuS3Client = S3AsyncClient
            .builder()
            .region(DEFAULT_REGION)
            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
            .overrideConfiguration(o -> o.addExecutionInterceptor(new UserAgentVerifyingExecutionInterceptor("NettyNio", ClientType.ASYNC))
                                         .addExecutionInterceptor(CAPTURING_INTERCEPTOR))
            .multipartEnabled(true)
            .build();
    }

    @AfterAll
    public static void teardown() throws Exception {
        mpuS3Client.close();
        testFile.delete();
        deleteBucketAndAllContents(TEST_BUCKET);
        executorService.shutdown();
    }

    @BeforeEach
    public void reset() {
        CAPTURING_INTERCEPTOR.reset();
    }

    @Test
    public void upload_blockingInputStream_shouldSucceed() throws IOException {
        String objectPath = UUID.randomUUID().toString();
        String expectedMd5 = Md5Utils.md5AsBase64(testFile);

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(null);

        CompletableFuture<PutObjectResponse> put =
            mpuS3Client.putObject(req -> req.bucket(TEST_BUCKET).key(objectPath)
                                            .build(), body);
        body.writeInputStream(new FileInputStream(testFile));
        put.join();

        ResponseInputStream<GetObjectResponse> objContent = s3.getObject(r -> r.bucket(TEST_BUCKET).key(objectPath),
                                                                         ResponseTransformer.toInputStream());

        String actualMd5 = BinaryUtils.toBase64(Md5Utils.computeMD5Hash(objContent));
        assertEquals(expectedMd5, actualMd5);
    }

    @Test
    void putObject_fileRequestBody_objectSentCorrectly() throws Exception {
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), body).join();

        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumAlgorithm).isEqualTo("CRC32");
        assertThat(CAPTURING_INTERCEPTOR.uploadPartChecksumAlgorithm).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent = s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                         ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));
        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    void putObject_inputStreamAsyncRequestBody_objectSentCorrectly() throws Exception {
        AsyncRequestBody body = AsyncRequestBody.fromInputStream(
            new FileInputStream(testFile),
            Long.valueOf(OBJ_SIZE),
            executorService);
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET)
                                    .key(TEST_KEY)
                                    .contentLength(Long.valueOf(OBJ_SIZE)), body).join();

        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumAlgorithm).isEqualTo("CRC32");
        assertThat(CAPTURING_INTERCEPTOR.uploadPartChecksumAlgorithm).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent = s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                         ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));
        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    void putObject_byteAsyncRequestBody_objectSentCorrectly() throws Exception {
        byte[] bytes = RandomStringUtils.randomAscii(OBJ_SIZE).getBytes(Charset.defaultCharset());
        AsyncRequestBody body = AsyncRequestBody.fromBytes(bytes);
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), body).join();

        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumAlgorithm).isEqualTo("CRC32");
        assertThat(CAPTURING_INTERCEPTOR.uploadPartChecksumAlgorithm).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent = s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                         ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(OBJ_SIZE);
        byte[] expectedSum = ChecksumUtils.computeCheckSum(new ByteArrayInputStream(bytes));
        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    void putObject_unknownContentLength_objectSentCorrectly() throws Exception {
        AsyncRequestBody body = FileAsyncRequestBody.builder()
                                                    .path(testFile.toPath())
                                                    .build();
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                body.subscribe(s);
            }
        }).get(30, SECONDS);

        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumAlgorithm).isEqualTo("CRC32");
        assertThat(CAPTURING_INTERCEPTOR.uploadPartChecksumAlgorithm).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent = s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                         ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));
        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    void putObject_withSSECAndChecksum_objectSentCorrectly() throws Exception {
        byte[] secretKey = generateSecretKey();
        String b64Key = getEncoder().encodeToString(secretKey);
        String b64KeyMd5 = Md5Utils.md5AsBase64(secretKey);

        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET)
                                    .key(TEST_KEY)
                                    .sseCustomerKey(b64Key)
                                    .sseCustomerAlgorithm(AES256.name())
                                    .sseCustomerKeyMD5(b64KeyMd5),
                              body).join();

        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumAlgorithm).isEqualTo("CRC32");
        assertThat(CAPTURING_INTERCEPTOR.uploadPartChecksumAlgorithm).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent = s3.getObject(r -> r.bucket(TEST_BUCKET)
                                                                               .key(TEST_KEY)
                                                                               .sseCustomerKey(b64Key)
                                                                               .sseCustomerAlgorithm(AES256.name())
                                                                               .sseCustomerKeyMD5(b64KeyMd5),
                                                                         ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));
        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    void putObject_withoutSpecifiedChecksum_shouldDefaultToCRC32() {
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET)
                                    .key(TEST_KEY),
                              body).join();

        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumAlgorithm).isEqualTo("CRC32");
        assertThat(CAPTURING_INTERCEPTOR.uploadPartChecksumAlgorithm).isEqualTo("CRC32");
        //assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumType).isNull();
        //assertThat(CAPTURING_INTERCEPTOR.completeMpuChecksumType).isNull();

        ResponseInputStream<GetObjectResponse> objContent =
            s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY).checksumMode(ChecksumMode.ENABLED),
                         ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        assertThat(objContent.response().checksumCRC32()).isNotNull();
    }

    @Test
    void putObject_withUserSpecifiedCrc32_setsChecksumTypeFullObject() throws Exception {
        String crc32Val = calculateCRC32AsString(testFile.getPath());
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET)
                                    .key(TEST_KEY)
                                    .checksumCRC32(crc32Val),
                              body).join();

        assertThat(CAPTURING_INTERCEPTOR.completeMpuHeaders.get("x-amz-checksum-crc32")).contains(crc32Val);
        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumAlgorithm).isEqualTo("CRC32");
        assertThat(CAPTURING_INTERCEPTOR.uploadPartChecksumAlgorithm).isEqualTo("CRC32");
        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumType).isEqualTo("FULL_OBJECT");
        assertThat(CAPTURING_INTERCEPTOR.completeMpuChecksumType).isEqualTo("FULL_OBJECT");

        ResponseInputStream<GetObjectResponse> objContent =
            s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY).checksumMode(ChecksumMode.ENABLED),
                         ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        assertThat(objContent.response().checksumCRC32()).isEqualTo(crc32Val);
    }

    @Test
    void putObject_withUserSpecifiedChecksumTypeOtherThanCrc32_shouldHonorChecksum() {
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET)
                                    .key(TEST_KEY)
                                    .checksumAlgorithm(ChecksumAlgorithm.SHA1),
                              body).join();

        assertThat(CAPTURING_INTERCEPTOR.createMpuChecksumAlgorithm).isEqualTo("SHA1");
        assertThat(CAPTURING_INTERCEPTOR.uploadPartChecksumAlgorithm).isEqualTo("SHA1");
    }

    private static String calculateCRC32AsString(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             CheckedInputStream cis = new CheckedInputStream(bis, new CRC32())) {

            IoUtils.drainInputStream(cis);
            long checksumValue = cis.getChecksum().getValue();
            byte[] checksumBytes = ByteBuffer.allocate(4).putInt((int) checksumValue).array();
            return getEncoder().encodeToString(checksumBytes);
        }
    }

    private static byte[] generateSecretKey() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
            generator.init(256, new SecureRandom());
            return generator.generateKey().getEncoded();
        } catch (Exception e) {
            return null;
        }
    }

    private static final class CapturingInterceptor implements ExecutionInterceptor {
        private static final String CHECKSUM_ALGORITHM_HEADER = "x-amz-checksum-algorithm";
        private static final String CHECKSUM_TYPE_HEADER = "x-amz-checksum-type";
        private static final String MP_OBJECT_SIZE_HEADER = "x-amz-mp-object-size";
        private static final String SDK_CHECKSUM_ALGORITHM_HEADER = "x-amz-sdk-checksum-algorithm";
        Map<String, List<String>> completeMpuHeaders;
        String createMpuChecksumType;
        String createMpuChecksumAlgorithm;
        String uploadPartChecksumAlgorithm;
        String completeMpuChecksumType;
        Long completeMpuMpObjectSize;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            Map<String, List<String>> headers = context.httpRequest().headers();
            if (isCreateMpuRequest(context) && headers.containsKey(CHECKSUM_ALGORITHM_HEADER)) {
                createMpuChecksumAlgorithm = headers.get(CHECKSUM_ALGORITHM_HEADER).get(0);
            }

            if (isUploadPartRequest(context) && headers.containsKey(SDK_CHECKSUM_ALGORITHM_HEADER)) {
                uploadPartChecksumAlgorithm = headers.get(SDK_CHECKSUM_ALGORITHM_HEADER).get(0);
            }

            if (headers.containsKey(CHECKSUM_TYPE_HEADER)) {
                if (isCreateMpuRequest(context)) {
                    createMpuChecksumType = headers.get(CHECKSUM_TYPE_HEADER).get(0);
                } else if (isCompleteMpuRequest(context)) {
                    completeMpuChecksumType = headers.get(CHECKSUM_TYPE_HEADER).get(0);
                }
            }

            if (isCompleteMpuRequest(context) && headers.containsKey(MP_OBJECT_SIZE_HEADER)) {
                completeMpuMpObjectSize = Long.valueOf(headers.get(MP_OBJECT_SIZE_HEADER).get(0));
                completeMpuHeaders = headers;
            }
        }

        public void reset() {
            createMpuChecksumType = null;
            createMpuChecksumAlgorithm = null;
            uploadPartChecksumAlgorithm = null;
            completeMpuChecksumType = null;
            completeMpuMpObjectSize = null;
        }

        private static boolean isCreateMpuRequest(Context.BeforeTransmission context) {
            return context.request() instanceof CreateMultipartUploadRequest;
        }

        private static boolean isCompleteMpuRequest(Context.BeforeTransmission context) {
            return context.request() instanceof CompleteMultipartUploadRequest;
        }

        private static boolean isUploadPartRequest(Context.BeforeTransmission context) {
            return context.request() instanceof UploadPartRequest;
        }
    }
}