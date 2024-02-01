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

package software.amazon.awssdk.regions.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

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
        String systemProperty, String envVar, boolean expected) {

        setUpSystemSettings(systemProperty, envVar);

        Ec2MetadataDisableV1Resolver resolver = Ec2MetadataDisableV1Resolver.create();
        assertThat(resolver.resolve()).isEqualTo(expected);
    }

    private static Stream<Arguments> booleanConfigValues() {
        return Stream.of(
            Arguments.of(null, null, false),
            Arguments.of("false", null, false),
            Arguments.of("true", null, true),
            Arguments.of(null, "false", false),
            Arguments.of(null, "true", true),
            Arguments.of(null, null, false),
            Arguments.of("false", "true", false),
            Arguments.of("true", "false", true)
        );
    }

    @ParameterizedTest(name = "{index} - sys:{0}, env:{1}")
    @MethodSource("nonBooleanConfigValues")
    public void resolveDisableValue_whenNonBoolean_throws(String systemProperty, String envVar) {
        setUpSystemSettings(systemProperty, envVar);

        Ec2MetadataDisableV1Resolver resolver = Ec2MetadataDisableV1Resolver.create();
        assertThatThrownBy(resolver::resolve).isInstanceOf(IllegalStateException.class)
                                             .hasMessageContaining("but should be 'false' or 'true'");
    }

    private static Stream<Arguments> nonBooleanConfigValues() {
        return Stream.of(
            Arguments.of("foo", null, null),
            Arguments.of(null, "foo", null)
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
}
