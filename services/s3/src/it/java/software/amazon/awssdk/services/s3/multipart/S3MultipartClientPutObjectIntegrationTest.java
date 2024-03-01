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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AES256;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.utils.ChecksumUtils;
import software.amazon.awssdk.utils.Md5Utils;

@Timeout(value = 30, unit = SECONDS)
public class S3MultipartClientPutObjectIntegrationTest extends S3IntegrationTestBase {

    private static final String TEST_BUCKET = temporaryBucketName(S3MultipartClientPutObjectIntegrationTest.class);
    private static final String TEST_KEY = "testfile.dat";
    private static final int OBJ_SIZE = 19 * 1024 * 1024;
    private static final CapturingInterceptor CAPTURING_INTERCEPTOR = new CapturingInterceptor();
    private static final byte[] CONTENT = RandomStringUtils.randomAscii(OBJ_SIZE).getBytes(Charset.defaultCharset());
    private static File testFile;
    private static S3AsyncClient mpuS3Client;

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(TEST_BUCKET);
        testFile = File.createTempFile("SplittingPublisherTest", UUID.randomUUID().toString());
        Files.write(testFile.toPath(), CONTENT);
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
    }

    @BeforeEach
    public void reset() {
        CAPTURING_INTERCEPTOR.reset();
    }

    @Test
    void putObject_fileRequestBody_objectSentCorrectly() throws Exception {
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), body).join();

        assertThat(CAPTURING_INTERCEPTOR.checksumHeader).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent =
            S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
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

        assertThat(CAPTURING_INTERCEPTOR.checksumHeader).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent =
            S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
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

        assertThat(CAPTURING_INTERCEPTOR.checksumHeader).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent =
            S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                               ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));
        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    void putObject_withSSECAndChecksum_objectSentCorrectly() throws Exception {
        byte[] secretKey = generateSecretKey();
        String b64Key = Base64.getEncoder().encodeToString(secretKey);
        String b64KeyMd5 = Md5Utils.md5AsBase64(secretKey);

        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET)
                                    .key(TEST_KEY)
                                    .sseCustomerKey(b64Key)
                                    .sseCustomerAlgorithm(AES256.name())
                                    .sseCustomerKeyMD5(b64KeyMd5),
                              body).join();

        assertThat(CAPTURING_INTERCEPTOR.checksumHeader).isEqualTo("CRC32");

        ResponseInputStream<GetObjectResponse> objContent =
            S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET)
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
    void putObject_withUserSpecifiedChecksumValue_objectSentCorrectly() throws Exception {
        String sha1Val = calculateSHA1AsString();
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET)
                                    .key(TEST_KEY)
                                    .checksumSHA1(sha1Val),
                              body).join();

        assertThat(CAPTURING_INTERCEPTOR.headers.get("x-amz-checksum-sha1")).contains(sha1Val);
        assertThat(CAPTURING_INTERCEPTOR.checksumHeader).isNull();

        ResponseInputStream<GetObjectResponse> objContent =
            S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                               ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));
        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    void putObject_withUserSpecifiedChecksumTypeOtherThanCrc32_shouldHonorChecksum() {
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET)
                                    .key(TEST_KEY)
                                    .checksumAlgorithm(ChecksumAlgorithm.SHA1),
                              body).join();

        assertThat(CAPTURING_INTERCEPTOR.checksumHeader).isEqualTo("SHA1");
    }

    private static String calculateSHA1AsString() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(CONTENT);
        byte[] checksum = md.digest();
        return Base64.getEncoder().encodeToString(checksum);
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
        String checksumHeader;
        Map<String, List<String>> headers;
        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkHttpRequest sdkHttpRequest = context.httpRequest();
            headers = sdkHttpRequest.headers();
            String checksumHeaderName = "x-amz-sdk-checksum-algorithm";
            if (headers.containsKey(checksumHeaderName)) {
                List<String> checksumHeaderVals = headers.get(checksumHeaderName);
                assertThat(checksumHeaderVals).hasSize(1);
                checksumHeader = checksumHeaderVals.get(0);
            }
        }

        public void reset() {
            checksumHeader = null;
        }
    }
}
