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

package software.amazon.awssdk.crtcore;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ProxySystemSetting;

/**
 * The base class for AWS CRT proxy configuration
 */
@SdkPublicApi
public abstract class CrtProxyConfiguration {
    private static final String HTTPS = "https";
    private final String scheme;
    private final String host;
    private final int port;

    private final String username;
    private final String password;
    private final Boolean useSystemPropertyValues;

    protected CrtProxyConfiguration(DefaultBuilder<?> builder) {
        this.useSystemPropertyValues = builder.useSystemPropertyValues;
        this.scheme = builder.scheme;
        this.host = resolveHost(builder.host);
        this.port = resolvePort(builder.port);
        this.username = builder.username;
        this.password = builder.password;
    }

    /**
     * @return The proxy scheme.
     */
    public final String scheme() {
        return scheme;
    }

    /**
     * @return The proxy host from the configuration if set, else from the "https.proxyHost" or "http.proxyHost" system property,
     * based on the scheme used, if {@link Builder#useSystemPropertyValues(Boolean)} is set to true
     */
    public final String host() {
        return host;
    }

    /**
     * @return The proxy port from the configuration if set, else from the "https.proxyPort" or "http.proxyPort" system property,
     * based on the scheme used, if {@link Builder#useSystemPropertyValues(Boolean)} is set to true
     */
    public final int port() {
        return port;
    }

    /**
     * @return The proxy username from the configuration if set, else from the "https.proxyUser" or "http.proxyUser" system
     * property, based on the scheme used, if {@link Builder#useSystemPropertyValues(Boolean)} is set to true
     * */
    public final String username() {
        if (Objects.equals(scheme(), HTTPS)) {
            return resolveValue(username, ProxySystemSetting.HTTPS_PROXY_USERNAME);
        }
        return resolveValue(username, ProxySystemSetting.PROXY_USERNAME);
    }

    /**
     * @return The proxy password from the configuration if set, else from the "https.proxyPassword" or "http.proxyPassword"
     * system property, based on the scheme used, if {@link Builder#useSystemPropertyValues(Boolean)} is set
     * to true
     * */
    public final String password() {
        if (Objects.equals(scheme(), HTTPS)) {
            return resolveValue(password, ProxySystemSetting.HTTPS_PROXY_PASSWORD);
        }
        return resolveValue(password, ProxySystemSetting.PROXY_PASSWORD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CrtProxyConfiguration that = (CrtProxyConfiguration) o;

        if (port != that.port) {
            return false;
        }
        if (!Objects.equals(scheme, that.scheme)) {
            return false;
        }
        if (!Objects.equals(host, that.host)) {
            return false;
        }
        if (!Objects.equals(username, that.username)) {
            return false;
        }
        if (!Objects.equals(password, that.password)) {
            return false;
        }
        return Objects.equals(useSystemPropertyValues, that.useSystemPropertyValues);
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (useSystemPropertyValues != null ? useSystemPropertyValues.hashCode() : 0);
        return result;
    }

    /**
     * Builder for {@link CrtProxyConfiguration}.
     */
    public interface Builder {

        /**
         * Set the hostname of the proxy.
         * @param host The proxy host.
         * @return This object for method chaining.
         */
        Builder host(String host);

        /**
         * Set the port that the proxy expects connections on.
         * @param port The proxy port.
         * @return This object for method chaining.
         */
        Builder port(int port);

        /**
         * The HTTP scheme to use for connecting to the proxy. Valid values are {@code http} and {@code https}.
         * <p>
         * The client defaults to {@code http} if none is given.
         *
         * @param scheme The proxy scheme.
         * @return This object for method chaining.
         */
        Builder scheme(String scheme);

        /**
         * The username to use for basic proxy authentication
         * <p>
         * If not set, the client will not use basic authentication
         *
         * @param username The basic authentication username.
         * @return This object for method chaining.
         */
        Builder username(String username);

        /**
         * The password to use for basic proxy authentication
         * <p>
         * If not set, the client will not use basic authentication
         *
         * @param password The basic authentication password.
         * @return This object for method chaining.
         */
        Builder password(String password);

        /**
         * The option whether to use system property values from {@link ProxySystemSetting} if any of the config options
         * are missing. The value is set to "true" by default which means SDK will automatically use system property values if
         * options are not provided during building the {@link CrtProxyConfiguration} object. To disable this behaviour, set this
         * value to false.
         *
         * @param useSystemPropertyValues The option whether to use system property values
         * @return This object for method chaining.
         */
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);

        CrtProxyConfiguration build();
    }

    private String resolveHost(String host) {
        if (Objects.equals(scheme(), HTTPS)) {
            return resolveValue(host, ProxySystemSetting.HTTPS_PROXY_HOST);
        }
        return resolveValue(host, ProxySystemSetting.PROXY_HOST);
    }

    private int resolvePort(int port) {
        if (port == 0 && Boolean.TRUE.equals(useSystemPropertyValues)) {
            if (Objects.equals(scheme(), HTTPS)) {
                return ProxySystemSetting.HTTPS_PROXY_PORT.getStringValue().map(Integer::parseInt).orElse(0);
            }
            return ProxySystemSetting.PROXY_PORT.getStringValue().map(Integer::parseInt).orElse(0);
        }
        return port;
    }

    /**
     * Uses the configuration options, system setting property and returns the final value of the given member.
     */
    private String resolveValue(String value, ProxySystemSetting systemSetting) {
        return value == null && Boolean.TRUE.equals(useSystemPropertyValues) ?
               systemSetting.getStringValue().orElse(null) : value;
    }

    protected abstract static class DefaultBuilder<B extends Builder> implements Builder {
        private String scheme;
        private String host;
        private int port = 0;
        private String username;
        private String password;
        private Boolean useSystemPropertyValues = Boolean.TRUE;

        protected DefaultBuilder() {
        }

        protected DefaultBuilder(CrtProxyConfiguration proxyConfiguration) {
            this.useSystemPropertyValues = proxyConfiguration.useSystemPropertyValues;
            this.scheme = proxyConfiguration.scheme;
            this.host = proxyConfiguration.host;
            this.port = proxyConfiguration.port;
            this.username = proxyConfiguration.username;
            this.password = proxyConfiguration.password;
        }

        @Override
        public B scheme(String scheme) {
            this.scheme = scheme;
            return (B) this;
        }

        @Override
        public B host(String host) {
            this.host = host;
            return (B) this;
        }

        @Override
        public B port(int port) {
            this.port = port;
            return (B) this;
        }

        @Override
        public B username(String username) {
            this.username = username;
            return (B) this;
        }

        @Override
        public B password(String password) {
            this.password = password;
            return (B) this;
        }

        @Override
        public B useSystemPropertyValues(Boolean useSystemPropertyValues) {
            this.useSystemPropertyValues = useSystemPropertyValues;
            return (B) this;
        }

        public void setUseSystemPropertyValues(Boolean useSystemPropertyValues) {
            useSystemPropertyValues(useSystemPropertyValues);
        }

    }
}