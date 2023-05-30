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

package software.amazon.awssdk.http.crt;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;


/**
 * Proxy configuration for {@link AwsCrtAsyncHttpClient}. This class is used to configure an HTTPS or HTTP proxy to be used by
 * the {@link AwsCrtAsyncHttpClient}.
 *
 * @see AwsCrtAsyncHttpClient.Builder#proxyConfiguration(ProxyConfiguration)
 */
@SdkPublicApi
public final class ProxyConfiguration implements ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration> {
    private static final String HTTPS = "https";
    private final String scheme;
    private final String host;
    private final int port;

    private final String username;
    private final String password;
    private final Boolean useSystemPropertyValues;

    private ProxyConfiguration(BuilderImpl builder) {
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
    public String scheme() {
        return scheme;
    }

    /**
     * @return The proxy host from the configuration if set, else from the "https.proxyHost" or "http.proxyHost" system property,
     * based on the scheme used, if {@link ProxyConfiguration.Builder#useSystemPropertyValues(Boolean)} is set to true
     */
    public String host() {
        return host;
    }

    /**
     * @return The proxy port from the configuration if set, else from the "https.proxyPort" or "http.proxyPort" system property,
     * based on the scheme used, if {@link ProxyConfiguration.Builder#useSystemPropertyValues(Boolean)} is set to true
     */
    public int port() {
        return port;
    }

    /**
     * @return The proxy username from the configuration if set, else from the "https.proxyUser" or "http.proxyUser" system
     * property, based on the scheme used, if {@link ProxyConfiguration.Builder#useSystemPropertyValues(Boolean)} is set to true
     * */
    public String username() {
        if (Objects.equals(scheme(), HTTPS)) {
            return resolveValue(username, ProxySystemSetting.HTTPS_PROXY_USERNAME);
        }
        return resolveValue(username, ProxySystemSetting.PROXY_USERNAME);
    }

    /**
     * @return The proxy password from the configuration if set, else from the "https.proxyPassword" or "http.proxyPassword"
     * system property, based on the scheme used, if {@link ProxyConfiguration.Builder#useSystemPropertyValues(Boolean)} is set
     * to true
     * */
    public String password() {
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

        ProxyConfiguration that = (ProxyConfiguration) o;

        if (port != that.port) {
            return false;
        }

        if (!Objects.equals(this.scheme, that.scheme)) {
            return false;
        }

        if (!Objects.equals(this.host, that.host)) {
            return false;
        }

        if (!Objects.equals(this.username, that.username)) {
            return false;
        }

        return Objects.equals(this.password, that.password);
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);

        return result;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Builder for {@link ProxyConfiguration}.
     */
    public interface Builder extends CopyableBuilder<Builder, ProxyConfiguration> {

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
         * options are not provided during building the {@link ProxyConfiguration} object. To disable this behaviour, set this
         * value to false.
         *
         * @param useSystemPropertyValues The option whether to use system property values
         * @return This object for method chaining.
         */
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);
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

    private static final class BuilderImpl implements Builder {
        private String scheme;
        private String host;
        private int port = 0;
        private String username;
        private String password;
        private Boolean useSystemPropertyValues = Boolean.TRUE;

        private BuilderImpl() {
        }

        private BuilderImpl(ProxyConfiguration proxyConfiguration) {
            this.useSystemPropertyValues = proxyConfiguration.useSystemPropertyValues;
            this.scheme = proxyConfiguration.scheme;
            this.host = proxyConfiguration.host;
            this.port = proxyConfiguration.port;
            this.username = proxyConfiguration.username;
            this.password = proxyConfiguration.password;
        }

        @Override
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        @Override
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        @Override
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        @Override
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        @Override
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        @Override
        public Builder useSystemPropertyValues(Boolean useSystemPropertyValues) {
            this.useSystemPropertyValues = useSystemPropertyValues;
            return this;
        }

        public void setUseSystemPropertyValues(Boolean useSystemPropertyValues) {
            useSystemPropertyValues(useSystemPropertyValues);
        }

        @Override
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}