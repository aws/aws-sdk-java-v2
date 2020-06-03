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

package software.amazon.awssdk.http.nio.netty;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.FileStoreTlsKeyManagersProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Tests to ensure that Netty layer can perform TLS client authentication.
 */
public class NettyClientTlsAuthTest extends ClientTlsAuthTestBase {
    private static final AttributeMap DEFAULTS = AttributeMap.builder()
            .put(TRUST_ALL_CERTIFICATES, true)
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static WireMockServer mockProxy;
    private static ProxyConfiguration proxyCfg;
    private static TlsKeyManagersProvider keyManagersProvider;

    private SdkAsyncHttpClient netty;

    @BeforeClass
    public static void setUp() throws IOException {
        ClientTlsAuthTestBase.setUp();

        // Will be used by both client and server to trust the self-signed
        // cert they present to each other
        System.setProperty("javax.net.ssl.trustStore", serverKeyStore.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", STORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", "jks");

        mockProxy = new WireMockServer(new WireMockConfiguration()
                .dynamicHttpsPort()
                .needClientAuth(true)
                .keystorePath(serverKeyStore.toAbsolutePath().toString())
                .keystorePassword(STORE_PASSWORD));

        mockProxy.start();

        mockProxy.stubFor(get(urlPathMatching(".*")).willReturn(aResponse().withStatus(200).withBody("hello")));

        proxyCfg = ProxyConfiguration.builder()
                .scheme("https")
                .host("localhost")
                .port(mockProxy.httpsPort())
                .build();

        keyManagersProvider = FileStoreTlsKeyManagersProvider.create(clientKeyStore, CLIENT_STORE_TYPE, STORE_PASSWORD);
    }

    @AfterClass
    public static void teardown() throws IOException {
        ClientTlsAuthTestBase.teardown();

        mockProxy.stop();

        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
        System.clearProperty("javax.net.ssl.trustStoreType");
    }

    @After
    public void methodTeardown() {
        if (netty != null) {
            netty.close();
        }
        netty = null;
    }

    @Test
    public void builderUsesProvidedKeyManagersProvider() {
        TlsKeyManagersProvider mockKeyManagersProvider = mock(TlsKeyManagersProvider.class);
        netty = NettyNioAsyncHttpClient.builder()
                .proxyConfiguration(proxyCfg)
                .tlsKeyManagersProvider(mockKeyManagersProvider)
                .buildWithDefaults(DEFAULTS);

        try {
            sendRequest(netty, new RecordingResponseHandler());
        } catch (Exception ignored) {
        }
        verify(mockKeyManagersProvider).keyManagers();
    }

    @Test
    public void proxyRequest_ableToAuthenticate() {
        thrown.expectCause(instanceOf(IOException.class));
        thrown.expectMessage("Could not connect to proxy");

        netty = NettyNioAsyncHttpClient.builder()
                .proxyConfiguration(proxyCfg)
                .tlsKeyManagersProvider(keyManagersProvider)
                .buildWithDefaults(DEFAULTS);

        sendRequest(netty, new RecordingResponseHandler());
    }

    @Test
    public void proxyRequest_noKeyManagerGiven_notAbleToSendConnect() throws Throwable {
        thrown.expectCause(instanceOf(IOException.class));
        thrown.expectMessage("Unable to send CONNECT request to proxy");

        netty = NettyNioAsyncHttpClient.builder()
                .proxyConfiguration(proxyCfg)
                .buildWithDefaults(DEFAULTS);

        sendRequest(netty, new RecordingResponseHandler());
    }

    @Test
    public void proxyRequest_keyStoreSystemPropertiesConfigured_ableToAuthenticate() throws Throwable {
        thrown.expectCause(instanceOf(IOException.class));
        thrown.expectMessage("Could not connect to proxy");

        System.setProperty("javax.net.ssl.keyStore", clientKeyStore.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.keyStoreType", CLIENT_STORE_TYPE);
        System.setProperty("javax.net.ssl.keyStorePassword", STORE_PASSWORD);

        netty = NettyNioAsyncHttpClient.builder()
                .proxyConfiguration(proxyCfg)
                .buildWithDefaults(DEFAULTS);

        try {
            sendRequest(netty, new RecordingResponseHandler());
        } finally {
            System.clearProperty("javax.net.ssl.keyStore");
            System.clearProperty("javax.net.ssl.keyStoreType");
            System.clearProperty("javax.net.ssl.keyStorePassword");
        }
    }

    private void sendRequest(SdkAsyncHttpClient client, SdkAsyncHttpResponseHandler responseHandler) {
        AsyncExecuteRequest req = AsyncExecuteRequest.builder()
                .request(testSdkRequest())
                .requestContentPublisher(new EmptyPublisher())
                .responseHandler(responseHandler)
                .build();

        client.execute(req).join();
    }

    private static SdkHttpFullRequest testSdkRequest() {
        return SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .protocol("https")
                .host("some-awesome-service.amazonaws.com")
                .port(443)
                .putHeader("host", "some-awesome-service.amazonaws.com")
                .build();
    }

}
