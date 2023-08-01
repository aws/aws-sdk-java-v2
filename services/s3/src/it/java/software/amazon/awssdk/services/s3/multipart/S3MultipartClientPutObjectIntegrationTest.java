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
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.utils.ChecksumUtils;

@Timeout(value = 30, unit = SECONDS)
public class S3MultipartClientPutObjectIntegrationTest extends S3IntegrationTestBase {

    private static final String TEST_BUCKET = temporaryBucketName(S3MultipartClientPutObjectIntegrationTest.class);
    private static final String TEST_KEY = "testfile.dat";
    private static final int OBJ_SIZE = 19 * 1024 * 1024;

    private static File testFile;
    private static S3AsyncClient mpuS3Client;

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(TEST_BUCKET);
        byte[] CONTENT =
            RandomStringUtils.randomAscii(OBJ_SIZE).getBytes(Charset.defaultCharset());

        testFile = File.createTempFile("SplittingPublisherTest", UUID.randomUUID().toString());
        Files.write(testFile.toPath(), CONTENT);
        mpuS3Client = S3AsyncClient
            .builder()
            .region(DEFAULT_REGION)
            .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
            .overrideConfiguration(o -> o.addExecutionInterceptor(
                new UserAgentVerifyingExecutionInterceptor("NettyNio", ClientType.ASYNC)))
            .multipartEnabled(true)
            .build();
    }

    @AfterAll
    public static void teardown() throws Exception {
        mpuS3Client.close();
        testFile.delete();
        deleteBucketAndAllContents(TEST_BUCKET);
    }

    @Test
    void putObject_fileRequestBody_objectSentCorrectly() throws Exception {
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        mpuS3Client.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), body).join();

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

        ResponseInputStream<GetObjectResponse> objContent =
            S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                               ResponseTransformer.toInputStream());

        assertThat(objContent.response().contentLength()).isEqualTo(testFile.length());
        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));
        assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

}
