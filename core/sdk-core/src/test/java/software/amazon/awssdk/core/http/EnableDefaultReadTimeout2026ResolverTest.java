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

package software.amazon.awssdk.core.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class EnableDefaultReadTimeout2026ResolverTest {
    private static String enableDefaultReadTimeout2026Save;

    @BeforeAll
    static void setup() {
        enableDefaultReadTimeout2026Save = System.getProperty(SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026.property());
    }

    @AfterAll
    static void teardown() {
        if (enableDefaultReadTimeout2026Save != null) {
            System.setProperty(SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026.property(),
                               enableDefaultReadTimeout2026Save);
        } else {
            System.clearProperty(SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026.property());
        }
    }

    @BeforeEach
    void methodSetup() {
        System.clearProperty(SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026.property());
    }

    @Test
    void systemSetting_usesExpectedEnvironmentVariableAndSystemPropertyNames() {
        assertThat(SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026.environmentVariable())
            .isEqualTo("AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026");
        assertThat(SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026.property())
            .isEqualTo("aws.enableDefaultReadTimeout2026");
    }

    @ParameterizedTest
    @MethodSource("params")
    void resolve_behavesCorrectly(TestParams params) {
        EnvironmentVariableHelper.run((env) -> {
            if (params.systemProperty != null) {
                System.setProperty(SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026.property(), params.systemProperty);
            }

            if (params.envVar != null) {
                env.set(SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026.environmentVariable(), params.envVar);
            }

            EnableDefaultReadTimeout2026Resolver resolver =
                new EnableDefaultReadTimeout2026Resolver().defaultEnableReadTimeout2026(params.defaultEnableReadTimeout2026);

            assertThat(resolver.resolve()).isEqualTo(params.expected);
        });
    }

    private static Stream<TestParams> params() {
        return Stream.of(
            // default
            new TestParams().expected(false),

            // precedence testing
            new TestParams().systemProperty("true").defaultEnableReadTimeout2026(true).expected(true),
            new TestParams().systemProperty("false").defaultEnableReadTimeout2026(true).expected(false),
            new TestParams().envVar("true").defaultEnableReadTimeout2026(true).expected(true),
            new TestParams().envVar("false").defaultEnableReadTimeout2026(true).expected(false),
            new TestParams().defaultEnableReadTimeout2026(true).expected(true),
            new TestParams().defaultEnableReadTimeout2026(false).expected(false)
        );
    }

    private static class TestParams {
        private String systemProperty;
        private String envVar;
        private Boolean defaultEnableReadTimeout2026;
        private boolean expected;

        public TestParams systemProperty(String systemProperty) {
            this.systemProperty = systemProperty;
            return this;
        }

        public TestParams envVar(String envVar) {
            this.envVar = envVar;
            return this;
        }

        public TestParams defaultEnableReadTimeout2026(Boolean defaultEnableReadTimeout2026) {
            this.defaultEnableReadTimeout2026 = defaultEnableReadTimeout2026;
            return this;
        }

        public TestParams expected(boolean expected) {
            this.expected = expected;
            return this;
        }
    }
}
