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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Validate;

@RunWith(Parameterized.class)
public class RetryPolicyMaxRetriesTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @Parameterized.Parameter
    public TestData testData;

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] {
            // Test defaults
            new TestData(null, null, null, null, null, 3),
            new TestData(null, null, null, null, "PropertyNotSet", 3),

            // Test precedence
            new TestData("9", "2", "standard", "standard", "PropertySetToStandard", 8),
            new TestData(null, "9", "standard", "standard", "PropertySetToStandard", 8),
            new TestData(null, null, "standard", "standard", "PropertySetToStandard", 2),
            new TestData(null, null, null, "standard", "PropertySetToStandard", 2),
            new TestData(null, null, null, null, "PropertySetToStandard", 2),

            // Test invalid values
            new TestData("wrongValue", null, null, null, null, null),
            new TestData(null, "wrongValue", null, null, null, null),
            new TestData(null, null, "wrongValue", null, null, null),
            new TestData(null, null, null, "wrongValue", null, null),
            new TestData(null, null, null, null, "PropertySetToUnsupportedValue", null),
            });
    }

    @BeforeClass
    public static void classSetup() {
        // If this caches any values, make sure it's cached with the default (non-modified) configuration.
        RetryPolicy.defaultRetryPolicy();
    }

    @Before
    @After
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_MAX_ATTEMPTS.property());
        System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_PROFILE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_CONFIG_FILE.property());
    }

    @Test
    public void differentCombinationOfConfigs_shouldResolveCorrectly() {
        if (testData.attemptCountEnvVarValue != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_MAX_ATTEMPTS.environmentVariable(), testData.attemptCountEnvVarValue);
        }

        if (testData.attemptCountSystemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_MAX_ATTEMPTS.property(), testData.attemptCountSystemProperty);
        }

        if (testData.envVarValue != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), testData.envVarValue);
        }

        if (testData.systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), testData.systemProperty);
        }

        if (testData.configFile != null) {
            String diskLocationForFile = diskLocationForConfig(testData.configFile);
            Validate.isTrue(Files.isReadable(Paths.get(diskLocationForFile)), diskLocationForFile + " is not readable.");
            System.setProperty(ProfileFileSystemSetting.AWS_PROFILE.property(), "default");
            System.setProperty(ProfileFileSystemSetting.AWS_CONFIG_FILE.property(), diskLocationForFile);
        }

        if (testData.expected == null) {
            assertThatThrownBy(() -> RetryPolicy.forRetryMode(RetryMode.defaultRetryMode())).isInstanceOf(RuntimeException.class);
        } else {
            assertThat(RetryPolicy.forRetryMode(RetryMode.defaultRetryMode()).numRetries()).isEqualTo(testData.expected);
        }
    }

    private String diskLocationForConfig(String configFileName) {
        return getClass().getResource(configFileName).getFile();
    }

    private static class TestData {
        private final String attemptCountSystemProperty;
        private final String attemptCountEnvVarValue;
        private final String envVarValue;
        private final String systemProperty;
        private final String configFile;
        private final Integer expected;

        TestData(String attemptCountSystemProperty,
                 String attemptCountEnvVarValue,
                 String retryModeSystemProperty,
                 String retryModeEnvVarValue,
                 String configFile,
                 Integer expected) {
            this.attemptCountSystemProperty = attemptCountSystemProperty;
            this.attemptCountEnvVarValue = attemptCountEnvVarValue;
            this.envVarValue = retryModeEnvVarValue;
            this.systemProperty = retryModeSystemProperty;
            this.configFile = configFile;
            this.expected = expected;
        }
    }
}