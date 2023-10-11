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

package software.amazon.awssdk.http.nio.netty;

import static software.amazon.awssdk.utils.http.SdkHttpUtils.fetchProxyFromEnvironment;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsEnvironment;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsProperty;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Proxy configuration for {@link NettyNioAsyncHttpClient}. This class is used to configure an HTTP or HTTPS proxy to be used by
 * the {@link NettyNioAsyncHttpClient}.
 *
 * @see NettyNioAsyncHttpClient.Builder#proxyConfiguration(ProxyConfiguration)
 */
@SdkPublicApi
public final class ProxyConfiguration implements ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration> {
    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private final Boolean useSystemPropertyValues;
    private final Boolean useEnvironmentVariables;
    private final Boolean explicitHttp;
    private final Boolean explicitHttps;
    private final String scheme;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final Set<String> nonProxyHosts;

    private ProxyConfiguration(BuilderImpl builder) {
        this.useSystemPropertyValues = builder.useSystemPropertyValues;
        this.useEnvironmentVariables = builder.useEnvironmentVariables;
        this.scheme = builder.scheme;
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.nonProxyHosts = builder.nonProxyHosts;
        this.explicitHttp = builder.explicitHttp == null || builder.explicitHttp;
        this.explicitHttps = builder.explicitHttps == null || builder.explicitHttps;
    }

    public static Builder builder() {
        return new BuilderImpl();
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
    public String scheme(String connectingToScheme) {
        boolean isHttps = Objects.equals(connectingToScheme, HTTPS);
        if (scheme != null) {
            if (isHttps ? explicitHttps : explicitHttp) {
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
    public String host(String scheme) {
        boolean isHttps = Objects.equals(scheme, HTTPS);
        if (host != null) {
            if (isHttps ? explicitHttps : explicitHttp) {
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
    public int port(String scheme) {
        boolean isHttps = Objects.equals(scheme, HTTPS);
        if (port > 0) {
            if (isHttps ? explicitHttps : explicitHttp) {
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
    public String username(String scheme) {
        boolean isHttps = Objects.equals(scheme, HTTPS);
        if (username != null) {
            if (isHttps ? explicitHttps : explicitHttp) {
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
    public String password(String scheme) {
        boolean isHttps = Objects.equals(scheme, HTTPS);
        if (password != null) {
            if (isHttps ? explicitHttps : explicitHttp) {
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

    /**
     * The hosts that the client is allowed to access without going through the proxy.
     * If the value is not set on the object, the value represent by <code>http.nonProxyHosts</code> system property is returned.
     * If system property is also not set, or empty we try to load from <code>no_proxy</code> environment variable.
     * If all are unset, we return an empty set.
     *
     * @see Builder#nonProxyHosts(Set)
     */
    public Set<String> nonProxyHosts() {
        Set<String> hosts = null;
        if (nonProxyHosts != null) {
            hosts = nonProxyHosts;
        }
        if (nonProxyHosts == null && useSystemPropertyValues) {
            hosts = parseNonProxyHostsProperty();
        }
        if (nonProxyHosts == null && (hosts == null || hosts.isEmpty()) && useEnvironmentVariables) {
            hosts = parseNonProxyHostsEnvironment();
        }

        return Collections.unmodifiableSet(hosts != null ? hosts : Collections.emptySet());
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

        if (!nonProxyHosts.equals(that.nonProxyHosts)) {
            return false;
        }

        if (!Objects.equals(useSystemPropertyValues, that.useSystemPropertyValues)) {
            return false;
        }

        if (!Objects.equals(useEnvironmentVariables, that.useEnvironmentVariables)) {
            return false;
        }

        if (!Objects.equals(explicitHttp, that.explicitHttp)) {
            return false;
        }

        return Objects.equals(explicitHttps, that.explicitHttps);
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + nonProxyHosts.hashCode();
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (useSystemPropertyValues != null ? useSystemPropertyValues.hashCode() : 0);
        result = 31 * result + (useEnvironmentVariables != null ? useEnvironmentVariables.hashCode() : 0);
        result = 31 * result + explicitHttp.hashCode();
        result = 31 * result + explicitHttps.hashCode();
        return result;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * Builder for {@link ProxyConfiguration}.
     */
    public interface Builder extends CopyableBuilder<Builder, ProxyConfiguration> {

        /**
         * Set the hostname of the proxy.
         *
         * @param host The proxy host.
         * @return This object for method chaining.
         */
        Builder host(String host);

        /**
         * Set the port that the proxy expects connections on.
         *
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
         * Set the set of hosts that should not be proxied. Any request whose host portion matches any of the patterns
         * given in the set will be sent to the remote host directly instead of through the proxy.
         *
         * @param nonProxyHosts The set of hosts that should not be proxied.
         * @return This object for method chaining.
         */
        Builder nonProxyHosts(Set<String> nonProxyHosts);

        /**
         * Set the username used to authenticate with the proxy username.
         *
         * @param username The proxy username.
         * @return This object for method chaining.
         */
        Builder username(String username);

        /**
         * Set the password used to authenticate with the proxy password.
         *
         * @param password The proxy password.
         * @return This object for method chaining.
         */
        Builder password(String password);

        /**
         * Set the option whether to use system property values from {@link ProxySystemSetting} if any of the config options
         * are missing. The value is set to "true" by default which means SDK will automatically use system property values if
         * options are not provided during building the {@link ProxyConfiguration} object. To disable this behaviour, set this
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
         * for options that are not provided during building the {@link ProxyConfiguration} object. To disable this behavior,
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

    }

    private Set<String> parseNonProxyHostsProperty() {
        String nonProxyHostsSystem = ProxySystemSetting.NON_PROXY_HOSTS.getStringValue().orElse(null);

        if (nonProxyHostsSystem != null && !nonProxyHostsSystem.isEmpty()) {
            return Arrays.stream(nonProxyHostsSystem.split("\\|"))
                         .map(String::toLowerCase)
                         .map(s -> StringUtils.replace(s, "*", ".*?"))
                         .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private static final class BuilderImpl implements Builder {
        private String scheme;
        private String host;
        private int port = 0;
        private String username;
        private String password;
        private Set<String> nonProxyHosts;
        private Boolean useSystemPropertyValues = Boolean.TRUE;
        private Boolean useEnvironmentVariables = Boolean.TRUE;
        private Boolean explicitHttp;
        private Boolean explicitHttps;

        private BuilderImpl() {
        }

        private BuilderImpl(ProxyConfiguration proxyConfiguration) {
            this.useSystemPropertyValues = proxyConfiguration.useSystemPropertyValues;
            this.useEnvironmentVariables = proxyConfiguration.useEnvironmentVariables;
            this.scheme = proxyConfiguration.scheme;
            this.host = proxyConfiguration.host;
            this.port = proxyConfiguration.port;
            this.nonProxyHosts = proxyConfiguration.nonProxyHosts != null ?
                                 new HashSet<>(proxyConfiguration.nonProxyHosts) : null;
            this.username = proxyConfiguration.username;
            this.password = proxyConfiguration.password;
            this.explicitHttp = proxyConfiguration.explicitHttp;
            this.explicitHttps = proxyConfiguration.explicitHttps;
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
        public Builder nonProxyHosts(Set<String> nonProxyHosts) {
            if (nonProxyHosts != null) {
                this.nonProxyHosts = new HashSet<>(nonProxyHosts);
            } else {
                this.nonProxyHosts = Collections.emptySet();
            }
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
        public Builder useEnvironmentVariables(Boolean useEnvironmentVariables) {
            this.useEnvironmentVariables = useEnvironmentVariables;
            return this;
        }

        public void setUseEnvironmentVariables(Boolean useEnvironmentVariables) {
            useEnvironmentVariables(useEnvironmentVariables);
        }

        @Override
        public Builder proxyOverHttp(Boolean proxyOverHttp) {
            this.explicitHttp = proxyOverHttp;
            return this;
        }

        public void setProxyOverHttp(Boolean shouldProxyOverHttp) {
            proxyOverHttp(shouldProxyOverHttp);
        }

        @Override
        public Builder proxyOverHttps(Boolean proxyOverHttps) {
            this.explicitHttps = proxyOverHttps;
            return this;
        }

        public void setProxyOverHttps(Boolean shouldProxyOverHttps) {
            proxyOverHttps(shouldProxyOverHttps);
        }

        @Override
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}