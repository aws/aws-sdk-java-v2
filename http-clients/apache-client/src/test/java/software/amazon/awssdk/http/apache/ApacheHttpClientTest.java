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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @see ApacheHttpClientWireMockTest
 */
public class ApacheHttpClientTest {
    @After
    public void cleanup() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
    }

    @Test
    public void connectionReaperCanBeManuallyEnabled() {
        ApacheHttpClient.builder()
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
            ApacheHttpClient.builder()
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
            ApacheHttpClient.builder()
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

        ApacheHttpClient.builder()
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
            ApacheHttpClient.builder()
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
            ApacheHttpClient.builder()
                            .credentialsProvider(Mockito.mock(CredentialsProvider.class))
                            .build();
        }).isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void credentialProviderCanBeUsedWithProxy() {
        ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                                                           .endpoint(URI.create("http://localhost:1234"))
                                                           .build();
        ApacheHttpClient.builder()
                        .proxyConfiguration(proxyConfig)
                        .credentialsProvider(Mockito.mock(CredentialsProvider.class))
                        .build();
    }
}
