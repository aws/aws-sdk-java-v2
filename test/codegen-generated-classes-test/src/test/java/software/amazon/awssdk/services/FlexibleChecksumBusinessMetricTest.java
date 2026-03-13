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
import static software.amazon.awssdk.core.useragent.BusinessMetricCollection.METRIC_SEARCH_PATTERN;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC32;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC32C;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC64;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA1;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA256;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA512;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH128;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH3;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH64;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED;
import static software.amazon.awssdk.core.useragent.BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Test class to verify that flexible checksum business metrics are correctly included
 * in the User-Agent header when checksum algorithms are used.
 */
class FlexibleChecksumBusinessMetricTest {
    private static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private static final StaticCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    private MockSyncHttpClient mockHttpClient;

    @BeforeEach
    public void setup() {
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockResponse());
    }

    @Test
    void when_noChecksumConfigurationIsSet_defaultConfigMetricsAreAdded() {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .region(Region.US_WEST_2)
                                                              .credentialsProvider(CREDENTIALS_PROVIDER)
                                                              .httpClient(mockHttpClient)
                                                              .build();

        client.allTypes(r -> {});
        String userAgent = getUserAgentFromLastRequest();

        assertThat(userAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value()))
            .matches(METRIC_SEARCH_PATTERN.apply(FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value()))
            .doesNotMatch(METRIC_SEARCH_PATTERN.apply(FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value()))
            .doesNotMatch(METRIC_SEARCH_PATTERN.apply(FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value()));
    }

    @ParameterizedTest
    @MethodSource("checksumAlgorithmTestCases")
    void when_checksumAlgorithmIsUsed_correctMetricIsAdded(ChecksumAlgorithm algorithm, String expectedMetric) {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .region(Region.US_WEST_2)
                                                              .credentialsProvider(CREDENTIALS_PROVIDER)
                                                              .httpClient(mockHttpClient)
                                                              .build();

        client.putOperationWithChecksum(r -> r.checksumAlgorithm(algorithm),
                                        RequestBody.fromString("test content"));

        String userAgent = getUserAgentFromLastRequest();
        assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply(expectedMetric));
    }

    static Stream<Arguments> checksumAlgorithmTestCases() {
        return Stream.of(
            Arguments.of(ChecksumAlgorithm.CRC32, FLEXIBLE_CHECKSUMS_REQ_CRC32.value()),
            Arguments.of(ChecksumAlgorithm.CRC32_C, FLEXIBLE_CHECKSUMS_REQ_CRC32C.value()),
            Arguments.of(ChecksumAlgorithm.CRC64_NVME, FLEXIBLE_CHECKSUMS_REQ_CRC64.value()),
            Arguments.of(ChecksumAlgorithm.SHA1, FLEXIBLE_CHECKSUMS_REQ_SHA1.value()),
            Arguments.of(ChecksumAlgorithm.SHA256, FLEXIBLE_CHECKSUMS_REQ_SHA256.value()),
            Arguments.of(ChecksumAlgorithm.SHA512, FLEXIBLE_CHECKSUMS_REQ_SHA512.value()),
            Arguments.of(ChecksumAlgorithm.XXHASH64, FLEXIBLE_CHECKSUMS_REQ_XXHASH64.value()),
            Arguments.of(ChecksumAlgorithm.XXHASH3, FLEXIBLE_CHECKSUMS_REQ_XXHASH3.value()),
            Arguments.of(ChecksumAlgorithm.XXHASH128, FLEXIBLE_CHECKSUMS_REQ_XXHASH128.value())
        );
    }

    @ParameterizedTest
    @MethodSource("checksumConfigurationTestCases")
    void when_checksumConfigurationIsSet_correctMetricIsAdded(RequestChecksumCalculation requestConfig,
                                                              ResponseChecksumValidation responseConfig,
                                                              String expectedRequestMetric,
                                                              String expectedResponseMetric) {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .region(Region.US_WEST_2)
                                                              .credentialsProvider(CREDENTIALS_PROVIDER)
                                                              .httpClient(mockHttpClient)
                                                              .requestChecksumCalculation(requestConfig)
                                                              .responseChecksumValidation(responseConfig)
                                                              .build();

        client.allTypes(r -> {});

        String userAgent = getUserAgentFromLastRequest();
        assertThat(userAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(expectedRequestMetric))
            .matches(METRIC_SEARCH_PATTERN.apply(expectedResponseMetric));
    }

    static Stream<Arguments> checksumConfigurationTestCases() {
        return Stream.of(
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED,
                         ResponseChecksumValidation.WHEN_SUPPORTED,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED,
                         ResponseChecksumValidation.WHEN_REQUIRED,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED,
                         ResponseChecksumValidation.WHEN_SUPPORTED,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED,
                         ResponseChecksumValidation.WHEN_REQUIRED,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value())
        );
    }

    @ParameterizedTest
    @MethodSource("checksumConfigurationWithAlgorithmTestCases")
    void when_checksumConfigurationAndAlgorithmAreSet_correctMetricsAreAdded(
        RequestChecksumCalculation requestConfig,
        ResponseChecksumValidation responseConfig,
        ChecksumAlgorithm algorithm,
        String expectedRequestMetric,
        String expectedResponseMetric,
        String expectedAlgorithmMetric) {
        
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .region(Region.US_WEST_2)
                                                              .credentialsProvider(CREDENTIALS_PROVIDER)
                                                              .httpClient(mockHttpClient)
                                                              .requestChecksumCalculation(requestConfig)
                                                              .responseChecksumValidation(responseConfig)
                                                              .build();

        client.putOperationWithChecksum(r -> r.checksumAlgorithm(algorithm),
                                        RequestBody.fromString("test content"));

        String userAgent = getUserAgentFromLastRequest();

        assertThat(userAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(expectedRequestMetric))
            .matches(METRIC_SEARCH_PATTERN.apply(expectedResponseMetric))
            .matches(METRIC_SEARCH_PATTERN.apply(expectedAlgorithmMetric));
    }

    static Stream<Arguments> checksumConfigurationWithAlgorithmTestCases() {
        return Stream.of(
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.CRC32,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_CRC32.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.CRC32_C,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_CRC32C.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.SHA256,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_SHA256.value()),

            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.CRC32,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_CRC32.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.CRC64_NVME,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_CRC64.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.SHA1,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_SHA1.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.SHA512,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_SHA512.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.XXHASH64,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_XXHASH64.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.XXHASH3,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_XXHASH3.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.XXHASH128,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_XXHASH128.value()),

            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.CRC32_C,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_CRC32C.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.SHA256,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_SHA256.value()),

            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.CRC64_NVME,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_CRC64.value()),
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.SHA1,
                         FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value(),
                         FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value(),
                         FLEXIBLE_CHECKSUMS_REQ_SHA1.value())
        );
    }

    private String getUserAgentFromLastRequest() {
        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get(USER_AGENT_HEADER_NAME);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        return userAgentHeaders.get(0);
    }

    private static HttpExecuteResponse mockResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                  .build();
    }
}
