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

package software.amazon.awssdk.utils.internal;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * This is a backdoor to add overrides to the results of querying {@link SystemSetting}s. This is used for testing environment
 * variables within the SDK
 */
@SdkTestInternalApi
@SdkInternalApi
public final class SystemSettingUtilsTestBackdoor {
    private static final Map<String, String> ENVIRONMENT_OVERRIDES = new HashMap<>();

    private SystemSettingUtilsTestBackdoor() {
    }

    public static void addEnvironmentVariableOverride(String key, String value) {
        ENVIRONMENT_OVERRIDES.put(key, value);
    }

    public static void clearEnvironmentVariableOverrides() {
        ENVIRONMENT_OVERRIDES.clear();
    }

    static String getEnvironmentVariable(String key) {
        if (!ENVIRONMENT_OVERRIDES.isEmpty() && ENVIRONMENT_OVERRIDES.containsKey(key)) {
            return ENVIRONMENT_OVERRIDES.get(key);
        }
        // CHECKSTYLE:OFF - This is the only place we should access environment variables
        return System.getenv(key);
        // CHECKSTYLE:ON
    }
}
