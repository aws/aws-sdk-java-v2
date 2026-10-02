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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;
import software.amazon.awssdk.http.apache5.internal.conn.SdkTlsSocketFactory;
import software.amazon.awssdk.utils.IoUtils;

public class ConnectionTtlTest {
    private static WireMockServer wireMockServer;
    private SdkHttpClient apache5;

    @BeforeAll
    public static void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort().dynamicHttpsPort());
        wireMockServer.start();

        wireMockServer.stubFor(WireMock.get(WireMock.anyUrl())
                                       .willReturn(WireMock.aResponse()
                                                           .withStatus(200)
                                                           .withBody("Hello there!")));
    }

    @AfterEach
    public void methodTeardown() {
        if (apache5 != null) {
            apache5.close();
            apache5 = null;
        }
    }

    @AfterAll
    public static void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void execute_ttlDefault_connectionNotClosed() throws Exception {
        TestTlsSocketStrategy socketStrategy = TestTlsSocketStrategy.create();

        apache5 = Apache5HttpClient.builder()
                                   .connectionMaxIdleTime(Duration.ofDays(1))
                                   .tlsSocketStrategy(socketStrategy)
                                   .build();

        doGetCall(apache5);
        doGetCall(apache5);

        List<SSLSocket> sockets = socketStrategy.getCreatedSockets();
        assertThat(sockets).hasSize(1);
    }

    @Test
    public void execute_ttlNegative_connectionNotClosed() throws Exception {
        TestTlsSocketStrategy socketStrategy = TestTlsSocketStrategy.create();

        apache5 = Apache5HttpClient.builder()
                                   .connectionTimeToLive(Duration.ofMillis(-1))
                                   .connectionMaxIdleTime(Duration.ofDays(1))
                                   .tlsSocketStrategy(socketStrategy)
                                   .build();

        doGetCall(apache5);
        doGetCall(apache5);

        List<SSLSocket> sockets = socketStrategy.getCreatedSockets();
        assertThat(sockets).hasSize(1);
    }

    @Test
    public void execute_ttlIsZero_connectionNotClosed() throws Exception {
        TestTlsSocketStrategy socketStrategy = TestTlsSocketStrategy.create();

        apache5 = Apache5HttpClient.builder()
                                   .connectionTimeToLive(Duration.ZERO)
                                   .connectionMaxIdleTime(Duration.ofDays(1))
                                   .tlsSocketStrategy(socketStrategy)
                                   .build();

        doGetCall(apache5);
        doGetCall(apache5);

        List<SSLSocket> sockets = socketStrategy.getCreatedSockets();
        assertThat(sockets).hasSize(1);
    }

    @Test
    public void execute_ttlIsShort_idleExceedsTtl_connectionClosed() throws Exception {
        TestTlsSocketStrategy socketStrategy = TestTlsSocketStrategy.create();

        long ttlMs = 5;

        apache5 = Apache5HttpClient.builder()
                                   .connectionTimeToLive(Duration.ofMillis(ttlMs))
                                   .connectionMaxIdleTime(Duration.ofDays(1))
                                   .tlsSocketStrategy(socketStrategy)
                                   .build();

        doGetCall(apache5);
        Thread.sleep(ttlMs * 10);
        doGetCall(apache5);

        List<SSLSocket> sockets = socketStrategy.getCreatedSockets();
        // second request should have created a second socket as the first goes over TTL
        assertThat(sockets).hasSize(2);
    }

    private void doGetCall(SdkHttpClient apache) throws IOException {
        SdkHttpRequest sdkRequest = SdkHttpFullRequest.builder()
                                                      .method(SdkHttpMethod.GET)
                                                      .uri(URI.create("https://localhost:" + wireMockServer.httpsPort()))
                                                      .build();

        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder().request(sdkRequest).build();

        HttpExecuteResponse response = apache.prepareRequest(executeRequest).call();
        IoUtils.drainInputStream(response.responseBody().get());
    }

    private static class TestTlsSocketStrategy extends SdkTlsSocketFactory {
        private List<SSLSocket> sslSockets = new ArrayList<>();

        TestTlsSocketStrategy(SSLContext ctx) {
            super(ctx, NoopHostnameVerifier.INSTANCE);
        }

        @Override
        public SSLSocket upgrade(Socket socket, String target, int port, Object attachment, HttpContext context) throws IOException {
            SSLSocket upgradedSocket = super.upgrade(socket, target, port, attachment, context);
            sslSockets.add(upgradedSocket);
            return upgradedSocket;
        }

        List<SSLSocket> getCreatedSockets() {
            return sslSockets;
        }

        static TestTlsSocketStrategy create() throws Exception {
            KeyManager[] keyManagers = SystemPropertyTlsKeyManagersProvider.create().keyManagers();

            TrustManager[] trustManagers = {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };
            SSLContext ssl = SSLContext.getInstance("SSL");
            ssl.init(keyManagers, trustManagers, null);
            return new TestTlsSocketStrategy(ssl);
        }
    }
}
