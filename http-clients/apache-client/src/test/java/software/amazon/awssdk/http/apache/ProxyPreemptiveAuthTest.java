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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
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
public class ProxyPreemptiveAuthTest {

    private WireMockServer proxyServer;
    private SdkHttpClient httpClient;

    @BeforeEach
    public void setup() {
        proxyServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        proxyServer.start();
    }

    @AfterEach
    public void teardown() {
        if (httpClient != null) {
            httpClient.close();
        }
        if (proxyServer != null) {
            proxyServer.stop();
        }
    }

    @Test
    public void testPreemptiveAuthenticationSendsProxyAuthorizationHeader() throws Exception {
        proxyServer.stubFor(any(anyUrl())
                .withHeader("Proxy-Authorization", matching("Basic .+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Success")));

        // Create HTTP client with preemptive proxy authentication enabled
        httpClient = ApacheHttpClient.builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                        .endpoint(URI.create("http://localhost:" + proxyServer.port()))
                        .username("testuser")
                        .password("testpass")
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
        
        // Verify that the proxy received the request with Proxy-Authorization header
        proxyServer.verify(WireMock.getRequestedFor(anyUrl())
                .withHeader("Proxy-Authorization", matching("Basic .+")));
    }
}
