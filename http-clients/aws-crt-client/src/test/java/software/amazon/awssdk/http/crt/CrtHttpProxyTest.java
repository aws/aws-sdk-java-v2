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

package software.amazon.awssdk.http.crt;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import software.amazon.awssdk.http.HttpProxyTestSuite;
import software.amazon.awssdk.http.proxy.TestProxySetting;

public class CrtHttpProxyTest extends HttpProxyTestSuite {
    @Override
    protected void assertProxyConfiguration(TestProxySetting userSetProxySettings,
                                            TestProxySetting expectedProxySettings,
                                            Boolean useSystemProperty, Boolean useEnvironmentVariable,
                                            String protocol) throws URISyntaxException {

        ProxyConfiguration.Builder proxyBuilder = ProxyConfiguration.builder();

        if (userSetProxySettings != null) {
            String hostName = userSetProxySettings.getHost();
            Integer portNumber = userSetProxySettings.getPort();
            String userName = userSetProxySettings.getUserName();
            String password = userSetProxySettings.getPassword();

            if (hostName != null) {
                proxyBuilder.host(hostName);
            }
            if (portNumber != null) {
                proxyBuilder.port(portNumber);
            }
            if (userName != null) {
                proxyBuilder.username(userName);
            }
            if (password != null) {
                proxyBuilder.password(password);
            }
        }

        if (!"http".equals(protocol)) {
            proxyBuilder.scheme(protocol);
        }
        if (useSystemProperty != null) {
            proxyBuilder.useSystemPropertyValues(useSystemProperty);
        }
        if (useEnvironmentVariable != null) {
            proxyBuilder.useEnvironmentVariableValues(useEnvironmentVariable);
        }
        ProxyConfiguration proxyConfiguration = proxyBuilder.build();
        assertThat(proxyConfiguration.host()).isEqualTo(expectedProxySettings.getHost());
        assertThat(proxyConfiguration.port()).isEqualTo(expectedProxySettings.getPort());
        assertThat(proxyConfiguration.username()).isEqualTo(expectedProxySettings.getUserName());
        assertThat(proxyConfiguration.password()).isEqualTo(expectedProxySettings.getPassword());
    }

}
