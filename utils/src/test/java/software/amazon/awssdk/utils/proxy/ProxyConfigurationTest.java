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

package software.amazon.awssdk.utils.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.ProxyConfigProvider;
import software.amazon.awssdk.utils.Pair;

public class ProxyConfigurationTest {

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    public static Stream<Arguments> proxyConfigurationSetting() {
        return Stream.of(
            Arguments.of(Arrays.asList(
                             Pair.of("%s.proxyHost", "foo.com"),
                             Pair.of("%s.proxyPort", "555"),
                             Pair.of("http.nonProxyHosts", "bar.com"),
                             Pair.of("%s.proxyUser", "UserOne"),
                             Pair.of("%s.proxyPassword", "passwordSecret")),
                         Arrays.asList(
                             Pair.of("%s_proxy", "http://UserOne:passwordSecret@foo.com:555/"),
                             Pair.of("no_proxy", "bar.com")
                         ),
                         new ExpectedProxySetting().host("foo.com")
                                                   .port(555).userName("UserOne")
                                                   .password("passwordSecret")
                                                   .nonProxyHost("bar.com"),
                         "All Proxy Parameters are Set"),

            Arguments.of(Arrays.asList(
                             Pair.of("%s.proxyHost", "foo.com"),
                             Pair.of("%s.proxyPort", "555")),
                         Collections.singletonList(
                             Pair.of("%s_proxy", "http://foo.com:555/")
                         ),
                         new ExpectedProxySetting().host("foo.com").port(555),
                         "Optional Parameters are not set"),

            Arguments.of(Collections.singletonList(
                             Pair.of("proxy", "")),
                         Arrays.asList(
                             Pair.of("%s_proxy", ""),
                             Pair.of("no_proxy", "")
                         ),
                         new ExpectedProxySetting().port(0),
                         "All parameters are Blank"),

            Arguments.of(Collections.singletonList(
                             Pair.of("http.nonProxyHosts", "one,two,three")),
                         Collections.singletonList(
                             Pair.of("no_proxy", "one,two,three")
                         ),
                         new ExpectedProxySetting().port(0).nonProxyHost("one,two,three"),
                         "Only Non Proxy Hosts are set with multiple value"),

            Arguments.of(Arrays.asList(
                             Pair.of("%s.proxyVaildHost", "foo.com"),
                             Pair.of("%s.proxyPorts", "555")),
                         Collections.singletonList(
                             Pair.of("%s_proxy", "http://foo:com:Incorrects:555/")
                         ),
                         new ExpectedProxySetting().port(0),
                         "Incorrect local Setting"),


            Arguments.of(Arrays.asList(
                             Pair.of("%s.proxyHost", "foo.com"),
                             Pair.of("%s.proxyPort", "555"),
                             Pair.of("http.nonProxyHosts", "bar.com"),
                             Pair.of("%s.proxyUser", ""),

                             Pair.of("%s.proxyPassword", "passwordSecret")),
                         Arrays.asList(
                             Pair.of("%s_proxy", "http://:passwordSecret@foo.com:555/"),
                             Pair.of("no_proxy", "bar.com")
                         ),
                         new ExpectedProxySetting().host("foo.com").userName("").port(555).password("passwordSecret").nonProxyHost("bar.com"),
                         "No User is left empty"),

            Arguments.of(Arrays.asList(
                             Pair.of("%s.proxyHost", "foo.com"),
                             Pair.of("%s.proxyPort", "555"),
                             Pair.of("http.nonProxyHosts", "bar.com"),
                             Pair.of("%s.proxyUser", "UserOne")
                         ),
                         Arrays.asList(
                             Pair.of("%s_proxy", "http://UserOne@foo.com:555/"),
                             Pair.of("no_proxy", "bar.com")
                         ),
                         new ExpectedProxySetting().host("foo.com").port(555).userName("UserOne").nonProxyHost("bar.com"),
                         "Password not present"),

            Arguments.of(Arrays.asList(
                             Pair.of("%s.proxyHost", "555"),
                             Pair.of("%s.proxyPort", "-1"),
                             Pair.of("http.nonProxyHosts", "bar.com")
                         ),
                         Arrays.asList(
                             Pair.of("%s_proxy", "http://555/"),
                             Pair.of("no_proxy", "bar.com")
                         ),
                         new ExpectedProxySetting().host("555").port(-1).nonProxyHost("bar.com"),
                         "Host name is just a number"),

            Arguments.of(Arrays.asList(
                             Pair.of("%s.proxyHost", "foo.com"),
                             Pair.of("%s.proxyPort", "abcde"),
                             Pair.of("http.nonProxyHosts", "bar.com"),
                             Pair.of("%s.proxyUser", "UserOne"),
                             Pair.of("%s.proxyPassword", "passwordSecret")),
                         Arrays.asList(
                             Pair.of("%s_proxy", "http://UserOne:passwordSecret@foo.com:0/"),
                             Pair.of("no_proxy", "bar.com")
                         ),
                         new ExpectedProxySetting().host("foo.com").port(0).userName("UserOne").password("passwordSecret").nonProxyHost("bar.com"),
                         "Number format exception for SystemProperty is handled by defaulting it to 0")
        );
    }

    private static void assertProxyEquals(ProxyConfigProvider actualConfiguration,
                                          ExpectedProxySetting expectedProxySetting) {
        assertThat(actualConfiguration.port()).isEqualTo(expectedProxySetting.port);
        assertThat(actualConfiguration.host()).isEqualTo(expectedProxySetting.host);
        assertThat(actualConfiguration.nonProxyHosts()).isEqualTo(expectedProxySetting.nonProxyHosts);

        assertThat(actualConfiguration.userName().orElse(null)).isEqualTo(expectedProxySetting.userName);
        assertThat(actualConfiguration.password().orElse(null)).isEqualTo(expectedProxySetting.password);
    }

    static void setSystemProperties(List<Pair<String, String>> settingsPairs, String protocol) {
        settingsPairs.forEach(settingsPair -> System.setProperty(String.format(settingsPair.left(), protocol),
                                                                 settingsPair.right()));
    }

    static void setEnvironmentProperties(List<Pair<String, String>> settingsPairs, String protocol) {
        settingsPairs.forEach(settingsPair -> ENVIRONMENT_VARIABLE_HELPER.set(String.format(settingsPair.left(), protocol),
                                                                              settingsPair.right()));
    }

    @BeforeEach
    void setUp() {
        Stream.of("http", "https").forEach(protocol ->
                                               Stream.of("%s.proxyHost", "%s.proxyPort", "%s.nonProxyHosts", "%s.proxyUser",
                                                         "%s.proxyPassword")
                                                     .forEach(property -> System.clearProperty(String.format(property, protocol)))
        );
        ENVIRONMENT_VARIABLE_HELPER.reset();

    }

    @ParameterizedTest(name = "{index} - {3}.")
    @MethodSource("proxyConfigurationSetting")
    void given_LocalSetting_when_httpProtocol_then_correctProxyConfiguration(List<Pair<String, String>> systemSettingsPair,
                                                                             List<Pair<String, String>> envSystemSetting,
                                                                             ExpectedProxySetting expectedProxySetting,
                                                                             String testCaseName) {

        setSystemProperties(systemSettingsPair, "http");
        setEnvironmentProperties(envSystemSetting, "http");
        assertProxyEquals(ProxyConfigProvider.fromSystemPropertySettings("http"), expectedProxySetting);
        assertProxyEquals(ProxyConfigProvider.fromEnvironmentSettings("http"), expectedProxySetting);

    }

    @ParameterizedTest(name = "{index} - {3}.")
    @MethodSource("proxyConfigurationSetting")
    void given_LocalSetting_when_httpsProtocol_then_correctProxyConfiguration(List<Pair<String, String>> systemSettingsPair,
                                                                              List<Pair<String, String>> envSystemSetting,
                                                                              ExpectedProxySetting expectedProxySetting,
                                                                              String testCaseName) {
        setSystemProperties(systemSettingsPair, "https");
        setEnvironmentProperties(envSystemSetting, "https");
        assertProxyEquals(ProxyConfigProvider.fromSystemPropertySettings("https"), expectedProxySetting);
        assertProxyEquals(ProxyConfigProvider.fromEnvironmentSettings("https"), expectedProxySetting);

    }

    private static class ExpectedProxySetting {
        private int port;
        private String host;
        private String userName;
        private String password;
        private Set<String> nonProxyHosts = new HashSet<>();


        public ExpectedProxySetting port(int port) {
            this.port = port;
            return this;
        }

        public ExpectedProxySetting host(String host) {
            this.host = host;
            return this;
        }

        public ExpectedProxySetting userName(String userName) {
            this.userName = userName;
            return this;
        }

        public ExpectedProxySetting password(String password) {
            this.password = password;
            return this;
        }

        public ExpectedProxySetting nonProxyHost(String... nonProxyHosts) {
            this.nonProxyHosts = nonProxyHosts != null ? Arrays.stream(nonProxyHosts)
                                                               .collect(Collectors.toSet()) : new HashSet<>();
            return this;
        }
    }
}
