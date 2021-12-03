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

package software.amazon.awssdk.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Validate;

public class RetryPolicyMaxRetriesTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    public static Stream<Arguments> testData() {
        return Stream.of(
            // Test defaults
            arguments(null, null, null, null, null, 3),
            arguments(null, null, null, null, "PropertyNotSet", 3),

            // Test precedence
            arguments("9", "2", "standard", "standard", "PropertySetToStandard", 8),
            arguments(null, "9", "standard", "standard", "PropertySetToStandard", 8),
            arguments(null, null, "standard", "standard", "PropertySetToStandard", 2),
            arguments(null, null, null, "standard", "PropertySetToStandard", 2),
            arguments(null, null, null, null, "PropertySetToStandard", 2),

            // Test invalid values
            arguments("wrongValue", null, null, null, null, null),
            arguments(null, "wrongValue", null, null, null, null),
            arguments(null, null, "wrongValue", null, null, null),
            arguments(null, null, null, "wrongValue", null, null),
            arguments(null, null, null, null, "PropertySetToUnsupportedValue", null)
        );
    }

    @BeforeAll
    public static void classSetup() {
        // If this caches any values, make sure it's cached with the default (non-modified) configuration.
        RetryPolicy.defaultRetryPolicy();
    }

    @BeforeEach
    @AfterEach
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_MAX_ATTEMPTS.property());
        System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_PROFILE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_CONFIG_FILE.property());
    }

    @ParameterizedTest
    @MethodSource("testData")
    void differentCombinationOfConfigs_shouldResolveCorrectly(String attemptCountSystemProperty,
                                                              String attemptCountEnvVarValue,
                                                              String systemProperty,
                                                              String envVarValue,
                                                              String configFile,
                                                              Integer expected) {
        if (attemptCountEnvVarValue != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_MAX_ATTEMPTS.environmentVariable(), attemptCountEnvVarValue);
        }

        if (attemptCountSystemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_MAX_ATTEMPTS.property(), attemptCountSystemProperty);
        }

        if (envVarValue != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), envVarValue);
        }

        if (systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), systemProperty);
        }

        if (configFile != null) {
            String diskLocationForFile = diskLocationForConfig(configFile);
            Validate.isTrue(Files.isReadable(Paths.get(diskLocationForFile)), diskLocationForFile + " is not readable.");
            System.setProperty(ProfileFileSystemSetting.AWS_PROFILE.property(), "default");
            System.setProperty(ProfileFileSystemSetting.AWS_CONFIG_FILE.property(), diskLocationForFile);
        }

        if (expected == null) {
            assertThatThrownBy(() -> RetryPolicy.forRetryMode(RetryMode.defaultRetryMode())).isInstanceOf(RuntimeException.class);
        } else {
            assertThat(RetryPolicy.forRetryMode(RetryMode.defaultRetryMode()).numRetries()).isEqualTo(expected);
        }
    }

    private String diskLocationForConfig(String configFileName) {
        return getClass().getResource(configFileName).getFile();
    }
}