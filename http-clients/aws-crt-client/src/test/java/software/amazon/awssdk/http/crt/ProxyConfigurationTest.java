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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
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
    private static final String TEST_USER = "testuser";
    private static final String TEST_PASSWORD = "123";
    private static final String ENV_HOST = "bar.com";
    private static final int ENV_PORT = 9999;
    private static final String ENV_USER = "env";
    private static final String ENV_PASSWORD = "321";

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
    void build_setsAllProperties() {
        verifyAllPropertiesSet(allPropertiesSetConfig());
    }

    @Test
    void build_systemPropertyDefault_Http() {
        setHttpProxyProperties();
        ProxyConfiguration config = ProxyConfiguration.builder().build();

        assertThat(config.host("http")).isEqualTo(TEST_HOST);
        assertThat(config.port("http")).isEqualTo(TEST_PORT);
        assertThat(config.username("http")).isEqualTo(TEST_USER);
        assertThat(config.password("http")).isEqualTo(TEST_PASSWORD);
        assertThat(config.scheme("http")).isEqualTo("http");
    }

    @Test
    void build_systemPropertyDefault_Https() {
        setHttpsProxyProperties();
        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .scheme("https")
                                                      .build();

        assertThat(config.host("https")).isEqualTo(TEST_HOST);
        assertThat(config.port("https")).isEqualTo(TEST_PORT);
        assertThat(config.username("https")).isEqualTo(TEST_USER);
        assertThat(config.password("https")).isEqualTo(TEST_PASSWORD);
        assertThat(config.scheme("https")).isEqualTo("http");
    }

    @Test
    void build_systemPropertyEnabled_Http() {
        setHttpProxyProperties();
        ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(Boolean.TRUE).build();

        assertThat(config.host("http")).isEqualTo(TEST_HOST);
        assertThat(config.port("http")).isEqualTo(TEST_PORT);
        assertThat(config.username("http")).isEqualTo(TEST_USER);
        assertThat(config.password("http")).isEqualTo(TEST_PASSWORD);
        assertThat(config.scheme("http")).isEqualTo("http");
    }

    @Test
    void build_systemPropertyEnabled_Https() {
        setHttpsProxyProperties();
        ProxyConfiguration config = ProxyConfiguration.builder().useSystemPropertyValues(Boolean.TRUE).build();

        assertThat(config.host("https")).isEqualTo(TEST_HOST);
        assertThat(config.port("https")).isEqualTo(TEST_PORT);
        assertThat(config.username("https")).isEqualTo(TEST_USER);
        assertThat(config.password("https")).isEqualTo(TEST_PASSWORD);
        assertThat(config.scheme("https")).isEqualTo("http");
    }

    @Test
    void build_systemPropertyDisabled() {
        setHttpProxyProperties();
        setHttpsProxyProperties();
        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .host("localhost")
                                                      .port(8888)
                                                      .username("username")
                                                      .password("password")
                                                      .useSystemPropertyValues(Boolean.FALSE).build();

        assertThat(config.host("http")).isEqualTo("localhost");
        assertThat(config.host("https")).isEqualTo("localhost");
        assertThat(config.port("http")).isEqualTo(8888);
        assertThat(config.port("https")).isEqualTo(8888);
        assertThat(config.username("http")).isEqualTo("username");
        assertThat(config.username("https")).isEqualTo("username");
        assertThat(config.password("http")).isEqualTo("password");
        assertThat(config.password("https")).isEqualTo("password");
        assertThat(config.scheme("http")).isNull();
        assertThat(config.scheme("https")).isNull();
    }

    @Test
    void build_systemPropertyOverride() {
        setHttpProxyProperties();
        ProxyConfiguration config = ProxyConfiguration.builder()
                                                      .host("localhost")
                                                      .port(8888)
                                                      .username("username")
                                                      .password("password")
                                                      .build();

        assertThat(config.host("http")).isEqualTo("localhost");
        assertThat(config.port("http")).isEqualTo(8888);
        assertThat(config.username("http")).isEqualTo("username");
        assertThat(config.password("http")).isEqualTo("password");
        assertThat(config.scheme("http")).isNull();
    }

    @Test
    void testExplicitEndpointOverridesEnvironmentVariables() {
        setHttpEnvVariables();
        setHttpsEnvVariables();
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                                                                  .host("localhost")
                                                                  .port(8888)
                                                                  .scheme("http")
                                                                  .username("username")
                                                                  .password("password")
                                                                  .build();

        assertThat(proxyConfiguration.host("http")).isEqualTo("localhost");
        assertThat(proxyConfiguration.host("https")).isEqualTo("localhost");
        assertThat(proxyConfiguration.port("http")).isEqualTo(8888);
        assertThat(proxyConfiguration.port("https")).isEqualTo(8888);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("http");
        assertThat(proxyConfiguration.scheme("https")).isEqualTo("http");
        assertThat(proxyConfiguration.username("http")).isEqualTo("username");
        assertThat(proxyConfiguration.username("https")).isEqualTo("username");
        assertThat(proxyConfiguration.password("http")).isEqualTo("password");
        assertThat(proxyConfiguration.password("https")).isEqualTo("password");
    }

    @Test
    void testExplicitPropertiesOverridesEnvironmentVariables() {
        setHttpEnvVariables();
        setHttpsEnvVariables();
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
        setHttpEnvVariables();
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();

        assertThat(proxyConfiguration.host("http")).isEqualTo(ENV_HOST);
        assertThat(proxyConfiguration.port("http")).isEqualTo(ENV_PORT);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("http");
        assertThat(proxyConfiguration.username("http")).isEqualTo(ENV_USER);
        assertThat(proxyConfiguration.password("http")).isEqualTo(ENV_PASSWORD);
    }

    @Test
    void testEnvWorksWhenExplicitNotConfigured() {
        setHttpsEnvVariables();
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                                                                 .host(TEST_HOST)
                                                                 .port(TEST_PORT)
                                                                 .username(TEST_USER)
                                                                 .password(TEST_PASSWORD)
                                                                 .scheme("https")
                                                                 .proxyOverHttps(false)
                                                                 .build();

        assertThat(proxyConfiguration.host("http")).isEqualTo(TEST_HOST);
        assertThat(proxyConfiguration.port("http")).isEqualTo(TEST_PORT);
        assertThat(proxyConfiguration.scheme("http")).isEqualTo("https");
        assertThat(proxyConfiguration.username("http")).isEqualTo(TEST_USER);
        assertThat(proxyConfiguration.password("http")).isEqualTo(TEST_PASSWORD);

        assertThat(proxyConfiguration.host("https")).isEqualTo(ENV_HOST);
        assertThat(proxyConfiguration.port("https")).isEqualTo(ENV_PORT);
        assertThat(proxyConfiguration.scheme("https")).isEqualTo("http");
        assertThat(proxyConfiguration.username("https")).isEqualTo(ENV_USER);
        assertThat(proxyConfiguration.password("https")).isEqualTo(ENV_PASSWORD);
    }

    @Test
    void testIgnoresEnvironmentWhenToldTo() {
        setHttpEnvVariables();
        setHttpsEnvVariables();
        ProxyConfiguration config = ProxyConfiguration.builder().useEnvironmentVariables(false).build();

        assertThat(config.host("http")).isNull();
        assertThat(config.host("https")).isNull();
        assertThat(config.port("http")).isEqualTo(0);
        assertThat(config.port("https")).isEqualTo(0);
        assertThat(config.username("http")).isNull();
        assertThat(config.username("https")).isNull();
        assertThat(config.password("http")).isNull();
        assertThat(config.password("https")).isNull();
        assertThat(config.scheme("http")).isNull();
    }

    @Test
    void toBuilder_roundTrip_producesExactCopy() {
        ProxyConfiguration original = allPropertiesSetConfig();

        ProxyConfiguration copy = original.toBuilder().build();

        assertThat(copy).isEqualTo(original);
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

    private void setHttpProxyProperties() {
        System.setProperty("http.proxyHost", TEST_HOST);
        System.setProperty("http.proxyPort", Integer.toString(TEST_PORT));
        System.setProperty("http.proxyUser", TEST_USER);
        System.setProperty("http.proxyPassword", TEST_PASSWORD);
    }

    private void setHttpsProxyProperties() {
        System.setProperty("https.proxyHost", TEST_HOST);
        System.setProperty("https.proxyPort", Integer.toString(TEST_PORT));
        System.setProperty("https.proxyUser", TEST_USER);
        System.setProperty("https.proxyPassword", TEST_PASSWORD);
    }

    private void setHttpEnvVariables() {
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
            "http_proxy",
            String.format(
                "http://%s:%s@%s:%d",
                ENV_USER,
                ENV_PASSWORD,
                ENV_HOST,
                ENV_PORT
            )
        );
    }

    private void setHttpsEnvVariables() {
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

    private static void clearProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");

        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("https.proxyUser");
        System.clearProperty("https.proxyPassword");
    }
}
