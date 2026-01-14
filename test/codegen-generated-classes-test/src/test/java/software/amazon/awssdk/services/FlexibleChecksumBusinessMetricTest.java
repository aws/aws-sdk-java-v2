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
            .matches(METRIC_SEARCH_PATTERN.apply("Z"))
            .matches(METRIC_SEARCH_PATTERN.apply("b"))
            .doesNotMatch(METRIC_SEARCH_PATTERN.apply("a"))
            .doesNotMatch(METRIC_SEARCH_PATTERN.apply("c"));
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
            Arguments.of(ChecksumAlgorithm.CRC32, "U"),
            Arguments.of(ChecksumAlgorithm.CRC32_C, "V"),
            Arguments.of(ChecksumAlgorithm.CRC64_NVME, "W"),
            Arguments.of(ChecksumAlgorithm.SHA1, "X"),
            Arguments.of(ChecksumAlgorithm.SHA256, "Y")
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
                         ResponseChecksumValidation.WHEN_SUPPORTED, "Z", "b"),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED,
                         ResponseChecksumValidation.WHEN_REQUIRED, "a", "c"),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED,
                         ResponseChecksumValidation.WHEN_SUPPORTED, "a", "b"),
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED,
                         ResponseChecksumValidation.WHEN_REQUIRED, "Z", "c")
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
                         ChecksumAlgorithm.CRC32, "Z", "b", "U"),
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.CRC32_C, "Z", "b", "V"),
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.SHA256, "Z", "b", "Y"),

            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.CRC32, "a", "c", "U"),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.CRC64_NVME, "a", "c", "W"),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.SHA1, "a", "c", "X"),

            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.CRC32_C, "a", "b", "V"),
            Arguments.of(RequestChecksumCalculation.WHEN_REQUIRED, 
                         ResponseChecksumValidation.WHEN_SUPPORTED, 
                         ChecksumAlgorithm.SHA256, "a", "b", "Y"),

            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.CRC64_NVME, "Z", "c", "W"),
            Arguments.of(RequestChecksumCalculation.WHEN_SUPPORTED, 
                         ResponseChecksumValidation.WHEN_REQUIRED, 
                         ChecksumAlgorithm.SHA1, "Z", "c", "X")
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
