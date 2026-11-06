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
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Tests that the default {@link RetryMode} is {@link RetryMode#STANDARD}, and that the removed retry behavior 2.1 opt-in
 * setting ({@code aws.newRetries2026} / {@code AWS_NEW_RETRIES_2026}) no longer has any effect.
 */
class RetryModeGatedDefaultTest {
    private static final String OLD_GATE_PROPERTY = "aws.newRetries2026";
    private static final String OLD_GATE_ENV_VAR = "AWS_NEW_RETRIES_2026";

    @BeforeEach
    @AfterEach
    void cleanup() {
        System.clearProperty(OLD_GATE_PROPERTY);
        System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
        System.clearProperty(ProfileFileSystemSetting.AWS_CONFIG_FILE.property());
    }

    @Test
    void defaultRetryMode_returnsStandard_whenGateIsUnset() {
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.STANDARD);
    }

    @ParameterizedTest(name = "gate=\"{0}\"")
    @ValueSource(strings = {"false", "true", "not-a-boolean"})
    void defaultRetryMode_returnsStandard_whenGateSetViaSystemProperty(String gateValue) {
        System.setProperty(OLD_GATE_PROPERTY, gateValue);
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.STANDARD);
    }

    @ParameterizedTest(name = "gate=\"{0}\"")
    @ValueSource(strings = {"false", "true"})
    void defaultRetryMode_returnsStandard_whenGateSetViaEnvVar(String gateValue) {
        EnvironmentVariableHelper.run(helper -> {
            helper.set(OLD_GATE_ENV_VAR, gateValue);
            assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.STANDARD);
        });
    }

    @Test
    void defaultRetryMode_returnsStandard_whenGateFalseAndNoProfileFile() {
        System.setProperty(ProfileFileSystemSetting.AWS_CONFIG_FILE.property(), "/tmp/does-not-exist-aws-config-file");
        System.setProperty(OLD_GATE_PROPERTY, "false");
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(RetryMode.STANDARD);
    }

    @Test
    @SuppressWarnings("deprecation")
    void resolver_ignoresDefaultNewRetries2026() {
        assertThat(RetryMode.resolver().defaultNewRetries2026(false).resolve()).isEqualTo(RetryMode.STANDARD);
        assertThat(RetryMode.resolver().defaultNewRetries2026(true).resolve()).isEqualTo(RetryMode.STANDARD);
    }

    @Test
    void resolver_honorsDefaultRetryMode() {
        assertThat(RetryMode.resolver().defaultRetryMode(RetryMode.LEGACY).resolve()).isEqualTo(RetryMode.LEGACY);
    }

    @ParameterizedTest(name = "gate=\"{0}\" retryMode=\"{1}\" -> {2}")
    @CsvSource({
        "true,  legacy,   LEGACY",
        "false, legacy,   LEGACY",
        "false, standard, STANDARD",
        "false, adaptive, ADAPTIVE_V2"
    })
    void resolve_honorsExplicitRetryMode_regardlessOfGate(String gateValue, String retryModeValue, RetryMode expected) {
        System.setProperty(OLD_GATE_PROPERTY, gateValue);
        System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), retryModeValue);
        assertThat(RetryMode.defaultRetryMode()).isEqualTo(expected);
    }

    @Test
    void resolve_throwsIllegalStateException_whenInvalidRetryModeConfigured() {
        System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), "invalid_mode");
        assertThatThrownBy(RetryMode::defaultRetryMode).isInstanceOf(IllegalStateException.class);
    }
}
