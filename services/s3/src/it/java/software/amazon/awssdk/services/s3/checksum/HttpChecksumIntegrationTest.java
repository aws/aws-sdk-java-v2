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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.services.s3.utils.ChecksumUtils.KB;
import static software.amazon.awssdk.services.s3.utils.ChecksumUtils.createDataOfSize;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.authcrt.signer.internal.DefaultAwsCrtS3V4aSigner;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.utils.CaptureChecksumValidationInterceptor;
import software.amazon.awssdk.services.s3.utils.ChecksumUtils;
import software.amazon.awssdk.testutils.RandomTempFile;

public class HttpChecksumIntegrationTest extends S3IntegrationTestBase {

    public static final int HUGE_MSG_SIZE = 1600 * KB;
    protected static final String KEY = "some-key";
    private static final String BUCKET = temporaryBucketName(HttpChecksumIntegrationTest.class);
    public static CaptureChecksumValidationInterceptor interceptor = new CaptureChecksumValidationInterceptor();
    protected static S3Client s3Https;
    protected static S3AsyncClient s3HttpAsync;

    @BeforeAll
    public static void setUp() throws Exception {

        // Http Client to generate Signed request
        s3 = s3ClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                              .endpointOverride(URI.create("http://s3." + DEFAULT_REGION + ".amazonaws.com")).build();

        s3Async = s3AsyncClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)).build();

        s3Https = s3ClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)).build();

        // Http Client to generate Signed request
        s3HttpAsync = s3AsyncClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .endpointOverride(URI.create("http://s3." + DEFAULT_REGION + ".amazonaws.com")).build();


        createBucket(BUCKET);
        s3.waiter().waitUntilBucketExists(s ->s.bucket(BUCKET));
        interceptor.reset();
    }

    @AfterAll
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET);
    }


    @AfterEach
    public void clear() {
        interceptor.reset();
    }

    @Test
    public void validHeaderChecksumCalculatedBySdkClient() {
        PutObjectResponse putObjectResponse = s3Https.putObject(PutObjectRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                                                .key(KEY)
                                                                                .build(), RequestBody.fromString("Hello world"));
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.contentEncoding()).isEqualTo(HttpChecksumConstant.AWS_CHUNKED_HEADER);
        assertThat(interceptor.requestChecksumInHeader()).isNull();
        assertThat(putObjectResponse.sdkHttpResponse().firstMatchingHeader("x-amz-checksum-crc32"))
            .hasValue("i9aeUg==");
    }

    @Test
    public void validHeaderChecksumSentDirectlyInTheField() {
        PutObjectResponse putObjectResponse = s3Https.putObject(PutObjectRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                                                .contentEncoding("gzip")
                                                                                .checksumCRC32("i9aeUg==")
                                                                                .key(KEY)
                                                                                .build(), RequestBody.fromString("Hello world"));
        assertThat(interceptor.requestChecksumInHeader()).isEqualTo("i9aeUg==");
        assertThat(interceptor.requestChecksumInTrailer()).isNull();
        assertThat(interceptor.contentEncoding()).isEqualTo("gzip");
        assertThat(putObjectResponse.sdkHttpResponse().firstMatchingHeader("x-amz-checksum-crc32")).hasValue("i9aeUg==");
    }

    @Test
    public void validHeaderChecksumSentDirectlyInTheFieldAndFeatureEnabled() {
        PutObjectResponse putObjectResponse = s3Https.putObject(PutObjectRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                                                .checksumCRC32("i9aeUg==")
                                                                                .key(KEY)
                                                                                .build(), RequestBody.fromString("Hello world"));
        assertThat(interceptor.requestChecksumInHeader()).isEqualTo("i9aeUg==");
        assertThat(interceptor.requestChecksumInTrailer()).isNull();
        assertThat(putObjectResponse.sdkHttpResponse().firstMatchingHeader("x-amz-checksum-crc32")).hasValue("i9aeUg==");
    }

    @Test
    public void invalidHeaderChecksumCalculatedByUserNotOverWrittenBySdkClient() {
        assertThatExceptionOfType(S3Exception.class).isThrownBy(
                                                        () -> s3Https.putObject(PutObjectRequest.builder()
                                                                                                .bucket(BUCKET)
                                                                                                .checksumCRC32("i9aeUgg=")
                                                                                                .key(KEY)
                                                                                                .build(),
                                                                                RequestBody.fromString("Hello world")))
                                                    .withMessageContaining("Value for x-amz-checksum-crc32 header is invalid");
    }

    @Test
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClient() throws InterruptedException {
        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromString("Hello world"));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3Https.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("Hello world");
    }


    @Test
    public void syncValidSignedTrailerChecksumCalculatedBySdkClient() {


        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .build(), RequestBody.fromString("Hello world"));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3.getObject(GetObjectRequest.builder()
                                         .bucket(BUCKET)
                                         .key(KEY)
                                         .checksumMode(ChecksumMode.ENABLED)
                                         .build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.contentEncoding()).isEmpty();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("Hello world");

    }

    @Test
    public void syncValidSignedTrailerChecksumCalculatedBySdkClient_Empty_String() {


        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .contentEncoding("gzip")
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .build(), RequestBody.fromString(""));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.contentEncoding()).isEqualTo("gzip,aws-chunked");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("");

    }

    @Test
    public void syncValidSignedTrailerChecksumCalculatedBySdkClientWithSigv4a() {

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .overrideConfiguration(o -> o.signer(DefaultAwsCrtS3V4aSigner.create()))
                                     .build(), RequestBody.fromString("Hello world"));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("Hello world");
    }

    @Test
    public void syncValidSignedTrailerChecksumCalculatedBySdkClientWithSigv4a_withContentEncoding() {

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .contentEncoding("gzip")
                                     .overrideConfiguration(o -> o.signer(DefaultAwsCrtS3V4aSigner.create()))
                                     .build(), RequestBody.fromString("Hello world"));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.contentEncoding()).isEqualTo("gzip,aws-chunked");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("Hello world");
    }

    @Test
    public void syncUnsignedPayloadForHugeMessage() throws InterruptedException {
        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .contentEncoding("gzip")
                                          .build(), RequestBody.fromString(createDataOfSize(HUGE_MSG_SIZE, 'a')));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.contentEncoding()).isEqualTo("gzip,aws-chunked");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        Thread.sleep(1000);
        ResponseInputStream<GetObjectResponse> s3HttpsObject = s3Https.getObject(
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .checksumMode(ChecksumMode.ENABLED)
                            .key(KEY)
                            .build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo(createDataOfSize(HUGE_MSG_SIZE, 'a'));
    }

    @Test
    public void syncSignedPayloadForHugeMessage() {
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .build(), RequestBody.fromString(createDataOfSize(HUGE_MSG_SIZE, 'a')));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject = s3.getObject(
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .checksumMode(ChecksumMode.ENABLED)
                            .key(KEY)
                            .build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo(createDataOfSize(HUGE_MSG_SIZE, 'a'));
    }

    @Test
    public void syncUnsignedPayloadMultiPartForHugeMessage() throws InterruptedException {
        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromString(createDataOfSize(HUGE_MSG_SIZE, 'a')));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();
        ResponseInputStream<GetObjectResponse> s3HttpsObject = s3Https.getObject(
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(KEY)
                            .build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

        assertThat(interceptor.validationAlgorithm()).isNull();
        assertThat(interceptor.responseValidation()).isNull();
        assertThat(text).isEqualTo(createDataOfSize(HUGE_MSG_SIZE, 'a'));
    }


    @Test
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClient_withSmallFileRequestBody() throws InterruptedException,
                                                                                                        IOException {
        File randomFileOfFixedLength = new RandomTempFile(10 * KB);

        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromFile(randomFileOfFixedLength.toPath()));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3Https.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        byte[] bytes = Files.readAllBytes(randomFileOfFixedLength.toPath());
        assertThat(text).isEqualTo(new String(bytes));
    }


    @Test
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClient_withHugeFileRequestBody() throws IOException {
        File randomFileOfFixedLength = new RandomTempFile(34 * KB);
        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromFile(randomFileOfFixedLength.toPath()));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3Https.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        byte[] bytes = Files.readAllBytes(randomFileOfFixedLength.toPath());
        assertThat(text).isEqualTo(new String(bytes));
    }

}
