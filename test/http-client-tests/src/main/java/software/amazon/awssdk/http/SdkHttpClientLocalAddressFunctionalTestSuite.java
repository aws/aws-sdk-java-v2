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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Abstract test suite for testing local address functionality across different HTTP client implementations.
 * Subclasses must implement the {@link #createHttpClient(InetAddress, Duration)} method to provide
 * their specific client implementation.
 */
@WireMockTest
public abstract class SdkHttpClientLocalAddressFunctionalTestSuite {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort())
                                                         .build();

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(2);
    private static final String TEST_BODY = "test body";
    private static final String SUCCESS_RESPONSE = "success";

    private SdkHttpClient client;

    /**
     * Creates an HTTP client with the specified local address configuration.
     *
     * @param localAddress the local address to bind to, or null for default behavior
     * @param connectionTimeout the connection timeout
     * @return the configured HTTP client
     */
    protected abstract SdkHttpClient createHttpClient(InetAddress localAddress, Duration connectionTimeout);

    @BeforeEach
    void setUp() {
        wireMock.stubFor(any(urlPathEqualTo("/"))
                             .willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody(SUCCESS_RESPONSE)));
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @ParameterizedTest(name = "Invalid local address {0} should fail with BindException")
    @ValueSource(strings = {
        "192.0.2.1",      // TEST-NET-1 reserved range
        "198.51.100.1",   // TEST-NET-2
        "203.0.113.1"     // TEST-NET-3
    })
    @DisplayName("Invalid local addresses should fail with BindException")
    void invalidLocalAddressesShouldFailWithBindexception(String invalidIpAddress) throws Exception {
        InetAddress invalidAddress = InetAddress.getByName(invalidIpAddress);
        client = createHttpClient(invalidAddress, CONNECTION_TIMEOUT);
        assertThatExceptionOfType(BindException.class)
            .isThrownBy(this::executeRequest);
    }

    @ParameterizedTest(name = "Valid local address: {1}")
    @MethodSource("provideValidLocalAddresses")
    @DisplayName("Valid local addresses should succeed")
    void validLocalAddressesShouldSucceed(InetAddress address, String description) throws Exception {
        client = createHttpClient(address, CONNECTION_TIMEOUT);
        HttpExecuteResponse response = executeRequest();
        assertThat(response.httpResponse().statusCode())
            .as("Request with %s should succeed", description)
            .isEqualTo(200);
        assertThat(readResponseBody(response))
            .isEqualTo(SUCCESS_RESPONSE);
    }

    @Test
    @DisplayName("Client without local address configuration should use system default")
    void withoutLocalAddressConfigurationShouldSucceed() throws Exception {
        client = createHttpClient(null, CONNECTION_TIMEOUT);
        HttpExecuteResponse response = executeRequest();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        assertThat(readResponseBody(response)).isEqualTo(SUCCESS_RESPONSE);
    }

    private static Stream<Arguments> provideValidLocalAddresses() throws Exception {
        return Stream.of(
            Arguments.of(InetAddress.getLoopbackAddress(), "loopback address"),
            Arguments.of(InetAddress.getByName("127.0.0.1"), "explicit localhost")
        );
    }

    private HttpExecuteResponse executeRequest() throws Exception {
        SdkHttpFullRequest request = createTestRequest();

        return client.prepareRequest(
                         HttpExecuteRequest.builder()
                                           .request(request)
                                           .contentStreamProvider(request.contentStreamProvider().orElse(null))
                                           .build())
                     .call();
    }

    private SdkHttpFullRequest createTestRequest() {
        URI uri = URI.create("http://localhost:" + wireMock.getPort());
        byte[] content = TEST_BODY.getBytes(StandardCharsets.UTF_8);

        return SdkHttpFullRequest.builder()
                                 .uri(uri)
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", uri.getHost())
                                 .putHeader("User-Agent", "test-client")
                                 .putHeader("Content-Length", Integer.toString(content.length))
                                 .contentStreamProvider(() -> new ByteArrayInputStream(content))
                                 .build();
    }

    private String readResponseBody(HttpExecuteResponse response) throws IOException {
        if (!response.responseBody().isPresent()) {
            return "";
        }
        return IoUtils.toUtf8String(response.responseBody().get());
    }
}
