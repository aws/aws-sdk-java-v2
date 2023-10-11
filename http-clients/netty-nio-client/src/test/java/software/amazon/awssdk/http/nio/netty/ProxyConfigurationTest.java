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

package software.amazon.awssdk.http.nio.netty;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;

/**
 * Tests for {@link ProxyConfiguration}.
 */
public class ProxyConfigurationTest {
    private static final Random RNG = new Random();
    private static final String TEST_HOST = "foo.com";
    private static final int TEST_PORT = 7777;
    private static final String TEST_NON_PROXY_HOST = "bar.com";
    private static final String TEST_USER = "testuser";
    private static final String TEST_PASSWORD = "123";
    private static final String ENV_HOST = "foobar.com";
    private static final int ENV_PORT = 8888;
    private static final String ENV_USER = "usertest";
    private static final String ENV_PASSWORD = "321";

    @BeforeEach
    public void setup() {
        clearProxyProperties();
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    @AfterAll
    public static void cleanup() {
        clearProxyProperties();
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    @Test
    void build_setsAllProperties() {
        verifyAllPropertiesSet(allPropertiesSetConfig());
    }

    @Test
    void build_systemPropertyDefault_Http() {
        setHttpProxyProperties();
        Set<String> nonProxyHost = new HashSet<>();
        nonProxyHost.add("bar.com");
        ProxyConfiguration config = ProxyConfiguration.builder().build();

        assertThat(config.host("http")).isEqualTo(TEST_HOST);
        assertThat(config.port("http")).isEqualTo(TEST_PORT);
        assertThat(config.username("http")).isEqualTo(TEST_USER);
        assertThat(config.password("http")).isEqualTo(TEST_PASSWORD);
        assertThat(config.scheme("http")).isEqualTo("http");
        assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHost);
    }

    @Test
    void build_systemPropertyEnabled_Https() {
        setHttpsProxyProperties();
        Set<String> nonProxyHost = new HashSet<>();
        nonProxyHost.add("bar.com");
        ProxyConfiguration config = ProxyConfiguration.builder().build();

        assertThat(config.host("https")).isEqualTo(TEST_HOST);
        assertThat(config.port("https")).isEqualTo(TEST_PORT);
        assertThat(config.username("https")).isEqualTo(TEST_USER);
        assertThat(config.password("https")).isEqualTo(TEST_PASSWORD);
        assertThat(config.scheme("https")).isEqualTo("http");
        assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHost);
    }

    @Test
    void build_systemPropertyDisabled() {
        setHttpProxyProperties();
        ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(false).build();

        assertThat(config.host("http")).isNull();
        assertThat(config.port("http")).isEqualTo(0);
        assertThat(config.username("http")).isNull();
        assertThat(config.password("http")).isNull();
        assertThat(config.scheme("http")).isNull();
        assertThat(config.nonProxyHosts()).isEmpty();
    }

    @Test
    void build_systemPropertyOverride() {
        setHttpProxyProperties();
        Set<String> nonProxyHost = new HashSet<>();
        nonProxyHost.add("test.com");

        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .host("localhost")
                                                      .port(8888)
                                                      .nonProxyHosts(nonProxyHost)
                                                      .username("username")
                                                      .password("password")
                                                      .scheme("https")
                                                      .build();

        assertThat(config.host("http")).isEqualTo("localhost");
        assertThat(config.port("http")).isEqualTo(8888);
        assertThat(config.nonProxyHosts()).isEqualTo(nonProxyHost);
        assertThat(config.username("http")).isEqualTo("username");
        assertThat(config.password("http")).isEqualTo("password");
        assertThat(config.scheme("https")).isEqualTo("https");
    }

    @Test
    void testExplicitEndpointOverridesEnvironmentVariables() {
        setEnvHttp();
        setEnvHttps();
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                                                                  .host("localhost")
                                                                  .port(9999)
                                                                  .username("mycooluser")
                                                                  .password("mycoolpass")
                                                                  .scheme("http")
                                                                  .build();

        assertThat(proxyConfiguration.host("http")).isEqualTo("localhost");
        assertThat(proxyConfiguration.host("https")).isEqualTo("localhost");
        assertThat(proxyConfiguration.port("http")).isEqualTo(9999);
        assertThat(proxyConfiguration.port("https")).isEqualTo(9999);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("http");
        assertThat(proxyConfiguration.scheme("https")).isEqualTo("http");
        assertThat(proxyConfiguration.username("http")).isEqualTo("mycooluser");
        assertThat(proxyConfiguration.username("https")).isEqualTo("mycooluser");
        assertThat(proxyConfiguration.password("http")).isEqualTo("mycoolpass");
        assertThat(proxyConfiguration.password("https")).isEqualTo("mycoolpass");
    }

    @Test
    void testExplicitPropertiesOverridesEnvironmentVariables() {
        setEnvHttp();
        setEnvHttps();
        setHttpProxyProperties();
        setHttpsProxyProperties();
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();

        assertThat(proxyConfiguration.host("http")).isEqualTo(TEST_HOST);
        assertThat(proxyConfiguration.host("https")).isEqualTo(TEST_HOST);
        assertThat(proxyConfiguration.port("http")).isEqualTo(TEST_PORT);
        assertThat(proxyConfiguration.port("https")).isEqualTo(TEST_PORT);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("http");
        assertThat(proxyConfiguration.scheme("https")).isEqualTo("http");
        assertThat(proxyConfiguration.username("http")).isEqualTo(TEST_USER);
        assertThat(proxyConfiguration.username("https")).isEqualTo(TEST_USER);
        assertThat(proxyConfiguration.password("http")).isEqualTo(TEST_PASSWORD);
        assertThat(proxyConfiguration.password("https")).isEqualTo(TEST_PASSWORD);
    }

    @Test
    void testCanParseEnvironmentVariables() {
        setEnvHttp();
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();

        assertThat(proxyConfiguration.host("http")).isEqualTo(ENV_HOST);
        assertThat(proxyConfiguration.port("http")).isEqualTo(ENV_PORT);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("https");
        assertThat(proxyConfiguration.username("http")).isEqualTo(ENV_USER);
        assertThat(proxyConfiguration.password("http")).isEqualTo(ENV_PASSWORD);
    }

    @Test
    void testEnvWorksWhenExplicitNotConfigured() {
        setEnvHttps();
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                                                                  .host("example.com")
                                                                  .port(80)
                                                                  .scheme("https")
                                                                  .username("insecure")
                                                                  .password("insecure")
                                                                  .proxyOverHttps(false)
                                                                  .build();

        assertThat(proxyConfiguration.host("http")).isEqualTo("example.com");
        assertThat(proxyConfiguration.port("http")).isEqualTo(80);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("https");
        assertThat(proxyConfiguration.username("http")).isEqualTo("insecure");
        assertThat(proxyConfiguration.password("http")).isEqualTo("insecure");

        assertThat(proxyConfiguration.host("https")).isEqualTo(ENV_HOST);
        assertThat(proxyConfiguration.port("https")).isEqualTo(ENV_PORT);
        assertThat(proxyConfiguration.scheme("https")).isEqualTo("http");
        assertThat(proxyConfiguration.username("https")).isEqualTo(ENV_USER);
        assertThat(proxyConfiguration.password("https")).isEqualTo(ENV_PASSWORD);
    }

    @Test
    void testCanInferSchemeBasedOnEnvironmentVariables() {
        setEnvHttp();
        setEnvHttps();

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

        assertThat(config.nonProxyHosts()).contains("test-two.example.com", ".*?.example.com", "169.254.169.254");
    }

    @Test
    void testIgnoresEnvironmentWhenToldTo() {
        setEnvHttp();
        setEnvHttps();
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

    @Test
    void toBuilder_roundTrip_producesExactCopy() {
        ProxyConfiguration original = allPropertiesSetConfig();

        ProxyConfiguration copy = original.toBuilder().build();

        assertThat(copy).isEqualTo(original);
    }

    @Test
    void setNonProxyHostsToNull_createsEmptySet() {
        ProxyConfiguration cfg = ProxyConfiguration.builder()
                                                   .nonProxyHosts(null)
                                                   .build();

        assertThat(cfg.nonProxyHosts()).isEmpty();
    }

    @Test
    void toBuilderModified_doesNotModifySource() {
        ProxyConfiguration original = allPropertiesSetConfig();

        ProxyConfiguration modified = setAllPropertiesToRandomValues(original.toBuilder()).build();

        assertThat(original).isNotEqualTo(modified);
    }

    private ProxyConfiguration allPropertiesSetConfig() {
        return setAllPropertiesToRandomValues(ProxyConfiguration.builder()).build();
    }

    private ProxyConfiguration.Builder setAllPropertiesToRandomValues(ProxyConfiguration.Builder builder) {
        Stream.of(builder.getClass().getDeclaredMethods())
              .filter(m -> m.getParameterCount() == 1 && m.getReturnType().equals(ProxyConfiguration.Builder.class))
              .forEach(m -> {
                  try {
                      m.setAccessible(true);
                      setRandomValue(builder, m);
                  } catch (Exception e) {
                      throw new RuntimeException("Could not create random proxy config", e);
                  }
              });
        return builder;
    }

    private void setRandomValue(Object o, Method setter) throws InvocationTargetException, IllegalAccessException {
        Class<?> paramClass = setter.getParameterTypes()[0];

        if (String.class.equals(paramClass)) {
            setter.invoke(o, randomString());
        } else if (int.class.equals(paramClass)) {
            setter.invoke(o, RNG.nextInt());
        } else if (Set.class.isAssignableFrom(paramClass)) {
            setter.invoke(o, randomSet());
        } else if (Boolean.class.equals(paramClass)) {
            setter.invoke(o, RNG.nextBoolean());
        } else {
            throw new RuntimeException("Don't know how create random value for type " + paramClass);
        }
    }

    private void verifyAllPropertiesSet(ProxyConfiguration cfg) {
        boolean hasNullProperty = Stream.of(cfg.getClass().getDeclaredMethods())
                                        .filter(m -> !m.getReturnType().equals(Void.class) && m.getParameterCount() == 0)
                                        .anyMatch(m -> {
                                            m.setAccessible(true);
                                            try {
                                                return m.invoke(cfg) == null;
                                            } catch (Exception e) {
                                                return true;
                                            }
                                        });

        if (hasNullProperty) {
            throw new RuntimeException("Given configuration has unset property");
        }
    }

    private String randomString() {
        String alpha = "abcdefghijklmnopqrstuwxyz";

        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; ++i) {
            sb.append(alpha.charAt(RNG.nextInt(16)));
        }

        return sb.toString();
    }

    private Set<String> randomSet() {
        Set<String> ss = new HashSet<>(16);
        for (int i = 0; i < 16; ++i) {
            ss.add(randomString());
        }
        return ss;
    }

    private void setEnvHttp() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "http_proxy",
            String.format(
                "https://%s:%s@%s:%d",
                ENV_USER,
                ENV_PASSWORD,
                ENV_HOST,
                ENV_PORT
            )
        );
    }

    private void setEnvHttps() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "https_proxy",
            String.format(
                "http://%s:%s@%s:%d",
                ENV_USER,
                ENV_PASSWORD,
                ENV_HOST,
                ENV_PORT
            )
        );
    }

    private void setHttpProxyProperties() {
        System.setProperty("http.proxyHost", TEST_HOST);
        System.setProperty("http.proxyPort", Integer.toString(TEST_PORT));
        System.setProperty("http.nonProxyHosts", TEST_NON_PROXY_HOST);
        System.setProperty("http.proxyUser", TEST_USER);
        System.setProperty("http.proxyPassword", TEST_PASSWORD);
    }

    private void setHttpsProxyProperties() {
        System.setProperty("https.proxyHost", TEST_HOST);
        System.setProperty("https.proxyPort", Integer.toString(TEST_PORT));
        System.setProperty("http.nonProxyHosts", TEST_NON_PROXY_HOST);
        System.setProperty("https.proxyUser", TEST_USER);
        System.setProperty("https.proxyPassword", TEST_PASSWORD);
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
