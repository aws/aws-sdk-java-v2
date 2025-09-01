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


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicSchemeFactory;
import org.apache.hc.client5.http.impl.auth.KerberosSchemeFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;


class Apache5HttpClientAuthRegistryTest {

    @RegisterExtension
    static WireMockExtension proxyWireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort())
                                                         .build();
    @RegisterExtension
    static WireMockExtension serverWireMock = WireMockExtension.newInstance()
                                                              .options(wireMockConfig().dynamicPort())
                                                              .build();

    private Apache5HttpClient httpClient;
    private static final String PROXY_AUTH_SCENARIO = "Proxy Auth";
    private static final String SERVER_AUTH_SCENARIO = "Server Auth";
    private static final String CHALLENGED_STATE = "Challenged";
    private static final String BASIC_AUTH = "Basic";
    private static final String KERBEROS_AUTH = "Kerberos";


    private Registry<AuthSchemeFactory> createAuthSchemeRegistry(String scheme, AuthSchemeFactory factory) {
        return RegistryBuilder.<AuthSchemeFactory>create()
                              .register(scheme, factory)
                              .build();
    }


    private Apache5HttpClient createHttpClient(Registry<AuthSchemeFactory> authSchemeRegistry) {
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
            new AuthScope("localhost", -1),
            new UsernamePasswordCredentials("u1", "p1".toCharArray()));

        return (Apache5HttpClient) Apache5HttpClient.builder()
                                                    .proxyConfiguration(ProxyConfiguration.builder().endpoint(URI.create("http://localhost:" + proxyWireMock.getPort()))
                                                                                          .build())
                                                    .authSchemeRegistry(authSchemeRegistry)
                                                    .credentialsProvider(credsProvider)
                                                    .build();
    }


    private SdkHttpRequest createHttpRequest() {
        return SdkHttpRequest.builder()
                             .uri(URI.create("http://localhost:" + serverWireMock.getPort()))
                             .method(SdkHttpMethod.GET)
                             .build();
    }
    private void setupProxyWireMockStub() {
        proxyWireMock.stubFor(get(urlMatching(".*"))
                             .inScenario(PROXY_AUTH_SCENARIO)
                             .whenScenarioStateIs(STARTED)
                             .willReturn(aResponse()
                                             .withStatus(401)
                                             .withHeader("WWW-Authenticate", "Basic"))
                             .willSetStateTo(CHALLENGED_STATE));

        proxyWireMock.stubFor(get(urlMatching(".*"))
                                  .inScenario(PROXY_AUTH_SCENARIO)
                                  .whenScenarioStateIs(CHALLENGED_STATE)
                                  //.withHeader("WWW-Authenticate", matching(".*"))
                                  .willReturn(aResponse()
                                                  .withStatus(200))
                                  .willSetStateTo("success"));
    }

    private void setupWireMockStub() {
        serverWireMock.stubFor(get(urlMatching(".*"))
                             .inScenario(SERVER_AUTH_SCENARIO)
                             .whenScenarioStateIs(STARTED)
                                   .withHeader("Authorization", matching(".*"))
                             .willReturn(aResponse().withStatus(200)));
    }

    private HttpExecuteResponse executeRequest(SdkHttpRequest request) throws Exception {
        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                                                              .request(request)
                                                              .build();
        ExecutableHttpRequest executableRequest = httpClient.prepareRequest(executeRequest);
        return executableRequest.call();
    }

    @Test
    void authSchemeRegistryConfigured_registeredAuthShouldPass() throws Exception {
        Registry<AuthSchemeFactory> authSchemeRegistry = createAuthSchemeRegistry(
            BASIC_AUTH,
            new BasicSchemeFactory()
        );



        httpClient = createHttpClient(authSchemeRegistry);
        setupProxyWireMockStub();
        setupWireMockStub();

        HttpExecuteResponse response = executeRequest(createHttpRequest());

        proxyWireMock.verify(1, getRequestedFor(urlMatching(".*"))
            .withHeader("Authorization", matching(".*"))
        );
    }

    @Test
    void authSchemeRegistryConfigured_unRegisteredAuthShouldWarn() throws Exception {
        Registry<AuthSchemeFactory> authSchemeRegistry = createAuthSchemeRegistry(
                  KERBEROS_AUTH,
            KerberosSchemeFactory.DEFAULT
        );


        httpClient = createHttpClient(authSchemeRegistry);
        setupProxyWireMockStub();
        setupWireMockStub();

        HttpExecuteResponse response = executeRequest(createHttpRequest());
        proxyWireMock.verify(0, getRequestedFor(urlMatching(".*"))
            .withHeader("Authorization", matching(".*"))
        );
    }
}
