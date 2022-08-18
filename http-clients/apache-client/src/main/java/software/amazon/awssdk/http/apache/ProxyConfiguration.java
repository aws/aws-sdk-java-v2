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
import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsProperty;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.EnvironmentProxyUtils;
import software.amazon.awssdk.utils.ProxyEnvironmentSetting;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration that defines how to communicate via an HTTP proxy.
 */
@SdkPublicApi
public final class ProxyConfiguration implements ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration> {

    private final URI endpoint;
    private final String username;
    private final String password;
    private final String ntlmDomain;
    private final String ntlmWorkstation;
    private final Set<String> nonProxyHosts;
    private final Boolean preemptiveBasicAuthenticationEnabled;
    private final Boolean useSystemPropertyValues;
    private final Boolean useEnvironmentVariables;
    private final String host;
    private final int port;
    private final String scheme;

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
        this.host = resolveHost();
        this.port = resolvePort();
        this.scheme = resolveScheme();
    }

    /**
     * The host to use when connecting through a proxy.
     *
     * If the value is not set in {@link ProxyConfiguration#endpoint}, the following sources are checked in order:
     *
     * <ul>
     *  <li>"http.proxyHost" system property (If {@link ProxyConfiguration#useSystemPropertyValues} is true)</li>
     *  <li>"https_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     *  <li>"http_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     * </ul>
     *
     * If no value is found, 0 is returned.
     */
    public String host() {
        return host;
    }

    /**
     * The port to use when connecting through a proxy.
     *
     * If the value is not set in {@link ProxyConfiguration#endpoint}, the following sources are checked in order:
     *
     * <ul>
     *  <li>"http.proxyPort" system property (If {@link ProxyConfiguration#useSystemPropertyValues} is true)</li>
     *  <li>"https_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     *  <li>"http_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     * </ul>
     *
     * If no value is found, 0 is returned.
     */
    public int port() {
        return port;
    }

    /**
     * Returns the {@link URI#scheme} from the configured endpoint. Otherwise return null.
     */
    public String scheme() {
        return scheme;
    }

    /**
     * The username to use when connecting through a proxy.
     *
     * If the value is not explicitly set on the object, the following sources are checked in order:
     *
     * <ul>
     *  <li>"http.proxyUsername" system property (If {@link ProxyConfiguration#useSystemPropertyValues} is true)</li>
     *  <li>"https_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     *  <li>"http_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     * </ul>
     *
     * @see Builder#username(String)
     */
    public String username() {
        if (username != null) {
            return username;
        }
        if (useSystemPropertyValues) {
            Optional<String> systemPropertyUsername = ProxySystemSetting.PROXY_USERNAME.getStringValue();
            if (systemPropertyUsername.isPresent()) {
                return systemPropertyUsername.get();
            }
        }
        if (useEnvironmentVariables) {
            Optional<String> httpsProxy = ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue();
            Optional<String> httpProxy = ProxyEnvironmentSetting.HTTP_PROXY.getStringValue();

            if (httpsProxy.isPresent()) {
                Optional<String> envProxyUsername = EnvironmentProxyUtils.parseUsername(httpsProxy.get());
                return envProxyUsername.orElse(null);
            }
            if (httpProxy.isPresent()) {
                Optional<String> envProxyUsername = EnvironmentProxyUtils.parseUsername(httpProxy.get());
                return envProxyUsername.orElse(null);
            }
        }
        return null;
    }

    /**
     * The password to use when connecting through a proxy.
     *
     * If the value is not explicitly set on the object, the following sources are checked in order:
     *
     * <ul>
     *  <li>"http.proxyPassword" system property (If {@link ProxyConfiguration#useSystemPropertyValues} is true)</li>
     *  <li>"https_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     *  <li>"http_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     * </ul>
     *
     * @see Builder#password(String)
     */
    public String password() {
        if (password != null) {
            return password;
        }
        if (useSystemPropertyValues) {
            Optional<String> systemPropertyUsername = ProxySystemSetting.PROXY_PASSWORD.getStringValue();
            if (systemPropertyUsername.isPresent()) {
                return systemPropertyUsername.get();
            }
        }
        if (useEnvironmentVariables) {
            Optional<String> httpsProxy = ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue();
            Optional<String> httpProxy = ProxyEnvironmentSetting.HTTP_PROXY.getStringValue();

            if (httpsProxy.isPresent()) {
                Optional<String> envProxyPassword = EnvironmentProxyUtils.parsePassword(httpsProxy.get());
                return envProxyPassword.orElse(null);
            }
            if (httpProxy.isPresent()) {
                Optional<String> envProxyPassword = EnvironmentProxyUtils.parsePassword(httpProxy.get());
                return envProxyPassword.orElse(null);
            }
        }
        return null;
    }

    /**
     * For NTLM proxies: The Windows domain name to use when authenticating with the proxy.
     *
     * @see Builder#ntlmDomain(String)
     */
    public String ntlmDomain() {
        return ntlmDomain;
    }

    /**
     * For NTLM proxies: The Windows workstation name to use when authenticating with the proxy.
     *
     * @see Builder#ntlmWorkstation(String)
     */
    public String ntlmWorkstation() {
        return ntlmWorkstation;
    }

    /**
     * The hosts that the client is allowed to access without going through the proxy.
     *
     * If the value is not explicitly set on the object, the following sources are checked in order:
     *
     * <ul>
     *  <li>"http.nonProxyHosts" system property (If {@link ProxyConfiguration#useSystemPropertyValues} is true)</li>
     *  <li>"no_proxy" environment variable (If {@link ProxyConfiguration#useEnvironmentVariables} is true)</li>
     * </ul>
     * @see Builder#nonProxyHosts(Set)
     */
    public Set<String> nonProxyHosts() {
        if (nonProxyHosts != null) {
            return Collections.unmodifiableSet(nonProxyHosts);
        }
        if (useSystemPropertyValues) {
            if(ProxySystemSetting.NON_PROXY_HOSTS.getStringValue().isPresent()) {
                return Collections.unmodifiableSet(parseNonProxyHostsProperty());
            }
        }
        if (useEnvironmentVariables) {
            Optional<String> envNonProxySetting = ProxyEnvironmentSetting.NO_PROXY.getStringValue();
            if (envNonProxySetting.isPresent()) {
                Set<String> envNonProxyHosts = EnvironmentProxyUtils.parseNonProxyHosts(envNonProxySetting.get());
                if (!envNonProxyHosts.isEmpty()) {
                    return Collections.unmodifiableSet(envNonProxyHosts);
                }
            }
        }

        return Collections.emptySet();
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
                .useEnvironmentVariables(useEnvironmentVariables);
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
                       .build();
    }


    private String resolveHost() {
        if (endpoint != null) {
            return endpoint.getHost();
        }
        if (useSystemPropertyValues) {
            Optional<String> systemPropertyProxyHost = ProxySystemSetting.PROXY_HOST.getStringValue();
            if (systemPropertyProxyHost.isPresent()) {
                return systemPropertyProxyHost.get();
            }
        }
        if (useEnvironmentVariables) {
            Optional<String> httpsProxy = ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue();
            Optional<String> httpProxy = ProxyEnvironmentSetting.HTTP_PROXY.getStringValue();

            if (httpsProxy.isPresent()) {
                Optional<String> envProxyHost = EnvironmentProxyUtils.parseHost(httpsProxy.get());
                return envProxyHost.orElse(null);
            }
            if (httpProxy.isPresent()) {
                Optional<String> envProxyHost = EnvironmentProxyUtils.parseHost(httpProxy.get());
                return envProxyHost.orElse(null);
            }
        }
        return null;
    }

    private int resolvePort() {
        if (endpoint != null) {
            return endpoint.getPort();
        }
        if (useSystemPropertyValues) {
            Optional<String> systemPropertyPort = ProxySystemSetting.PROXY_PORT.getStringValue();
            if (systemPropertyPort.isPresent()) {
                return systemPropertyPort.map(Integer::parseInt).orElse(0);
            }
        }
        if (useEnvironmentVariables) {
            Optional<String> httpsProxy = ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue();
            Optional<String> httpProxy = ProxyEnvironmentSetting.HTTP_PROXY.getStringValue();

            if (httpsProxy.isPresent()) {
                Optional<Integer> envProxyPort = EnvironmentProxyUtils.parsePort(httpsProxy.get());
                return envProxyPort.orElse(0);
            }
            if (httpProxy.isPresent()) {
                Optional<Integer> envProxyPort = EnvironmentProxyUtils.parsePort(httpProxy.get());
                return envProxyPort.orElse(0);
            }
        }
        return 0;
    }

    public String resolveScheme() {
        if(endpoint != null) {
            return endpoint.getScheme();
        }
        if(useSystemPropertyValues) {
            if(ProxySystemSetting.PROXY_HOST.getStringValue().isPresent()) {
                return "http";
            }
        }
        if(useEnvironmentVariables) {
            Optional<String> httpsProxy = ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue();
            Optional<String> httpProxy = ProxyEnvironmentSetting.HTTP_PROXY.getStringValue();

            if (httpsProxy.isPresent()) {
                Optional<String> envProxyProtocol = EnvironmentProxyUtils.parseProtocol(httpsProxy.get());
                return envProxyProtocol.orElse(null);
            }
            if (httpProxy.isPresent()) {
                Optional<String> envProxyProtocol = EnvironmentProxyUtils.parseProtocol(httpProxy.get());
                return envProxyProtocol.orElse(null);
            }
        }
        return null;
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
         *
         * This value is set to "true" by default which means SDK will automatically use system property values
         * for options that are not provided during building the {@link ProxyConfiguration} object. To disable this behavior,
         * set this value to "false".
         */
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);

        /**
         * Option whether to use environment variables from {@link ProxyEnvironmentSetting} if any of the config options are
         * missing.
         *
         * If this is used in conjunction with {@link #useSystemPropertyValues(Boolean)}, any system proxy properties provided
         * will take precedence over environment variables.
         */
        Builder useEnvironmentVariables(Boolean useEnvironmentVariables);

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
        private Boolean useEnvironmentVariables = Boolean.FALSE;

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
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}
