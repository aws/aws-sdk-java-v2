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

package software.amazon.awssdk.imds.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;

class EndpointProviderConnectionTimeoutTest {

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    private static Stream<Arguments> timeoutConfigValues() {
        ProfileFile emptyProfile = configFile("profile test", Pair.of("foo", "bar"));

        Function<String, ProfileFile> profileTimeoutValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.METADATA_SERVICE_TIMEOUT, s));

        return Stream.of(
            Arguments.of(null, emptyProfile, Duration.ofSeconds(1)), // Default value
            Arguments.of("10", null, Duration.ofSeconds(10)), // System property
            Arguments.of(null, profileTimeoutValues.apply("5"), Duration.ofSeconds(5)), // Profile file
            Arguments.of("15", profileTimeoutValues.apply("10"), Duration.ofSeconds(15)), // System property overrides profile
            Arguments.of(null, profileTimeoutValues.apply("0.5"), Duration.ofMillis(500)) // Fractional timeout from profile
        );
    }

    private static Stream<Arguments> invalidTimeoutValues() {
        Function<String, ProfileFile> profileTimeoutValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.METADATA_SERVICE_TIMEOUT, s));

        return Stream.of(
            Arguments.of("invalid", null), // Invalid system property
            Arguments.of(null, profileTimeoutValues.apply("invalid")), // Invalid profile value
            Arguments.of("invalid", profileTimeoutValues.apply("alsoInvalid")) // Both invalid
        );
    }

    private static Stream<Arguments> systemAndEnvironmentConfigValues() {
        ProfileFile emptyProfile = configFile("profile test", Pair.of("foo", "bar"));

        Function<String, ProfileFile> profileTimeoutValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.METADATA_SERVICE_TIMEOUT, s));

        return Stream.of(
            Arguments.of("20", null, emptyProfile, Duration.ofSeconds(20)), // System property
            Arguments.of(null, "15", emptyProfile, Duration.ofSeconds(15)), // Environment variable
            Arguments.of("25", "10", emptyProfile, Duration.ofSeconds(25)), // System property overrides environment variable
            Arguments.of(null, null, profileTimeoutValues.apply("5"), Duration.ofSeconds(5)), // Profile file
            Arguments.of("30", "10", profileTimeoutValues.apply("5"), Duration.ofSeconds(30)) // System property takes precedence
        );
    }

    private static void setUpSystemSettings(String systemProperty) {
        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property(), systemProperty);
        }
    }

    private static ProfileFile configFile(String name, Pair<?, ?>... pairs) {
        String values = Arrays.stream(pairs)
                              .map(pair -> String.format("%s=%s", pair.left(), pair.right()))
                              .collect(Collectors.joining(System.lineSeparator()));
        String contents = String.format("[%s]%n%s", name, values);

        return ProfileFile.builder()
                          .content(new StringInputStream(contents))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    @ParameterizedTest(name = "{index} - EXPECTED:{2} (sys:{0}, cfg:{1})")
    @MethodSource("timeoutConfigValues")
    void resolveServiceTimeout_whenConfigured_resolvesCorrectly(
        String systemProperty, ProfileFile profileFile, Duration expected) {

        // Set up system property
        setUpSystemSettings(systemProperty);

        Ec2MetadataConfigProvider resolver =
            Ec2MetadataConfigProvider.builder().profileFile(() -> profileFile).profileName("test").build();

        // Verify resolved value
        assertThat(resolver.resolveServiceTimeout()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} - sys:{0}, cfg:{1}")
    @MethodSource("invalidTimeoutValues")
    void resolveServiceTimeout_whenInvalid_throws(String systemProperty, ProfileFile profileFile) {
        // Set up system property
        setUpSystemSettings(systemProperty);

        Ec2MetadataConfigProvider resolver =
            Ec2MetadataConfigProvider.builder().profileFile(() -> profileFile).profileName("test").build();

        // Verify exception
        assertThatThrownBy(resolver::resolveServiceTimeout)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("is not a valid integer or double");
    }

    @ParameterizedTest(name = "{index} - sys:{0}, env:{1}, cfg:{2} - EXPECTED:{3}")
    @MethodSource("systemAndEnvironmentConfigValues")
    void resolveServiceTimeout_whenSystemAndEnvironmentSet_resolvesCorrectly(
        String systemProperty, String environmentVariable, ProfileFile profileFile, Duration expected) {

        // Set up system property
        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property(), systemProperty);
        }

        // Set up environment variable
        if (environmentVariable != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT, environmentVariable);
        }

        Ec2MetadataConfigProvider resolver =
            Ec2MetadataConfigProvider.builder().profileFile(() -> profileFile).profileName("test").build();

        // Verify resolved value
        assertThat(resolver.resolveServiceTimeout()).isEqualTo(expected);

        // Clean up system property
        System.clearProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property());
    }

    @BeforeEach
    void reset() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property());
    }

}
