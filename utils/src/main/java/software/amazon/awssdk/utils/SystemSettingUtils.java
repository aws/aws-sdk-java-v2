/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.utils.OptionalUtils.firstPresent;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of static utility methods for shared code in {@link SystemSetting}.
 */
final class SystemSettingUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SystemSettingUtils.class);

    private SystemSettingUtils() {}

    /**
     * Resolve the value of this system setting, loading it from the System by checking:
     * <ol>
     *     <li>The system properties.</li>
     *     <li>The environment variables.</li>
     *     <li>The default value.</li>
     * </ol>
     */
    static Optional<String> resolveSetting(SystemSetting setting) {
        return firstPresent(resolveProperty(setting), () -> resolveEnvironmentVariable(setting), () -> resolveDefault(setting))
                .map(String::trim);
    }

    /**
     * Attempt to load this setting from the system properties.
     */
    private static Optional<String> resolveProperty(SystemSetting setting) {
        // CHECKSTYLE:OFF - This is the only place we're allowed to use System.getProperty
        return Optional.ofNullable(setting.property()).map(System::getProperty);
        // CHECKSTYLE:ON
    }

    /**
     * Attempt to load this setting from the environment variables.
     */
    private static Optional<String> resolveEnvironmentVariable(SystemSetting setting) {
        try {
            // CHECKSTYLE:OFF - This is the only place we're allowed to use System.getenv
            return Optional.ofNullable(setting.environmentVariable()).map(System::getenv);
            // CHECKSTYLE:ON
        } catch (SecurityException e) {
            LOG.debug("Unable to load the environment variable '{}' because the security manager did not allow the SDK" +
                      " to read this system property. This setting will be assumed to be null", setting.environmentVariable(), e);
            return Optional.empty();
        }
    }

    /**
     * Load the default value from the setting.
     */
    private static Optional<String> resolveDefault(SystemSetting setting) {
        return Optional.ofNullable(setting.defaultValue());
    }

    /**
     * Convert a string to boolean safely (as opposed to the less strict {@link Boolean#parseBoolean(String)}). If a customer
     * specifies a boolean value it should be "true" or "false" (case insensitive) or an exception will be thrown.
     */
    static Boolean safeStringToBoolean(SystemSetting setting, String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        }

        throw new IllegalStateException("Environment variable '" + setting.environmentVariable() + "' or system property '" +
                                        setting.property() + "' was defined as '" + value + "', but should be 'false' or 'true'");
    }
}
