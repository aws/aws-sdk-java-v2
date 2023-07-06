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

package software.amazon.awssdk.http.apache;

import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.fetchProxyFromEnvironment;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsEnvironment;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsProperty;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Configuration that defines how to communicate via an HTTP or HTTPS proxy.
 */
@SdkPublicApi
public final class ProxyConfiguration implements ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration> {

    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private final URI endpoint;
    private final String username;
    private final String password;
    private final String ntlmDomain;
    private final String ntlmWorkstation;
    private final Set<String> nonProxyHosts;
    private final Boolean preemptiveBasicAuthenticationEnabled;
    private final Boolean useSystemPropertyValues;
    private final Boolean useEnvironmentVariables;
    private final Boolean proxyOverHttp;
    private final Boolean proxyOverHttps;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ProxyConfiguration(DefaultClientProxyConfigurationBuilder builder) {
        this.endpoint = builder.endpoint;
        this.username = builder.username;
        this.password = builder.password;
        this.ntlmDomain = builder.ntlmDomain;
        this.ntlmWorkstation = builder.ntlmWorkstation;
        this.nonProxyHosts = builder.nonProxyHosts;
        this.preemptiveBasicAuthenticationEnabled = builder.preemptiveBasicAuthenticationEnabled == null ? Boolean.FALSE :
                builder.preemptiveBasicAuthenticationEnabled;
        this.useSystemPropertyValues = builder.useSystemPropertyValues;
        this.useEnvironmentVariables = builder.useEnvironmentVariables;
        this.proxyOverHttp = builder.proxyOverHttp == null || builder.proxyOverHttp;
        this.proxyOverHttps = builder.proxyOverHttps == null || builder.proxyOverHttps;
    }

    /**
     * The proxy host is determined in the following order:
     *
     * <ul>
     *     <li>
     *         If a user has manually configured the scheme in the builder through {@link Builder#endpoint(URI)},
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
        if (endpoint != null) {
            if (isHttps ? proxyOverHttps : proxyOverHttp) {
                return endpoint.getHost();
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
     *         If a user has manually configured the scheme in the builder through {@link Builder#endpoint(URI)},
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
        if (endpoint != null) {
            if (isHttps ? proxyOverHttps : proxyOverHttp) {
                return endpoint.getPort();
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
     * The proxy scheme is determined in the following order:
     *
     * <ul>
     *     <li>
     *        If a user has manually configured the scheme in the builder through {@link Builder#endpoint(URI)},
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
        if (endpoint != null) {
            if (isHttps ? proxyOverHttps : proxyOverHttp) {
                return endpoint.getScheme();
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
            if (isHttps ? proxyOverHttps : proxyOverHttp) {
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
            if (isHttps ? proxyOverHttps : proxyOverHttp) {
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
     * For NTLM proxies: The Windows domain name to use when authenticating with the proxy.
     *
     * @see Builder#ntlmDomain(String)
     */
    public String ntlmDomain(String scheme) {
        if (Objects.equals(scheme, HTTPS) ? proxyOverHttps : proxyOverHttp) {
            return ntlmDomain;
        }
        return null;
    }

    /**
     * For NTLM proxies: The Windows workstation name to use when authenticating with the proxy.
     *
     * @see Builder#ntlmWorkstation(String)
     */
    public String ntlmWorkstation(String scheme) {
        if (Objects.equals(scheme, HTTPS) ? proxyOverHttps : proxyOverHttp) {
            return ntlmDomain;
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

    /**
     * Whether to attempt to authenticate preemptively against the proxy server using basic authentication.
     *
     * @see Builder#preemptiveBasicAuthenticationEnabled(Boolean)
     */
    public Boolean preemptiveBasicAuthenticationEnabled() {
        return preemptiveBasicAuthenticationEnabled;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .endpoint(endpoint)
                .username(username)
                .password(password)
                .ntlmDomain(ntlmDomain)
                .ntlmWorkstation(ntlmWorkstation)
                .nonProxyHosts(nonProxyHosts)
                .preemptiveBasicAuthenticationEnabled(preemptiveBasicAuthenticationEnabled)
                .useSystemPropertyValues(useSystemPropertyValues)
                .useEnvironmentVariables(useEnvironmentVariables)
                .proxyOverHttp(proxyOverHttp)
                .proxyOverHttps(proxyOverHttps);
    }

    /**
     * Create a {@link Builder}, used to create a {@link ProxyConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientProxyConfigurationBuilder();
    }

    @Override
    public String toString() {
        return ToString.builder("ProxyConfiguration")
                       .add("endpoint", endpoint)
                       .add("username", username)
                       .add("ntlmDomain", ntlmDomain)
                       .add("ntlmWorkstation", ntlmWorkstation)
                       .add("nonProxyHosts", nonProxyHosts)
                       .add("preemptiveBasicAuthenticationEnabled", preemptiveBasicAuthenticationEnabled)
                       .add("usingEndpointForHttp", proxyOverHttp)
                       .add("usingEndpointForHttps", proxyOverHttps)
                       .build();
    }

    /**
     * A builder for {@link ProxyConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ProxyConfiguration> {

        /**
         * Configure the endpoint of the proxy server that the SDK should connect through. Currently, the endpoint is limited to
         * a host and port. Any other URI components will result in an exception being raised.
         */
        Builder endpoint(URI endpoint);

        /**
         * Configure the username to use when connecting through a proxy.
         */
        Builder username(String username);

        /**
         * Configure the password to use when connecting through a proxy.
         */
        Builder password(String password);

        /**
         * For NTLM proxies: Configure the Windows domain name to use when authenticating with the proxy.
         */
        Builder ntlmDomain(String proxyDomain);

        /**
         * For NTLM proxies: Configure the Windows workstation name to use when authenticating with the proxy.
         */
        Builder ntlmWorkstation(String proxyWorkstation);

        /**
         * Configure the hosts that the client is allowed to access without going through the proxy.
         */
        Builder nonProxyHosts(Set<String> nonProxyHosts);

        /**
         * Add a host that the client is allowed to access without going through the proxy.
         *
         * @see ProxyConfiguration#nonProxyHosts()
         */
        Builder addNonProxyHost(String nonProxyHost);

        /**
         * Configure whether to attempt to authenticate pre-emptively against the proxy server using basic authentication.
         */
        Builder preemptiveBasicAuthenticationEnabled(Boolean preemptiveBasicAuthenticationEnabled);

        /**
         * Option whether to use system property values from {@link ProxySystemSetting} if any of the config options are missing.
         * This value is set to "true" by default which means SDK will automatically use system property values
         * for options that are not provided during building the {@link ProxyConfiguration} object. To disable this behavior,
         * set this value to "false".
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

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientProxyConfigurationBuilder implements Builder {

        private URI endpoint;
        private String username;
        private String password;
        private String ntlmDomain;
        private String ntlmWorkstation;
        private Set<String> nonProxyHosts;
        private Boolean preemptiveBasicAuthenticationEnabled;
        private Boolean useSystemPropertyValues = Boolean.TRUE;
        private Boolean useEnvironmentVariables = Boolean.TRUE;
        private Boolean proxyOverHttp;
        private Boolean proxyOverHttps;

        @Override
        public Builder endpoint(URI endpoint) {
            if (endpoint != null) {
                Validate.isTrue(isEmpty(endpoint.getUserInfo()), "Proxy endpoint user info is not supported.");
                Validate.isTrue(isEmpty(endpoint.getPath()), "Proxy endpoint path is not supported.");
                Validate.isTrue(isEmpty(endpoint.getQuery()), "Proxy endpoint query is not supported.");
                Validate.isTrue(isEmpty(endpoint.getFragment()), "Proxy endpoint fragment is not supported.");
            }

            this.endpoint = endpoint;
            return this;
        }

        public void setEndpoint(URI endpoint) {
            endpoint(endpoint);
        }

        @Override
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public void setUsername(String username) {
            username(username);
        }

        @Override
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public void setPassword(String password) {
            password(password);
        }

        @Override
        public Builder ntlmDomain(String proxyDomain) {
            this.ntlmDomain = proxyDomain;
            return this;
        }

        public void setNtlmDomain(String ntlmDomain) {
            ntlmDomain(ntlmDomain);
        }

        @Override
        public Builder ntlmWorkstation(String proxyWorkstation) {
            this.ntlmWorkstation = proxyWorkstation;
            return this;
        }

        public void setNtlmWorkstation(String ntlmWorkstation) {
            ntlmWorkstation(ntlmWorkstation);
        }

        @Override
        public Builder nonProxyHosts(Set<String> nonProxyHosts) {
            this.nonProxyHosts = nonProxyHosts != null ? new HashSet<>(nonProxyHosts) : null;
            return this;
        }

        @Override
        public Builder addNonProxyHost(String nonProxyHost) {
            if (this.nonProxyHosts == null) {
                this.nonProxyHosts = new HashSet<>();
            }
            this.nonProxyHosts.add(nonProxyHost);
            return this;
        }

        public void setNonProxyHosts(Set<String> nonProxyHosts) {
            nonProxyHosts(nonProxyHosts);
        }

        @Override
        public Builder preemptiveBasicAuthenticationEnabled(Boolean preemptiveBasicAuthenticationEnabled) {
            this.preemptiveBasicAuthenticationEnabled = preemptiveBasicAuthenticationEnabled;
            return this;
        }

        public void setPreemptiveBasicAuthenticationEnabled(Boolean preemptiveBasicAuthenticationEnabled) {
            preemptiveBasicAuthenticationEnabled(preemptiveBasicAuthenticationEnabled);
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
            this.proxyOverHttp = proxyOverHttp;
            return this;
        }

        public void setProxyOverHttp(Boolean shouldProxyOverHttp) {
            proxyOverHttp(shouldProxyOverHttp);
        }

        @Override
        public Builder proxyOverHttps(Boolean proxyOverHttps) {
            this.proxyOverHttps = proxyOverHttps;
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
