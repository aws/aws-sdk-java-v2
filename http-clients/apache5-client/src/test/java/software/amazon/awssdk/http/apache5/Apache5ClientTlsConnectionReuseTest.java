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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.FileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Tests that {@link Apache5HttpClient} reuses pooled connections when it presents a TLS client certificate.
 */
class Apache5ClientTlsConnectionReuseTest extends ClientTlsAuthTestBase {
    private static final int REQUEST_COUNT = 5;
    private static final AtomicInteger OPENED_CONNECTIONS = new AtomicInteger();

    private static WireMockServer wireMockServer;
    private static TlsKeyManagersProvider keyManagersProvider;
    private SdkHttpClient client;

    @BeforeAll
    static void setUpServer() {
        // ClientTlsAuthTestBase's @BeforeAll has already copied the keystores to the temp directory.
        wireMockServer = new WireMockServer(wireMockConfig()
                                                .dynamicHttpsPort()
                                                .dynamicPort()
                                                .needClientAuth(true)
                                                .keystorePath(serverKeyStore.toAbsolutePath().toString())
                                                .keystorePassword(STORE_PASSWORD)
                                                .trustStorePath(serverKeyStore.toAbsolutePath().toString())
                                                .trustStorePassword(STORE_PASSWORD)
                                                .networkTrafficListener(new ConnectionCountingListener()));
        wireMockServer.start();
        keyManagersProvider = FileStoreTlsKeyManagersProvider.create(clientKeyStore, CLIENT_STORE_TYPE, STORE_PASSWORD);
    }

    @AfterAll
    static void tearDownServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUpStub() {
        wireMockServer.stubFor(any(urlMatching(".*")).willReturn(aResponse().withStatus(200).withBody("{}")));
        OPENED_CONNECTIONS.set(0);
    }

    @AfterEach
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void sequentialRequests_withClientCertificate_reuseOneConnection() throws IOException {
        client = Apache5HttpClient.builder()
                                  .tlsKeyManagersProvider(keyManagersProvider)
                                  .buildWithDefaults(AttributeMap.builder()
                                                                 .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES,
                                                                      Boolean.TRUE)
                                                                 .build());

        for (int i = 0; i < REQUEST_COUNT; i++) {
            HttpExecuteResponse response = client.prepareRequest(request()).call();
            assertThat(response.httpResponse().isSuccessful()).isTrue();
            // Drain and close the body so that the connection goes back to the pool.
            try (AbortableInputStream body = response.responseBody().get()) {
                IoUtils.drainInputStream(body);
            }
        }

        assertThat(OPENED_CONNECTIONS.get()).isEqualTo(1);
    }

    private static HttpExecuteRequest request() {
        return HttpExecuteRequest.builder()
                                 .request(SdkHttpFullRequest.builder()
                                                            .method(SdkHttpMethod.GET)
                                                            .protocol("https")
                                                            .host("localhost")
                                                            .port(wireMockServer.httpsPort())
                                                            .build())
                                 .build();
    }

    private static final class ConnectionCountingListener implements WiremockNetworkTrafficListener {
        @Override
        public void opened(Socket socket) {
            OPENED_CONNECTIONS.incrementAndGet();
        }

        @Override
        public void incoming(Socket socket, ByteBuffer bytes) {
        }

        @Override
        public void outgoing(Socket socket, ByteBuffer bytes) {
        }

        @Override
        public void closed(Socket socket) {
        }
    }
}
