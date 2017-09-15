/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.http.urlconnection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@RunWith(MockitoJUnitRunner.class)
public final class UrlConnectionHttpClientIntegrationTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    private static SdkHttpClient client = UrlConnectionSdkHttpClientFactory.builder()
                                                                           .build()
                                                                           .createHttpClient();

    @Mock
    private SdkRequestContext requestContext;

    @Test
    public void canMakeBasicRequests() throws Exception {

        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withHeader("Some-Header", "With Value").withBody("hello")));

        URI uri = URI.create("http://localhost:" + mockServer.port());
        SdkHttpFullRequest request = mockSdkRequest(uri);

        SdkHttpFullResponse response = client.prepareRequest(request, requestContext).call();

        verify(1, getRequestedFor(urlMatching("/"))
            .withHeader("Host", containing("localhost"))
            .withHeader("User-Agent", containing("hello")));
        assertThat(IoUtils.toString(response.content().orElse(null))).isEqualTo("hello");
        assertThat(SdkHttpUtils.firstMatchingHeader(response.headers(), "Some-Header")).contains("With Value");
        assertThat(SdkHttpUtils.firstMatchingHeader(response.headers(), "Some-Header")).contains("With Value");
    }

    @Test
    public void canGetResponsesForAllResponseCodes() throws Exception {
        testForResponseCode(HttpURLConnection.HTTP_OK);
        testForResponseCode(HttpURLConnection.HTTP_ACCEPTED);
        testForResponseCode(HttpURLConnection.HTTP_FORBIDDEN);
        testForResponseCode(HttpURLConnection.HTTP_MOVED_PERM);
        testForResponseCode(HttpURLConnection.HTTP_MOVED_TEMP);
        testForResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
    }

    private void testForResponseCode(int returnCode) throws Exception {
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withStatus(returnCode).withBody("response")));

        URI uri = URI.create("http://localhost:" + mockServer.port());
        SdkHttpFullRequest request = mockSdkRequest(uri);

        SdkHttpFullResponse response = client.prepareRequest(request, requestContext).call();

        verify(1, getRequestedFor(urlMatching("/")).withHeader("Host", containing("localhost")));
        assertThat(IoUtils.toString(response.content().orElse(null))).isEqualTo("response");
        assertThat(response.statusCode()).isEqualTo(returnCode);
        mockServer.resetMappings();
    }

    private SdkHttpFullRequest mockSdkRequest(URI uri) {
        return SdkHttpFullRequest.builder()
                                 .host(uri.getHost())
                                 .protocol(uri.getScheme())
                                 .port(uri.getPort())
                                 .method(SdkHttpMethod.GET)
                                 .header("Host", uri.getHost())
                                 .header("User-Agent", "hello-world!")
                                 .build();
    }
}
