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

package software.amazon.awssdk.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Abstract test suite for testing URI sanitization functionality across different HTTP client implementations.
 * Verifies that consecutive slashes in URIs are properly encoded.
 */
@WireMockTest
public abstract class SdkHttpClientUriSanitizationTestSuite {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort())
                                                         .build();

    private SdkHttpClient client;

    protected abstract SdkHttpClient createHttpClient();

    @BeforeEach
    void setUp() {
        client = createHttpClient();
        // Generic stub for all requests
        wireMock.stubFor(any(anyUrl())
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody("success")));
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @ParameterizedTest(name = "URI path: ''{0}'' should become ''{1}''")
    @MethodSource("provideUriSanitizationTestCases")
    @DisplayName("URI paths should be properly sanitized")
    void uriPathsShouldBeProperlySanitized(String inputPath, String expectedPath) throws Exception {
        SdkHttpFullRequest request = createRequestWithPath(inputPath);
        HttpExecuteResponse response = executeRequest(request);

        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        wireMock.verify(getRequestedFor(urlPathEqualTo(expectedPath)));
    }

    private static Stream<Arguments> provideUriSanitizationTestCases() {
        return Stream.of(
            // Normal paths should remain unchanged
            Arguments.of("/normal/path", "/normal/path"),
            Arguments.of("/api/v1/users/123", "/api/v1/users/123"),
            Arguments.of("/single/slash/only", "/single/slash/only"),

            // Consecutive slashes should be encoded
            Arguments.of("/path//to//resource", "/path/%2Fto/%2Fresource"),
            Arguments.of("/folder//file.txt", "/folder/%2Ffile.txt"),
            Arguments.of("//leading//double", "/%2Fleading/%2Fdouble"),
            Arguments.of("/trailing//", "/trailing/%2F"),
            Arguments.of("/multiple///slashes", "/multiple/%2F/slashes"),
            Arguments.of("/four////slashes", "/four/%2F/%2Fslashes"),

            // Edge cases
            Arguments.of("//", "/%2F"),
            Arguments.of("///", "/%2F/"),
            Arguments.of("////", "/%2F/%2F")
        );
    }

    private SdkHttpFullRequest createRequestWithPath(String path) {
        URI uri = URI.create("http://localhost:" + wireMock.getPort() + path);

        return SdkHttpFullRequest.builder()
                                 .uri(uri)
                                 .method(SdkHttpMethod.GET)
                                 .putHeader("Host", uri.getHost() + ":" + uri.getPort())
                                 .build();
    }

    private HttpExecuteResponse executeRequest(SdkHttpFullRequest request) throws Exception {
        return client.prepareRequest(
                         HttpExecuteRequest.builder()
                                           .request(request)
                                           .build())
                     .call();
    }
}