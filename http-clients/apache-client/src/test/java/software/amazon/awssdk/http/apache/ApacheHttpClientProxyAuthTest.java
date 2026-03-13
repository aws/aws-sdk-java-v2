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

package software.amazon.awssdk.http.apache;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Tests proxy preemptive authentication functionality.
 * 
 * Verifies that when preemptiveBasicAuthenticationEnabled(true) is configured,
 * the Proxy-Authorization header is sent with the first request to the proxy.
 */
public class ApacheHttpClientProxyAuthTest {
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpass";

    // Header value is "Basic " + base64(<username> + ':' + <password>)
    // https://datatracker.ietf.org/doc/html/rfc7617#section-2
    private static final String BASIC_PROXY_AUTH_HEADER =
        "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());

    private WireMockServer mockProxy;
    private SdkHttpClient httpClient;

    @BeforeEach
    public void setup() {
        mockProxy = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockProxy.start();
    }

    @AfterEach
    public void teardown() {
        if (httpClient != null) {
            httpClient.close();
        }
        if (mockProxy != null) {
            mockProxy.stop();
        }
    }

    @Test
    public void proxyAuthentication_whenPreemptiveAuthEnabled_shouldSendProxyAuthorizationHeader() throws Exception {
        mockProxy.stubFor(any(anyUrl())
                .withHeader("Proxy-Authorization", equalTo(BASIC_PROXY_AUTH_HEADER))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Success")));

        // Create HTTP client with preemptive proxy authentication enabled
        httpClient = ApacheHttpClient.builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                        .endpoint(URI.create("http://localhost:" + mockProxy.port()))
                        .username(USERNAME)
                        .password(PASSWORD)
                        .preemptiveBasicAuthenticationEnabled(true)
                        .build())
                .build();

        // Create a request
        SdkHttpRequest request = SdkHttpRequest.builder()
                .method(SdkHttpMethod.GET)
                .uri(URI.create("http://example.com/test"))
                .build();

        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                .request(request)
                .build();

        // Execute the request - should succeed with preemptive auth header
        HttpExecuteResponse response = httpClient.prepareRequest(executeRequest).call();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);

        mockProxy.verify(1, anyRequestedFor(anyUrl()));
        mockProxy.verify(WireMock.getRequestedFor(anyUrl())
                .withHeader("Proxy-Authorization", equalTo(BASIC_PROXY_AUTH_HEADER)));
    }

    @Test
    public void proxyAuthentication_whenPreemptiveAuthDisabled_shouldUseChallengeResponseAuth() throws Exception {
        // First request without auth header should get 407
        mockProxy.stubFor(any(anyUrl())
                .willReturn(aResponse()
                        .withStatus(407)
                        .withHeader("Proxy-Authenticate", "Basic realm=\"proxy\"")));

        // Second request with auth header should succeed
        mockProxy.stubFor(any(anyUrl())
                .withHeader("Proxy-Authorization", equalTo(BASIC_PROXY_AUTH_HEADER))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Success")));

        // Create HTTP client with preemptive proxy authentication disabled
        httpClient = ApacheHttpClient.builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                        .endpoint(URI.create("http://localhost:" + mockProxy.port()))
                        .username(USERNAME)
                        .password(PASSWORD)
                        .preemptiveBasicAuthenticationEnabled(false)
                        .build())
                .build();

        // Create a request
        SdkHttpRequest request = SdkHttpRequest.builder()
                .method(SdkHttpMethod.GET)
                .uri(URI.create("http://example.com/test"))
                .build();

        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                .request(request)
                .build();

        // Execute the request - should succeed after challenge-response
        HttpExecuteResponse response = httpClient.prepareRequest(executeRequest).call();
        assertThat(response.httpResponse().statusCode()).isEqualTo(200);

        // Verify challenge-response flow - 2 requests total
        mockProxy.verify(2, anyRequestedFor(anyUrl()));
        // First request without auth header
        mockProxy.verify(1, anyRequestedFor(anyUrl()).withoutHeader("Proxy-Authorization"));
        // Second request with auth header
        mockProxy.verify(1, anyRequestedFor(anyUrl()).withHeader("Proxy-Authorization", equalTo(BASIC_PROXY_AUTH_HEADER)));
    }
}
