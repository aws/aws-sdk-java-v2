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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE_PASSWORD;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE_TYPE;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import software.amazon.awssdk.http.FileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.apache5.internal.conn.SdkTlsSocketFactory;
import software.amazon.awssdk.internal.http.NoneTlsKeyManagersProvider;

/**
 * Tests to ensure that {@link Apache5HttpClient} can properly support TLS
 * client authentication.
 */
public class Apache5ClientTlsAuthTest extends ClientTlsAuthTestBase {
    private static WireMockServer wireMockServer;
    private static TlsKeyManagersProvider keyManagersProvider;
    private SdkHttpClient client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws IOException {
        ClientTlsAuthTestBase.setUp();

        // Will be used by both client and server to trust the self-signed
        // cert they present to each other
        System.setProperty("javax.net.ssl.trustStore", serverKeyStore.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", STORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", "jks");

        wireMockServer = new WireMockServer(wireMockConfig()
                .dynamicHttpsPort()
                .dynamicPort()
                .needClientAuth(true)
                .keystorePath(serverKeyStore.toAbsolutePath().toString())
                .keystorePassword(STORE_PASSWORD)
        );

        wireMockServer.start();

        keyManagersProvider = FileStoreTlsKeyManagersProvider.create(clientKeyStore, CLIENT_STORE_TYPE, STORE_PASSWORD);
    }

    @Before
    public void methodSetup() {
        wireMockServer.stubFor(any(urlMatching(".*")).willReturn(aResponse().withStatus(200).withBody("{}")));
    }

    @AfterClass
    public static void teardown() throws IOException {
        wireMockServer.stop();
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
        System.clearProperty("javax.net.ssl.trustStoreType");
        ClientTlsAuthTestBase.teardown();
    }

    @After
    public void methodTeardown() {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Test
    public void prepareRequest_whenKeyProviderConfigured_successfullyMakesHttpsRequest() throws IOException {
        client = Apache5HttpClient.builder()
                .tlsKeyManagersProvider(keyManagersProvider)
                .build();
        HttpExecuteResponse httpExecuteResponse = makeRequestWithHttpClient(client);
        assertThat(httpExecuteResponse.httpResponse().isSuccessful()).isTrue();
    }

    @Test
    public void prepareRequest_whenKeyProviderNotConfigured_throwsSslException() throws IOException {
        client = Apache5HttpClient.builder().tlsKeyManagersProvider(NoneTlsKeyManagersProvider.getInstance()).build();
        assertThatThrownBy(() -> makeRequestWithHttpClient(client)).isInstanceOfAny(SSLException.class, SocketException.class);
    }

    @Test
    public void prepareRequest_whenTlsProxyConfigured_authenticatesSuccessfully() throws IOException {
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                .endpoint(URI.create("https://localhost:" + wireMockServer.httpsPort()))
                .build();

        client = Apache5HttpClient.builder()
                .proxyConfiguration(proxyConfig)
                .tlsKeyManagersProvider(keyManagersProvider)
                .build();

        HttpExecuteResponse httpExecuteResponse = makeRequestWithHttpClient(client);

        // WireMock doesn't mock 'CONNECT' methods and will return a 404 for this
        assertThat(httpExecuteResponse.httpResponse().statusCode()).isEqualTo(404);
    }

    @Test
    public void build_whenNoTlsKeyManagersProviderSet_usesSystemPropertyProvider() throws IOException {
        System.setProperty(SSL_KEY_STORE.property(), clientKeyStore.toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), CLIENT_STORE_TYPE);
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), STORE_PASSWORD);

        client = Apache5HttpClient.builder().build();
        try {
            makeRequestWithHttpClient(client);
        } finally {
            System.clearProperty(SSL_KEY_STORE.property());
            System.clearProperty(SSL_KEY_STORE_TYPE.property());
            System.clearProperty(SSL_KEY_STORE_PASSWORD.property());
        }
    }

    @Test
    public void build_whenTlsKeyManagersProviderExplicitlySetToNull_usesSystemPropertyProvider() throws IOException {
        System.setProperty(SSL_KEY_STORE.property(), clientKeyStore.toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), CLIENT_STORE_TYPE);
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), STORE_PASSWORD);

        client = Apache5HttpClient.builder().tlsKeyManagersProvider(null).build();
        try {
            makeRequestWithHttpClient(client);
        } finally {
            System.clearProperty(SSL_KEY_STORE.property());
            System.clearProperty(SSL_KEY_STORE_TYPE.property());
            System.clearProperty(SSL_KEY_STORE_PASSWORD.property());
        }
    }

    @Test
    public void build_whenSocketFactoryNotSet_configuresDefaultSocketFactory() throws Exception {
        System.setProperty(SSL_KEY_STORE.property(), clientKeyStore.toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), CLIENT_STORE_TYPE);
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), STORE_PASSWORD);

        TlsKeyManagersProvider provider = FileStoreTlsKeyManagersProvider.create(clientKeyStore,
                                                                                 CLIENT_STORE_TYPE,
                                                                                 STORE_PASSWORD);
        KeyManager[] keyManagers = provider.keyManagers();

        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(keyManagers, null, null);

        // Use TlsSocketStrategy instead of ConnectionSocketFactory
        TlsSocketStrategy socketFactory = new SdkTlsSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
        TlsSocketStrategy socketFactoryMock = Mockito.spy(socketFactory);

        client = Apache5HttpClient.builder().build();

        try {
            HttpExecuteResponse httpExecuteResponse = makeRequestWithHttpClient(client);
            assertThat(httpExecuteResponse.httpResponse().statusCode()).isEqualTo(200);
        } finally {
            System.clearProperty(SSL_KEY_STORE.property());
            System.clearProperty(SSL_KEY_STORE_TYPE.property());
            System.clearProperty(SSL_KEY_STORE_PASSWORD.property());
        }

        Mockito.verifyNoInteractions(socketFactoryMock);
    }

    @Test
    public void build_whenCustomSocketFactorySet_usesProvidedSocketFactory() throws IOException,
                                                                                                 NoSuchAlgorithmException,
                                                                                                 KeyManagementException {
        TlsKeyManagersProvider provider = FileStoreTlsKeyManagersProvider.create(clientKeyStore,
                                                                                 CLIENT_STORE_TYPE,
                                                                                 STORE_PASSWORD);
        KeyManager[] keyManagers = provider.keyManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, null, null);

        // Create actual SSLConnectionSocketFactory instead of SdkTlsSocketFactory
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
            sslContext,
            NoopHostnameVerifier.INSTANCE
        );
        SSLConnectionSocketFactory socketFactorySpy = Mockito.spy(socketFactory);

        client = Apache5HttpClient.builder()
                                  .socketFactory(socketFactorySpy)  // Now passes correct type
                                  .build();
        makeRequestWithHttpClient(client);

        // Verify the legacy method signature
        Mockito.verify(socketFactorySpy).createLayeredSocket(
            Mockito.any(Socket.class),
            Mockito.anyString(),
            Mockito.anyInt(),
            Mockito.any(HttpContext.class)
        );
    }

    private HttpExecuteResponse makeRequestWithHttpClient(SdkHttpClient httpClient) throws IOException {
        SdkHttpRequest httpRequest = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .protocol("https")
                .host("localhost:" + wireMockServer.httpsPort())
                .build();

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                .request(httpRequest)
                .build();

        return httpClient.prepareRequest(request).call();
    }

    @Test
    public void build_whenTlsSocketStrategyConfigured_usesProvidedStrategy() throws Exception {
        // Setup TLS context
        KeyManager[] keyManagers = keyManagersProvider.keyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, null, null);

        // Create and spy on TlsSocketStrategy
        TlsSocketStrategy tlsStrategy = new SdkTlsSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        TlsSocketStrategy tlsStrategySpy = Mockito.spy(tlsStrategy);

        // Build client with TLS strategy
        client = Apache5HttpClient.builder()
                                  .tlsSocketStrategy(tlsStrategySpy)
                                  .build();

        // Make request and verify
        HttpExecuteResponse response = makeRequestWithHttpClient(client);
        assertThat(response.httpResponse().isSuccessful()).isTrue();

        // Verify upgrade method was called
        Mockito.verify(tlsStrategySpy).upgrade(
            Mockito.any(Socket.class),
            Mockito.anyString(),
            Mockito.anyInt(),
            Mockito.any(),
            Mockito.any(HttpContext.class)
        );
    }

    @Test
    public void build_whenBothTlsStrategyAndLegacyFactorySet_throwsIllegalArgumentException() throws Exception {
        // Setup TLS context
        KeyManager[] keyManagers = keyManagersProvider.keyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, null, null);

        // Create both socket factory and TLS strategy
        SSLConnectionSocketFactory legacyFactory = new SSLConnectionSocketFactory(
            sslContext,
            NoopHostnameVerifier.INSTANCE
        );

        TlsSocketStrategy tlsStrategy = new SdkTlsSocketFactory(
            sslContext,
            NoopHostnameVerifier.INSTANCE
        );

        // Attempt to build client with both - should throw exception
        assertThatThrownBy(() -> Apache5HttpClient.builder()
                                                  .socketFactory(legacyFactory)
                                                  .tlsSocketStrategy(tlsStrategy)
                                                  .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot configure both tlsSocketStrategy and socketFactory")
            .hasMessageContaining("deprecated")
            .hasMessageContaining("use tlsSocketStrategy");
    }

}
