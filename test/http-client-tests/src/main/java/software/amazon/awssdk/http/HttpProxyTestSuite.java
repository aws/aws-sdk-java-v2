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

package software.amazon.awssdk.http;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.proxy.ProxyConfigCommonTestData;
import software.amazon.awssdk.http.proxy.TestProxySetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

public abstract class HttpProxyTestSuite {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();


    public static Stream<Arguments> proxyConfigurationSetting() {
        return ProxyConfigCommonTestData.proxyConfigurationSetting();
    }

    static void setSystemProperties(List<Pair<String, String>> settingsPairs, String protocol) {
        settingsPairs.stream()
                     .filter(p -> StringUtils.isNotBlank(p.left()))
                     .forEach(settingsPair -> System.setProperty(String.format(settingsPair.left(), protocol),
                                                                 settingsPair.right()));
    }

    static void setEnvironmentProperties(List<Pair<String, String>> settingsPairs, String protocol) {
        settingsPairs.forEach(settingsPair -> ENVIRONMENT_VARIABLE_HELPER.set(String.format(settingsPair.left(), protocol),
                                                                              settingsPair.right()));
    }

    @BeforeEach
    void setUp() {
        Stream.of(HTTP, HTTPS)
              .forEach(protocol -> Stream.of("%s.proxyHost", "%s.proxyPort",
                                             "%s.nonProxyHosts", "%s.proxyUser", "%s.proxyPassword")
                                         .forEach(property -> System.clearProperty(String.format(property, protocol))));
        ENVIRONMENT_VARIABLE_HELPER.reset();
    }

    @ParameterizedTest(name =
        "{index} -{0}  useSystemProperty {4} useEnvironmentVariable {5}  userSetProxy {3} then expected " + "is {6}")
    @MethodSource("proxyConfigurationSetting")
    void givenLocalSettingForHttpThenCorrectProxyConfig(String testCaseName,
                                                        List<Pair<String, String>> systemSettingsPair,
                                                        List<Pair<String, String>> envSystemSetting,
                                                        TestProxySetting userSetProxySettings,
                                                        Boolean useSystemProperty,
                                                        Boolean useEnvironmentVariable,
                                                        TestProxySetting expectedProxySettings) throws URISyntaxException {
        setSystemProperties(systemSettingsPair, HTTP);
        setEnvironmentProperties(envSystemSetting, HTTP);
        assertProxyConfiguration(userSetProxySettings, expectedProxySettings, useSystemProperty, useEnvironmentVariable, HTTP);
    }

    @ParameterizedTest(name = "{index} -{0}  useSystemProperty {4} useEnvironmentVariable {5}  userSetProxy {3} then expected "
                              + "is {6}")
    @MethodSource("proxyConfigurationSetting")
    void givenLocalSettingForHttpsThenCorrectProxyConfig(
        String testCaseName,
        List<Pair<String, String>> systemSettingsPair,
        List<Pair<String, String>> envSystemSetting,
        TestProxySetting userSetProxySettings,
        Boolean useSystemProperty,
        Boolean useEnvironmentVariable,
        TestProxySetting expectedProxySettings) throws URISyntaxException {
        setSystemProperties(systemSettingsPair, HTTPS);
        setEnvironmentProperties(envSystemSetting, HTTPS);
        assertProxyConfiguration(userSetProxySettings, expectedProxySettings,
                                 useSystemProperty, useEnvironmentVariable, HTTPS);
    }

    protected abstract void assertProxyConfiguration(TestProxySetting userSetProxySettings,
                                                     TestProxySetting expectedProxySettings,
                                                     Boolean useSystemProperty,
                                                     Boolean useEnvironmentVariable,
                                                     String protocol) throws URISyntaxException;
}
