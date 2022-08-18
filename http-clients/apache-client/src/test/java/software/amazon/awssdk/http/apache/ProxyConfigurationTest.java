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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;

public class ProxyConfigurationTest {

    @BeforeEach
    public void setup() {
        clearProxyProperties();
        clearProxyEnvironmentVariables();
    }

    @AfterAll
    public static void cleanup() {
        clearProxyProperties();
        clearProxyEnvironmentVariables();
    }

    @Test
    public void testEndpointValues_SystemPropertyEnabled() {
        String host = "foo.com";
        int port = 7777;
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", Integer.toString(port));

        ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(true).build();

        assertThat(config.host()).isEqualTo(host);
        assertThat(config.port()).isEqualTo(port);
        assertThat(config.scheme()).isEqualTo("http");
    }

    @Test
    public void testEndpointValues_SystemPropertyDisabled() {
        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .endpoint(URI.create("http://localhost:1234"))
                                                      .useSystemPropertyValues(Boolean.FALSE)
                                                      .build();

        assertThat(config.host()).isEqualTo("localhost");
        assertThat(config.port()).isEqualTo(1234);
        assertThat(config.scheme()).isEqualTo("http");
    }

    @Test
    public void testProxyConfigurationWithSystemPropertyDisabled() throws Exception {
        Set<String> nonProxyHosts = new HashSet<>();
        nonProxyHosts.add("foo.com");

        // system property should not be used
        System.setProperty("http.proxyHost", "foo.com");
        System.setProperty("http.proxyPort", "5555");
        System.setProperty("http.nonProxyHosts", "bar.com");
        System.setProperty("http.proxyUser", "user");

        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .endpoint(URI.create("http://localhost:1234"))
                                                      .nonProxyHosts(nonProxyHosts)
                                                      .useSystemPropertyValues(Boolean.FALSE)
                                                      .build();

        assertThat(config.host()).isEqualTo("localhost");
        assertThat(config.port()).isEqualTo(1234);
        assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHosts);
        assertThat(config.username()).isNull();
    }

    @Test
    public void testProxyConfigurationWithSystemPropertyEnabled() throws Exception {
        Set<String> nonProxyHosts = new HashSet<>();
        nonProxyHosts.add("foo.com");

        // system property should not be used
        System.setProperty("http.proxyHost", "foo.com");
        System.setProperty("http.proxyPort", "5555");
        System.setProperty("http.nonProxyHosts", "bar.com");
        System.setProperty("http.proxyUser", "user");

        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .nonProxyHosts(nonProxyHosts)
                                                      .build();

        assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHosts);
        assertThat(config.host()).isEqualTo("foo.com");
        assertThat(config.username()).isEqualTo("user");
    }

    @Test
    public void testProxyConfigurationWithoutNonProxyHosts_toBuilder_shouldNotThrowNPE() {
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .endpoint(URI.create("http://localhost:4321"))
                              .username("username")
                              .password("password")
                              .build();

        assertThat(proxyConfiguration.toBuilder()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http_proxy", "https_proxy"})
    public void testSystemPropertiesOverrideEnvironmentVariables(String proxyEnvVariable) {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(proxyEnvVariable, "https://user1:password1@env.com:4444");
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("no_proxy", "env.com");

        System.setProperty("http.proxyHost", "foo.com");
        System.setProperty("http.proxyPort", "5555");
        System.setProperty("http.nonProxyHosts", "bar.com");
        System.setProperty("http.proxyUser", "user");
        System.setProperty("http.proxyPassword", "password");

        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .useSystemPropertyValues(true)
                              .useEnvironmentVariables(true)
                              .build();

        assertThat(proxyConfiguration.nonProxyHosts()).isEqualTo(Collections.singleton("bar.com"));
        assertThat(proxyConfiguration.scheme()).isEqualTo("http");
        assertThat(proxyConfiguration.host()).isEqualTo("foo.com");
        assertThat(proxyConfiguration.port()).isEqualTo(5555);
        assertThat(proxyConfiguration.username()).isEqualTo("user");
        assertThat(proxyConfiguration.password()).isEqualTo("password");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http_proxy", "https_proxy"})
    public void testProxyConfigurationWithProxyEnvironmentVariable(String proxyEnvVariable) {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(proxyEnvVariable, "http://1.2.3.4:8080");
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .useEnvironmentVariables(true)
                              .build();

        assertThat(proxyConfiguration.scheme()).isEqualTo("http");
        assertThat(proxyConfiguration.host()).isEqualTo("1.2.3.4");
        assertThat(proxyConfiguration.port()).isEqualTo(8080);
        assertThat(proxyConfiguration.username()).isEqualTo(null);
        assertThat(proxyConfiguration.password()).isEqualTo(null);
    }

    @Test
    public void testProxyConfigurationWithBothEnvironmentVariables() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("https_proxy", "https://1.2.3.4:8080");
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("http_proxy", "http://5.6.7.8:9090");
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .useEnvironmentVariables(true)
                              .build();

        // https_proxy should be preferred over http_proxy
        assertThat(proxyConfiguration.scheme()).isEqualTo("https");
        assertThat(proxyConfiguration.host()).isEqualTo("1.2.3.4");
        assertThat(proxyConfiguration.port()).isEqualTo(8080);
        assertThat(proxyConfiguration.username()).isEqualTo(null);
        assertThat(proxyConfiguration.password()).isEqualTo(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http_proxy", "https_proxy"})
    public void testProxyConfigurationWithMalformedEnvironmentVariable(String proxyEnvVariable) {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(proxyEnvVariable, "noprotocol.com:8080");
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .useEnvironmentVariables(true)
                              .build();

        // no proxy should be set
        assertThat(proxyConfiguration.scheme()).isEqualTo(null);
        assertThat(proxyConfiguration.nonProxyHosts()).isEqualTo(Collections.emptySet());
        assertThat(proxyConfiguration.host()).isEqualTo(null);
        assertThat(proxyConfiguration.port()).isEqualTo(0);
        assertThat(proxyConfiguration.username()).isEqualTo(null);
        assertThat(proxyConfiguration.password()).isEqualTo(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http_proxy", "https_proxy"})
    public void testProxyConfigurationWithEnvironmentVariableWithUsernamePassword(String proxyEnvVariable) {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(proxyEnvVariable, "https://username:password@1.2.3.4:8080");
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .useEnvironmentVariables(true)
                              .build();

        assertThat(proxyConfiguration.scheme()).isEqualTo("https");
        assertThat(proxyConfiguration.host()).isEqualTo("1.2.3.4");
        assertThat(proxyConfiguration.port()).isEqualTo(8080);
        assertThat(proxyConfiguration.username()).isEqualTo("username");
        assertThat(proxyConfiguration.password()).isEqualTo("password");
    }

    @Test
    public void testProxyConfigurationWithNonProxyHostsEnvironmentVariable() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("no_proxy", "test.com,bar.com,foo.com,1.2.3.4");
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .useEnvironmentVariables(true)
                              .build();

        Set<String> expectedNonProxyHosts = new HashSet<>();
        expectedNonProxyHosts.add("test.com");
        expectedNonProxyHosts.add("bar.com");
        expectedNonProxyHosts.add("foo.com");
        expectedNonProxyHosts.add("1.2.3.4");
        assertThat(proxyConfiguration.nonProxyHosts()).isEqualTo(expectedNonProxyHosts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http_proxy", "https_proxy", "no_proxy"})
    public void testProxyConfigurationWithEmptyEnvironmentVariables(String envProxyVariable) {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(envProxyVariable, "");
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .useEnvironmentVariables(true)
                              .build();

        assertThat(proxyConfiguration.scheme()).isEqualTo(null);
        assertThat(proxyConfiguration.host()).isEqualTo(null);
        assertThat(proxyConfiguration.port()).isEqualTo(0);
        assertThat(proxyConfiguration.username()).isEqualTo(null);
        assertThat(proxyConfiguration.password()).isEqualTo(null);
        assertThat(proxyConfiguration.nonProxyHosts()).isEqualTo(Collections.emptySet());
    }

    @Test
    public void testExplicitConfigurationOverridesEnvironment() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("https_proxy", "http://username:password@proxy.com:8080");
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("http_proxy", "http://username:password@proxy.com:8080");
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("no_proxy", "foo.com");
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .endpoint(URI.create("https://explicit.com:9999"))
                              .username("explicit-username")
                              .password("explicit-password")
                              .nonProxyHosts(Collections.singleton("bar.com"))
                              .useEnvironmentVariables(true)
                              .build();

        assertThat(proxyConfiguration.scheme()).isEqualTo("https");
        assertThat(proxyConfiguration.host()).isEqualTo("explicit.com");
        assertThat(proxyConfiguration.port()).isEqualTo(9999);
        assertThat(proxyConfiguration.username()).isEqualTo("explicit-username");
        assertThat(proxyConfiguration.password()).isEqualTo("explicit-password");
        assertThat(proxyConfiguration.nonProxyHosts()).isEqualTo(Collections.singleton("bar.com"));
    }

    private static void clearProxyEnvironmentVariables() {
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    private static void clearProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
    }
}
