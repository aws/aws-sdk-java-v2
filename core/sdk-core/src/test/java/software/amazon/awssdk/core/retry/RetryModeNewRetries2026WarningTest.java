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

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.LogCaptor;

@SuppressWarnings("deprecation")
class RetryModeNewRetries2026WarningTest {
    private static final String OLD_GATE_PROPERTY = SdkSystemSetting.AWS_NEW_RETRIES_2026.property();

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @BeforeEach
    void setup() throws Exception {
        // Keep the host's environment from setting the old gate
        environmentVariableHelper.remove(SdkSystemSetting.AWS_NEW_RETRIES_2026);
        System.clearProperty(OLD_GATE_PROPERTY);
        warningLogged().set(false);
    }

    @AfterEach
    void cleanup() throws Exception {
        environmentVariableHelper.reset();
        System.clearProperty(OLD_GATE_PROPERTY);
        warningLogged().set(false);
    }

    @Test
    void oldGateSet_logsWarningOnce() {
        System.setProperty(OLD_GATE_PROPERTY, "true");
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            RetryMode.resolver().resolve();
            RetryMode.resolver().resolve();

            assertThat(logCaptor.loggedEvents()).filteredOn(RetryModeNewRetries2026WarningTest::isOldGateWarning).hasSize(1);
        }
    }

    @Test
    void oldGateNotSet_logsNoWarning() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            RetryMode.resolver().resolve();

            assertThat(logCaptor.loggedEvents()).noneMatch(RetryModeNewRetries2026WarningTest::isOldGateWarning);
        }
    }

    private static boolean isOldGateWarning(LogEvent event) {
        return event.getLevel() == Level.WARN && event.getMessage().getFormattedMessage().contains("AWS_NEW_RETRIES_2026");
    }

    private static AtomicBoolean warningLogged() throws Exception {
        Field field = RetryMode.Resolver.class.getDeclaredField("NEW_RETRIES_2026_WARNING_LOGGED");
        field.setAccessible(true);
        return (AtomicBoolean) field.get(null);
    }
}
