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
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.Collections;
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
import software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpClientFactory;
import software.amazon.awssdk.utils.IoUtils;

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
        SdkHttpFullRequest request = mock(SdkHttpFullRequest.class);
        when(request.getEndpoint()).thenReturn(uri);
        when(request.getHttpMethod()).thenReturn(SdkHttpMethod.GET);
        when(request.getResourcePath()).thenReturn("/");
        when(request.getParameters()).thenReturn(Collections.emptyMap());
        when(request.getHeaders()).thenReturn(Collections.singletonMap("Host", Collections.singletonList(uri.getHost())));

        SdkHttpFullResponse response = client.prepareRequest(request, requestContext).call();

        verify(1, getRequestedFor(urlMatching("/")).withHeader("Host", containing("localhost")));
        assertThat(IoUtils.toString(response.getContent())).isEqualTo("hello");
        assertThat(response.getFirstHeaderValue("Some-Header")).contains("With Value");
    }
}
