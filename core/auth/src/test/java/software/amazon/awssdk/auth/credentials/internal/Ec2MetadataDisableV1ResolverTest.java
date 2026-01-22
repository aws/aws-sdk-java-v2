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

public class Ec2MetadataDisableV1ResolverTest {

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @BeforeEach
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.property());
    }

    @ParameterizedTest(name = "{index} - EXPECTED:{3}  (sys:{0}, env:{1}, cfg:{2})")
    @MethodSource("booleanConfigValues")
    public void resolveDisableValue_whenBoolean_resolvesCorrectly(
        String systemProperty, String envVar, ProfileFile profileFile, boolean expected) {

        setUpSystemSettings(systemProperty, envVar);
        Ec2MetadataConfigProvider provider = Ec2MetadataConfigProvider.builder()
                                                                      .profileFile(() -> profileFile)
                                                                      .profileName("test")
                                                                      .build();
        assertThat(provider.isMetadataV1Disabled()).isEqualTo(expected);
    }

    private static Stream<Arguments> booleanConfigValues() {
        ProfileFile emptyProfile = configFile("profile test", Pair.of("foo", "bar"));

        Function<String, ProfileFile> profileDisableValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.EC2_METADATA_V1_DISABLED, s));

        return Stream.of(
            Arguments.of(null, null, emptyProfile, false),
            Arguments.of("false", null, null, false),
            Arguments.of("true", null, null, true),
            Arguments.of(null, "false", null, false),
            Arguments.of(null, "true", null, true),
            Arguments.of(null, null, profileDisableValues.apply("false"), false),
            Arguments.of(null, null, profileDisableValues.apply("true"), true),
            Arguments.of(null, null, configFile("profile test", Pair.of("bar", "baz")), false),
            Arguments.of(null, null, configFile("profile foo", Pair.of(ProfileProperty.EC2_METADATA_V1_DISABLED, "true")),
                         false),
            Arguments.of("false", "true", null, false),
            Arguments.of("true", "false", null, true),
            Arguments.of("false", null, profileDisableValues.apply("true"), false),
            Arguments.of("true", null, profileDisableValues.apply("false"), true)
        );
    }

    @ParameterizedTest(name = "{index} - sys:{0}, env:{1}, cfg:{2}")
    @MethodSource("nonBooleanConfigValues")
    public void resolveDisableValue_whenNonBoolean_throws(
        String systemProperty, String envVar, ProfileFile profileFile) {

        setUpSystemSettings(systemProperty, envVar);

        Ec2MetadataConfigProvider configProvider = Ec2MetadataConfigProvider.builder()
                                                                            .profileFile(() -> profileFile)
                                                                            .profileName("test")
                                                                            .build();
        assertThatThrownBy(configProvider::isMetadataV1Disabled).isInstanceOf(IllegalStateException.class);
    }

    private static Stream<Arguments> nonBooleanConfigValues() {
        Function<String, ProfileFile> profileDisableValues =
            s -> configFile("profile test", Pair.of(ProfileProperty.EC2_METADATA_V1_DISABLED, s));

        return Stream.of(
            Arguments.of("foo", null, null),
            Arguments.of(null, "foo", null),
            Arguments.of(null, null, profileDisableValues.apply("foo"))
        );
    }

    private static void setUpSystemSettings(String systemProperty, String envVar) {
        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.property(), systemProperty);

        }
        if (envVar != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.environmentVariable(),
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
