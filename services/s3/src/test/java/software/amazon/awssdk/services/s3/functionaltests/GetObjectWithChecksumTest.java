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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.utils.CaptureChecksumValidationInterceptor;

@WireMockTest
public class GetObjectWithChecksumTest {
    public static final Function<InputStream, String> stringFromStream = inputStream ->
        new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    private static final String EXAMPLE_BUCKET = "Example-Bucket";
    private static final String EXAMPLE_RESPONSE_BODY = "Hello world";
    private static final CaptureChecksumValidationInterceptor captureChecksumValidationInterceptor =
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

    @Test
    public void async_getObject_with_correct_http_checksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "i9aeUg==")

                                                    .withBody(EXAMPLE_RESPONSE_BODY)));

        S3AsyncClient s3Client =
            getAsyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();
        String response = s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key").checksumMode(ChecksumMode.ENABLED),
                                             AsyncResponseTransformer.toBytes()).join().asUtf8String();

        assertThat(response).isEqualTo("Hello world");
        assertThat(captureChecksumValidationInterceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
    }


    @Test
    public void async_getObject_with_validation_enabled_no_http_checksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withBody(EXAMPLE_RESPONSE_BODY)));

        S3AsyncClient s3Client =
            getAsyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();
        String response = s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key").checksumMode(ChecksumMode.ENABLED),
                                             AsyncResponseTransformer.toBytes()).join().asUtf8String();

        assertThat(response).isEqualTo("Hello world");
        assertThat(captureChecksumValidationInterceptor.responseValidation()).isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
    }

    @Test
    public void async_getObject_with_validation_not_enabled_incorrect_http_checksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "incorrect")
                                                    .withBody(EXAMPLE_RESPONSE_BODY)));

        S3AsyncClient s3Client =
            getAsyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();
        String response = s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key"),
                                             AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(response).isEqualTo("Hello world");
        assertThat(captureChecksumValidationInterceptor.responseValidation()).isNull();
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
    }

    @Test
    public void async_getObject_with_customized_multipart_checksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "abcdef==-12")
                                                    .withBody(EXAMPLE_RESPONSE_BODY)));

        S3AsyncClient s3Client =
            getAsyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();
        String response = s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key").checksumMode(ChecksumMode.ENABLED),
                                             AsyncResponseTransformer.toBytes()).join().asUtf8String();

        assertThat(response).isEqualTo("Hello world");

        assertThat(captureChecksumValidationInterceptor.responseValidation()).isEqualTo(ChecksumValidation.FORCE_SKIP);
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
    }

    @Test
    public void sync_getObject_with_correct_http_checksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "i9aeUg==")

                                                    .withBody(EXAMPLE_RESPONSE_BODY)));

        S3Client s3Client =
            getSyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();

        ResponseInputStream<GetObjectResponse> getObject =
            s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key").checksumMode(ChecksumMode.ENABLED));

        assertThat(stringFromStream.apply(getObject)).isEqualTo("Hello world");
        assertThat(captureChecksumValidationInterceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
    }


    @Test
    public void sync_getObject_with_validation_enabled_no_http_checksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withBody(EXAMPLE_RESPONSE_BODY)));

        S3Client s3Client =
            getSyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();

        ResponseInputStream<GetObjectResponse> getObject =
            s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key").checksumMode(ChecksumMode.ENABLED));

        assertThat(stringFromStream.apply(getObject)).isEqualTo("Hello world");
        assertThat(captureChecksumValidationInterceptor.responseValidation()).isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
    }

    @Test
    public void sync_getObject_with_validation_not_enabled_incorrect_http_checksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "incorrect")
                                                    .withBody(EXAMPLE_RESPONSE_BODY)));

        S3Client s3Client =
            getSyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();

        ResponseInputStream<GetObjectResponse> getObject =
            s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key"));

        assertThat(stringFromStream.apply(getObject)).isEqualTo("Hello world");
        assertThat(captureChecksumValidationInterceptor.responseValidation()).isNull();
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
    }

    @Test
    public void sync_getObject_with_customized_multipart_checksum(WireMockRuntimeInfo wm) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("x-amz-checksum-crc32", "i9aeUg==-12")
                                                    .withBody(EXAMPLE_RESPONSE_BODY)));

        S3Client s3Client =
            getSyncClientBuilder(wm).overrideConfiguration(o -> o.addExecutionInterceptor(captureChecksumValidationInterceptor)).build();

        ResponseInputStream<GetObjectResponse> getObject =
            s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key").checksumMode(ChecksumMode.ENABLED));

        assertThat(stringFromStream.apply(getObject)).isEqualTo("Hello world");
        assertThat(captureChecksumValidationInterceptor.responseValidation()).isEqualTo(ChecksumValidation.FORCE_SKIP);
        assertThat(captureChecksumValidationInterceptor.validationAlgorithm()).isNull();
    }

}
