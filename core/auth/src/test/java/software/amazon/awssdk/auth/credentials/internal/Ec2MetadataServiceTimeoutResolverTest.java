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

package software.amazon.awssdk.auth.credentials.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

class Ec2MetadataServiceTimeoutResolverTest {

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    private static Stream<Arguments> timeoutConfigValues() {
        ProfileFile emptyProfile = configFile("profile test", Pair.of("foo", "bar"));

        Function<String, ProfileFile> profileTimeoutValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.METADATA_SERVICE_TIMEOUT, s));

        return Stream.of(
            Arguments.of(null, emptyProfile, Duration.ofSeconds(1).toMillis()), // Default value
            Arguments.of("10", null, Duration.ofSeconds(10).toMillis()),
            Arguments.of(null, profileTimeoutValues.apply("5"), Duration.ofSeconds(5).toMillis()),
            Arguments.of("15", profileTimeoutValues.apply("10"), Duration.ofSeconds(15).toMillis()),
            Arguments.of(null, profileTimeoutValues.apply(".5"), 500)
        );
    }

    private static Stream<Arguments> invalidTimeoutValues() {
        Function<String, ProfileFile> profileTimeoutValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.METADATA_SERVICE_TIMEOUT, s));

        return Stream.of(
            Arguments.of("foo", null),
            Arguments.of(null, profileTimeoutValues.apply("foo")),
            Arguments.of("invalid", profileTimeoutValues.apply("invalid"))
        );
    }

    private static Stream<Arguments> systemAndEnvironmentConfigValues() {
        ProfileFile emptyProfile = configFile("profile test", Pair.of("foo", "bar"));

        Function<String, ProfileFile> profileTimeoutValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.METADATA_SERVICE_TIMEOUT, s));

        return Stream.of(
            Arguments.of("20", null, emptyProfile, Duration.ofSeconds(20).toMillis()), // Only system property
            Arguments.of(null, "15", emptyProfile, Duration.ofSeconds(15).toMillis()), // Only environment variable
            Arguments.of("25", "10", emptyProfile, Duration.ofSeconds(25).toMillis()), // System property overrides environment
            Arguments.of(null, null, profileTimeoutValues.apply("5"), Duration.ofSeconds(5).toMillis()), // Only profile file
            Arguments.of("30", "10", profileTimeoutValues.apply("5"), Duration.ofSeconds(30).toMillis()) // System property
            // takes precedence
        );
    }

    private static Stream<Arguments> invalidSystemAndEnvironmentConfigValues() {
        Function<String, ProfileFile> profileTimeoutValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.METADATA_SERVICE_TIMEOUT, s));

        return Stream.of(
            Arguments.of("invalid", null, profileTimeoutValues.apply("5")), // Invalid system property
            Arguments.of(null, "invalid", profileTimeoutValues.apply("5")), // Invalid environment variable
            Arguments.of("invalid", "alsoInvalid", profileTimeoutValues.apply("5")) // Both invalid
        );
    }

    private static void setUpSystemSettings(String systemProperty) {
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT, systemProperty);
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

    @BeforeEach
    void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property());
    }

    @ParameterizedTest(name = "{index} - EXPECTED:{3} (sys:{0}, cfg:{1})")
    @MethodSource("timeoutConfigValues")
    void resolveTimeoutValue_whenConfigured_resolvesCorrectly(
        String systemProperty, ProfileFile profileFile, long expected) {

        setUpSystemSettings(systemProperty);

        Ec2MetadataConfigProvider resolver =
            Ec2MetadataConfigProvider.builder().profileFile(() -> profileFile).profileName("test").build();
        assertThat(resolver.serviceTimeout()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} - sys:{0}, cfg:{1}")
    @MethodSource("invalidTimeoutValues")
    void resolveTimeoutValue_whenInvalid_throws(String systemProperty, ProfileFile profileFile) {
        setUpSystemSettings(systemProperty);

        Ec2MetadataConfigProvider resolver =
            Ec2MetadataConfigProvider.builder().profileFile(() -> profileFile).profileName("test").build();
        assertThatThrownBy(resolver::serviceTimeout)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("is not a valid integer or double");
    }

    @ParameterizedTest(name = "{index} - sys:{0}, env:{1}, cfg:{2} - EXPECTED:{3}")
    @MethodSource("systemAndEnvironmentConfigValues")
    void resolveTimeoutValue_whenSystemAndEnvironmentSet_resolvesCorrectly(
        String systemProperty, String environmentVariable, ProfileFile profileFile, long expected) {

        // Set up system property
        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property(), systemProperty);
        }

        // Set up environment variable
        if (environmentVariable != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT, environmentVariable);
        }

        // Create the resolver
        Ec2MetadataConfigProvider resolver =
            Ec2MetadataConfigProvider.builder().profileFile(() -> profileFile).profileName("test").build();

        // Verify the resolved value
        assertThat(resolver.serviceTimeout()).isEqualTo(expected);

        // Clean up the system property
        System.clearProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property());
    }

    @ParameterizedTest(name = "{index} - sys:{0}, env:{1}, cfg:{2}")
    @MethodSource("invalidSystemAndEnvironmentConfigValues")
    void resolveTimeoutValue_whenSystemAndEnvironmentInvalid_throws(String systemProperty, String environmentVariable,
                                                                    ProfileFile profileFile) {

        // Set up system property
        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property(), systemProperty);
        }

        // Set up environment variable
        if (environmentVariable != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT, environmentVariable);
        }

        // Create the configProvider
        Ec2MetadataConfigProvider configProvider =
            Ec2MetadataConfigProvider.builder().profileFile(() -> profileFile).profileName("test").build();

        // Verify exception is thrown
        assertThatThrownBy(configProvider::serviceTimeout)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("is not a valid integer or double");

        // Clean up the system property
        System.clearProperty(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.property());
    }
}