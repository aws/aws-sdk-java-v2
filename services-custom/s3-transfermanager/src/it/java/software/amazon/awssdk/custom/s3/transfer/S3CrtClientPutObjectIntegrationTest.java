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

package software.amazon.awssdk.custom.s3.transfer;

import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.custom.s3.transfer.internal.S3CrtAsyncClient;
import software.amazon.awssdk.custom.s3.transfer.util.ChecksumUtils;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;

public class S3CrtClientPutObjectIntegrationTest extends S3IntegrationTestBase {
    private static final String TEST_BUCKET = temporaryBucketName(S3CrtClientPutObjectIntegrationTest.class);
    private static final String TEST_KEY = "8mib_file.dat";
    private static final int OBJ_SIZE = 8 * 1024 * 1024;

    private static RandomTempFile testFile;

    private S3CrtAsyncClient s3Crt;

    @BeforeClass
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(TEST_BUCKET);

        testFile = new RandomTempFile(TEST_KEY, OBJ_SIZE);
    }

    @Before
    public void methodSetup() {
        s3Crt = S3CrtAsyncClient.builder()
                                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                .region(S3IntegrationTestBase.DEFAULT_REGION)
                                .build();
    }

    @After
    public void methodTeardown() {
        s3Crt.close();
    }

    @AfterClass
    public static void teardown() throws IOException {
        S3IntegrationTestBase.deleteBucketAndAllContents(TEST_BUCKET);
        Files.delete(testFile.toPath());
    }

    @Test
    public void putObject_fileRequestBody_objectSentCorrectly() throws IOException, NoSuchAlgorithmException {
        AsyncRequestBody body = AsyncRequestBody.fromFile(testFile.toPath());
        s3Crt.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), body).join();

        ResponseInputStream<GetObjectResponse> objContent = S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                                               ResponseTransformer.toInputStream());

        byte[] expectedSum = ChecksumUtils.computeCheckSum(Files.newInputStream(testFile.toPath()));

        Assertions.assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }

    @Test
    public void putObject_byteBufferBody_objectSentCorrectly() throws IOException, NoSuchAlgorithmException {
        byte[] data = new byte[16384];
        new Random().nextBytes(data);
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        AsyncRequestBody body = AsyncRequestBody.fromByteBuffer(byteBuffer);

        s3Crt.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), body).join();

        ResponseBytes<GetObjectResponse> responseBytes = S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                                            ResponseTransformer.toBytes());

        byte[] expectedSum = ChecksumUtils.computeCheckSum(byteBuffer);

        Assertions.assertThat(ChecksumUtils.computeCheckSum(responseBytes.asByteBuffer())).isEqualTo(expectedSum);
    }

    @Test
    public void putObject_customRequestBody_objectSentCorrectly() throws IOException, NoSuchAlgorithmException {
        Random rng = new Random();
        int bufferSize = 16384;
        int nBuffers = 15;
        List<ByteBuffer> bodyData = Stream.generate(() -> {
            byte[] data = new byte[bufferSize];
            rng.nextBytes(data);
            return ByteBuffer.wrap(data);
        }).limit(nBuffers).collect(Collectors.toList());

        long contentLength = bufferSize * nBuffers;

        byte[] expectedSum = ChecksumUtils.computeCheckSum(bodyData);

        Flowable<ByteBuffer> publisher = Flowable.fromIterable(bodyData);

        AsyncRequestBody customRequestBody = new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(contentLength);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                publisher.subscribe(subscriber);
            }
        };

        s3Crt.putObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY), customRequestBody).join();

        ResponseInputStream<GetObjectResponse> objContent = S3IntegrationTestBase.s3.getObject(r -> r.bucket(TEST_BUCKET).key(TEST_KEY),
                                                                                               ResponseTransformer.toInputStream());


        Assertions.assertThat(ChecksumUtils.computeCheckSum(objContent)).isEqualTo(expectedSum);
    }
}
