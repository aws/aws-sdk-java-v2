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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @see Apache5HttpClientWireMockTest
 */
public class Apache5HttpClientTest {
    @AfterEach
    public void cleanup() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
    }

    @Test
    public void connectionReaperCanBeManuallyEnabled() {
        Apache5HttpClient.builder()
                        .useIdleConnectionReaper(true)
                        .build()
                        .close();
    }

    @Test
    public void httpRoutePlannerCantBeUsedWithProxy() {
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                                                      .endpoint(URI.create("http://localhost:1234"))
                                                      .useSystemPropertyValues(Boolean.FALSE)
                                                      .build();
        assertThatThrownBy(() -> {
            Apache5HttpClient.builder()
                            .proxyConfiguration(proxyConfig)
                            .httpRoutePlanner(Mockito.mock(HttpRoutePlanner.class))
                            .build();
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void httpRoutePlannerCantBeUsedWithProxy_SystemPropertiesEnabled() {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "1234");

        assertThatThrownBy(() -> {
            Apache5HttpClient.builder()
                            .httpRoutePlanner(Mockito.mock(HttpRoutePlanner.class))
                            .build();
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void httpRoutePlannerCantBeUsedWithProxy_SystemPropertiesDisabled() {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "1234");

        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                                                           .useSystemPropertyValues(Boolean.FALSE)
                                                           .build();

        Apache5HttpClient.builder()
                        .proxyConfiguration(proxyConfig)
                        .httpRoutePlanner(Mockito.mock(HttpRoutePlanner.class))
                        .build();
    }

    @Test
    public void credentialProviderCantBeUsedWithProxyCredentials() {
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                                                           .endpoint(URI.create("http://localhost:1234"))
                                                           .username("foo")
                                                           .password("bar")
                                                           .build();
        assertThatThrownBy(() -> {
            Apache5HttpClient.builder()
                            .proxyConfiguration(proxyConfig)
                            .credentialsProvider(Mockito.mock(CredentialsProvider.class))
                            .build();
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void credentialProviderCantBeUsedWithProxyCredentials_SystemProperties() {
        System.setProperty("http.proxyUser", "foo");
        System.setProperty("http.proxyPassword", "bar");

        assertThatThrownBy(() -> {
            Apache5HttpClient.builder()
                            .credentialsProvider(Mockito.mock(CredentialsProvider.class))
                            .build();
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void credentialProviderCanBeUsedWithProxy() {
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                                                           .endpoint(URI.create("http://localhost:1234"))
                                                           .build();
        Apache5HttpClient.builder()
                        .proxyConfiguration(proxyConfig)
                        .credentialsProvider(Mockito.mock(CredentialsProvider.class))
                        .build();
    }

    @Test
    public void dnsResolverCanBeUsed() {
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
            @Override
            public InetAddress[] resolve(final String host) throws UnknownHostException {
                if (host.equalsIgnoreCase("my.host.com")) {
                    return new InetAddress[] { InetAddress.getByName("127.0.0.1") };
                } else {
                    return super.resolve(host);
                }
            }
        };

        Apache5HttpClient.builder()
                        .dnsResolver(dnsResolver)
                        .build()
                        .close();
    }
}
