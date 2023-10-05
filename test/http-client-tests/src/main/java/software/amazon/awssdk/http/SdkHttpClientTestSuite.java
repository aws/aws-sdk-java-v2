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
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * A set of tests validating that the functionality implemented by a {@link SdkHttpClient}.
 * <p>
 * This is used by an HTTP plugin implementation by extending this class and implementing the abstract methods to provide this
 * suite with a testable HTTP client implementation.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class SdkHttpClientTestSuite {
    private static final Logger LOG = Logger.loggerFor(SdkHttpClientTestSuite.class);

    private static final ConnectionCountingTrafficListener CONNECTION_COUNTER = new ConnectionCountingTrafficListener();

    @Rule
    public WireMockRule mockServer = createWireMockRule();

    private final Random rng = new Random();


    @Test
    public void supportsResponseCode200() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_OK);
    }

    @Test
    public void supportsResponseCode200Head() throws Exception {
        // HEAD is special due to closing of the connection immediately and streams are null
        testForResponseCode(HttpURLConnection.HTTP_FORBIDDEN, SdkHttpMethod.HEAD);
    }

    @Test
    public void supportsResponseCode202() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_ACCEPTED);
    }

    @Test
    public void supportsResponseCode403() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test
    public void supportsResponseCode403Head() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_FORBIDDEN, SdkHttpMethod.HEAD);
    }

    @Test
    public void supportsResponseCode301() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_MOVED_PERM);
    }

    @Test
    public void supportsResponseCode302() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_MOVED_TEMP);
    }

    @Test
    public void supportsResponseCode500() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
    }

    @Test
    public void validatesHttpsCertificateIssuer() {
        SdkHttpClient client = createSdkHttpClient();

        SdkHttpFullRequest request = mockSdkRequest("https://localhost:" + mockServer.httpsPort(), SdkHttpMethod.POST);

        assertThatThrownBy(client.prepareRequest(HttpExecuteRequest.builder().request(request).build())::call)
                .isInstanceOf(SSLHandshakeException.class);
    }

    @Test
    public void connectionPoolingWorks() throws Exception {
        int initialOpenedConnections = CONNECTION_COUNTER.openedConnections();

        SdkHttpClientOptions httpClientOptions = new SdkHttpClientOptions();
        httpClientOptions.trustAll(true);
        SdkHttpClient client = createSdkHttpClient(httpClientOptions);

        stubForMockRequest(200);

        for (int i = 0; i < 5; i++) {
            SdkHttpFullRequest req = mockSdkRequest("http://localhost:" + mockServer.port(), SdkHttpMethod.POST);
            HttpExecuteResponse response =
                client.prepareRequest(HttpExecuteRequest.builder()
                                                        .request(req)
                                                        .contentStreamProvider(req.contentStreamProvider().orElse(null))
                                                        .build())
                      .call();
            response.responseBody().ifPresent(IoUtils::drainInputStream);
        }

        // connection pool growth strategies vary across client implementations. Some, such as the CRT grow connection counts
        // by a factor of 2, while some grow strictly as requested. Mainly we want to test that it kicks in at some point and
        // doesn't create a new connection for all 5 requests. This proves that while allowing variance in this behavior.
        assertThat(CONNECTION_COUNTER.openedConnections()).isGreaterThanOrEqualTo(initialOpenedConnections + 1);
        assertThat(CONNECTION_COUNTER.openedConnections()).isLessThanOrEqualTo(initialOpenedConnections + 2);

    }

    @Test
    public void connectionsAreNotReusedOn5xxErrors() throws Exception {
        int initialOpenedConnections = CONNECTION_COUNTER.openedConnections();

        SdkHttpClientOptions httpClientOptions = new SdkHttpClientOptions();
        httpClientOptions.trustAll(true);
        SdkHttpClient client = createSdkHttpClient(httpClientOptions);

        stubForMockRequest(503);

        for (int i = 0; i < 5; i++) {
            SdkHttpFullRequest req = mockSdkRequest("http://localhost:" + mockServer.port(), SdkHttpMethod.POST);
            HttpExecuteResponse response =
                client.prepareRequest(HttpExecuteRequest.builder()
                                                        .request(req)
                                                        .contentStreamProvider(req.contentStreamProvider().orElse(null))
                                                        .build())
                      .call();
            response.responseBody().ifPresent(IoUtils::drainInputStream);
        }

        // don't couple this test to connection manager behaviors we don't have to. We want to make sure that the connection count
        // increased by at least as many connections as we got 5xx errors back on. But the connection manager also predictively
        // creates connections and we need to take those into account in a way that lets it remain a dynamic behavior.
        assertThat(CONNECTION_COUNTER.openedConnections()).isGreaterThanOrEqualTo(initialOpenedConnections + 5);
    }

    @Test
    public void testCustomTlsTrustManager() throws Exception {
        WireMockServer selfSignedServer = HttpTestUtils.createSelfSignedServer();

        TrustManagerFactory managerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        managerFactory.init(HttpTestUtils.getSelfSignedKeyStore());

        SdkHttpClientOptions httpClientOptions = new SdkHttpClientOptions();
        httpClientOptions.tlsTrustManagersProvider(managerFactory::getTrustManagers);

        selfSignedServer.start();

        try {
            SdkHttpClient client = createSdkHttpClient(httpClientOptions);
            SdkHttpFullRequest request = mockSdkRequest("https://localhost:" + selfSignedServer.httpsPort(), SdkHttpMethod.POST);

            client.prepareRequest(HttpExecuteRequest.builder().request(request).build()).call();
        } finally {
            selfSignedServer.stop();
        }
    }

    @Test
    public void testTrustAllWorks() throws Exception {
        SdkHttpClientOptions httpClientOptions = new SdkHttpClientOptions();
        httpClientOptions.trustAll(true);

        testForResponseCodeUsingHttps(createSdkHttpClient(httpClientOptions), HttpURLConnection.HTTP_OK);
    }

    @Test
    public void testCustomTlsTrustManagerAndTrustAllFails() throws Exception {
        SdkHttpClientOptions httpClientOptions = new SdkHttpClientOptions();
        httpClientOptions.tlsTrustManagersProvider(() -> new TrustManager[0]);
        httpClientOptions.trustAll(true);

        assertThatThrownBy(() -> createSdkHttpClient(httpClientOptions)).isInstanceOf(IllegalArgumentException.class);
    }

    protected void testForResponseCode(int returnCode) throws Exception {
        testForResponseCode(returnCode, SdkHttpMethod.POST);
    }

    private void testForResponseCode(int returnCode, SdkHttpMethod method) throws Exception {
        SdkHttpClient client = createSdkHttpClient();

        stubForMockRequest(returnCode);

        SdkHttpFullRequest req = mockSdkRequest("http://localhost:" + mockServer.port(), method);
        HttpExecuteResponse rsp = client.prepareRequest(HttpExecuteRequest.builder()
                                                                          .request(req)
                                                                          .contentStreamProvider(req.contentStreamProvider()
                                                                                                    .orElse(null))
                                                                          .build())
                                        .call();

        validateResponse(rsp, returnCode, method);
    }

    protected void testForResponseCodeUsingHttps(SdkHttpClient client, int returnCode) throws Exception {
        SdkHttpMethod sdkHttpMethod = SdkHttpMethod.POST;
        stubForMockRequest(returnCode);

        SdkHttpFullRequest req = mockSdkRequest("https://localhost:" + mockServer.httpsPort(), sdkHttpMethod);
        HttpExecuteResponse rsp = client.prepareRequest(HttpExecuteRequest.builder()
                                                                          .request(req)
                                                                          .contentStreamProvider(req.contentStreamProvider()
                                                                                                    .orElse(null))
                                                                          .build())
                                        .call();

        validateResponse(rsp, returnCode, sdkHttpMethod);
    }

    protected void stubForMockRequest(int returnCode) {
        ResponseDefinitionBuilder responseBuilder = aResponse().withStatus(returnCode)
                                                               .withHeader("Some-Header", "With Value")
                                                               .withBody("hello");

        if (returnCode >= 300 && returnCode <= 399) {
            responseBuilder.withHeader("Location", "Some New Location");
        }

        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(responseBuilder));
    }

    private void validateResponse(HttpExecuteResponse response, int returnCode, SdkHttpMethod method) throws IOException {
        RequestMethod requestMethod = RequestMethod.fromString(method.name());

        RequestPatternBuilder patternBuilder = RequestPatternBuilder.newRequestPattern(requestMethod, urlMatching("/"))
                                                                           .withHeader("Host", containing("localhost"))
                                                                           .withHeader("User-Agent", equalTo("hello-world!"));

        if (method == SdkHttpMethod.HEAD) {
            patternBuilder.withRequestBody(absent());
        } else {
            patternBuilder.withRequestBody(equalTo("Body"));
        }

        mockServer.verify(1, patternBuilder);

        if (method == SdkHttpMethod.HEAD) {
            assertThat(response.responseBody()).isEmpty();
        } else {
            assertThat(IoUtils.toUtf8String(response.responseBody().orElse(null))).isEqualTo("hello");
        }

        assertThat(response.httpResponse().firstMatchingHeader("Some-Header")).contains("With Value");
        assertThat(response.httpResponse().statusCode()).isEqualTo(returnCode);
        mockServer.resetMappings();
    }

    protected SdkHttpFullRequest mockSdkRequest(String uriString, SdkHttpMethod method) {
        URI uri = URI.create(uriString);
        SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder()
                                                            .uri(uri)
                                                            .method(method)
                                                            .putHeader("Host", uri.getHost())
                                                            .putHeader("User-Agent", "hello-world!");
        if (method != SdkHttpMethod.HEAD) {
            byte[] content = "Body".getBytes(StandardCharsets.UTF_8);
            requestBuilder.putHeader("Content-Length", Integer.toString(content.length));
            requestBuilder.contentStreamProvider(() -> new ByteArrayInputStream(content));
        }

        return requestBuilder.build();
    }

    /**
     * {@link #createSdkHttpClient(SdkHttpClientOptions)} with default options.
     */
    protected final SdkHttpClient createSdkHttpClient() {
        return createSdkHttpClient(new SdkHttpClientOptions());
    }

    /**
     * Implemented by a child class to create an HTTP client to validate based on the provided options.
     */
    protected abstract SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options);

    /**
     * The options that should be considered when creating the client via {@link #createSdkHttpClient(SdkHttpClientOptions)}.
     */
    protected static final class SdkHttpClientOptions {
        private TlsTrustManagersProvider tlsTrustManagersProvider = null;
        private boolean trustAll = false;

        public TlsTrustManagersProvider tlsTrustManagersProvider() {
            return tlsTrustManagersProvider;
        }

        public void tlsTrustManagersProvider(TlsTrustManagersProvider tlsTrustManagersProvider) {
            this.tlsTrustManagersProvider = tlsTrustManagersProvider;
        }

        public boolean trustAll() {
            return trustAll;
        }

        public void trustAll(boolean trustAll) {
            this.trustAll = trustAll;
        }
    }

    private WireMockRule createWireMockRule() {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; ++i) {
            try {
                return new WireMockRule(wireMockConfig().dynamicPort()
                                                        .dynamicHttpsPort()
                                                        .networkTrafficListener(CONNECTION_COUNTER));
            } catch (FatalStartupException e) {
                int attemptNum = i + 1;
                LOG.debug(() -> "Was not able to start WireMock server. Attempt " + attemptNum, e);

                if (attemptNum != maxAttempts) {
                    try {
                        long sleepMillis = 1_000L + rng.nextInt(1_000);
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Backoff interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Unable to setup WireMock rule");
    }

}
