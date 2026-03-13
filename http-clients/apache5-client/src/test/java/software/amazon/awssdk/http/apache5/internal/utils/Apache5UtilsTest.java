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

package software.amazon.awssdk.http.apache5.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.apache5.ProxyConfiguration;

public class Apache5UtilsTest {
    private static final AuthScope AUTH_SCOPE = new AuthScope("localhost", 8080);

    @Test
    public void proxyCredentials_ntlmDetailsNotPresent_usesUsernameAndPassword() {
        ProxyConfiguration config =
            ProxyConfiguration.builder().username("name").password("pass").endpoint(URI.create("localhost:8080")).build();

        assertThat(Apache5Utils.newProxyCredentialsProvider(config).getCredentials(AUTH_SCOPE, null))
            .isInstanceOf(UsernamePasswordCredentials.class);
    }

    @Test
    public void proxyCredentials_ntlmWorkstationPresent_usesNtCredentials() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .username("name")
                                                      .password("pass")
                                                      .ntlmWorkstation("workstation")
                                                      .endpoint(URI.create("localhost:8080")).build();

        assertThat(Apache5Utils.newProxyCredentialsProvider(config).getCredentials(AUTH_SCOPE, null))
            .isInstanceOf(NTCredentials.class);
    }

    @Test
    public void proxyCredentials_ntlmDomainPresent_usesNtCredentials() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .username("name")
                                                      .password("pass")
                                                      .ntlmDomain("domain")
                                                      .endpoint(URI.create("localhost:8080")).build();

        assertThat(Apache5Utils.newProxyCredentialsProvider(config).getCredentials(AUTH_SCOPE, null))
            .isInstanceOf(NTCredentials.class);
    }
}
