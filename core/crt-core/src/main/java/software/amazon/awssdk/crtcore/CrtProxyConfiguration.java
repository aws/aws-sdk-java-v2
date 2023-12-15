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

import static software.amazon.awssdk.utils.ProxyConfigProvider.fromSystemEnvironmentSettings;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ProxyConfigProvider;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.StringUtils;

/**
 * The base class for AWS CRT proxy configuration
 */
@SdkPublicApi
public abstract class CrtProxyConfiguration {
    private final String scheme;
    private final String host;
    private final int port;

    private final String username;
    private final String password;
    private final Boolean useSystemPropertyValues;
    private final Boolean useEnvironmentVariableValues;

    protected CrtProxyConfiguration(DefaultBuilder<?> builder) {
        this.useSystemPropertyValues = builder.useSystemPropertyValues;
        this.useEnvironmentVariableValues = builder.useEnvironmentVariableValues;
        this.scheme = builder.scheme;

        ProxyConfigProvider proxyConfigProvider = fromSystemEnvironmentSettings(builder.useSystemPropertyValues,
                                                                                builder.useEnvironmentVariableValues ,
                                                                                builder.scheme);
        this.host = resolveHost(builder, proxyConfigProvider);
        this.port = resolvePort(builder, proxyConfigProvider);
        this.username = resolveUsername(builder, proxyConfigProvider);
        this.password = resolvePassword(builder, proxyConfigProvider);
    }

    private static String resolvePassword(DefaultBuilder<?> builder, ProxyConfigProvider proxyConfigProvider) {
        if (!StringUtils.isEmpty(builder.password) || proxyConfigProvider == null) {
            return builder.password;
        } else {
            return proxyConfigProvider.password().orElseGet(() -> builder.password);
        }
    }

    private static String resolveUsername(DefaultBuilder<?> builder, ProxyConfigProvider proxyConfigProvider) {
        if (!StringUtils.isEmpty(builder.username) || proxyConfigProvider == null) {
            return builder.username;
        } else {
            return proxyConfigProvider.userName().orElseGet(() -> builder.username);
        }
    }

    private static int resolvePort(DefaultBuilder<?> builder, ProxyConfigProvider proxyConfigProvider) {
        if (builder.port != 0 || proxyConfigProvider == null) {
            return builder.port;
        } else {
            return proxyConfigProvider.port();
        }
    }

    private static String resolveHost(DefaultBuilder<?> builder, ProxyConfigProvider proxyConfigProvider) {
        if (builder.host != null || proxyConfigProvider == null) {
            return builder.host;
        } else {
            return proxyConfigProvider.host();
        }
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
        return username;
    }

    /**
     * @return The proxy password from the configuration if set, else from the "https.proxyPassword" or "http.proxyPassword"
     * system property, based on the scheme used, if {@link Builder#useSystemPropertyValues(Boolean)} is set
     * to true
     * */
    public final String password() {
        return password;
    }

    /**
     * Indicates whether environment variables are utilized for proxy configuration.
     *
     * @return {@code true} if environment variables are being used for proxy configuration, {@code false} otherwise.
     */
    public final Boolean isUseEnvironmentVariableValues() {
        return useEnvironmentVariableValues;
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

        if (!Objects.equals(useSystemPropertyValues, that.useSystemPropertyValues)) {
            return false;
        }
        return Objects.equals(useEnvironmentVariableValues, that.useEnvironmentVariableValues);
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (useSystemPropertyValues != null ? useSystemPropertyValues.hashCode() : 0);
        result = 31 * result + (useEnvironmentVariableValues != null ? useEnvironmentVariableValues.hashCode() : 0);
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
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
         * The option whether to use system property values from {@link ProxySystemSetting} if any of the config options are
         * missing. The value is set to "true" by default which means SDK will automatically use system property values if options
         * are not provided during building the {@link CrtProxyConfiguration} object. To disable this behaviour, set this value to
         * false.It is important to note that when this property is set to "true," all proxy settings will exclusively originate
         * from system properties, and no partial settings will be obtained from EnvironmentVariableValues.
         *
         * @param useSystemPropertyValues The option whether to use system property values
         * @return This object for method chaining.
         */
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);

        /**
         * The option whether to use environment variable values from {@link ProxySystemSetting} if any of the config options are
         * missing. The value is set to "true" by default which means SDK will automatically use environment variable values if
         * options are not provided during building the {@link CrtProxyConfiguration} object. To disable this behavior, set this
         * value to false.It is important to note that when this property is set to "true," all proxy settings will exclusively
         * originate from environment variableValues, and no partial settings will be obtained from SystemPropertyValues.
         * <p>Comma-separated host names in the NO_PROXY environment variable indicate multiple hosts to exclude from
         * proxy settings.
         *
         * @param useEnvironmentVariableValues The option whether to use environment variable values
         * @return This object for method chaining.
         */
        Builder useEnvironmentVariableValues(Boolean useEnvironmentVariableValues);


        CrtProxyConfiguration build();
    }


    protected abstract static class DefaultBuilder<B extends Builder> implements Builder {
        private String scheme;
        private String host;
        private int port = 0;
        private String username;
        private String password;
        private Boolean useSystemPropertyValues = Boolean.TRUE;
        private Boolean useEnvironmentVariableValues = Boolean.TRUE;

        protected DefaultBuilder() {
        }

        protected DefaultBuilder(CrtProxyConfiguration proxyConfiguration) {
            this.useSystemPropertyValues = proxyConfiguration.useSystemPropertyValues;
            this.useEnvironmentVariableValues = proxyConfiguration.useEnvironmentVariableValues;
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

        @Override
        public B useEnvironmentVariableValues(Boolean useEnvironmentVariableValues) {
            this.useEnvironmentVariableValues = useEnvironmentVariableValues;
            return (B) this;
        }

        public B setuseEnvironmentVariableValues(Boolean useEnvironmentVariableValues) {
            return useEnvironmentVariableValues(useEnvironmentVariableValues);
        }

        public void setUseSystemPropertyValues(Boolean useSystemPropertyValues) {
            useSystemPropertyValues(useSystemPropertyValues);
        }

    }
}