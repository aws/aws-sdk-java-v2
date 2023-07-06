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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class ProxyEnvironmentSettingTest {
    private final Map<String, String> savedEnvironmentVariableValues = new HashMap<>();

    private static final List<String> SAVED_ENVIRONMENT_VARIABLES = Arrays.asList("http_proxy",
                                                                                  "https_proxy",
                                                                                  "no_proxy",
                                                                                  "HTTP_PROXY",
                                                                                  "HTTPS_PROXY",
                                                                                  "NO_PROXY",
                                                                                  "nO_pRoXy");

    private EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    /**
     * Save the current state of the environment variables we're messing around with in these tests so that we can restore them
     * when we are done.
     */
    @BeforeEach
    public void saveEnvironment() throws Exception {
        for (String variable : SAVED_ENVIRONMENT_VARIABLES) {
            savedEnvironmentVariableValues.put(variable, System.getenv(variable));
        }
    }

    /**
     * Reset the environment variables after each test.
     */
    @AfterEach
    public void restoreEnvironment() throws Exception {
        for (String variable : SAVED_ENVIRONMENT_VARIABLES) {
            String savedValue = savedEnvironmentVariableValues.get(variable);

            if (savedValue == null) {
                ENVIRONMENT_VARIABLE_HELPER.remove(variable);
            } else {
                ENVIRONMENT_VARIABLE_HELPER.set(variable, savedValue);
            }
        }
    }

    @Test
    public void settingsReturnSomeForLowercaseAndUppercaseOnly() throws Exception {
        ENVIRONMENT_VARIABLE_HELPER.set("HTTP_PROXY", "http://localhost:25565");
        ENVIRONMENT_VARIABLE_HELPER.set("https_proxy", "https://localhost:25566");
        ENVIRONMENT_VARIABLE_HELPER.set("nO_pRoXy", "    ");

        assertThat(ProxyEnvironmentSetting.HTTP_PROXY.getStringValue()).isEqualTo(Optional.of("http://localhost:25565"));
        assertThat(ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue()).isEqualTo(Optional.of("https://localhost:25566"));
        assertThat(ProxyEnvironmentSetting.NO_PROXY.getStringValue()).isEmpty();

        ENVIRONMENT_VARIABLE_HELPER.remove("HTTP_PROXY");
        ENVIRONMENT_VARIABLE_HELPER.remove("https_proxy");
        ENVIRONMENT_VARIABLE_HELPER.remove("nO_pRoXy");
    }

    @Test
    public void settingsReturnEmptyForJustSpacesLowercase() throws Exception {
        ENVIRONMENT_VARIABLE_HELPER.set("http_proxy", "    ");
        ENVIRONMENT_VARIABLE_HELPER.set("https_proxy", "    ");
        ENVIRONMENT_VARIABLE_HELPER.set("no_proxy", "    ");

        assertThat(ProxyEnvironmentSetting.HTTP_PROXY.getStringValue()).isEmpty();
        assertThat(ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue()).isEmpty();
        assertThat(ProxyEnvironmentSetting.NO_PROXY.getStringValue()).isEmpty();

        ENVIRONMENT_VARIABLE_HELPER.remove("http_proxy");
        ENVIRONMENT_VARIABLE_HELPER.remove("https_proxy");
        ENVIRONMENT_VARIABLE_HELPER.remove("no_proxy");
    }

    @Test
    public void settingsReturnEmptyForJustSpacesUppercase() throws Exception {
        ENVIRONMENT_VARIABLE_HELPER.set("HTTP_PROXY", "    ");
        ENVIRONMENT_VARIABLE_HELPER.set("HTTPS_PROXY", "    ");
        ENVIRONMENT_VARIABLE_HELPER.set("NO_PROXY", "    ");

        assertThat(ProxyEnvironmentSetting.HTTP_PROXY.getStringValue()).isEmpty();
        assertThat(ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue()).isEmpty();
        assertThat(ProxyEnvironmentSetting.NO_PROXY.getStringValue()).isEmpty();

        ENVIRONMENT_VARIABLE_HELPER.remove("HTTP_PROXY");
        ENVIRONMENT_VARIABLE_HELPER.remove("HTTPS_PROXY");
        ENVIRONMENT_VARIABLE_HELPER.remove("NO_PROXY");
    }

}
