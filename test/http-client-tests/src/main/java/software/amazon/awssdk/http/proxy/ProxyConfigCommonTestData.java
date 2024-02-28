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

package software.amazon.awssdk.http.proxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.utils.Pair;

public final class ProxyConfigCommonTestData {


    public static final String SYSTEM_PROPERTY_HOST = "systemProperty.com";
    public static final String SYSTEM_PROPERTY_PORT_NUMBER = "2222";
    public static final String SYSTEM_PROPERTY_NON_PROXY = "systemPropertyNonProxy.com".toLowerCase(Locale.US);
    public static final String SYSTEM_PROPERTY_USER = "systemPropertyUserOne";
    public static final String SYSTEM_PROPERTY_PASSWORD = "systemPropertyPassword";
    public static final String ENV_VARIABLE_USER = "envUserOne";
    public static final String ENV_VARIABLE_PASSWORD = "envPassword";
    public static final String ENVIRONMENT_HOST = "environmentVariable.com".toLowerCase(Locale.US);
    public static final String ENVIRONMENT_VARIABLE_PORT_NUMBER = "3333";
    public static final String ENVIRONMENT_VARIABLE_NON_PROXY = "environmentVariableNonProxy".toLowerCase(Locale.US);
    public static final String USER_HOST_ON_BUILDER = "proxyBuilder.com";
    public static final int USER_PORT_NUMBER_ON_BUILDER = 9999;
    public static final String USER_USERNAME_ON_BUILDER = "proxyBuilderUser";
    public static final String USER_PASSWORD_ON_BUILDER = "proxyBuilderPassword";
    public static final String USER_NONPROXY_ON_BUILDER = "proxyBuilderNonProxy.com".toLowerCase(Locale.US);

    private ProxyConfigCommonTestData() {
    }

    public static Stream<Arguments> proxyConfigurationSetting() {
        return Stream.of(
            Arguments.of(
                "Provided system and environment variable when configured default setting then uses System property",
                systemPropertySettings(),
                environmentSettings(),
                new TestProxySetting(), null, null, getSystemPropertyProxySettings()),

            Arguments.of(
                "Provided system and environment variable when Host and port used from Builder then resolved Poxy "
                + "config uses User password from System setting",
                systemPropertySettings(),
                environmentSettings(),
                new TestProxySetting().host("localhost").port(80), null, null, getSystemPropertyProxySettings().host("localhost"
                ).port(80)),

            Arguments.of(
                "Provided system and environment variable when configured user setting then uses User provider setting",
                systemPropertySettings(),
                environmentSettings(),
                getTestProxySettings(), null, null, getTestProxySettings()),

            Arguments.of(
                "Provided: System property settings and environment variables are set. "
                + "When: useEnvironmentVariable is set to \true. And: useSystemProperty is left at its default value. Then: The"
                + " proxy configuration gets resolved to "
                + "use system properties ",
                systemPropertySettings(),
                environmentSettings(),
                new TestProxySetting(), null, true, getSystemPropertyProxySettings()),

            Arguments.of(
                "Provided: System property settings and environment variables are set. "
                + "When: useEnvironmentVariable is set to true. And: useSystemProperty is set to false. Then: "
                + "The proxy configuration gets resolved to use environment variable values",
                systemPropertySettings(),
                environmentSettings(),
                new TestProxySetting(), false, true, getEnvironmentVariableProxySettings()),

            // No System Property only Environment variable set
            Arguments.of(
                "Provided with no system property and valid environment variables, "
                + "when using the default proxy builder, the proxy configuration is resolved to use environment variables.",
                Collections.singletonList(Pair.of("", "")),
                environmentSettings(),
                new TestProxySetting(), null, null, getEnvironmentVariableProxySettings()),

            Arguments.of(
                "Provided with no system property and valid environment variables, when using the host,"
                + "port on builder , the proxy configuration is resolved to username and password of environment variables.",
                Collections.singletonList(Pair.of("", "")),
                environmentSettings(),
                new TestProxySetting().host(USER_HOST_ON_BUILDER).port(USER_PORT_NUMBER_ON_BUILDER), null, true,
                getEnvironmentVariableProxySettings().host(USER_HOST_ON_BUILDER).port(USER_PORT_NUMBER_ON_BUILDER)),

            Arguments.of(
                "Provided with no system property and valid environment variables, when using the host,port on builder"
                + " , the proxy configuration is resolved to Builder values.",
                Collections.singletonList(Pair.of("", "")),
                environmentSettings(),
                getTestProxySettings(), null, null, getTestProxySettings()),

            Arguments.of(
                "Provided environment variable and No System Property when default ProxyConfig then uses environment "
                + "variable ",
                Collections.singletonList(Pair.of("", "")),
                environmentSettings(),
                new TestProxySetting(), null, true, getEnvironmentVariableProxySettings()),

            Arguments.of(
                "Provided only environment variable when useSytemProperty set to true "
                + "then proxy resolved to environment",
                Collections.singletonList(Pair.of("", "")),
                environmentSettings(),
                null, true, null, getEnvironmentVariableProxySettings()),

            Arguments.of(
                "Provided only environment variable when useEnvironmentVariable set to false then proxy resolved "
                + "to null",
                Collections.singletonList(Pair.of("", "")),
                environmentSettings(),
                new TestProxySetting(), null, true, getEnvironmentVariableProxySettings()),

            // Only System Property and no Environment variable

            Arguments.of(
                "Provided system and no environment variable when default ProxyConfig then used System Proxy config",
                systemPropertySettings(),
                Collections.singletonList(Pair.of("", "")),
                null, null, null, getSystemPropertyProxySettings()),

            Arguments.of(
                "Provided system and no environment variable when host from builder then Host is resolved from builder",
                systemPropertySettings(),
                Collections.singletonList(Pair.of("", "")),
                new TestProxySetting().port(USER_PORT_NUMBER_ON_BUILDER).host(USER_HOST_ON_BUILDER),
                null, null, getSystemPropertyProxySettings().host(USER_HOST_ON_BUILDER).port(USER_PORT_NUMBER_ON_BUILDER)),

            Arguments.of(
                "Provided system and no environment variable when user ProxyConfig on builder "
                + "then  User Proxy resolved",
                systemPropertySettings(),
                Collections.singletonList(Pair.of("", "")),
                getTestProxySettings(), true, true, getTestProxySettings()),

            Arguments.of(
                "Provided system and no environment variable when useEnvironmentVariable then System property proxy resolved",
                systemPropertySettings(),
                Collections.singletonList(Pair.of("", "")),
                new TestProxySetting(), null, true, getSystemPropertyProxySettings()),

            Arguments.of(
                "Provided system and no environment variable "
                + "when useSystemProperty and useEnvironment set to false then resolved config is null ",
                systemPropertySettings(),
                Collections.singletonList(Pair.of("", "")),
                new TestProxySetting(), false, true, new TestProxySetting()),

            // when both system property and environment variable are null
            Arguments.of(
                "Provided  no system property and no environment variable when default ProxyConfig "
                + "then no Proxy config resolved",
                Collections.singletonList(Pair.of("", "")),
                Collections.singletonList(Pair.of("", "")),
                new TestProxySetting(), null, null, new TestProxySetting()),

            Arguments.of(
                "Provided  no system property and no environment variable when user ProxyConfig then user Proxy config resolved",
                Collections.singletonList(Pair.of("", "")),
                Collections.singletonList(Pair.of("", "")),
                new TestProxySetting().host(USER_HOST_ON_BUILDER).port(USER_PORT_NUMBER_ON_BUILDER), null, null,
                new TestProxySetting().host(USER_HOST_ON_BUILDER).port(USER_PORT_NUMBER_ON_BUILDER)),

            Arguments.of(
                "Provided  no system property and no environment variable when user ProxyConfig "
                + "then user Proxy config resolved",
                Collections.singletonList(Pair.of("", "")),
                Collections.singletonList(Pair.of("", "")),
                getTestProxySettings(), null, null, getTestProxySettings()),

            Arguments.of(
                "Provided  no system property and no environment variable when useSystemProperty and "
                + "useEnvironmentVariable set  then resolved host is null ",
                Collections.singletonList(Pair.of("", "")),
                Collections.singletonList(Pair.of("", "")), null, true, true, new TestProxySetting()),

            // Incomplete Proxy setting in systemProperty and environment variable
            Arguments.of(
                "Given System property with No user name and Environment variable with user name "
                + "when Default proxy config then resolves proxy config with no user name same as System property",
                getSystemPropertiesWithNoUserName(),
                environmentSettingsWithNoPassword(),
                new TestProxySetting(), null, null,
                new TestProxySetting().host(SYSTEM_PROPERTY_HOST).port(Integer.parseInt(SYSTEM_PROPERTY_PORT_NUMBER))
                                      .password(SYSTEM_PROPERTY_PASSWORD).nonProxyHost(ENVIRONMENT_VARIABLE_NON_PROXY)),

            Arguments.of(
                "Given password in System property when Password present in system property "
                + "then proxy resolves to Builder password",
                getSystemPropertiesWithNoUserName(),
                environmentSettingsWithNoPassword(),
                new TestProxySetting().password("passwordFromBuilder"), null, null,
                getSystemPropertyProxySettings().password("passwordFromBuilder")
                                                .userName(null)
                                                .nonProxyHost(ENVIRONMENT_VARIABLE_NON_PROXY)),

            Arguments.of(
                "Given partial System Property and partial Environment variables when Builder method with Host "
                + "and port only then Proxy config uses password from System property and no User name since System property"
                + " has none",
                getSystemPropertiesWithNoUserName(),
                environmentSettingsWithNoPassword(),
                new TestProxySetting().host(USER_HOST_ON_BUILDER).port(USER_PORT_NUMBER_ON_BUILDER),
                null, null,
                getSystemPropertyProxySettings().host(USER_HOST_ON_BUILDER)
                                                .port(USER_PORT_NUMBER_ON_BUILDER)
                                                .userName(null)
                                                .nonProxyHost(ENVIRONMENT_VARIABLE_NON_PROXY)),

            Arguments.of(
                "Given System Property and Environment variables when valid empty Proxy config on Builder then "
                + "Proxy config resolves to Proxy on builder.",
                systemPropertySettings(),
                environmentSettings(),
                getTestProxySettings(), null, null, getTestProxySettings()),

            Arguments.of(
                "Given partial system property and partial environment variable when User "
                + "set useEnvironmentVariable to true and default System property then default system property gets used.",
                getSystemPropertiesWithNoUserName(),
                environmentSettingsWithNoPassword(),
                new TestProxySetting(), null, true, getSystemPropertyProxySettings().nonProxyHost(ENVIRONMENT_VARIABLE_NON_PROXY)
                                                                                    .userName(null)),

            Arguments.of(
                "Given partial system property and partial environment variable  when User "
                + "set useEnvironmentVariable and explicitly sets useSystemProperty to fals then only environment variable is "
                + "resolved",
                getSystemPropertiesWithNoUserName(),
                environmentSettingsWithNoPassword(),
                new TestProxySetting(), false, true, getEnvironmentVariableProxySettings().password(null)),

            Arguments.of(
                "Given",
                systemPropertySettingsWithNoNonProxyHosts(),
                environmentSettings(),
                new TestProxySetting(), true, true,
                getSystemPropertyProxySettings().nonProxyHost(ENVIRONMENT_VARIABLE_NON_PROXY)),

            Arguments.of(
                "Given",
                systemPropertySettingsWithNoNonProxyHosts(),
                environmentSettingsWithNoNonProxy(),
                new TestProxySetting(), true, true,
                getSystemPropertyProxySettings().nonProxyHost(null))
        );
    }

    private static List<Pair<String, String>> getSystemPropertiesWithNoUserName() {
        return Arrays.asList(
            Pair.of("%s.proxyHost", SYSTEM_PROPERTY_HOST),
            Pair.of("%s.proxyPort", SYSTEM_PROPERTY_PORT_NUMBER),
            Pair.of("%s.proxyPassword", SYSTEM_PROPERTY_PASSWORD));
    }

    private static TestProxySetting getTestProxySettings() {
        return new TestProxySetting().host(USER_HOST_ON_BUILDER)
                                     .port(USER_PORT_NUMBER_ON_BUILDER)
                                     .userName(USER_USERNAME_ON_BUILDER)
                                     .password(USER_PASSWORD_ON_BUILDER)
                                     .nonProxyHost(USER_NONPROXY_ON_BUILDER);
    }

        private static TestProxySetting getTestProxySettingsWithNoProxy() {
            return new TestProxySetting().host(USER_HOST_ON_BUILDER)
                                         .port(USER_PORT_NUMBER_ON_BUILDER)
                                         .userName(USER_USERNAME_ON_BUILDER)
                                         .password(USER_PASSWORD_ON_BUILDER);
    }


    private static TestProxySetting getSystemPropertyProxySettings() {
        return new TestProxySetting().host(SYSTEM_PROPERTY_HOST)
                                     .port(Integer.parseInt(SYSTEM_PROPERTY_PORT_NUMBER))
                                     .userName(SYSTEM_PROPERTY_USER)
                                     .password(SYSTEM_PROPERTY_PASSWORD)
                                     .nonProxyHost(SYSTEM_PROPERTY_NON_PROXY);
    }


    private static TestProxySetting getEnvironmentVariableProxySettings() {
        return new TestProxySetting().host(ENVIRONMENT_HOST)
                                     .port(Integer.parseInt(ENVIRONMENT_VARIABLE_PORT_NUMBER))
                                     .userName(ENV_VARIABLE_USER)
                                     .password(ENV_VARIABLE_PASSWORD)
                                     .nonProxyHost(ENVIRONMENT_VARIABLE_NON_PROXY);
    }


    private static List<Pair<String, String>> environmentSettings() {
        return Arrays.asList(
            Pair.of("%s_proxy",
                    "http://" + ENV_VARIABLE_USER + ":" + ENV_VARIABLE_PASSWORD + "@" + ENVIRONMENT_HOST
                    + ":" + ENVIRONMENT_VARIABLE_PORT_NUMBER + "/"),
            Pair.of("no_proxy", ENVIRONMENT_VARIABLE_NON_PROXY)
        );
    }


    private static List<Pair<String, String>> environmentSettingsWithNoNonProxy() {
        return Arrays.asList(
            Pair.of("%s_proxy",
                    "http://" + ENV_VARIABLE_USER + ":" + ENV_VARIABLE_PASSWORD + "@" + ENVIRONMENT_HOST
                    + ":" + ENVIRONMENT_VARIABLE_PORT_NUMBER + "/")
        );
    }

    private static List<Pair<String, String>> environmentSettingsWithNoPassword() {
        return Arrays.asList(
            Pair.of("%s_proxy",
                    "http://" + ENV_VARIABLE_USER + "@" + ENVIRONMENT_HOST + ":" + ENVIRONMENT_VARIABLE_PORT_NUMBER + "/"),
            Pair.of("no_proxy", ENVIRONMENT_VARIABLE_NON_PROXY)
        );
    }

    private static List<Pair<String, String>> systemPropertySettings() {
        return Arrays.asList(
            Pair.of("%s.proxyHost", SYSTEM_PROPERTY_HOST),
            Pair.of("%s.proxyPort", SYSTEM_PROPERTY_PORT_NUMBER),
            Pair.of("http.nonProxyHosts", SYSTEM_PROPERTY_NON_PROXY),
            Pair.of("%s.proxyUser", SYSTEM_PROPERTY_USER),
            Pair.of("%s.proxyPassword", SYSTEM_PROPERTY_PASSWORD));
    }


    private static List<Pair<String, String>> systemPropertySettingsWithNoNonProxyHosts() {
        return Arrays.asList(
            Pair.of("%s.proxyHost", SYSTEM_PROPERTY_HOST),
            Pair.of("%s.proxyPort", SYSTEM_PROPERTY_PORT_NUMBER),
            Pair.of("%s.proxyUser", SYSTEM_PROPERTY_USER),
            Pair.of("%s.proxyPassword", SYSTEM_PROPERTY_PASSWORD));
    }
}
