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

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

class IgnoreConfiguredEndpointUrlsProviderTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    private static final String PROFILE = "test";

    @BeforeEach
    void setup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property());
    }

    @AfterEach
    void teardown() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property());
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("testCases")
    void resolvesCorrectly(String description, String systemProperty, String envVar, String profileValue,
                           Optional<Boolean> expected) {
        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS.property(), systemProperty);
        }
        if (envVar != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_IGNORE_CONFIGURED_ENDPOINT_URLS, envVar);
        }

        ProfileFile profileFile = profileFile(profileValue);

        IgnoreConfiguredEndpointUrlsProvider provider =
            IgnoreConfiguredEndpointUrlsProvider.builder()
                                                .profileFile(() -> profileFile)
                                                .profileName(PROFILE)
                                                .build();

        assertThat(provider.ignoreConfiguredEndpointUrls()).isEqualTo(expected);
    }

    private static Stream<Arguments> testCases() {
        return Stream.of(
            Arguments.of("nothing set returns empty", null, null, null, Optional.empty()),
            Arguments.of("system property true", "true", null, null, Optional.of(true)),
            Arguments.of("system property false", "false", null, null, Optional.of(false)),
            Arguments.of("system property case insensitive True", "True", null, null, Optional.of(true)),
            Arguments.of("system property case insensitive TRUE", "TRUE", null, null, Optional.of(true)),
            Arguments.of("env var true", null, "true", null, Optional.of(true)),
            Arguments.of("env var false", null, "false", null, Optional.of(false)),
            Arguments.of("profile true", null, null, "true", Optional.of(true)),
            Arguments.of("profile false", null, null, "false", Optional.of(false)),
            Arguments.of("system property wins over env var", "true", "false", null, Optional.of(true)),
            Arguments.of("system property false wins over env var true", "false", "true", null, Optional.of(false)),
            Arguments.of("system property wins over profile", "true", null, "false", Optional.of(true)),
            Arguments.of("env var wins over profile", null, "true", "false", Optional.of(true)),
            Arguments.of("env var false wins over profile true", null, "false", "true", Optional.of(false)),
            Arguments.of("system property wins over both", "true", "false", "false", Optional.of(true)),
            Arguments.of("system property false wins over both", "false", "true", "true", Optional.of(false))
        );
    }

    private static ProfileFile profileFile(String ignoreConfiguredEndpointUrlsValue) {
        StringBuilder content = new StringBuilder();
        content.append("[profile test]\n");
        if (ignoreConfiguredEndpointUrlsValue != null) {
            content.append("ignore_configured_endpoint_urls = ").append(ignoreConfiguredEndpointUrlsValue).append("\n");
        }
        return ProfileFile.builder()
                          .type(ProfileFile.Type.CONFIGURATION)
                          .content(new StringInputStream(content.toString()))
                          .build();
    }
}
