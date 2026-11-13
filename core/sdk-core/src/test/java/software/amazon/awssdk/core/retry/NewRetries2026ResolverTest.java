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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

@SuppressWarnings("deprecation")
public class NewRetries2026ResolverTest {
    private static final String OLD_GATE_PROPERTY = "aws.newRetries2026";
    private static final String OLD_GATE_ENV_VAR = "AWS_NEW_RETRIES_2026";

    @BeforeEach
    @AfterEach
    void cleanup() {
        System.clearProperty(OLD_GATE_PROPERTY);
        System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
    }

    @ParameterizedTest(name = "retryMode={0}, oldGate={1}, default={2}")
    @CsvSource(value = {
        "null,     null,  null",
        "null,     true,  true",
        "standard, true,  true",
        "adaptive, null,  null",
        "legacy,   null,  null",
        "legacy,   true,  true"
    }, nullValues = "null")
    void resolve_isAlwaysFalse(String retryMode, String oldGate, Boolean defaultNewRetries2026) {
        EnvironmentVariableHelper.run(env -> {
            env.remove(SdkSystemSetting.AWS_RETRY_MODE);
            env.set(ProfileFileSystemSetting.AWS_CONFIG_FILE, "does-not-exist");
            if (retryMode != null) {
                System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), retryMode);
            }
            if (oldGate != null) {
                env.set(OLD_GATE_ENV_VAR, oldGate);
                System.setProperty(OLD_GATE_PROPERTY, oldGate);
            }

            NewRetries2026Resolver resolver = new NewRetries2026Resolver().defaultNewRetries2026(defaultNewRetries2026);

            assertThat(resolver.resolve()).isFalse();
        });
    }

    /**
     * Service modules released before retry behavior 2.1 became the default (DynamoDB, STS) resolve the retry mode from their
     * client's profile, then call {@link NewRetries2026Resolver#resolve()}, which only sees the default profile. Here the
     * client's profile says {@code legacy} while the default profile resolves to {@code standard}. The resolver must still
     * return false, or those modules would apply their retry behavior 2.1 settings to a {@code LEGACY} client.
     */
    @Test
    void oldServiceModuleSequence_clientProfileLegacy_resolverStaysFalse() {
        EnvironmentVariableHelper.run(env -> {
            env.remove(SdkSystemSetting.AWS_RETRY_MODE);
            env.set(ProfileFileSystemSetting.AWS_CONFIG_FILE, "does-not-exist");
            ProfileFile clientProfile = ProfileFile.builder()
                                                   .content(new StringInputStream("[profile client]\nretry_mode = legacy"))
                                                   .type(ProfileFile.Type.CONFIGURATION)
                                                   .build();

            RetryMode clientRetryMode = RetryMode.resolver()
                                                 .profileFile(() -> clientProfile)
                                                 .profileName("client")
                                                 .resolve();

            assertThat(clientRetryMode).isEqualTo(RetryMode.LEGACY);
            assertThat(RetryMode.resolver().resolve()).isEqualTo(RetryMode.STANDARD);
            assertThat(new NewRetries2026Resolver().resolve()).isFalse();
        });
    }
}
