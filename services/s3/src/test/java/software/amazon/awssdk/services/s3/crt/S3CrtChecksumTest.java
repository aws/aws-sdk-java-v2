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

package software.amazon.awssdk.services.s3.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.DefaultRetention;
import software.amazon.awssdk.services.s3.model.ObjectLockConfiguration;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.ObjectLockRule;
import software.amazon.awssdk.services.s3.model.PutObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@WireMockTest
@Timeout(10)
public class S3CrtChecksumTest {

    private S3CrtAsyncClientBuilder initializeAsync(WireMockRuntimeInfo wiremock,
                                                    RequestChecksumCalculation calculation) {
        return S3AsyncClient.crtBuilder()
                            .credentialsProvider(AnonymousCredentialsProvider.create())
                            .requestChecksumCalculation(calculation)
                            .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                            .forcePathStyle(true)
                            .region(Region.US_WEST_2);
    }

    private S3CrtAsyncClientBuilder initializeAsync(WireMockRuntimeInfo wiremock,
                                                    ResponseChecksumValidation responseChecksumValidation) {
        return S3AsyncClient.crtBuilder()
                            .credentialsProvider(AnonymousCredentialsProvider.create())
                            .responseChecksumValidation(responseChecksumValidation)
                            .endpointOverride(URI.create("http://localhost:" + wiremock.getHttpPort()))
                            .forcePathStyle(true)
                            .region(Region.US_WEST_2);
    }

    @BeforeEach
    public void setup() throws IOException {
        stubFor(put(anyUrl()).willReturn(WireMock.aResponse().withStatus(200)));

    }

    public static Stream<Arguments> streamingInputChecksumCalculationParams() {
        return Stream.of(Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, "x-amz-checksum-crc32",
                                      "requestChecksumWhenSupported_checksumAlgorithmNotProvided_shouldAddCrc32ChecksumTrailerByDefault"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, ChecksumAlgorithm.SHA1,
                                      "x-amz-checksum-sha1",
                                      "requestChecksumWhenSupported_checksumAlgorithmProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, null,
                                      "requestChecksumWhenRequired_checksumAlgorithmNotProvided_shouldNotAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.CRC32_C,
                                      "x-amz-checksum-crc32c",
                                      "requestChecksumWhenRequired_checksumAlgorithmProvided_shouldAddChecksumTrailer"));
    }

    public static Stream<Arguments> checksumInHeaderRequiredParams() {
        return Stream.of(Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, "x-amz-checksum-crc32", "Rs0ofQ==",
                                      "requestChecksumWhenSupported_checksumAlgorithmNotProvided_shouldAddCrc32ChecksumTrailerByDefault"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, ChecksumAlgorithm.SHA1,
                                      "x-amz-checksum-sha1", "4wnI4cDeFPttl6wSYksrgmk41qk=",
                                      "requestChecksumWhenSupported_checksumAlgorithmProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, "x-amz-checksum-crc32", "Rs0ofQ==",
                                      "requestChecksumWhenRequired_checksumAlgorithmNotProvided_shouldAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.CRC32_C,
                                      "x-amz-checksum-crc32c", "Zx3Wjw==",
                                      "requestChecksumWhenRequired_checksumAlgorithmProvided_shouldAddChecksum"));
    }


    @ParameterizedTest(name = "{index} {3}")
    @MethodSource("streamingInputChecksumCalculationParams")
    public void streamingInput_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                   ChecksumAlgorithm checksumAlgorithm,
                                                   String expectedTrailer,
                                                   String description,
                                                   WireMockRuntimeInfo wiremock) {

        try (S3AsyncClient client = initializeAsync(wiremock, requestChecksumCalculation).build()) {
            client.putObject(PutObjectRequest.builder()
                                             .bucket("bucket").key("key")
                                             .checksumAlgorithm(checksumAlgorithm)
                                             .build(),
                             AsyncRequestBody.fromString("Hello world")).join();

            validateChecksumTrailerHeader(expectedTrailer, wiremock);
        }
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("checksumInHeaderRequiredParams")
    public void checksumInHeaderRequired_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                             ChecksumAlgorithm checksumAlgorithm,
                                                             String expectedChecksumHeader,
                                                             String expectedChecksumValue,
                                                             String description,
                                                             WireMockRuntimeInfo wiremock) {

        try (S3AsyncClient client = initializeAsync(wiremock, requestChecksumCalculation).build()) {
            PutObjectLockConfigurationRequest request =
                PutObjectLockConfigurationRequest.builder()
                                                 .bucket("bucket")
                                                 .checksumAlgorithm(checksumAlgorithm)
                                                 .objectLockConfiguration(
                                                     ObjectLockConfiguration.builder()
                                                                            .objectLockEnabled(ObjectLockEnabled.ENABLED)
                                                                            .rule(ObjectLockRule.builder()
                                                                                                .defaultRetention(DefaultRetention.builder().mode(ObjectLockRetentionMode.COMPLIANCE).days(Integer.valueOf(1)).build())
                                                                                                .build())
                                                                            .build())
                                                 .build();

            client.putObjectLockConfiguration(request).join();
            validateChecksumHeader(expectedChecksumHeader, expectedChecksumValue);
        }
    }

    private static void validateChecksumHeader(String expectedChecksumHeader,
                                               String expectedChecksumValue) {
        verify(putRequestedFor(anyUrl()).withoutHeader(HttpChecksumConstant.X_AMZ_TRAILER));
        if (expectedChecksumHeader != null) {
            verify(putRequestedFor(anyUrl()).withHeader(expectedChecksumHeader, equalTo(expectedChecksumValue)));
            verify(putRequestedFor(anyUrl()).withHeader("x-amz-sdk-checksum-algorithm", new AnythingPattern()));
        } else {
            verify(putRequestedFor(anyUrl()).withoutHeader("x-amz-sdk-checksum-algorithm"));
        }
    }

    private static void validateChecksumTrailerHeader(String expectedTrailer,
                                                      WireMockRuntimeInfo wiremock) {


        if (expectedTrailer != null) {
            verify(putRequestedFor(anyUrl()).withHeader(HttpChecksumConstant.X_AMZ_TRAILER, equalTo(expectedTrailer)));
            verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", equalTo("STREAMING-UNSIGNED-PAYLOAD-TRAILER")));
            verify(putRequestedFor(anyUrl()).withHeader("x-amz-sdk-checksum-algorithm", new AnythingPattern()));
        } else {
            verify(putRequestedFor(anyUrl()).withoutHeader(HttpChecksumConstant.X_AMZ_TRAILER));
            verify(putRequestedFor(anyUrl()).withoutHeader("x-amz-sdk-checksum-algorithm"));
        }
    }
}