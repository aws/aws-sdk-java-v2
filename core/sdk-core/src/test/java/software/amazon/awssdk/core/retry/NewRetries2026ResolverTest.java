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

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class NewRetries2026ResolverTest {
    private static String newRetries2026Save;

    @BeforeAll
    static void setup() {
        newRetries2026Save = System.getProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
    }

    @AfterAll
    static void teardown() {
        if (newRetries2026Save != null) {
            System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), newRetries2026Save);
        } else {
            System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
        }
    }

    @BeforeEach
    void methodSetup() {
        System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
    }

    @ParameterizedTest
    @MethodSource("params")
    void resolve_behavesCorrectly(TestParams params) {
        EnvironmentVariableHelper.run((env) -> {
            if (params.systemProperty != null) {
                System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), params.systemProperty);
            }

            if (params.envVar != null) {
                env.set(SdkSystemSetting.AWS_NEW_RETRIES_2026.environmentVariable(), params.envVar);
            }

            NewRetries2026Resolver resolver = new NewRetries2026Resolver().defaultNewRetries2026(params.defaultNewRetries2026);

            assertThat(resolver.resolve()).isEqualTo(params.expected);
        });
    }

    private static Stream<TestParams> params() {
        return Stream.of(
            // default
            new TestParams().expected(false),

            // precedence testing
            new TestParams().systemProperty("true").defaultNewRetries2026(true).expected(true),
            new TestParams().systemProperty("false").defaultNewRetries2026(true).expected(false),
            new TestParams().envVar("true").defaultNewRetries2026(true).expected(true),
            new TestParams().envVar("false").defaultNewRetries2026(true).expected(false),
            new TestParams().defaultNewRetries2026(true).expected(true),
            new TestParams().defaultNewRetries2026(false).expected(false)
        );
    }

    private static class TestParams {
        private String systemProperty;
        private String envVar;
        private Boolean defaultNewRetries2026;
        private boolean expected;

        public TestParams systemProperty(String systemProperty) {
            this.systemProperty = systemProperty;
            return this;
        }

        public TestParams envVar(String envVar) {
            this.envVar = envVar;
            return this;
        }

        public TestParams defaultNewRetries2026(Boolean defaultNewRetries2026) {
            this.defaultNewRetries2026 = defaultNewRetries2026;
            return this;
        }

        public TestParams expected(boolean expected) {
            this.expected = expected;
            return this;
        }
    }
}
