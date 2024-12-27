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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;

import java.util.Set;
import java.util.stream.Stream;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class Sigv4aSigningRegionSetProviderTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    private static final String PROFILE = "test";

    @BeforeEach
    public void setup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET.property());
    }

    @DisplayName("Region Set Resolution Tests")
    @ParameterizedTest(name = "{4}")
    @MethodSource("configValues")
    void resolveRegionSet_whenValidValues_resolvesCorrectly(
        String systemProperty, String envVar, ProfileFile profileFile, Set<String> expected, String testDescription) {
        setUpSystemSettings(systemProperty, envVar);

        Sigv4aSigningRegionSetProvider provider = Sigv4aSigningRegionSetProvider.builder()
                                                                                .profileFile(() -> profileFile)
                                                                                .profileName(PROFILE)
                                                                                .build();

        assertThat(provider.resolveRegionSet()).isEqualTo(expected);
    }

    private static Stream<Arguments> configValues() {
        return Stream.of(
            Arguments.of(null, null, emptyProfile(), null,
                         "No values set anywhere"),

            Arguments.of("us-west-2",
                         null,
                         null,
                         Collections.singleton("us-west-2"),
                         "System Property value takes precedence"),

            Arguments.of(null,
                         "us-west-2",
                         null,
                         Collections.singleton("us-west-2"),
                         "Environment used when System Property null"),

            Arguments.of(null,
                         null,
                         configWithRegion("us-west-2"),
                         Collections.singleton("us-west-2"),
                         "Config file used when others null"),

            Arguments.of("us-west-2",
                         "us-east-1",
                         null,
                         Collections.singleton("us-west-2"),
                         "System Property overrides Environment"),

            Arguments.of("us-west-2",
                         null,
                         configWithRegion("us-east-1"),
                         Collections.singleton("us-west-2"),
                         "System Property overrides Config File"),

            Arguments.of(null,
                         "us-west-2",
                         configWithRegion("us-east-1"),
                         Collections.singleton("us-west-2"),
                         "Environment overrides Config File"),

            Arguments.of("us-west-2",
                         "us-east-1",
                         configWithRegion("us-north-1"),
                         Collections.singleton("us-west-2"),
                         "System Property highest precedence"),

            Arguments.of("*",
                         "us-west-2",
                         null,
                         Collections.singleton("*"),
                         "Wildcard in System Property overrides specific value"),

            Arguments.of("us-west-2",
                         "*",
                         null,
                         Collections.singleton("us-west-2"),
                         "Specific Environment overrides wildcard"),

            Arguments.of(null,
                         "*",
                         configWithRegion("us-west-2"),
                         Collections.singleton("*"),
                         "Wildcard in Environment overrides Config"),

            Arguments.of("us-west-1,us-east-1",
                         "us-west-2",
                         null,
                         createSet("us-west-1", "us-east-1"),
                         "Multi-region System Property overrides single"),

            Arguments.of("us-west-1,us-east-1",
                         null,
                         configWithRegion("us-west-2"),
                         createSet("us-west-1", "us-east-1"),
                         "Multi-region System Property overrides Config"),

            Arguments.of(null,
                         "us-west-1,us-east-1",
                         configWithRegion("us-west-2"),
                         createSet("us-west-1", "us-east-1"),
                         "Multi-region Environment overrides Config")
        );
    }

    private static void setUpSystemSettings(String systemProperty, String envVar) {
        if (systemProperty != null) {
            System.setProperty(
                SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET.property(), systemProperty);
        }
        if (envVar != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(
                SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET.environmentVariable(), envVar);
        }
    }

    private static ProfileFile emptyProfile() {
        return configFile("profile test", Pair.of("foo", "bar"));
    }

    private static ProfileFile configWithRegion(String region) {
        return configFile("profile test",
                          Pair.of(ProfileProperty.SIGV4A_SIGNING_REGION_SET, region));
    }

    private static ProfileFile configFile(String profileName, Pair<?, ?>... pairs) {
        String values = Arrays.stream(pairs)
                              .map(pair -> String.format("%s=%s", pair.left(), pair.right()))
                              .collect(Collectors.joining(System.lineSeparator()));
        String contents = String.format("[%s]%n%s", profileName, values);

        return ProfileFile.builder()
                          .content(new StringInputStream(contents))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    private static Set<String> createSet(String... values) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, values);
        return set;
    }
}
