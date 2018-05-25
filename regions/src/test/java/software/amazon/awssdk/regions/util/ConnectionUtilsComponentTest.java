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

package software.amazon.awssdk.regions.util;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URI;
import java.util.Collections;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class ConnectionUtilsComponentTest {

    @ClassRule
    public static WireMockRule mockProxyServer = new WireMockRule(WireMockConfiguration.wireMockConfig().port(0), false);

    @Rule
    public WireMockRule mockServer = new WireMockRule(WireMockConfiguration.wireMockConfig().port(0), false);

    private final ConnectionUtils sut = ConnectionUtils.create();

    @After
    public void cleanup() {
        System.getProperties().remove("http.proxyHost");
        System.getProperties().remove("http.proxyPort");
    }

    @Test
    public void proxiesAreNotUsedEvenIfPropertyIsSet() throws IOException {
        assumeTrue(Inet4Address.getLocalHost().isReachable(100));
        System.getProperties().put("http.proxyHost", "localhost");
        System.getProperties().put("http.proxyPort", String.valueOf(mockProxyServer.port()));
        HttpURLConnection connection = sut.connectToEndpoint(URI.create("http://" + Inet4Address.getLocalHost().getHostAddress() + ":" + mockServer.port()), emptyMap());

        assertThat(connection.usingProxy()).isFalse();
    }

    @Test
    public void headersArePassedAsPartOfRequest() throws IOException {
        HttpURLConnection connection = sut.connectToEndpoint(URI.create("http://localhost:" + mockServer.port()), Collections.singletonMap("HeaderA", "ValueA"));
        connection.getResponseCode();
        mockServer.verify(WireMock.getRequestedFor(WireMock.urlMatching("/")).withHeader("HeaderA", WireMock.equalTo("ValueA")));
    }

    @Test
    public void shouldNotFollowRedirects() throws IOException {
        mockServer.stubFor(WireMock.get(WireMock.urlMatching("/")).willReturn(WireMock.aResponse().withStatus(301).withHeader("Location", "http://localhost:" + mockServer.port() + "/hello")));
        HttpURLConnection connection = sut.connectToEndpoint(URI.create("http://localhost:" + mockServer.port()), Collections.emptyMap());
        assertThat(connection.getResponseCode()).isEqualTo(301);
    }
}