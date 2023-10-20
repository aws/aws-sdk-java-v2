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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.http.HttpProxyTestSuite;
import software.amazon.awssdk.http.proxy.TestProxySetting;

public class UrlProxyConfigurationTest extends HttpProxyTestSuite {
    @Override
    protected void assertProxyConfiguration(TestProxySetting userSetProxySettings, TestProxySetting expectedProxySettings,
                                            Boolean useSystemProperty, Boolean useEnvironmentVariable, String protocol) throws URISyntaxException {
        ProxyConfiguration.Builder builder = ProxyConfiguration.builder();

        if (userSetProxySettings != null) {
            String hostName = userSetProxySettings.getHost();
            Integer portNumber = userSetProxySettings.getPort();
            String userName = userSetProxySettings.getUserName();
            String password = userSetProxySettings.getPassword();
            Set<String> nonProxyHosts = userSetProxySettings.getNonProxyHosts();

            if (hostName != null && portNumber != null) {
                builder.endpoint(URI.create(String.format("%s://%s:%d", protocol, hostName, portNumber)));
            }
            Optional.ofNullable(userName).ifPresent(builder::username);
            Optional.ofNullable(password).ifPresent(builder::password);
            if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
                builder.nonProxyHosts(nonProxyHosts);
            }
        }
        if (!"http".equals(protocol)) {
            builder.scheme(protocol);
        }
        if (useSystemProperty != null) {
            builder.useSystemPropertyValues(useSystemProperty);
        }
        if (useEnvironmentVariable != null) {
            builder.useEnvironmentVariablesValues(useEnvironmentVariable);
        }
        ProxyConfiguration proxyConfiguration = builder.build();
        assertThat(proxyConfiguration.host()).isEqualTo(expectedProxySettings.getHost());
        assertThat(proxyConfiguration.port()).isEqualTo(expectedProxySettings.getPort());
        assertThat(proxyConfiguration.username()).isEqualTo(expectedProxySettings.getUserName());
        assertThat(proxyConfiguration.password()).isEqualTo(expectedProxySettings.getPassword());
        assertThat(proxyConfiguration.nonProxyHosts()).isEqualTo(expectedProxySettings.getNonProxyHosts());

    }
}
