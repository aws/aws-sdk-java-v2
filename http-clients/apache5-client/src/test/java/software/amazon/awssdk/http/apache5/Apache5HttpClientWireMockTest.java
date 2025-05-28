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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.CredentialsProviderBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.io.CloseMode;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache5.internal.Apache5HttpRequestConfig;
import software.amazon.awssdk.http.apache5.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

@RunWith(MockitoJUnitRunner.class)
public class Apache5HttpClientWireMockTest extends SdkHttpClientTestSuite {
    @Rule
    public WireMockRule mockProxyServer = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Mock
    private ConnectionManagerAwareHttpClient httpClient;

    @Mock
    private HttpClientConnectionManager connectionManager;

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        Apache5HttpClient.Builder builder = Apache5HttpClient.builder();

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
        Apache5HttpClient client = new Apache5HttpClient(httpClient, Apache5HttpRequestConfig.builder().build(),
                                                         AttributeMap.empty());
        when(httpClient.getHttpClientConnectionManager()).thenReturn(connectionManager);

        client.close();
        verify(connectionManager).close(CloseMode.IMMEDIATE);
    }

    @Test
    public void routePlannerIsInvoked() throws Exception {
        mockProxyServer.resetToDefaultMappings();
        mockProxyServer.addStubMapping(any(urlPathEqualTo("/"))
                                               .willReturn(aResponse().proxiedFrom("http://localhost:" + mockServer.port()))
                                               .build());

        SdkHttpClient client = Apache5HttpClient.builder()
                                                .httpRoutePlanner(
                                                    (request, context) ->
                                                        new HttpRoute(
                                                            new HttpHost("https", "localhost", mockProxyServer.httpsPort())
                                                        ))
                                                .buildWithDefaults(AttributeMap.builder()
                                                                               .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                               .build());

        testForResponseCodeUsingHttps(client, HttpURLConnection.HTTP_OK);

        mockProxyServer.verify(1, RequestPatternBuilder.allRequests());
    }

    @Test
    public void credentialPlannerIsInvoked() throws Exception {

        mockProxyServer.addStubMapping(any(urlPathEqualTo("/"))
                                               .willReturn(aResponse()
                                                               .withHeader("WWW-Authenticate", "Basic realm=\"proxy server\"")
                                                               .withStatus(401))
                                               .build());

        mockProxyServer.addStubMapping(any(urlPathEqualTo("/"))
                                               .withBasicAuth("foo", "bar")
                                               .willReturn(aResponse()
                                                               .proxiedFrom("http://localhost:" + mockServer.port()))
                                               .build());

        CredentialsProvider credentialsProvider = CredentialsProviderBuilder.create()
                                                                            .add(new AuthScope("localhost", -1),
                                                                                 new UsernamePasswordCredentials("foo", "bar".toCharArray()))
                                                                            .build();



        SdkHttpClient client = Apache5HttpClient.builder()
                                                .credentialsProvider(credentialsProvider)
                                                .httpRoutePlanner(
                                                    (request, context) ->
                                                        new HttpRoute(
                                                            new HttpHost("https", "localhost", mockProxyServer.httpsPort())

                                                        ))
                                                .buildWithDefaults(AttributeMap.builder()
                                                                               .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                               .build());
        testForResponseCodeUsingHttps(client, HttpURLConnection.HTTP_OK);

        mockProxyServer.verify(2, RequestPatternBuilder.allRequests());
    }


    @Override
    public void connectionPoolingWorks() throws Exception {
        // TODO : future PR will handle this.
    }

    @Test
    public void overrideDnsResolver_WithDnsMatchingResolver_successful() throws Exception {
        overrideDnsResolver("magic.local.host");
    }

    @Test(expected = UnknownHostException.class)
    public void overrideDnsResolver_WithUnknownHost_throwsException() throws Exception {
        overrideDnsResolver("sad.local.host");
    }

    @Test
    public void overrideDnsResolver_WithLocalhost_successful() throws Exception {
        overrideDnsResolver("localhost");
    }

    @Test
    public void explicitNullDnsResolver_WithLocalhost_successful() throws Exception {
        overrideDnsResolver("localhost", true);
    }

    private void overrideDnsResolver(String hostName) throws IOException {
        overrideDnsResolver(hostName, false);
    }

    private void overrideDnsResolver(String hostName, boolean nullifyResolver) throws IOException {

        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                if ("magic.local.host".equalsIgnoreCase(host)) {
                    return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
                }
                return super.resolve(host);
            }
        };
        if (nullifyResolver) {
            dnsResolver = null;
        }

        SdkHttpClient client = Apache5HttpClient.builder()
                                                .dnsResolver(dnsResolver)
                                                .buildWithDefaults(AttributeMap.builder()
                                                                               .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                               .build());

        mockProxyServer.resetToDefaultMappings();
        mockProxyServer.stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpURLConnection.HTTP_OK)));

        URI uri = URI.create("https://" + hostName + ":" + mockProxyServer.httpsPort());
        SdkHttpFullRequest req = SdkHttpFullRequest.builder()
                                                   .uri(uri)
                                                   .method(SdkHttpMethod.POST)
                                                   .putHeader("Host", uri.getHost())
                                                   .build();

        client.prepareRequest(HttpExecuteRequest.builder()
                                                .request(req)
                                                .contentStreamProvider(req.contentStreamProvider().orElse(null))
                                                .build())
              .call();

        mockProxyServer.verify(1, RequestPatternBuilder.allRequests());
    }
}
