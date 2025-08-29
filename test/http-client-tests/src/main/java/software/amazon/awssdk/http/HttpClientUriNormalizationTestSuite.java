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
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class HttpClientUriNormalizationTestSuite {

    protected static SdkHttpClient httpClient;
    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @BeforeEach
    void prepare() {
        wireMockServer.stubFor(any(urlMatching(".*"))
                                   .willReturn(aResponse()
                                                   .withStatus(200)
                                                   .withBody("success")));
    }

    @AfterEach
    void reset() {
        wireMockServer.resetAll();
    }

    @AfterAll
    static void tearDown() {
        if (httpClient != null) {
            httpClient.close();
        }
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private static Stream<Arguments> uriTestCases() {
        return Stream.of(
            Arguments.of(
                "Encoded spaces",
                "/path/with%20spaces/file.txt",
                "%20"
            ),
            Arguments.of(
                "Encoded slashes",
                "/path/with%2Fslash/file.txt",
                "%2F"
            ),
            Arguments.of(
                "Encoded plus",
                "/path/with%2Bplus/file.txt",
                "%2B"
            ),
            Arguments.of(
                "Plus sign",
                "/path/with+plus/file.txt",
                "+"
            ),
            Arguments.of(
                "Encoded question mark",
                "/path/with%3Fquery/file.txt",
                "%3F"
            ),
            Arguments.of(
                "Encoded ampersand",
                "/path/with%26ampersand/file.txt",
                "%26"
            ),
            Arguments.of(
                "Encoded equals",
                "/path/with%3Dequals/file.txt",
                "%3D"
            ),
            Arguments.of(
                "AWS S3 style path",
                "/my-bucket/folder%2Fsubfolder/file%20name.txt",
                "%2F"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("uriTestCases")
    @DisplayName("Verify URI normalization is disabled (encoded characters are preserved)")
    void testUriNormalizationDisabled(String testName, String path, String encodedChar) throws Exception {
        httpClient = createSdkHttpClient();

        // Create and execute request
        HttpExecuteRequest request = createTestRequest(path);
        ExecutableHttpRequest executableRequest = httpClient.prepareRequest(request);
        HttpExecuteResponse response = executableRequest.call();

        // Verify response was successful
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);

        // Capture the actual request sent to server
        List<LoggedRequest> requests = wireMockServer.findAll(anyRequestedFor(anyUrl()));
        assertThat(requests).hasSize(1);

        String actualPathSent = requests.get(0).getUrl();
        assertThat(actualPathSent).contains(encodedChar);
    }

    private HttpExecuteRequest createTestRequest(String path) {
        String baseUrl = "http://localhost:" + wireMockServer.port();
        return HttpExecuteRequest.builder()
                                 .request(SdkHttpRequest.builder()
                                                        .method(SdkHttpMethod.GET)
                                                        .uri(URI.create(baseUrl + path))
                                                        .build())
                                 .build();
    }

    @ParameterizedTest
    @MethodSource("uriTestCases")
    @DisplayName("Test end-to-end execution flow with client context")
    void testExecuteFlowWithClientContext(String testName, String path, String encodedChar) throws Exception {
        httpClient = createSdkHttpClient();
        HttpExecuteRequest request = createTestRequest(path);
        HttpExecuteResponse response = httpClient.prepareRequest(request).call();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        List<LoggedRequest> requests = wireMockServer.findAll(anyRequestedFor(anyUrl()));
        assertThat(requests).hasSize(1);

        String actualUrl = requests.get(0).getUrl();
        assertThat(actualUrl).contains(encodedChar);
    }

    protected abstract SdkHttpClient createSdkHttpClient();
}