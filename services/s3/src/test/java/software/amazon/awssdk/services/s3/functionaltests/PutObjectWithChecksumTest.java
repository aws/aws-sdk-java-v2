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

package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.utils.CaptureChecksumValidationInterceptor;

@WireMockTest
public class PutObjectWithChecksumTest {

    String CONTENT = "Hello, World!";
    String INCORRECT_ETAG = "65A8E27D8879283831B664BD8B7F0AD5";
    String ETAG = "65A8E27D8879283831B664BD8B7F0AD4";
    public static final Function<InputStream, String> stringFromStream = inputStream ->
        new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    private static final String BUCKET = "Example-Bucket";
    private static final String EXAMPLE_RESPONSE_BODY = "Hello world";
    private final CaptureChecksumValidationInterceptor captureChecksumValidationInterceptor =
        new CaptureChecksumValidationInterceptor();

    private S3ClientBuilder getSyncClientBuilder(WireMockRuntimeInfo wm) {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                       .credentialsProvider(
                           StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    @AfterEach
    public void reset() {
        captureChecksumValidationInterceptor.reset();
    }

    private S3AsyncClientBuilder getAsyncClientBuilder(WireMockRuntimeInfo wm) {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                            .credentialsProvider(
                                StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    // Exception is thrown means the default Md5 validation has taken place.
    @Test
    void sync_putObject_default_MD5_validation_withIncorrectChecksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "i9aeUg==")
                                                    .withHeader("etag", INCORRECT_ETAG)));
        S3Client s3Client =
            getSyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(BUCKET).key("KEY").build();
        RequestBody requestBody = RequestBody.fromBytes(CONTENT.getBytes());

        assertThatExceptionOfType(RetryableException.class)
           .isThrownBy(() -> s3Client.putObject(putObjectRequest, requestBody))
           .withMessage("Data read has a different checksum than expected. Was 0x" + ETAG.toLowerCase()  + ", but expected 0x"
                        + INCORRECT_ETAG.toLowerCase() + ". This commonly means that the data was corrupted between the client "
                        + "and service. Note: Despite this error, the upload still completed and was persisted in S3.");
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
    }

    @Test
    void sync_putObject_default_MD5_validation_withCorrectChecksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "WRONG_CHECKSUM")
                                                    .withHeader("etag", ETAG)));
        S3Client s3Client =
            getSyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor))
                                    .build();
        PutObjectResponse putObjectResponse = s3Client.putObject(b -> b.bucket(BUCKET)
                                                                       .key("KEY"),
                                                                 RequestBody.fromBytes(CONTENT.getBytes()));
        assertThat(putObjectResponse.eTag()).isEqualTo(ETAG);
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();

    }

    // Exception is thrown means the default Md5 validation has taken place.
    @Test
    void async_putObject_default_MD5_validation_withIncorrectChecksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "i9aeUg==")
                                                    .withHeader("etag", INCORRECT_ETAG)));
        S3Client s3Client =
            getSyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(BUCKET).key("KEY").build();
        RequestBody requestBody = RequestBody.fromBytes(CONTENT.getBytes());

        assertThatExceptionOfType(RetryableException.class)
            .isThrownBy(() -> s3Client.putObject(putObjectRequest, requestBody))
            .withMessage("Data read has a different checksum than expected. Was 0x" + ETAG.toLowerCase()  + ", but expected 0x"
                         + INCORRECT_ETAG.toLowerCase() + ". This commonly means that the data was corrupted between the client "
                         + "and service. Note: Despite this error, the upload still completed and was persisted in S3.");
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();

    }

    @Test
    void async_putObject_default_MD5_validation_withCorrectChecksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "WRONG_CHECKSUM")
                                                    .withHeader("etag", ETAG)));
        S3AsyncClient s3Async =
            getAsyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();
        PutObjectResponse putObjectResponse = s3Async.putObject(PutObjectRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .key("KEY")
                                                                                .build(), AsyncRequestBody.fromString(CONTENT)).join();

        assertThat(putObjectResponse.eTag()).isEqualTo(ETAG);
        assertThat(captureChecksumValidationInterceptor.requestChecksumInTrailer()).isNull();
    }

    // Even with incorrect  Etag, exception is not thrown because default check is skipped when checksumAlgorithm is set
    @Test
    void sync_putObject_httpChecksumValidation_withIncorrectChecksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "7ErD0A==")
                                                    .withHeader("etag", INCORRECT_ETAG)));
        S3Client s3Client =
            getSyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();

        s3Client.putObject(b -> b.bucket(BUCKET).key("KEY").checksumAlgorithm(ChecksumAlgorithm.CRC32),
                           RequestBody.fromBytes(CONTENT.getBytes()));
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
        assertThat(captureChecksumValidationInterceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");

        verify(putRequestedFor(anyUrl()).withRequestBody(containing("Hello, World!"))
                                        .withRequestBody(containing("x-amz-checksum-crc32:7ErD0A=="))
                                        .withRequestBody(containing("0;")));
    }

    // Even with incorrect  Etag, exception is not thrown because default check is skipped when checksumAlgorithm is set
    @Test
    void async_putObject_httpChecksumValidation_withIncorrectChecksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "7ErD0A==")
                                                    .withHeader("etag", INCORRECT_ETAG)));
        S3AsyncClient s3Async =
            getAsyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();

        s3Async.putObject(PutObjectRequest.builder().bucket(BUCKET).checksumAlgorithm(ChecksumAlgorithm.CRC32).key("KEY").build(),
                          AsyncRequestBody.fromString(CONTENT)).join();

        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
        assertThat(captureChecksumValidationInterceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");

        verify(putRequestedFor(anyUrl()).withRequestBody(containing("x-amz-checksum-crc32:7ErD0A=="))
                                                             .withRequestBody(containing("Hello, World!")));
    }
}
