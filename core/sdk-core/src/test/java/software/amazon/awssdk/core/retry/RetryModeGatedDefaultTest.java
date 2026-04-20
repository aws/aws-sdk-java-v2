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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Tests for the gated default {@link RetryMode} behavior controlled by
 * {@link SdkSystemSetting#AWS_NEW_RETRIES_2026}.
 */
class RetryModeGatedDefaultTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
        System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
    }

    @Test
    void defaultRetryMode_returnsLegacy_whenGateIsUnset() {
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.LEGACY);
    }

    @ParameterizedTest(name = "gate=\"{0}\" -> {1}")
    @CsvSource({
        "false, LEGACY",
        "true,  STANDARD"
    })
    void defaultRetryMode_reflectsGate_whenSetViaSystemProperty(String gateValue, RetryMode expected) {
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), gateValue);
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "gate=\"{0}\" -> {1}")
    @CsvSource({
        "false, LEGACY",
        "true,  STANDARD"
    })
    void defaultRetryMode_reflectsGate_whenSetViaEnvVar(String gateValue, RetryMode expected) {
        EnvironmentVariableHelper.run(helper -> {
            helper.set(SdkSystemSetting.AWS_NEW_RETRIES_2026, gateValue);
            assertThat(RetryMode.defaultRetryMode()).isEqualTo(expected);
        });
    }

    @Test
    void defaultRetryMode_changesDynamically_whenGateSystemPropertyChangesAtRuntime() {
        // Initially unset — should be LEGACY
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.LEGACY);

        // Enable gate — should switch to STANDARD
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "true");
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.STANDARD);

        // Disable gate — should revert to LEGACY
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "false");
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.LEGACY);

        // Clear gate — should fall back to default (LEGACY)
        System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.LEGACY);
    }

    @ParameterizedTest(name = "gate=\"{0}\" retryMode=\"{1}\" -> {2}")
    @CsvSource({
        "true,  legacy,   LEGACY",
        "false, standard, STANDARD",
        "false, adaptive, ADAPTIVE_V2"
    })
    void resolve_honorsExplicitRetryMode_regardlessOfGate(String gateValue, String retryModeValue, RetryMode expected) {
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), gateValue);
        System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), retryModeValue);
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(expected);
    }

    @Test
    void resolve_throwsIllegalStateException_whenInvalidRetryModeConfigured() {
        System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), "invalid_mode");
        assertThatThrownBy(RetryMode::defaultRetryMode).isInstanceOf(IllegalStateException.class);
    }
}
