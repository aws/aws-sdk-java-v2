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

package software.amazon.awssdk.awscore.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode.DISABLED;
import static software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode.PREFERRED;
import static software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode.REQUIRED;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointModeResolver;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;

class AccountIdEndpointModeResolverTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    private static final String PROFILE = "test";

    @BeforeEach
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_ACCOUNT_ID_ENDPOINT_MODE.property());
    }

    @ParameterizedTest(name = "{index} - EXPECTED:{3}  (sys:{0}, env:{1}, cfg:{2})")
    @MethodSource("configValues")
    void resolveMode_whenValidValues_resolvesCorrectly(
        String systemProperty, String envVar, ProfileFile profileFile, AccountIdEndpointMode expected) {

        setUpSystemSettings(systemProperty, envVar);

        AccountIdEndpointModeResolver resolver = AccountIdEndpointModeResolver.create()
                                                                              .profileFile(() -> profileFile)
                                                                              .profileName(PROFILE);
        assertThat(resolver.resolve()).isEqualTo(expected);
    }

    private static Stream<Arguments> configValues() {
        ProfileFile emptyProfile = configFile("profile test", Pair.of("foo", "bar"));

        Function<String, ProfileFile> testProfileConfig =
            s -> configFile("profile test", Pair.of(ProfileProperty.ACCOUNT_ID_ENDPOINT_MODE, s));

        return Stream.of(
            Arguments.of(null, null, emptyProfile, PREFERRED),
            Arguments.of("preferred", null, null, PREFERRED),
            Arguments.of("required", null, null, REQUIRED),
            Arguments.of("disabled", null, null, DISABLED),
            Arguments.of("required", "preferred", null, REQUIRED),
            Arguments.of("required", null, testProfileConfig.apply("preferred"), REQUIRED),
            Arguments.of(null, "preferred", null, PREFERRED),
            Arguments.of(null, "required", null, REQUIRED),
            Arguments.of(null, "disabled", null, DISABLED),
            Arguments.of(null, "disabled", testProfileConfig.apply("required"), DISABLED),
            Arguments.of(null, null, testProfileConfig.apply("preferred"), PREFERRED),
            Arguments.of(null, null, testProfileConfig.apply("required"), REQUIRED),
            Arguments.of(null, null, testProfileConfig.apply("disabled"), DISABLED),
            Arguments.of(null, null, configFile("profile test", Pair.of("bar", "baz")), PREFERRED),
            Arguments.of(null, null, configFile("profile foo", Pair.of(ProfileProperty.ACCOUNT_ID_ENDPOINT_MODE, "required")),
                         PREFERRED)
        );
    }

    @ParameterizedTest(name = "{index} - sys:{0}, env:{1}, cfg:{2}")
    @MethodSource("invalidConfigValues")
    void resolveMode_whenInvalidConfig_throws(
        String systemProperty, String envVar, ProfileFile profileFile) {

        setUpSystemSettings(systemProperty, envVar);

        AccountIdEndpointModeResolver resolver = AccountIdEndpointModeResolver.create()
                                                                              .profileFile(() -> profileFile)
                                                                              .profileName(PROFILE);
        assertThatThrownBy(resolver::resolve).isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> invalidConfigValues() {
        Function<String, ProfileFile> testProfileConfig =
            s -> configFile("profile test", Pair.of(ProfileProperty.ACCOUNT_ID_ENDPOINT_MODE, s));

        return Stream.of(
            Arguments.of("foo", null, null),
            Arguments.of(null, "foo", null),
            Arguments.of(null, null, testProfileConfig.apply("foo"))
        );
    }

    private static void setUpSystemSettings(String systemProperty, String envVar) {
        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_ACCOUNT_ID_ENDPOINT_MODE.property(), systemProperty);

        }
        if (envVar != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_ACCOUNT_ID_ENDPOINT_MODE.environmentVariable(),
                                            envVar);
        }
    }

    private static ProfileFile configFile(String name, Pair<?, ?>... pairs) {
        String values = Arrays.stream(pairs)
                              .map(pair -> String.format("%s=%s", pair.left(), pair.right()))
                              .collect(Collectors.joining(System.lineSeparator()));
        String contents = String.format("[%s]\n%s", name, values);

        return configFile(contents);
    }

    private static ProfileFile configFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }
}
