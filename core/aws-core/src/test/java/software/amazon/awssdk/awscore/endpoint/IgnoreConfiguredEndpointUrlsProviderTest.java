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

    @ParameterizedTest(name = "{index} - EXPECTED:{3} (sys:{0}, env:{1}, profile:{2})")
    @MethodSource("testCases")
    void resolvesCorrectly(String systemProperty, String envVar, String profileValue, boolean expected) {
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
            // Nothing set: defaults to false
            Arguments.of(null, null, null, false),

            // System property alone
            Arguments.of("true", null, null, true),
            Arguments.of("false", null, null, false),
            Arguments.of("True", null, null, true),
            Arguments.of("TRUE", null, null, true),

            // Environment variable alone
            Arguments.of(null, "true", null, true),
            Arguments.of(null, "false", null, false),

            // Profile alone
            Arguments.of(null, null, "true", true),
            Arguments.of(null, null, "false", false),

            // System property wins over env var
            Arguments.of("true", "false", null, true),
            Arguments.of("false", "true", null, false),

            // System property wins over profile
            Arguments.of("true", null, "false", true),

            // Env var wins over profile
            Arguments.of(null, "true", "false", true),
            Arguments.of(null, "false", "true", false),

            // System property wins over both
            Arguments.of("true", "false", "false", true),
            Arguments.of("false", "true", "true", false)
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
