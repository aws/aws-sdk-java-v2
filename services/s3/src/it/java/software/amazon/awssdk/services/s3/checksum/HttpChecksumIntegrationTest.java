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
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.authcrt.signer.internal.DefaultAwsCrtS3V4aSigner;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Transition;
import software.amazon.awssdk.services.s3.utils.CaptureChecksumValidationInterceptor;
import software.amazon.awssdk.testutils.RandomTempFile;

public class HttpChecksumIntegrationTest extends S3IntegrationTestBase {

    public static final int HUGE_MSG_SIZE = 1600 * KB;
    protected static final String KEY = "some-key";
    private static final String BUCKET = temporaryBucketName(HttpChecksumIntegrationTest.class);
    public static final String CHECKSUM_CRC32C_HEADER = "x-amz-checksum-crc32c";
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
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClient() {
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo(createDataOfSize(HUGE_MSG_SIZE, 'a'));
    }

    @Test
    public void syncUnsignedPayloadMultiPartForHugeMessage() {
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
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClient_withSmallFileRequestBody() throws IOException {
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
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
        assertThat(interceptor.validationAlgorithm()).isEqualTo(DefaultChecksumAlgorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        byte[] bytes = Files.readAllBytes(randomFileOfFixedLength.toPath());
        assertThat(text).isEqualTo(new String(bytes));
    }

    private S3Client createS3Client(RequestChecksumCalculation calculation) {
        S3ClientBuilder builder = s3ClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor));
        builder.requestChecksumCalculation(calculation);
        return builder.build();
    }

    private S3Client createS3Client(ResponseChecksumValidation responseChecksumValidation) {
        S3ClientBuilder builder = s3ClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor));
        builder.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
               .responseChecksumValidation(responseChecksumValidation);
        return builder.build();
    }

    @ParameterizedTest(name = "{index} {5}")
    @MethodSource("software.amazon.awssdk.services.s3.checksum.HttpChecksumTestUtils#getObjectChecksumValidationParams")
    public void getObject_checksumValidation(
        ResponseChecksumValidation responseChecksumValidation,
        ChecksumMode checksumMode,
        ChecksumAlgorithm checksumAlgorithm,
        boolean shouldValidateMd5Checksum,
        String expectedTrailerHeader,
        String description) {
        try (S3Client s3 = s3ClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                                            .responseChecksumValidation(responseChecksumValidation)
                                            .build()) {
            s3.putObject(PutObjectRequest.builder()
                                         .bucket(BUCKET)
                                         .checksumAlgorithm(checksumAlgorithm)
                                         .key(KEY)
                                         .build(), RequestBody.fromString("Hello world"));

            ResponseBytes<GetObjectResponse> responseBytes = s3.getObject(GetObjectRequest.builder()
                                                                                          .bucket(BUCKET)
                                                                                          .checksumMode(checksumMode)
                                                                                          .key(KEY)
                                                                                          .build(),
                                                                          ResponseTransformer.toBytes());

            if (shouldValidateMd5Checksum) {
                assertRequestAndResponseContainMd5Header();
                assertThat(responseBytes.response()
                                        .sdkHttpResponse()
                                        .anyMatchingHeader(k -> k.startsWith("x-amz-checksum"))).isFalse();
            } else {
                assertRequestAndResponseDoNotContainMd5Header();
                if (expectedTrailerHeader != null) {
                    assertThat(responseBytes.response()
                                            .sdkHttpResponse()
                                            .firstMatchingHeader(expectedTrailerHeader)).isNotNull();
                }
            }
        }
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("software.amazon.awssdk.services.s3.checksum.HttpChecksumTestUtils#putObjectChecksumCalculationParams")
    public void putObject_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                              ChecksumAlgorithm checksumAlgorithm,
                                              String checksumCrc32CValue,
                                              String expectedTrailer,
                                              String description) {
        try (S3Client s3 = createS3Client(requestChecksumCalculation)) {
            s3.putObject(PutObjectRequest.builder()
                                         .bucket(BUCKET)
                                         .checksumCRC32C(checksumCrc32CValue)
                                         .key(KEY)
                                         .checksumAlgorithm(checksumAlgorithm)
                                         .build(), RequestBody.fromString("Hello world"));

            assertThat(interceptor.requestChecksumInTrailer()).isEqualTo(expectedTrailer);
            if (checksumCrc32CValue != null) {

                assertThat(interceptor.requestHeaders().keySet()).filteredOn(h -> h.contains("x-amz-checksum-"))
                                                                 .containsExactly(CHECKSUM_CRC32C_HEADER);
                assertThat(interceptor.requestHeaders().get(CHECKSUM_CRC32C_HEADER)).containsExactly(checksumCrc32CValue);
            }
            interceptor.reset();
        }
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("software.amazon.awssdk.services.s3.checksum.HttpChecksumTestUtils#putObjectLifecycleChecksumCalculationParams")
    public void putBucketLifecycleChecksumInHeaderRequired_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                                               ChecksumAlgorithm checksumAlgorithm,
                                                                               String checksumCrc32CValue,
                                                                               String expectedHeader,
                                                                               String description) {
        try (S3Client s3 = createS3Client(requestChecksumCalculation)) {
            LifecycleRule lifecycleRule = LifecycleRule.builder()
                                                       .status(ExpirationStatus.ENABLED)
                                                       .expiration(e -> e.days(1))
                                                       .filter(f -> f.prefix("test"))
                                                       .build();
            PutBucketLifecycleConfigurationRequest.Builder builder =
                PutBucketLifecycleConfigurationRequest.builder()
                                                      .bucket(BUCKET)
                                                      .checksumAlgorithm(checksumAlgorithm)
                                                      .lifecycleConfiguration(l -> l.rules(lifecycleRule));
            if (checksumCrc32CValue != null) {
                builder.overrideConfiguration(o -> o.putHeader(CHECKSUM_CRC32C_HEADER,
                                                            checksumCrc32CValue));
            }
            s3.putBucketLifecycleConfiguration(builder.build());
            assertThat(interceptor.requestHeaders().keySet()).filteredOn(h -> h.contains("x-amz-checksum-"))
                                                             .containsExactly(expectedHeader);
            assertThat(interceptor.requestChecksumInTrailer()).isNull();
            if (checksumCrc32CValue != null) {
                assertThat(interceptor.requestHeaders().get(CHECKSUM_CRC32C_HEADER));
            }
        }
    }

    private void validateChecksumValidation(
        ResponseChecksumValidation responseChecksumValidation,
        ChecksumAlgorithm checksumAlgorithm,
        ChecksumMode checksumMode) {
        if (responseChecksumValidation == ResponseChecksumValidation.WHEN_SUPPORTED) {
            if (checksumMode == ChecksumMode.ENABLED) {
                assertChecksumModeEnabledWithChecksumValidationEnabled(checksumAlgorithm);
            } else {
                assertChecksumModeNotEnabledWithChecksumValidationEnabled();
            }
        } else {
            if (checksumMode == ChecksumMode.ENABLED) {
                assertChecksumModeEnabledWithChecksumValidationDisabled(checksumAlgorithm);
            } else {
                assertChecksumModeNotEnabledWithChecksumValidationDisabled();
            }
        }
    }

    private void assertChecksumModeEnabledWithChecksumValidationEnabled(ChecksumAlgorithm checksumAlgorithm) {
        if (checksumAlgorithm == null) {
            assertRequestAndResponseDoNotContainMd5Header();
            assertThat(interceptor.responseFlexibleChecksumHeader()).isNull();
        } else {
            assertRequestAndResponseDoNotContainMd5Header();
            assertThat(interceptor.responseFlexibleChecksumHeader()).isNotNull();
        }
    }


    private void assertChecksumModeNotEnabledWithChecksumValidationEnabled() {
        assertRequestAndResponseContainMd5Header();
        assertThat(interceptor.responseFlexibleChecksumHeader()).isNull();
    }

    private void assertChecksumModeEnabledWithChecksumValidationDisabled(ChecksumAlgorithm checksumAlgorithm) {
        if (checksumAlgorithm == null) {
            assertRequestAndResponseDoNotContainMd5Header();
            assertThat(interceptor.responseFlexibleChecksumHeader()).isNull();
        } else {
            assertRequestAndResponseDoNotContainMd5Header();
            assertThat(interceptor.responseFlexibleChecksumHeader()).isNotNull();
        }
    }

    private void assertChecksumModeNotEnabledWithChecksumValidationDisabled() {
        assertRequestAndResponseDoNotContainMd5Header();
        assertThat(interceptor.responseFlexibleChecksumHeader()).isNull();
    }

    private void assertRequestAndResponseContainMd5Header() {
        assertThat(interceptor.requestTransferEncodingHeader()).isEqualTo("append-md5");
        assertThat(interceptor.responseTransferEncodingHeader()).isEqualTo("append-md5");
    }

    private void assertRequestAndResponseDoNotContainMd5Header() {
        assertThat(interceptor.requestTransferEncodingHeader()).isNull();
        assertThat(interceptor.responseTransferEncodingHeader()).isNull();
    }
}
