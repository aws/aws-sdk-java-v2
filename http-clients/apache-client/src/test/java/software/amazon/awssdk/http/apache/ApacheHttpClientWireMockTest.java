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

package software.amazon.awssdk.http.apache;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.net.HttpURLConnection;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

@RunWith(MockitoJUnitRunner.class)
public class ApacheHttpClientWireMockTest extends SdkHttpClientTestSuite {
    @Rule
    public WireMockRule mockProxyServer = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Mock
    private ConnectionManagerAwareHttpClient httpClient;

    @Mock
    private HttpClientConnectionManager connectionManager;

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        ApacheHttpClient.Builder builder = ApacheHttpClient.builder();

        AttributeMap.Builder attributeMap = AttributeMap.builder();

        if (options.tlsTrustManagersProvider() != null) {
            builder.tlsTrustManagersProvider(options.tlsTrustManagersProvider());
        }

        if (options.trustAll()) {
            attributeMap.put(TRUST_ALL_CERTIFICATES, options.trustAll());
        }

        return builder.buildWithDefaults(attributeMap.build());
    }

    @Test
    public void closeClient_shouldCloseUnderlyingResources() {
        ApacheHttpClient client = new ApacheHttpClient(httpClient, ApacheHttpRequestConfig.builder().build(), AttributeMap.empty());
        when(httpClient.getHttpClientConnectionManager()).thenReturn(connectionManager);

        client.close();
        verify(connectionManager).shutdown();
    }

    @Test
    public void routePlannerIsInvoked() throws Exception {
        mockProxyServer.resetToDefaultMappings();
        mockProxyServer.addStubMapping(WireMock.any(urlPathEqualTo("/"))
                                               .willReturn(aResponse().proxiedFrom("http://localhost:" + mockServer.port()))
                                               .build());

        SdkHttpClient client = ApacheHttpClient.builder()
                                               .httpRoutePlanner(
                                                   (host, request, context) ->
                                                       new HttpRoute(
                                                           new HttpHost("localhost", mockProxyServer.httpsPort(), "https")
                                                       )
                                               )
                                               .buildWithDefaults(AttributeMap.builder()
                                                                              .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                              .build());

        testForResponseCodeUsingHttps(client, HttpURLConnection.HTTP_OK);

        mockProxyServer.verify(1, RequestPatternBuilder.allRequests());
    }

    @Test
    public void credentialPlannerIsInvoked() throws Exception {
        mockProxyServer.addStubMapping(WireMock.any(urlPathEqualTo("/"))
                                               .willReturn(aResponse()
                                                               .withHeader("WWW-Authenticate", "Basic realm=\"proxy server\"")
                                                               .withStatus(401))
                                               .build());

        mockProxyServer.addStubMapping(WireMock.any(urlPathEqualTo("/"))
                                               .withBasicAuth("foo", "bar")
                                               .willReturn(aResponse()
                                                               .proxiedFrom("http://localhost:" + mockServer.port()))
                                               .build());

        SdkHttpClient client = ApacheHttpClient.builder()
                                               .credentialsProvider(new CredentialsProvider() {
                                                   @Override
                                                   public void setCredentials(AuthScope authScope, Credentials credentials) {

                                                   }

                                                   @Override
                                                   public Credentials getCredentials(AuthScope authScope) {
                                                       return new UsernamePasswordCredentials("foo", "bar");
                                                   }

                                                   @Override
                                                   public void clear() {

                                                   }
                                               })
                                               .httpRoutePlanner(
                                                   (host, request, context) ->
                                                       new HttpRoute(
                                                           new HttpHost("localhost", mockProxyServer.httpsPort(), "https")
                                                       )
                                               )
                                               .buildWithDefaults(AttributeMap.builder()
                                                                              .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                              .build());
        testForResponseCodeUsingHttps(client, HttpURLConnection.HTTP_OK);

        mockProxyServer.verify(2, RequestPatternBuilder.allRequests());
    }
}
