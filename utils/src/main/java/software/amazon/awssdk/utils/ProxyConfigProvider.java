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

import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.internal.proxy.ProxyEnvironmentVariableConfigProvider;
import software.amazon.awssdk.utils.internal.proxy.ProxySystemPropertyConfigProvider;


/**
 * Interface for providing proxy configuration settings. Implementations of this interface can retrieve proxy configuration
 * from various sources such as system properties and environment variables.
 **/
@SdkProtectedApi
public interface ProxyConfigProvider {

    /**
     * Constant representing the HTTPS scheme.
     */
    String HTTPS = "https";



    /**
     * Returns a new {@code ProxyConfigProvider} that retrieves proxy configuration from system properties.
     *
     * @param scheme The URI scheme for which the proxy configuration is needed (e.g., "http" or "https").
     * @return A {@code ProxyConfigProvider} for system property-based proxy configuration.
     */
    static ProxyConfigProvider fromSystemPropertySettings(String scheme) {
        return new ProxySystemPropertyConfigProvider(scheme);
    }


    /**
     * Returns a new {@code ProxyConfigProvider} that retrieves proxy configuration from environment variables.
     *
     * @param scheme The URI scheme for which the proxy configuration is needed (e.g., "http" or "https").
     * @return A {@code ProxyConfigProvider} for environment variable-based proxy configuration.
     */
    static ProxyConfigProvider fromEnvironmentSettings(String scheme) {
        return new ProxyEnvironmentVariableConfigProvider(scheme);
    }

    /**
     * Returns a {@code ProxyConfigProvider} based on the specified settings for using system properties, environment
     * variables, and the scheme.
     *
     * @param useSystemPropertyValues       A {@code Boolean} indicating whether to use system property values.
     * @param useEnvironmentVariableValues  A {@code Boolean} indicating whether to use environment variable values.
     * @param scheme                        The URI scheme for which the proxy configuration is needed (e.g., "http" or "https").
     * @return A {@code ProxyConfigProvider} based on the specified settings.
     */
    static ProxyConfigProvider fromSystemEnvironmentSettings(Boolean useSystemPropertyValues,
                                                             Boolean useEnvironmentVariableValues,
                                                             String scheme) {
        ProxyConfigProvider resultProxyConfig = null;
        if (useSystemPropertyValues) {
            resultProxyConfig = fromSystemPropertySettings(scheme);
        } else if (useEnvironmentVariableValues) {
            return fromEnvironmentSettings(scheme);
        }
        boolean isProxyConfigurationNotSet = resultProxyConfig != null && resultProxyConfig.host() == null
                                             && resultProxyConfig.port() == 0
                                             && !resultProxyConfig.password().isPresent()
                                             && !resultProxyConfig.userName().isPresent()
                                             && CollectionUtils.isNullOrEmpty(resultProxyConfig.nonProxyHosts());

        if (isProxyConfigurationNotSet && useEnvironmentVariableValues) {
            return fromEnvironmentSettings(scheme);

        }
        return resultProxyConfig;
    }

    /**
     * Gets the proxy port.
     *
     * @return The proxy port.
     */
    int port();

    /**
     * Gets the proxy username if available.
     *
     * @return An optional containing the proxy username, if available.
     */
    Optional<String> userName();

    /**
     * Gets the proxy password if available.
     *
     * @return An optional containing the proxy password, if available.
     */
    Optional<String> password();

    /**
     * Gets the proxy host.
     *
     * @return The proxy host.
     */
    String host();

    /**
     * Gets the set of non-proxy hosts.
     *
     * @return A set containing the non-proxy host names.
     */
    Set<String> nonProxyHosts();
}
