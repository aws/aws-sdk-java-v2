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

package software.amazon.awssdk.services.s3.checksum;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.utils.ChecksumUtils.KB;
import static software.amazon.awssdk.services.s3.utils.ChecksumUtils.createDataOfSize;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.internal.DefaultAwsCrtS3V4aSigner;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.utils.CaptureChecksumValidationInterceptor;
import software.amazon.awssdk.services.s3.utils.ChecksumUtils;
import software.amazon.awssdk.testutils.RandomTempFile;

public class AsyncHttpChecksumIntegrationTest extends S3IntegrationTestBase {

    protected static final String KEY = "some-key";
    private static final String BUCKET = temporaryBucketName(AsyncHttpChecksumIntegrationTest.class);
    public static CaptureChecksumValidationInterceptor interceptor = new CaptureChecksumValidationInterceptor();
    protected static S3AsyncClient s3HttpAsync;

    @BeforeAll
    public static void setUp() throws Exception {

        s3 = s3ClientBuilder().build();
        s3Async = s3AsyncClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)).build();

        // Http Client to generate Signed request
        s3HttpAsync = s3AsyncClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .endpointOverride(URI.create("http://s3." + DEFAULT_REGION + ".amazonaws.com")).build();

        createBucket(BUCKET);
        s3.waiter().waitUntilBucketExists(s -> s.bucket(BUCKET));
        interceptor.reset();
    }

    @AfterEach
    public void clear() {
        interceptor.reset();
    }

    @AfterAll
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    void asyncValidUnsignedTrailerChecksumCalculatedBySdkClient() {
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .overrideConfiguration(o -> o.signer(DefaultAwsCrtS3V4aSigner.create()))
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), AsyncRequestBody.fromString("Hello world")).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();
        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(response).isEqualTo("Hello world");
    }

    @Test
    void asyncHttpsValidUnsignedTrailerChecksumCalculatedBySdkClient_withSmallRequestBody() throws InterruptedException {
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), AsyncRequestBody.fromString("Hello world")).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();
        assertThat(interceptor.contentEncoding()).isEqualTo("aws-chunked");

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(response).isEqualTo("Hello world");
    }


    @ParameterizedTest
    @ValueSource(ints = {1 * KB, 3 * KB, 12 * KB, 16 * KB, 17 * KB, 32 * KB, 33 * KB})
    void asyncHttpsValidUnsignedTrailerChecksumCalculatedBySdkClient_withHugeRequestBody(int dataSize) throws InterruptedException {
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .contentEncoding("gzip")
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), AsyncRequestBody.fromString(createDataOfSize(64 * KB, 'a'))).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();
        assertThat(interceptor.contentEncoding()).isEqualTo("gzip,aws-chunked");

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(response).isEqualTo(createDataOfSize(64 * KB, 'a'));
    }

    @ParameterizedTest
    @ValueSource(ints = {1 * KB, 12 * KB, 16 * KB, 17 * KB, 32 * KB, 33 * KB, 65 * KB})
    void asyncHttpsValidUnsignedTrailerChecksumCalculatedBySdkClient_withDifferentChunkSize_OfFileAsyncFileRequestBody
        (int chunkSize) throws IOException {
        File randomFileOfFixedLength = new RandomTempFile(32 * KB + 23);
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), FileAsyncRequestBody.builder().path(randomFileOfFixedLength.toPath())
                                                                        .chunkSizeInBytes(chunkSize)
                                                                        .build()).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);

        byte[] bytes = Files.readAllBytes(randomFileOfFixedLength.toPath());
        assertThat(response).isEqualTo(new String(bytes));
    }

    /**
     * Test two async call made back to back with different sizes parameterized to test for different chunk sizes
     */
    @ParameterizedTest
    @ValueSource(ints = {1 * KB, 12 * KB, 16 * KB, 17 * KB, 32 * KB, 33 * KB})
    void asyncHttpsValidUnsignedTrailer_TwoRequests_withDifferentChunkSize_OfFileAsyncFileRequestBody(int chunkSize)
        throws IOException {

        File randomFileOfFixedLengthOne = new RandomTempFile(64 * KB);
        File randomFileOfFixedLengthTwo = new RandomTempFile(17 * KB);
        CompletableFuture<PutObjectResponse> putObjectFutureOne =
            s3Async.putObject(PutObjectRequest.builder()
                                              .bucket(BUCKET)
                                              .key(KEY)
                                              .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                              .build(),
                              FileAsyncRequestBody.builder().path(randomFileOfFixedLengthOne.toPath()).chunkSizeInBytes(chunkSize).build());

        String keyTwo = KEY + "_two";
        CompletableFuture<PutObjectResponse> putObjectFutureTwo =
            s3Async.putObject(PutObjectRequest.builder()
                                              .bucket(BUCKET)
                                              .key(keyTwo)
                                              .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                              .build(),
                              FileAsyncRequestBody.builder().path(randomFileOfFixedLengthTwo.toPath()).chunkSizeInBytes(chunkSize).build());

        putObjectFutureOne.join();
        putObjectFutureTwo.join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();

        String responseTwo = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                               .key(keyTwo).checksumMode(ChecksumMode.ENABLED)
                                                               .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();

        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);

        assertThat(response).isEqualTo(new String(Files.readAllBytes(randomFileOfFixedLengthOne.toPath())));
        assertThat(responseTwo).isEqualTo(new String(Files.readAllBytes(randomFileOfFixedLengthTwo.toPath())));

    }


    @Disabled("Http Async Signing is not supported for S3")
    void asyncValidSignedTrailerChecksumCalculatedBySdkClient() {
        ExecutionAttributes executionAttributes = ExecutionAttributes.builder()
                                                                     .put(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING,
                                                                          true).build();
        s3HttpAsync.putObject(PutObjectRequest.builder()
                                              .bucket(BUCKET)
                                              .overrideConfiguration(o -> o.executionAttributes(executionAttributes))
                                              .key(KEY)
                                              .build(), AsyncRequestBody.fromString("Hello world")).join();
        String response = s3HttpAsync.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                                .key(KEY)
                                                                .build(), AsyncResponseTransformer.toBytes()).join()
                                     .asUtf8String();
        assertThat(response).isEqualTo("Hello world");
    }

    @Test
    public void putObject_with_bufferCreatedFromEmptyString() {

        s3HttpAsync.putObject(PutObjectRequest.builder()
                                              .bucket(BUCKET)
                                              .key(KEY)
                                              .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                              .build(), AsyncRequestBody.fromString(""))
                   .join();

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");

        String response = s3HttpAsync.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                                .key(KEY)
                                                                .checksumMode(ChecksumMode.ENABLED)
                                                                .build(), AsyncResponseTransformer.toBytes()).join()
                                     .asUtf8String();

        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(response).isEqualTo("");
    }

    @Test
    public void putObject_with_bufferCreatedFromZeroCapacityByteBuffer() {
        ByteBuffer content = ByteBuffer.allocate(0);
        s3HttpAsync.putObject(PutObjectRequest.builder()
                                              .bucket(BUCKET)
                                              .key(KEY)
                                              .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                              .build(), AsyncRequestBody.fromByteBuffer(content))
                   .join();

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");

        String response = s3HttpAsync.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                                .key(KEY)
                                                                .checksumMode(ChecksumMode.ENABLED)
                                                                .build(), AsyncResponseTransformer.toBytes()).join()
                                     .asUtf8String();

        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(response).isEqualTo("");
    }
}
