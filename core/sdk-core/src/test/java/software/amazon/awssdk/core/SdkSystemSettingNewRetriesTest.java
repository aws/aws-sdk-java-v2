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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Tests for the {@link SdkSystemSetting#AWS_NEW_RETRIES_2026} system setting.
 */
class SdkSystemSettingNewRetriesTest {

    @AfterEach
    void cleanup() {
        System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
    }

    @Test
    void defaultsToFalse_whenUnset() {
        assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.getBooleanValue()).hasValue(false);
    }

    @Test
    void returnsFalse_whenSystemPropertySetToFalse() {
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "false");
        assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.getBooleanValue()).hasValue(false);
    }

    @Test
    void returnsTrue_whenSystemPropertySetToTrue() {
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "true");
        assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.getBooleanValue()).hasValue(true);
    }

    @Test
    void returnsFalse_whenEnvVarSetToFalse() {
        EnvironmentVariableHelper.run(helper -> {
            helper.set(SdkSystemSetting.AWS_NEW_RETRIES_2026, "false");
            assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.getBooleanValue()).hasValue(false);
        });
    }

    @Test
    void returnsTrue_whenEnvVarSetToTrue() {
        EnvironmentVariableHelper.run(helper -> {
            helper.set(SdkSystemSetting.AWS_NEW_RETRIES_2026, "true");
            assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.getBooleanValue()).hasValue(true);
        });
    }

    @Test
    void systemPropertyTakesPrecedenceOverEnvVar() {
        EnvironmentVariableHelper.run(helper -> {
            System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "false");
            helper.set(SdkSystemSetting.AWS_NEW_RETRIES_2026, "true");
            assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.getBooleanValue()).hasValue(false);
        });
    }

    @Test
    void environmentVariable_isCorrectName() {
        assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.environmentVariable())
            .isEqualTo("AWS_NEW_RETRIES_2026");
    }

    @Test
    void systemProperty_isCorrectName() {
        assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.property())
            .isEqualTo("aws.newRetries2026");
    }

    @Test
    void defaultValue_isFalse() {
        assertThat(SdkSystemSetting.AWS_NEW_RETRIES_2026.defaultValue())
            .isEqualTo("false");
    }
}
