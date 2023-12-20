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

package software.amazon.awssdk.utils.internal.proxy;

import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsProperty;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ProxyConfigProvider;
import software.amazon.awssdk.utils.ProxySystemSetting;

/**
 * An implementation of the {@link ProxyConfigProvider} interface that retrieves proxy configuration settings from system
 * properties. This class is responsible for extracting proxy host, port, username, and password settings from system properties
 * based on the specified proxy scheme (HTTP or HTTPS).
 *
 * @see ProxyConfigProvider
 */
@SdkInternalApi
public class ProxySystemPropertyConfigProvider implements ProxyConfigProvider {
    private static final Logger log = Logger.loggerFor(ProxySystemPropertyConfigProvider.class);

    private final String scheme;

    public ProxySystemPropertyConfigProvider(String scheme) {
        this.scheme = scheme == null ? "http" : scheme;
    }

    private static Integer safelyParseInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            log.warn(() -> "Failed to parse string.", e);
        }
        return null;
    }

    @Override
    public int port() {
        return Objects.equals(this.scheme, HTTPS) ?
               ProxySystemSetting.HTTPS_PROXY_PORT.getStringValue()
                                                  .map(ProxySystemPropertyConfigProvider::safelyParseInt)
                                                  .orElse(0) :
               ProxySystemSetting.PROXY_PORT.getStringValue()
                                            .map(ProxySystemPropertyConfigProvider::safelyParseInt)
                                            .orElse(0);
    }

    @Override
    public Optional<String> userName() {
        return Objects.equals(this.scheme, HTTPS) ? ProxySystemSetting.HTTPS_PROXY_USERNAME.getStringValue() :
               ProxySystemSetting.PROXY_USERNAME.getStringValue();
    }

    @Override
    public Optional<String> password() {
        return Objects.equals(scheme, HTTPS) ? ProxySystemSetting.HTTPS_PROXY_PASSWORD.getStringValue() :
               ProxySystemSetting.PROXY_PASSWORD.getStringValue();
    }

    @Override
    public String host() {
        return Objects.equals(scheme, HTTPS) ? ProxySystemSetting.HTTPS_PROXY_HOST.getStringValue().orElse(null) :
               ProxySystemSetting.PROXY_HOST.getStringValue().orElse(null);
    }

    @Override
    public Set<String> nonProxyHosts() {
        return parseNonProxyHostsProperty();
    }
}
