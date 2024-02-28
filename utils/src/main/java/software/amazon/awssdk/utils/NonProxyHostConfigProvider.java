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

import java.util.Collections;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.internal.proxy.NonProxyHostEnvironmentVariableConfigProvider;
import software.amazon.awssdk.utils.internal.proxy.NonProxyHostSystemPropertyConfigProvider;


/**
 * Interface for providing proxy configuration settings. Implementations of this interface can retrieve proxy configuration from
 * various sources such as system properties and environment variables.
 **/

@SdkProtectedApi
public interface NonProxyHostConfigProvider {


    /**
     * Returns a {@link NonProxyHostConfigProvider} based on the specified settings for using system properties, environment
     * variables, and the scheme.
     *
     * @param useSystemPropertyValues       A {@code Boolean} indicating whether to use system property values.
     * @param useEnvironmentVariableValues  A {@code Boolean} indicating whether to use environment variable values.
     * @return A {@link NonProxyHostConfigProvider} based on the specified settings.
     */
    static NonProxyHostConfigProvider fromSystemEnvironmentSettings(Boolean useSystemPropertyValues,
                                                                    Boolean useEnvironmentVariableValues) {
        NonProxyHostConfigProvider resultProxyConfig = null;
        if (Boolean.TRUE.equals(useSystemPropertyValues)) {
            resultProxyConfig = fromSystemPropertySettings();
        } else if (Boolean.TRUE.equals(useEnvironmentVariableValues)) {
            return fromEnvironmentSettings();
        }
        if (resultProxyConfig != null && resultProxyConfig.nonProxyHosts() == null
            && Boolean.TRUE.equals(useEnvironmentVariableValues)) {
            return fromEnvironmentSettings();
        }
        return resultProxyConfig;
    }

    static NonProxyHostConfigProvider fromEnvironmentSettings() {
        return new NonProxyHostEnvironmentVariableConfigProvider();
    }

    static NonProxyHostConfigProvider fromSystemPropertySettings() {
        return new NonProxyHostSystemPropertyConfigProvider();
    }

    /**
     * Gets the set of non-proxy hosts.
     *
     * @return A set containing the non-proxy host names.
     */
    Set<String> nonProxyHosts();
}
