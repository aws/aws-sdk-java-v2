/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLHandshakeException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.utils.IoUtils;

/**
 * A set of tests validating that the functionality implemented by a {@link SdkHttpClient}.
 *
 * This is used by an HTTP plugin implementation by extending this class and implementing the abstract methods to provide this
 * suite with a testable HTTP client implementation.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class SdkHttpClientTestSuite {
    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Mock
    private SdkRequestContext requestContext;

    @Test
    public void supportsResponseCode200() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_OK);
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
    public void validatesHttpsCertificateIssuer() throws Exception {
        SdkHttpClient client = createSdkHttpClient();

        SdkHttpFullRequest request = mockSdkRequest("https://localhost:" + mockServer.httpsPort());

        assertThatThrownBy(client.prepareRequest(request, requestContext)::call).isInstanceOf(SSLHandshakeException.class);
    }

    private void testForResponseCode(int returnCode) throws Exception {
        SdkHttpClient client = createSdkHttpClient();

        stubForMockRequest(returnCode);

        SdkHttpFullRequest request = mockSdkRequest("http://localhost:" + mockServer.port());
        SdkHttpFullResponse response = client.prepareRequest(request, requestContext).call();

        validateResponse(response, returnCode);
    }

    protected void testForResponseCodeUsingHttps(SdkHttpClient client, int returnCode) throws Exception {
        stubForMockRequest(returnCode);

        SdkHttpFullRequest request = mockSdkRequest("https://localhost:" + mockServer.httpsPort());
        SdkHttpFullResponse response = client.prepareRequest(request, requestContext).call();

        validateResponse(response, returnCode);
    }

    private void stubForMockRequest(int returnCode) {
        stubFor(any(urlPathEqualTo("/")).willReturn(
                aResponse().withStatus(returnCode).withHeader("Some-Header", "With Value").withBody("hello")));
    }

    private void validateResponse(SdkHttpFullResponse response, int returnCode) throws IOException {
        verify(1, postRequestedFor(urlMatching("/"))
                .withHeader("Host", containing("localhost"))
                .withHeader("User-Agent", equalTo("hello-world!"))
                .withRequestBody(equalTo("Body")));

        assertThat(IoUtils.toUtf8String(response.content().orElse(null))).isEqualTo("hello");
        assertThat(response.firstMatchingHeader("Some-Header")).contains("With Value");
        assertThat(response.statusCode()).isEqualTo(returnCode);
        mockServer.resetMappings();
    }

    private SdkHttpFullRequest mockSdkRequest(String uriString) {
        URI uri = URI.create(uriString);
        return SdkHttpFullRequest.builder()
                                 .host(uri.getHost())
                                 .protocol(uri.getScheme())
                                 .port(uri.getPort())
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", uri.getHost())
                                 .putHeader("User-Agent", "hello-world!")
                                 .content(new ByteArrayInputStream("Body".getBytes(StandardCharsets.UTF_8)))
                                 .build();
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

    }
}
