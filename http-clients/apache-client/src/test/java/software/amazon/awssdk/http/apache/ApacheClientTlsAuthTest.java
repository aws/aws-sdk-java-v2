/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE_PASSWORD;
import static software.amazon.awssdk.utils.JavaSystemSetting.SSL_KEY_STORE_TYPE;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import javax.net.ssl.SSLException;
import org.apache.http.NoHttpResponseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.http.FileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.TlsKeyManagersProvider;

/**
 * Tests to ensure that {@link ApacheHttpClient} can properly support TLS
 * client authentication.
 */
public class ApacheClientTlsAuthTest extends ClientTlsAuthTestBase {
    private static WireMockServer wireMockServer;
    private static TlsKeyManagersProvider keyManagersProvider;
    private SdkHttpClient client;

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
    public void canMakeHttpsRequestWhenKeyProviderConfigured() throws IOException {
        client = ApacheHttpClient.builder()
                .tlsKeyManagersProvider(keyManagersProvider)
                .build();
        HttpExecuteResponse httpExecuteResponse = makeRequestWithHttpClient(client);
        assertThat(httpExecuteResponse.httpResponse().isSuccessful()).isTrue();
    }

    @Test
    public void requestFailsWhenKeyProviderNotConfigured() throws IOException {
        client = ApacheHttpClient.builder().build();
        try {
            makeRequestWithHttpClient(client);
            fail("HTTP request should have failed");
        } catch (NoHttpResponseException | SSLException | SocketException expected) {
            // The client doesn't seem to consistently throw a single error,
            // and can also vary depending on actual JVM used.
        }
    }

    @Test
    public void authenticatesWithTlsProxy() throws IOException {
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                .endpoint(URI.create("https://localhost:" + wireMockServer.httpsPort()))
                .build();

        client = ApacheHttpClient.builder()
                .proxyConfiguration(proxyConfig)
                .tlsKeyManagersProvider(keyManagersProvider)
                .build();

        HttpExecuteResponse httpExecuteResponse = makeRequestWithHttpClient(client);

        // WireMock doesn't mock 'CONNECT' methods and will return a 404 for this
        assertThat(httpExecuteResponse.httpResponse().statusCode()).isEqualTo(404);
    }

    @Test
    public void defaultTlsKeyManagersProviderIsSystemPropertyProvider() throws IOException {
        System.setProperty(SSL_KEY_STORE.property(), clientKeyStore.toAbsolutePath().toString());
        System.setProperty(SSL_KEY_STORE_TYPE.property(), CLIENT_STORE_TYPE);
        System.setProperty(SSL_KEY_STORE_PASSWORD.property(), STORE_PASSWORD);

        client = ApacheHttpClient.builder().build();
        try {
            makeRequestWithHttpClient(client);
        } finally {
            System.clearProperty(SSL_KEY_STORE.property());
            System.clearProperty(SSL_KEY_STORE_TYPE.property());
            System.clearProperty(SSL_KEY_STORE_PASSWORD.property());
        }
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

}
