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

package software.amazon.awssdk.core.internal.useragent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.StringUtils;

class AppIdResolutionTest {

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    private static final String PROFILE = "test";

    @AfterEach
    public void cleanup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_SDK_UA_APP_ID.property());
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("inputValues")
    void resolveAppIdFromEnvironment(String description, String systemProperty, String envVar,
                                     ProfileFile profileFile, String expected) {

        setUpSystemSettings(systemProperty, envVar);

        AppIdResolver resolver = AppIdResolver.create().profileName(PROFILE);
        if (profileFile != null) {
            resolver.profileFile(() -> profileFile);
        }

        if (expected != null) {
            assertThat(resolver.resolve()).isNotEmpty().contains(expected);
        } else {
            assertThat(resolver.resolve()).isEmpty();
        }
    }

    private static Stream<Arguments> inputValues() {
        ProfileFile emptyProfile = configFile("profile test", Pair.of("foo", "bar"));

        Function<String, ProfileFile> testProfileConfig =
            s -> configFile("profile test", Pair.of(ProfileProperty.SDK_UA_APP_ID, s));

        return Stream.of(
            Arguments.of("Without input, resolved value is null", null, null, null, null),
            Arguments.of("Setting system property only gives result", "SystemPropertyAppId", null, null, "SystemPropertyAppId"),
            Arguments.of("Setting env var only gives result", null, "EnvVarAppId", null, "EnvVarAppId"),
            Arguments.of("System property takes precedence over env var", "SystemPropertyAppId", "EnvVarAppId", null,
                         "SystemPropertyAppId"),
            Arguments.of("Setting profile file only gives result", null, null, testProfileConfig.apply("profileAppId"),
                         "profileAppId"),
            Arguments.of("When profile file exists but has no input, resolved value is null", null, null, emptyProfile, null),
            Arguments.of("System property takes precedence over profile file", "SystemPropertyAppId", null,
                         testProfileConfig.apply("profileAppId"), "SystemPropertyAppId"),
            Arguments.of("Env var takes precedence over profile file", null, "EnvVarAppId",
                         testProfileConfig.apply("profileAppId"), "EnvVarAppId"),
            Arguments.of("System prop var takes precedence over profile file", null, "EnvVarAppId",
                         testProfileConfig.apply("profileAppId"), "EnvVarAppId")
        );
    }

    private static void setUpSystemSettings(String systemProperty, String envVar) {
        if (!StringUtils.isEmpty(systemProperty)) {
            System.setProperty(SdkSystemSetting.AWS_SDK_UA_APP_ID.property(), systemProperty);
        }
        if (!StringUtils.isEmpty(envVar)) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_SDK_UA_APP_ID.environmentVariable(), envVar);
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
