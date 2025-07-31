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

package software.amazon.awssdk.http.apache5;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache5.internal.conn.SdkTlsSocketFactory;
import software.amazon.awssdk.utils.IoUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

class Apache5HttpClientWithSocketFactoryWireMockTest {

    private WireMockServer httpMockServer;
    private WireMockServer httpsMockServer;

    @BeforeEach
    void setUp() {
        httpMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        httpMockServer.start();

        httpsMockServer = new WireMockServer(WireMockConfiguration.options().dynamicHttpsPort());
        httpsMockServer.start();
    }

    @AfterEach
    void tearDown() {
        httpMockServer.stop();
        httpsMockServer.stop();
    }

    @Test
    void prepareRequest_withTlsStrategyOverHttp_doesNotCallUpgrade() throws Exception {
        TlsSocketStrategy tlsStrategySpy = spy(new TlsSocketStrategy() {
            @Override
            public SSLSocket upgrade(Socket socket, String target, int port,
                                     Object attachment, HttpContext context) {
                fail("TLS upgrade should not be called for HTTP");
                return null;
            }
        });
        SdkHttpClient client = Apache5HttpClient.builder()
                                                .tlsSocketStrategy(tlsStrategySpy)
                                                .build();

        stubForMockRequest(httpMockServer, 200);
        SdkHttpFullRequest request = mockSdkRequest("http://localhost:" + httpMockServer.port(), SdkHttpMethod.GET);
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(request)
                                                                               .contentStreamProvider(request.contentStreamProvider().orElse(null))
                                                                               .build())
                                             .call();
        validateResponse(response, 200);
        verify(tlsStrategySpy, never()).upgrade(any(), any(), anyInt(), any(), any());
    }

    @Test
    void prepareRequest_withTlsStrategyOverHttps_callsUpgrade() throws Exception {
        SSLContext trustAllContext = createTrustAllSslContext();
        TlsSocketStrategy tlsStrategySpy = spy(new SdkTlsSocketFactory(
            trustAllContext,
            NoopHostnameVerifier.INSTANCE
        ));
        SdkHttpClient client = Apache5HttpClient.builder()
                                                .tlsSocketStrategy(tlsStrategySpy)
                                                .build();

        stubForMockRequest(httpsMockServer, 200);
        SdkHttpFullRequest request = mockSdkRequest("https://localhost:" + httpsMockServer.httpsPort(), SdkHttpMethod.GET);
        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(request)
                                                                               .contentStreamProvider(request.contentStreamProvider().orElse(null))
                                                                               .build())
                                             .call();

        validateResponse(response, 200);
        verify(tlsStrategySpy, atLeastOnce()).upgrade(any(), eq("localhost"), eq(httpsMockServer.httpsPort()), any(), any());
    }

    private void stubForMockRequest(WireMockServer server, int returnCode) {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo("/test"))
                               .willReturn(WireMock.aResponse()
                                                   .withStatus(returnCode)
                                                   .withBody("test response body")));
    }

    private SdkHttpFullRequest mockSdkRequest(String url, SdkHttpMethod method) {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create(url + "/test"))
                                 .method(method)
                                 .build();
    }

    private void validateResponse(HttpExecuteResponse response, int expectedStatusCode) throws Exception {
        assertThat(response).isNotNull();
        assertThat(response.httpResponse()).isNotNull();
        assertThat(response.httpResponse().statusCode()).isEqualTo(expectedStatusCode);

        if (response.responseBody().isPresent()) {
            try (InputStream is = response.responseBody().get()) {
                String body = IoUtils.toUtf8String(is);
                assertThat(body).isEqualTo("test response body");
            }
        }
    }

    private SSLContext createTrustAllSslContext() throws Exception {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        return SSLContexts.custom()
                          .loadTrustMaterial(null, acceptingTrustStrategy)
                          .build();
    }
}
