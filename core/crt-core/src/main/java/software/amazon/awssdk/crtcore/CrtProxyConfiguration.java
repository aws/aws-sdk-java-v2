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

import static software.amazon.awssdk.utils.http.SdkHttpUtils.fetchProxyFromEnvironment;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * The base class for AWS CRT proxy configuration
 */
@SdkPublicApi
public abstract class CrtProxyConfiguration {
    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private final String scheme;
    private final String host;
    private final int port;

    private final String username;
    private final String password;
    private final Boolean useSystemPropertyValues;
    private final Boolean useEnvironmentVariables;
    private final Boolean useExplicitForHttp;
    private final Boolean useExplicitForHttps;

    protected CrtProxyConfiguration(DefaultBuilder<?> builder) {
        this.useSystemPropertyValues = builder.useSystemPropertyValues;
        this.scheme = builder.scheme;
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.useEnvironmentVariables = builder.useEnvironmentVariables;
        this.useExplicitForHttp = builder.explicitHttp == null || builder.explicitHttp;
        this.useExplicitForHttps = builder.explicitHttps == null || builder.explicitHttps;
    }

    /**
     * The proxy scheme is determined in the following order:
     *
     * <ul>
     *     <li>
     *        If a user has manually configured the scheme in the builder through {@link Builder#scheme(String)},
     *        and the user has not explicitly disabled explicit use for <code>connectingToScheme</code>
     *        through one of the builder interfaces {@link Builder#proxyOverHttp(Boolean)}, or
     *        {@link Builder#proxyOverHttps(Boolean)}.
     *     </li>
     *     <li>
     *         If {@link Builder#useSystemPropertyValues(Boolean)} is set to <code>true</code>
     *         System properties are checked next. Depending on the <code>connectingToScheme</code>
     *         parameter, it will check one of two system properties <code>https.proxyHost</code>,
     *         or <code>http.proxyHost</code>. If these properties exist, it will return
     *         <code>HTTP</code>.
     *     </li>
     *     <li>
     *         If {@link Builder#useEnvironmentVariables(Boolean)} is set to <code>true</code>
     *         Environment variables will be checked last. Depending on the <code>connectingToScheme</code>
     *         parameter, it will check one of two environment variables <code>http_proxy</code>, or
     *         <code>https_proxy</code> (in all lowercase, or all uppercase). If the correct
     *         environment variable exists, it will parse the URI in this variable, and return it's
     *         scheme.
     *     </li>
     *     <li>
     *         If all else fails <code>null</code> is returned.
     *     </li>
     * </ul>
     *
     * @param connectingToScheme the scheme, or protocol of the URL that we're making a request too.
     * @return The current determined proxy scheme.
     * */
    public final String scheme(String connectingToScheme) {
        boolean isHttps = Objects.equals(connectingToScheme, HTTPS);
        if (scheme != null) {
            if (isHttps ? useExplicitForHttps : useExplicitForHttp) {
                return scheme;
            }
        }

        if (useSystemPropertyValues) {
            if (isHttps ?
                    ProxySystemSetting.HTTPS_PROXY_HOST.getStringValue().isPresent() :
                    ProxySystemSetting.PROXY_HOST.getStringValue().isPresent()) {
                return HTTP;
            }
        }

        if (useEnvironmentVariables) {
            Optional<URL> proxyToUse = fetchProxyFromEnvironment(connectingToScheme);
            if (proxyToUse.isPresent()) {
                return proxyToUse.get().getProtocol();
            }
        }

        return null;
    }

    /**
     * The proxy host is determined in the following order:
     *
     * <ul>
     *     <li>
     *         If a user has manually configured the scheme in the builder through {@link Builder#host(String)},
     *         and the user has not explicitly disabled explicit use for <code>scheme</code>
     *         through one of the builder interfaces {@link Builder#proxyOverHttp(Boolean)}, or
     *         {@link Builder#proxyOverHttps(Boolean)}.
     *     </li>
     *     <li>
     *         If {@link Builder#useSystemPropertyValues(Boolean)} is set to <code>true</code>
     *         System properties are checked next. Depending on the <code>scheme</code>
     *         parameter, it will check one of two system properties <code>https.proxyHost</code>,
     *         or <code>http.proxyHost</code>. If these properties exist, it will return
     *         the property hostname.
     *     </li>
     *     <li>
     *         If {@link Builder#useEnvironmentVariables(Boolean)} is set to <code>true</code>
     *         Environment variables will be checked last. Depending on the <code>scheme</code>
     *         parameter, it will check one of two environment variables <code>http_proxy</code>, or
     *         <code>https_proxy</code> (in all lowercase, or all uppercase). If the correct
     *         environment variable exists, it will parse the URI in this variable, and return it's
     *         hostname.
     *     </li>
     *     <li>
     *         If all else fails <code>null</code> is returned.
     *     </li>
     * </ul>
     *
     * @param scheme the scheme, or protocol of the URL that we're making a request too.
     * @return The current determined proxy hostname to connect too.
     * */
    public final String host(String scheme) {
        boolean isHttps = Objects.equals(scheme, HTTPS);
        if (host != null) {
            if (isHttps ? useExplicitForHttps : useExplicitForHttp) {
                return host;
            }
        }

        if (useSystemPropertyValues) {
            String hostProperty;
            if (isHttps) {
                hostProperty = ProxySystemSetting.HTTPS_PROXY_HOST.getStringValue().orElse(null);
            } else {
                hostProperty = ProxySystemSetting.PROXY_HOST.getStringValue().orElse(null);
            }
            if (hostProperty != null) {
                return hostProperty;
            }
        }

        if (useEnvironmentVariables) {
            return fetchProxyFromEnvironment(scheme).map(URL::getHost).orElse(null);
        }

        return null;
    }

    /**
     * The proxy port is determined in the following order:
     *
     * <ul>
     *     <li>
     *         If a user has manually configured the scheme in the builder through {@link Builder#port(int)},
     *         and the user has not explicitly disabled explicit use for <code>scheme</code>
     *         through one of the builder interfaces {@link Builder#proxyOverHttp(Boolean)}, or
     *         {@link Builder#proxyOverHttps(Boolean)}.
     *     </li>
     *     <li>
     *         If {@link Builder#useSystemPropertyValues(Boolean)} is set to <code>true</code>
     *         System properties are checked next. Depending on the <code>scheme</code>
     *         parameter, it will check one of two system properties <code>https.proxyPort</code>,
     *         or <code>http.proxyPort</code>. If these properties exist, it will return
     *         the property port.
     *     </li>
     *     <li>
     *         If {@link Builder#useEnvironmentVariables(Boolean)} is set to <code>true</code>
     *         Environment variables will be checked last. Depending on the <code>scheme</code>
     *         parameter, it will check one of two environment variables <code>http_proxy</code>, or
     *         <code>https_proxy</code> (in all lowercase, or all uppercase). If the correct
     *         environment variable exists, it will parse the URI in this variable, and return it's
     *         port.
     *     </li>
     *     <li>
     *         If all else fails <code>0</code> is returned.
     *     </li>
     * </ul>
     *
     * @param scheme the scheme, or protocol of the URL that we're making a request too.
     * @return The current determined proxy port to connect too.
     * */
    public final int port(String scheme) {
        boolean isHttps = Objects.equals(scheme, HTTPS);
        if (port > 0) {
            if (isHttps ? useExplicitForHttps : useExplicitForHttp) {
                return port;
            }
        }

        if (useSystemPropertyValues) {
            int portProperty;
            if (isHttps) {
                portProperty = ProxySystemSetting.HTTPS_PROXY_PORT.getStringValue()
                                                                  .map(Integer::parseInt)
                                                                  .orElse(0);
            } else {
                portProperty = ProxySystemSetting.PROXY_PORT.getStringValue()
                                                            .map(Integer::parseInt)
                                                            .orElse(0);
            }
            if (portProperty != 0) {
                return portProperty;
            }
        }

        if (useEnvironmentVariables) {
            return fetchProxyFromEnvironment(scheme).map(url -> {
                int portAttempted = url.getPort();
                if (portAttempted == -1) {
                    return 0;
                }
                return portAttempted;
            }).orElse(0);
        }

        return 0;
    }

    /**
     * The proxy username is determined in the following order:
     *
     * <ul>
     *     <li>
     *         If a user has manually configured the scheme in the builder through {@link Builder#username(String)},
     *         and the user has not explicitly disabled explicit use for <code>scheme</code>
     *         through one of the builder interfaces {@link Builder#proxyOverHttp(Boolean)}, or
     *         {@link Builder#proxyOverHttps(Boolean)}.
     *     </li>
     *     <li>
     *         If {@link Builder#useSystemPropertyValues(Boolean)} is set to <code>true</code>
     *         System properties are checked next. Depending on the <code>scheme</code>
     *         parameter, it will check one of two system properties <code>https.proxyUsername</code>,
     *         or <code>http.proxyUsername</code>. If these properties exist, it will return
     *         the property username.
     *     </li>
     *     <li>
     *         If {@link Builder#useEnvironmentVariables(Boolean)} is set to <code>true</code>
     *         Environment variables will be checked last. Depending on the <code>scheme</code>
     *         parameter, it will check one of two environment variables <code>http_proxy</code>, or
     *         <code>https_proxy</code> (in all lowercase, or all uppercase). If the correct
     *         environment variable exists, it will parse the URI in this variable, and return it's
     *         username if it has one.
     *     </li>
     *     <li>
     *         If all else fails <code>null</code> is returned.
     *     </li>
     * </ul>
     *
     * @param scheme the scheme, or protocol of the URL that we're making a request too.
     * @return The current determined username to authenticate to the proxy with.
     * */
    public final String username(String scheme) {
        boolean isHttps = Objects.equals(scheme, HTTPS);
        if (username != null) {
            if (isHttps ? useExplicitForHttps : useExplicitForHttp) {
                return username;
            }
        }

        if (useSystemPropertyValues) {
            Optional<String> usernameProperty;
            if (isHttps) {
                usernameProperty = ProxySystemSetting.HTTPS_PROXY_USERNAME.getStringValue();
            } else {
                usernameProperty = ProxySystemSetting.PROXY_USERNAME.getStringValue();
            }
            if (usernameProperty.isPresent()) {
                return usernameProperty.get();
            }
        }

        if (useEnvironmentVariables) {
            return fetchProxyFromEnvironment(scheme)
                .flatMap(SdkHttpUtils::parseUsernameFromUrl)
                .orElse(null);
        }

        return null;
    }

    /**
     * The proxy password is determined in the following order:
     *
     * <ul>
     *     <li>
     *         If a user has manually configured the scheme in the builder through {@link Builder#password(String)},
     *         and the user has not explicitly disabled explicit use for <code>scheme</code>
     *         through one of the builder interfaces {@link Builder#proxyOverHttp(Boolean)}, or
     *         {@link Builder#proxyOverHttps(Boolean)}.
     *     </li>
     *     <li>
     *         If {@link Builder#useSystemPropertyValues(Boolean)} is set to <code>true</code>
     *         System properties are checked next. Depending on the <code>scheme</code>
     *         parameter, it will check one of two system properties <code>https.proxyPassword</code>,
     *         or <code>http.proxyPassword</code>. If these properties exist, it will return
     *         the property password.
     *     </li>
     *     <li>
     *         If {@link Builder#useEnvironmentVariables(Boolean)} is set to <code>true</code>
     *         Environment variables will be checked last. Depending on the <code>scheme</code>
     *         parameter, it will check one of two environment variables <code>http_proxy</code>, or
     *         <code>https_proxy</code> (in all lowercase, or all uppercase). If the correct
     *         environment variable exists, it will parse the URI in this variable, and return it's
     *         password if it has one.
     *     </li>
     *     <li>
     *         If all else fails <code>null</code> is returned.
     *     </li>
     * </ul>
     *
     * @param scheme the scheme, or protocol of the URL that we're making a request too.
     * @return The current determined password to authenticate to the proxy with.
     * */
    public final String password(String scheme) {
        boolean isHttps = Objects.equals(scheme, HTTPS);
        if (password != null) {
            if (isHttps ? useExplicitForHttps : useExplicitForHttp) {
                return password;
            }
        }

        if (useSystemPropertyValues) {
            Optional<String> passwordProperty;
            if (Objects.equals(scheme, HTTPS)) {
                passwordProperty = ProxySystemSetting.HTTPS_PROXY_PASSWORD.getStringValue();
            } else {
                passwordProperty = ProxySystemSetting.PROXY_PASSWORD.getStringValue();
            }
            if (passwordProperty.isPresent()) {
                return passwordProperty.get();
            }
        }

        if (useEnvironmentVariables) {
            return fetchProxyFromEnvironment(scheme)
                .flatMap(SdkHttpUtils::parsePasswordFromUrl)
                .orElse(null);
        }

        return null;
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
        if (!Objects.equals(useEnvironmentVariables, that.useEnvironmentVariables)) {
            return false;
        }
        if (!Objects.equals(useExplicitForHttp, that.useExplicitForHttp)) {
            return false;
        }
        return Objects.equals(useExplicitForHttps, that.useExplicitForHttps);
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

        /**
         * Option whether to use environment variable values from {@link ProxySystemSetting} if any of the config options are
         * missing.
         * This value is set to "true" by default which means SDK will automatically use environment variable values
         * for options that are not provided during building the {@link CrtProxyConfiguration} object. To disable this behavior,
         * set this value to "false".
         */
        Builder useEnvironmentVariables(Boolean useEnvironmentVariables);

        /**
         * Configure whether to attempt to use this proxy for HTTP requests.
         */
        Builder proxyOverHttp(Boolean overHttp);

        /**
         * Configure whether to attempt to use this proxy for HTTPS requests.
         */
        Builder proxyOverHttps(Boolean overHttps);

        CrtProxyConfiguration build();
    }

    protected abstract static class DefaultBuilder<B extends Builder> implements Builder {
        private String scheme;
        private String host;
        private int port = 0;
        private String username;
        private String password;
        private Boolean useSystemPropertyValues = Boolean.TRUE;
        private Boolean useEnvironmentVariables = Boolean.TRUE;
        private Boolean explicitHttp;
        private Boolean explicitHttps;

        protected DefaultBuilder() {
        }

        protected DefaultBuilder(CrtProxyConfiguration proxyConfiguration) {
            this.useSystemPropertyValues = proxyConfiguration.useSystemPropertyValues;
            this.scheme = proxyConfiguration.scheme;
            this.host = proxyConfiguration.host;
            this.port = proxyConfiguration.port;
            this.username = proxyConfiguration.username;
            this.password = proxyConfiguration.password;
            this.useEnvironmentVariables = proxyConfiguration.useEnvironmentVariables;
            this.explicitHttp = proxyConfiguration.useExplicitForHttp;
            this.explicitHttps = proxyConfiguration.useExplicitForHttps;
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

        @Override
        public B useEnvironmentVariables(Boolean useEnvironmentVariables) {
            this.useEnvironmentVariables = useEnvironmentVariables;
            return (B) this;
        }

        public void setUseEnvironmentVariables(Boolean useEnvironmentVariables) {
            useEnvironmentVariables(useEnvironmentVariables);
        }

        @Override
        public B proxyOverHttp(Boolean proxyOverHttp) {
            this.explicitHttp = proxyOverHttp;
            return (B) this;
        }

        public void setProxyOverHttp(Boolean shouldProxyOverHttp) {
            proxyOverHttp(shouldProxyOverHttp);
        }

        @Override
        public B proxyOverHttps(Boolean proxyOverHttps) {
            this.explicitHttps = proxyOverHttps;
            return (B) this;
        }

        public void setProxyOverHttps(Boolean shouldProxyOverHttps) {
            proxyOverHttps(shouldProxyOverHttps);
        }

    }
}