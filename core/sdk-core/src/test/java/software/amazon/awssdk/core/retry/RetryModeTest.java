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
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Validate;

public class RetryModeTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    public static Stream<Arguments> testData() {
        return Stream.of(
            // Test defaults
            arguments(null, null, null, null, RetryMode.LEGACY),
            arguments(null, null, "PropertyNotSet", null, RetryMode.LEGACY),

            // Test resolution
            arguments("legacy", null, null, null, RetryMode.LEGACY),
            arguments("standard", null, null, null, RetryMode.STANDARD),
            arguments("adaptive", null, null, null, RetryMode.ADAPTIVE),
            arguments("lEgAcY", null, null, null, RetryMode.LEGACY),
            arguments("sTanDaRd", null, null, null, RetryMode.STANDARD),
            arguments("aDaPtIvE", null, null, null, RetryMode.ADAPTIVE),

            // Test precedence
            arguments("standard", "legacy", "PropertySetToLegacy", RetryMode.LEGACY, RetryMode.STANDARD),
            arguments("standard", null, null, RetryMode.LEGACY, RetryMode.STANDARD),
            arguments(null, "standard", "PropertySetToLegacy", RetryMode.LEGACY, RetryMode.STANDARD),
            arguments(null, "standard", null, RetryMode.LEGACY, RetryMode.STANDARD),
            arguments(null, null, "PropertySetToStandard", RetryMode.LEGACY, RetryMode.STANDARD),
            arguments(null, null, null, RetryMode.STANDARD, RetryMode.STANDARD),

            // Test invalid values
            arguments("wrongValue", null, null, null, IllegalStateException.class),
            arguments(null, "wrongValue", null, null, IllegalStateException.class),
            arguments(null, null, "PropertySetToUnsupportedValue", null, IllegalStateException.class),

            // Test capitalization standardization
            arguments("sTaNdArD", null, null, null, RetryMode.STANDARD),
            arguments(null, "sTaNdArD", null, null, RetryMode.STANDARD),
            arguments(null, null, "PropertyMixedCase", null, RetryMode.STANDARD)
        );
    }

    @BeforeEach
    @AfterEach
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_PROFILE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_CONFIG_FILE.property());
    }

    @ParameterizedTest
    @MethodSource("testData")
    void differentCombinationOfConfigs_shouldResolveCorrectly(String systemProperty,
                                                              String envVarValue,
                                                              String configFile,
                                                              RetryMode defaultMode,
                                                              Object expected) throws Exception {
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

        Callable<RetryMode> result = RetryMode.resolver().defaultRetryMode(defaultMode)::resolve;
        if (expected instanceof Class<?>) {
            Class<?> expectedClassType = (Class<?>) expected;
            assertThatThrownBy(result::call).isInstanceOf(expectedClassType);
        } else {
            assertThat(result.call()).isEqualTo(expected);
        }
    }

    private String diskLocationForConfig(String configFileName) {
        return getClass().getResource(configFileName).getFile();
    }
}