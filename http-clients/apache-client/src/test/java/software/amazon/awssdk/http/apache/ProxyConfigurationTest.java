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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;

public class ProxyConfigurationTest {
    @BeforeEach
    public void setup() {
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
        clearProxyProperties();
    }

    @AfterAll
    public static void cleanup() {
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
        clearProxyProperties();
    }

    @Test
    void testEndpointValues_Http_SystemPropertyEnabled() {
        String host = "foo.com";
        int port = 7777;
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", Integer.toString(port));

        ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(true).build();

        assertThat(config.host("http")).isEqualTo(host);
        assertThat(config.port("http")).isEqualTo(port);
        assertThat(config.scheme("http")).isEqualTo("http");
    }

    @Test
    void testEndpointValues_Https_SystemPropertyEnabled() {
        String host = "foo.com";
        int port = 7777;
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", Integer.toString(port));

        ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(true).build();
        assertThat(config.host("https")).isEqualTo(host);
        assertThat(config.port("https")).isEqualTo(port);
        assertThat(config.scheme("https")).isEqualTo("http");
    }

    @Test
    void testEndpointValues_SystemPropertyDisabled() {
        String host = "foo.com";
        int port = 7777;
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", Integer.toString(port));
        ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(Boolean.FALSE).build();

        assertNull(config.host("http"));
        assertEquals(0, config.port("http"));
        assertNull(config.scheme("http"));
    }

    @Test
    void testProxyConfigurationWithSystemPropertyDisabled() throws Exception {
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

        assertThat(config.host("http")).isEqualTo("localhost");
        assertThat(config.port("http")).isEqualTo(1234);
        assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHosts);
        assertThat(config.username("http")).isNull();
    }

    @Test
    void testProxyConfigurationWithSystemPropertyEnabled_Http() throws Exception {
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
        assertThat(config.host("http")).isEqualTo("foo.com");
        assertThat(config.username("http")).isEqualTo("user");
    }

    @Test
    void testProxyConfigurationWithSystemPropertyEnabled_Https() throws Exception {
        Set<String> nonProxyHosts = new HashSet<>();
        nonProxyHosts.add("foo.com");

        // system property should not be used
        System.setProperty("https.proxyHost", "foo.com");
        System.setProperty("https.proxyPort", "5555");
        System.setProperty("http.nonProxyHosts", "bar.com");
        System.setProperty("https.proxyUser", "user");

        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .endpoint(URI.create("https://foo.com:1234"))
                                                      .nonProxyHosts(nonProxyHosts)
                                                      .build();

        assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHosts);
        assertThat(config.host("https")).isEqualTo("foo.com");
        assertThat(config.username("https")).isEqualTo("user");
    }

    @Test
    void testProxyConfigurationWithoutNonProxyHosts_toBuilder_shouldNotThrowNPE() {
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .endpoint(URI.create("http://localhost:4321"))
                              .username("username")
                              .password("password")
                              .build();

        assertThat(proxyConfiguration.toBuilder()).isNotNull();
    }

    @Test
    void testExplicitEndpointOverridesEnvironmentVariables() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "http_proxy",
            "https://user:pass@localhost:25565/"
        );
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "https_proxy",
            "https://user:pass@localhost:25566/"
        );
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .endpoint(URI.create("http://example.com:4321"))
                              .username("mycooluser")
                              .password("mycoolpass")
                              .build();

        assertThat(proxyConfiguration.host("http")).isEqualTo("example.com");
        assertThat(proxyConfiguration.host("https")).isEqualTo("example.com");
        assertThat(proxyConfiguration.port("http")).isEqualTo(4321);
        assertThat(proxyConfiguration.port("https")).isEqualTo(4321);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("http");
        assertThat(proxyConfiguration.scheme("https")).isEqualTo("http");
        assertThat(proxyConfiguration.username("http")).isEqualTo("mycooluser");
        assertThat(proxyConfiguration.username("https")).isEqualTo("mycooluser");
        assertThat(proxyConfiguration.password("http")).isEqualTo("mycoolpass");
        assertThat(proxyConfiguration.password("https")).isEqualTo("mycoolpass");
    }

    @Test
    void testExplicitPropertiesOverridesEnvironmentVariables() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "http_proxy",
            "https://user:pass@localhost:25565/"
        );
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "https_proxy",
            "https://user:pass@localhost:25566/"
        );
        System.setProperty("http.proxyHost", "example.com");
        System.setProperty("http.proxyPort", "4321");
        System.setProperty("http.proxyUser", "mycooluser");
        System.setProperty("http.proxyPassword", "mycoolpass");
        System.setProperty("https.proxyHost", "example.com");
        System.setProperty("https.proxyPort", "4321");
        System.setProperty("https.proxyUser", "mycooluser");
        System.setProperty("https.proxyPassword", "mycoolpass");

        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();

        assertThat(proxyConfiguration.host("http")).isEqualTo("example.com");
        assertThat(proxyConfiguration.host("https")).isEqualTo("example.com");
        assertThat(proxyConfiguration.port("http")).isEqualTo(4321);
        assertThat(proxyConfiguration.port("https")).isEqualTo(4321);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("http");
        assertThat(proxyConfiguration.scheme("https")).isEqualTo("http");
        assertThat(proxyConfiguration.username("http")).isEqualTo("mycooluser");
        assertThat(proxyConfiguration.username("https")).isEqualTo("mycooluser");
        assertThat(proxyConfiguration.password("http")).isEqualTo("mycoolpass");
        assertThat(proxyConfiguration.password("https")).isEqualTo("mycoolpass");
    }

    @Test
    void testCanParseEnvironmentVariables() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "http_proxy",
            "https://user:pass@localhost:25565/"
        );
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();

        assertThat(proxyConfiguration.host("http")).isEqualTo("localhost");
        assertThat(proxyConfiguration.port("http")).isEqualTo(25565);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("https");
        assertThat(proxyConfiguration.username("http")).isEqualTo("user");
        assertThat(proxyConfiguration.password("http")).isEqualTo("pass");
    }

    @Test
    void testEnvWorksWhenExplicitNotConfigured() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "https_proxy",
            "http://user:pass@localhost:25565/"
        );
        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .endpoint(URI.create("https://example.com:8080"))
                              .username("insecure")
                              .password("insecure")
                              .proxyOverHttps(false)
                              .build();

        assertThat(proxyConfiguration.host("http")).isEqualTo("example.com");
        assertThat(proxyConfiguration.port("http")).isEqualTo(8080);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("https");
        assertThat(proxyConfiguration.username("http")).isEqualTo("insecure");
        assertThat(proxyConfiguration.password("http")).isEqualTo("insecure");

        assertThat(proxyConfiguration.host("https")).isEqualTo("localhost");
        assertThat(proxyConfiguration.port("https")).isEqualTo(25565);
        assertThat(proxyConfiguration.scheme("https")).isEqualTo("http");
        assertThat(proxyConfiguration.username("https")).isEqualTo("user");
        assertThat(proxyConfiguration.password("https")).isEqualTo("pass");
    }

    @Test
    void testCanInferSchemeBasedOnEnvironmentVariables() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "http_proxy",
            "https://user:pass@localhost:25565/"
        );
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "https_proxy",
            "http://user:pass@localhost:25566/"
        );

        ProxyConfiguration config = ProxyConfiguration.builder().build();
        assertThat(config.scheme("http")).isEqualTo("https");
        assertThat(config.scheme("https")).isEqualTo("http");
    }

    @Test
    void testEnvironmentVariableNoProxy() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "no_proxy",
            "169.254.169.254,test-two.example.com,*.example.com"
        );
        // This shouldn't be taken as it isn't being used.
        System.setProperty("http.nonProxyHosts", "");
        ProxyConfiguration config = ProxyConfiguration.builder().build();

        assertThat(config.nonProxyHosts()).contains(
            "test-two.example.com", ".*?.example.com", "169.254.169.254");
    }

    @Test
    void testIgnoresEnvironmentWhenToldTo() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "http_proxy",
            "http://user:pass@localhost:25565/index.html"
        );
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "https_proxy",
            "https://user:pass@localhost:25566/index.html"
        );
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "no_proxy",
            "localhost,test-two.example.com,*.example.com"
        );
        ProxyConfiguration config = ProxyConfiguration.builder().useEnvironmentVariables(false).build();

        assertThat(config.host("http")).isNull();
        assertThat(config.host("https")).isNull();
        assertThat(config.port("http")).isEqualTo(0);
        assertThat(config.port("https")).isEqualTo(0);
        assertThat(config.username("http")).isNull();
        assertThat(config.username("https")).isNull();
        assertThat(config.password("http")).isNull();
        assertThat(config.password("https")).isNull();
        assertThat(config.nonProxyHosts()).isEmpty();
        assertThat(config.scheme("http")).isNull();
    }

    private static void clearProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");

        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("https.proxyUser");
        System.clearProperty("https.proxyPassword");
    }
}
