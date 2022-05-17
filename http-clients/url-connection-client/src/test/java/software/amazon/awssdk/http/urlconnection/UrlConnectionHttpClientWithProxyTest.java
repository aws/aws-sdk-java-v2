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

package software.amazon.awssdk.http.urlconnection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.requestMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.anyString;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.AttributeMap;

class UrlConnectionHttpClientWithProxyTest {
    @RegisterExtension
    static WireMockExtension httpsWm = WireMockExtension.newInstance()
                                                        .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
                                                        .configureStaticDsl(true)
                                                        .build();
    @RegisterExtension
    static WireMockExtension httpWm = WireMockExtension.newInstance()
                                                       .options(wireMockConfig().dynamicPort())
                                                       .proxyMode(true)
                                                       .build();
    private SdkHttpClient client;

    @Test
    void Http_ProxyCallFrom_Https_Client_getsRejectedWith_404() throws IOException {
        httpWm.stubFor(requestMatching(
            request -> MatchResult.of(request.getUrl().contains(anyString()))
        ).willReturn(aResponse()));
        WireMockRuntimeInfo wireMockRuntimeInfoHttp = httpsWm.getRuntimeInfo();
        client = createHttpsClientForHttpServer(
            ProxyConfiguration.builder()
                              .endpoint(URI.create(httpWm.getRuntimeInfo().getHttpBaseUrl()))
                              .build());
        HttpExecuteResponse httpExecuteResponse = makeRequestWithHttps_Client(client, wireMockRuntimeInfoHttp);
        Assertions.assertThat(httpExecuteResponse.httpResponse().statusCode()).isEqualTo(404);
    }

    @Test
    void Http_ProxyCallFromWithDenyList_HttpsClient_bypassesProxy_AndReturns_OK() throws IOException {
        httpWm.stubFor(requestMatching(
            request -> MatchResult.of(request.getUrl().contains(anyString()))
        ).willReturn(aResponse()));
        WireMockRuntimeInfo wireMockRuntimeInfoHttp = httpsWm.getRuntimeInfo();
        Set<String> nonProxyHost = new HashSet<>();
        nonProxyHost.add(httpWm.getRuntimeInfo().getHttpBaseUrl());
        client = createHttpsClientForHttpServer(
            ProxyConfiguration.builder()
                              .endpoint(URI.create(httpWm.getRuntimeInfo().getHttpBaseUrl()))
                              .nonProxyHosts(nonProxyHost)
                              .build());
        HttpExecuteResponse httpExecuteResponse = makeRequestWithHttps_Client(client, wireMockRuntimeInfoHttp);
        Assertions.assertThat(httpExecuteResponse.httpResponse().statusCode()).isEqualTo(200);
    }

    @Test
    void emptyProxyConfig_Https_Client_byPassesProxy_dReturns_OK() throws IOException {
        httpWm.stubFor(requestMatching(
            request -> MatchResult.of(request.getUrl().contains(anyString()))
        ).willReturn(aResponse()));
        httpsWm.stubFor(requestMatching(
            request -> MatchResult.of(request.getUrl().contains(anyString()))
        ).willReturn(aResponse()));
        WireMockRuntimeInfo wireMockRuntimeInfoHttp = httpsWm.getRuntimeInfo();
        client = createHttpsClientForHttpServer(
            ProxyConfiguration.builder().build());
        HttpExecuteResponse httpExecuteResponse = makeRequestWithHttps_Client(client, wireMockRuntimeInfoHttp);
        Assertions.assertThat(httpExecuteResponse.httpResponse().statusCode()).isEqualTo(200);
    }

    @Test
    void http_ProxyCallFrom_Http_Client_isAcceptedByHttpProxy_AndReturns_OK() throws IOException {

        httpWm.stubFor(requestMatching(
            request -> MatchResult.of(request.getUrl().contains(anyString()))
        ).willReturn(aResponse()));

        httpsWm.stubFor(requestMatching(
            request -> MatchResult.of(request.getUrl().contains(anyString()))
        ).willReturn(aResponse()));

        client = createHttpsClientForHttpServer(
            ProxyConfiguration.builder()
                              .endpoint(URI.create(httpWm.getRuntimeInfo().getHttpBaseUrl()))
                              .build());
        HttpExecuteResponse httpExecuteResponse = makeRequestWithHttp_Client(client, httpWm.getRuntimeInfo());
        Assertions.assertThat(httpExecuteResponse.httpResponse().statusCode()).isEqualTo(200);
    }

    private HttpExecuteResponse makeRequestWithHttps_Client(SdkHttpClient httpClient, WireMockRuntimeInfo wm) throws IOException {
        SdkHttpRequest httpRequest = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .protocol("https")
                                                       .host("localhost:" + wm.getHttpsPort())
                                                       .build();

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();

        return httpClient.prepareRequest(request).call();
    }

    private HttpExecuteResponse makeRequestWithHttp_Client(SdkHttpClient httpClient, WireMockRuntimeInfo wm) throws IOException {
        SdkHttpRequest httpRequest = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .protocol("http")
                                                       .host(wm.getHttpBaseUrl())
                                                       .build();

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .build();

        return httpClient.prepareRequest(request).call();
    }

    private SdkHttpClient createHttpsClientForHttpServer(ProxyConfiguration proxyConfiguration) {

        UrlConnectionHttpClient.Builder builder =
            UrlConnectionHttpClient.builder().proxyConfiguration(proxyConfiguration);
        AttributeMap.Builder attributeMap = AttributeMap.builder();
        attributeMap.put(TRUST_ALL_CERTIFICATES, true);
        return builder.buildWithDefaults(attributeMap.build());
    }
}
