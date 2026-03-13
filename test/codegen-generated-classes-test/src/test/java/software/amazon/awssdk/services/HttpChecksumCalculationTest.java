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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.HttpChecksumConstant.HTTP_CHECKSUM_HEADER_PREFIX;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.protocolrestjson.model.OperationWithCustomRequestChecksumRequest;
import software.amazon.awssdk.services.protocolrestjson.model.PutOperationWithChecksumRequest;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * Verify HTTP checksum calculation behavior with different requestChecksumCalculation settings.
 */
public class HttpChecksumCalculationTest {
    private MockSyncHttpClient httpClient;
    private MockAsyncHttpClient httpAsyncClient;

    private ProtocolRestJsonClientBuilder initializeSync(RequestChecksumCalculation calculation) {
        return ProtocolRestJsonClient.builder().httpClient(httpClient)
                                     .credentialsProvider(AnonymousCredentialsProvider.create())
                                     .requestChecksumCalculation(calculation)
                                     .region(Region.US_WEST_2);
    }

    private ProtocolRestJsonAsyncClientBuilder initializeAsync(RequestChecksumCalculation calculation) {
        return ProtocolRestJsonAsyncClient.builder().httpClient(httpAsyncClient)
                                          .credentialsProvider(AnonymousCredentialsProvider.create())
                                          .requestChecksumCalculation(calculation)
                                          .region(Region.US_WEST_2);
    }

    @BeforeEach
    public void setup() {
        httpClient = new MockSyncHttpClient();
        httpClient.stubNextResponse200();

        httpAsyncClient = new MockAsyncHttpClient();
        httpAsyncClient.stubNextResponse200();
    }

    // Expected base64 checksum values of "Hello world" for each algorithm
    private static final Map<ChecksumAlgorithm, String> HELLO_WORLD_CHECKSUMS;

    // Expected base64 checksum values of empty JSON body "{}" for each algorithm
    private static final Map<ChecksumAlgorithm, String> EMPTY_BODY_CHECKSUMS;

    static {
        Map<ChecksumAlgorithm, String> helloWorld = new HashMap<>();
        helloWorld.put(ChecksumAlgorithm.CRC32, "i9aeUg==");
        helloWorld.put(ChecksumAlgorithm.CRC32_C, "crUfeA==");
        helloWorld.put(ChecksumAlgorithm.SHA1, "e1AsOh9IyGCa4hLN+2Od7jlnP14=");
        helloWorld.put(ChecksumAlgorithm.SHA256, "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=");
        helloWorld.put(ChecksumAlgorithm.XXHASH3, "tqy52Eo4/3Q=");
        helloWorld.put(ChecksumAlgorithm.XXHASH64, "xQCwyRKzdtg=");
        helloWorld.put(ChecksumAlgorithm.XXHASH128, "c1H4mBL5c4K5HQWzHgTdfw==");
        HELLO_WORLD_CHECKSUMS = Collections.unmodifiableMap(helloWorld);

        Map<ChecksumAlgorithm, String> emptyBody = new HashMap<>();
        emptyBody.put(ChecksumAlgorithm.CRC32, "o6a/Qw==");
        emptyBody.put(ChecksumAlgorithm.CRC32_C, "KXvQqg==");
        emptyBody.put(ChecksumAlgorithm.SHA1, "vyGp6PvFo4RvsFtPoIWeCReyIC8=");
        emptyBody.put(ChecksumAlgorithm.SHA256, "RBNvo1WzZ4oRRq0W9+hknpT7T8If536DEMBg9hyq/4o=");
        emptyBody.put(ChecksumAlgorithm.XXHASH3, "E0nN4SdwXBY=");
        emptyBody.put(ChecksumAlgorithm.XXHASH64, "LhRytXrylNE=");
        emptyBody.put(ChecksumAlgorithm.XXHASH128, "3HBI+Ph0f1YTSc3hJ3BcFg==");
        EMPTY_BODY_CHECKSUMS = Collections.unmodifiableMap(emptyBody);
    }

    public static Stream<Arguments> streamingInputChecksumCalculationParams() {
        return Stream.of(Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, "x-amz-checksum-crc32",
                                      HELLO_WORLD_CHECKSUMS.get(ChecksumAlgorithm.CRC32),
                                      "requestChecksumWhenSupported_checksumAlgorithmNotProvided_shouldAddCrc32ChecksumTrailerByDefault"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, ChecksumAlgorithm.SHA1,
                                      "x-amz-checksum-sha1",
                                      HELLO_WORLD_CHECKSUMS.get(ChecksumAlgorithm.SHA1),
                                      "requestChecksumWhenSupported_checksumAlgorithmProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, null,
                                      null,
                                      "requestChecksumWhenRequired_checksumAlgorithmNotProvided_shouldNotAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.CRC32_C,
                                      "x-amz-checksum-crc32c",
                                      HELLO_WORLD_CHECKSUMS.get(ChecksumAlgorithm.CRC32_C),
                                      "requestChecksumWhenRequired_checksumAlgorithmProvided_shouldAddChecksumTrailer"),
                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.XXHASH3,
                                      "x-amz-checksum-xxhash3",
                                      HELLO_WORLD_CHECKSUMS.get(ChecksumAlgorithm.XXHASH3),
                                      "requestChecksumWhenRequired_xxHash3_shouldAddChecksumTrailer"),
                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.XXHASH64,
                                      "x-amz-checksum-xxhash64",
                                      HELLO_WORLD_CHECKSUMS.get(ChecksumAlgorithm.XXHASH64),
                                      "requestChecksumWhenRequired_xxHash64_shouldAddChecksumTrailer"),
                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.XXHASH128,
                                      "x-amz-checksum-xxhash128",
                                      HELLO_WORLD_CHECKSUMS.get(ChecksumAlgorithm.XXHASH128),
                                      "requestChecksumWhenRequired_xxHash128_shouldAddChecksumTrailer"),
                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.SHA256,
                                      "x-amz-checksum-sha256",
                                      HELLO_WORLD_CHECKSUMS.get(ChecksumAlgorithm.SHA256),
                                      "requestChecksumWhenRequired_sha256_shouldAddChecksumTrailer"));
    }

    public static Stream<Arguments> checksumInHeaderRequiredParams() {
        return Stream.of(Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, null, "x-amz-checksum-crc32",
                                      EMPTY_BODY_CHECKSUMS.get(ChecksumAlgorithm.CRC32),
                                      "requestChecksumWhenSupported_checksumAlgorithmNotProvided_shouldAddCrc32ChecksumTrailerByDefault"),

                         Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, ChecksumAlgorithm.SHA1,
                                      "x-amz-checksum-sha1",
                                      EMPTY_BODY_CHECKSUMS.get(ChecksumAlgorithm.SHA1),
                                      "requestChecksumWhenSupported_checksumAlgorithmProvided_shouldHonor"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, null, "x-amz-checksum-crc32",
                                      EMPTY_BODY_CHECKSUMS.get(ChecksumAlgorithm.CRC32),
                                      "requestChecksumWhenRequired_checksumAlgorithmNotProvided_shouldAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.CRC32_C,
                                      "x-amz-checksum-crc32c",
                                      EMPTY_BODY_CHECKSUMS.get(ChecksumAlgorithm.CRC32_C),
                                      "requestChecksumWhenRequired_checksumAlgorithmProvided_shouldAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.SHA256,
                                      "x-amz-checksum-sha256",
                                      EMPTY_BODY_CHECKSUMS.get(ChecksumAlgorithm.SHA256),
                                      "requestChecksumWhenRequired_sha256_shouldAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.XXHASH3,
                                      "x-amz-checksum-xxhash3",
                                      EMPTY_BODY_CHECKSUMS.get(ChecksumAlgorithm.XXHASH3),
                                      "requestChecksumWhenRequired_xxHash3_shouldAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.XXHASH64,
                                      "x-amz-checksum-xxhash64",
                                      EMPTY_BODY_CHECKSUMS.get(ChecksumAlgorithm.XXHASH64),
                                      "requestChecksumWhenRequired_xxHash64_shouldAddChecksum"),

                         Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, ChecksumAlgorithm.XXHASH128,
                                      "x-amz-checksum-xxhash128",
                                      EMPTY_BODY_CHECKSUMS.get(ChecksumAlgorithm.XXHASH128),
                                      "requestChecksumWhenRequired_xxHash128_shouldAddChecksum"));
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("streamingInputChecksumCalculationParams")
    public void syncStreamingInput_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                       ChecksumAlgorithm checksumAlgorithm,
                                                       String expectedTrailer,
                                                       String expectedChecksumValue,
                                                       String description) {

        try (ProtocolRestJsonClient client = initializeSync(requestChecksumCalculation).build()) {
            client.putOperationWithChecksum(PutOperationWithChecksumRequest.builder()
                                                                           .checksumAlgorithm(checksumAlgorithm)
                                                                           .build(),
                                            RequestBody.fromString("Hello world"));

            validateChecksumTrailerHeader(expectedTrailer, httpClient.getLastRequest());
            if (expectedTrailer != null) {
                SdkHttpFullRequest lastRequest = (SdkHttpFullRequest) httpClient.getLastRequest();
                String body = new String(SdkBytes.fromInputStream(
                    lastRequest.contentStreamProvider().get().newStream()).asByteArray(), StandardCharsets.UTF_8);
                assertThat(body).isEqualTo("b\r\nHello world\r\n0\r\n"
                                           + expectedTrailer + ":" + expectedChecksumValue + "\r\n\r\n");
            }
        }
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("streamingInputChecksumCalculationParams")
    public void asyncStreamingInput_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                        ChecksumAlgorithm checksumAlgorithm,
                                                        String expectedTrailer,
                                                        String expectedChecksumValue,
                                                        String description) {

        try (ProtocolRestJsonAsyncClient client = initializeAsync(requestChecksumCalculation).build()) {
            client.putOperationWithChecksum(PutOperationWithChecksumRequest.builder()
                                                                           .checksumAlgorithm(checksumAlgorithm)
                                                                           .build(),
                                            AsyncRequestBody.fromString("Hello world")).join();

            validateChecksumTrailerHeader(expectedTrailer, httpAsyncClient.getLastRequest());
            if (expectedTrailer != null) {
                String payload = new String(httpAsyncClient.getStreamingPayload().get(), StandardCharsets.UTF_8);
                assertThat(payload).isEqualTo("b\r\nHello world\r\n0\r\n"
                                              + expectedTrailer + ":" + expectedChecksumValue + "\r\n\r\n");
            }
        }
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("checksumInHeaderRequiredParams")
    public void syncChecksumInHeaderRequired_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                                 ChecksumAlgorithm checksumAlgorithm,
                                                                 String expectedChecksumHeader,
                                                                 String expectedChecksumValue,
                                                                 String description) {

        try (ProtocolRestJsonClient client = initializeSync(requestChecksumCalculation).build()) {
            client.operationWithCustomRequestChecksum(OperationWithCustomRequestChecksumRequest.builder()
                                                                                               .checksumAlgorithm(checksumAlgorithm)
                                                                                               .build());
            validateChecksumHeader(expectedChecksumHeader, expectedChecksumValue, httpClient.getLastRequest());
        }
    }

    @ParameterizedTest(name = "{index} {4}")
    @MethodSource("checksumInHeaderRequiredParams")
    public void asyncChecksumInHeaderRequired_checksumCalculation(RequestChecksumCalculation requestChecksumCalculation,
                                                                  ChecksumAlgorithm checksumAlgorithm,
                                                                  String expectedChecksumHeader,
                                                                  String expectedChecksumValue,
                                                                  String description) {

        try (ProtocolRestJsonAsyncClient client = initializeAsync(requestChecksumCalculation).build()) {
            client.operationWithCustomRequestChecksum(OperationWithCustomRequestChecksumRequest.builder()
                                                                                               .checksumAlgorithm(checksumAlgorithm)
                                                                                               .build()).join();
            validateChecksumHeader(expectedChecksumHeader, expectedChecksumValue, httpAsyncClient.getLastRequest());
        }
    }

    private static void validateChecksumHeader(String expectedChecksumHeader,
                                               String expectedChecksumValue,
                                               SdkHttpRequest request) {
        assertThat(request.firstMatchingHeader(HttpChecksumConstant.X_AMZ_TRAILER)).isEmpty();
        List<String> checksumHeaders = request.headers()
                                              .keySet()
                                              .stream()
                                              .filter(header -> header.contains(HTTP_CHECKSUM_HEADER_PREFIX) && !header.equals(
                                                  "x-amz-checksum-algorithm"))
                                              .collect(Collectors.toList());

        if (expectedChecksumHeader != null) {
            assertThat(checksumHeaders).containsExactly(expectedChecksumHeader);
            assertThat(request.firstMatchingHeader(expectedChecksumHeader)).contains(expectedChecksumValue);
            assertThat(request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isNotEmpty();
        } else {
            assertThat(checksumHeaders).isEmpty();
            assertThat(request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
        }
    }

    private static void validateChecksumTrailerHeader(String expectedTrailer, SdkHttpRequest request) {
        if (expectedTrailer != null) {
            assertThat(request.firstMatchingHeader(HttpChecksumConstant.X_AMZ_TRAILER)).contains(expectedTrailer);
            assertThat(request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isNotEmpty();
            assertThat(request.firstMatchingHeader("x-amz-content-sha256")).contains("STREAMING-UNSIGNED-PAYLOAD-TRAILER");
        } else {
            assertThat(request.firstMatchingHeader(HttpChecksumConstant.X_AMZ_TRAILER)).isEmpty();
            assertThat(request.firstMatchingHeader("x-amz-sdk-checksum-algorithm")).isEmpty();
        }
    }

    @Test
    public void syncMd5ChecksumCalculation_shouldThrowException() {
        try (ProtocolRestJsonClient client = initializeSync(RequestChecksumCalculation.WHEN_SUPPORTED).build()) {
            assertThatThrownBy(() -> client.putOperationWithChecksum(
                PutOperationWithChecksumRequest.builder()
                                               .checksumAlgorithm(ChecksumAlgorithm.MD5)
                                               .build(),
                RequestBody.fromString("Hello world")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MD5 is not supported");
        }
    }

    @Test
    public void asyncMd5ChecksumCalculation_shouldThrowException() {
        try (ProtocolRestJsonAsyncClient client = initializeAsync(RequestChecksumCalculation.WHEN_SUPPORTED).build()) {
            assertThatThrownBy(() -> client.putOperationWithChecksum(
                PutOperationWithChecksumRequest.builder()
                                               .checksumAlgorithm(ChecksumAlgorithm.MD5)
                                               .build(),
                AsyncRequestBody.fromString("Hello world")).join())
                .hasCauseInstanceOf(SdkException.class)
                .hasMessageContaining("MD5 is not supported");
        }
    }

    @Test
    public void syncMd5ChecksumCalculation_userProvidedMd5Header_shouldSucceed() {
        try (ProtocolRestJsonClient client = initializeSync(RequestChecksumCalculation.WHEN_SUPPORTED).build()) {
            client.putOperationWithChecksum(r -> r.overrideConfiguration(o -> o.putHeader("x-amz-checksum-md5", "PiWWCnnbxptnTNTsZ6csYg==")),
                                           RequestBody.fromString("Hello world"));
            assertMd5HeaderPresent(httpClient.getLastRequest());
        }
    }

    @Test
    public void asyncMd5ChecksumCalculation_userProvidedMd5Header_shouldSucceed() {
        try (ProtocolRestJsonAsyncClient client = initializeAsync(RequestChecksumCalculation.WHEN_SUPPORTED).build()) {
            client.putOperationWithChecksum(r -> r.overrideConfiguration(o -> o.putHeader("x-amz-checksum-md5", "PiWWCnnbxptnTNTsZ6csYg==")),
                                           AsyncRequestBody.fromString("Hello world")).join();
            assertMd5HeaderPresent(httpAsyncClient.getLastRequest());
        }
    }

    private void assertMd5HeaderPresent(SdkHttpRequest request) {
        assertThat(request.firstMatchingHeader("x-amz-checksum-md5"))
            .isPresent()
            .hasValue("PiWWCnnbxptnTNTsZ6csYg==");
    }
}
