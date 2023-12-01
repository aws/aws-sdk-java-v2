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

package software.amazon.awssdk.http.urlconnection;

import static software.amazon.awssdk.utils.StringUtils.isEmpty;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ProxyConfigProvider;
import software.amazon.awssdk.utils.ProxyEnvironmentSetting;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Proxy configuration for {@link UrlConnectionHttpClient}. This class is used to configure an HTTP proxy to be used by
 * the {@link UrlConnectionHttpClient}.
 *
 * @see UrlConnectionHttpClient.Builder#proxyConfiguration(ProxyConfiguration)
 */
@SdkPublicApi
public final class ProxyConfiguration implements ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration> {

    private final URI endpoint;
    private final String username;
    private final String password;
    private final Set<String> nonProxyHosts;
    private final String host;
    private final int port;
    private final String scheme;
    private final boolean useSystemPropertyValues;
    private final boolean useEnvironmentVariablesValues;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ProxyConfiguration(DefaultClientProxyConfigurationBuilder builder) {
        this.endpoint = builder.endpoint;
        String resolvedScheme = resolveScheme(builder);
        this.scheme = resolvedScheme;
        ProxyConfigProvider proxyConfigProvider =
            ProxyConfigProvider.fromSystemEnvironmentSettings(
                builder.useSystemPropertyValues,
                builder.useEnvironmentVariablesValues,
                resolvedScheme);

        this.username = resolveUsername(builder, proxyConfigProvider);
        this.password = resolvePassword(builder, proxyConfigProvider);
        this.nonProxyHosts = resolveNonProxyHosts(builder, proxyConfigProvider);
        this.useSystemPropertyValues = builder.useSystemPropertyValues;

        if (builder.endpoint != null) {
            this.host = builder.endpoint.getHost();
            this.port = builder.endpoint.getPort();
        } else {
            this.host = proxyConfigProvider != null ? proxyConfigProvider.host() : null;
            this.port = proxyConfigProvider != null ? proxyConfigProvider.port() : 0;
        }
        this.useEnvironmentVariablesValues = builder.useEnvironmentVariablesValues;
    }

    private String resolveScheme(DefaultClientProxyConfigurationBuilder builder) {
        if (endpoint != null) {
            return endpoint.getScheme();
        } else {
            return builder.scheme;
        }
    }

    private static String resolvePassword(DefaultClientProxyConfigurationBuilder builder,
                                          ProxyConfigProvider proxyConfigProvider) {
        if (builder.password != null || proxyConfigProvider == null) {
            return builder.password;
        }
        return proxyConfigProvider.password().orElseGet(() -> builder.password);
    }

    private static Set<String> resolveNonProxyHosts(DefaultClientProxyConfigurationBuilder builder,
                                                    ProxyConfigProvider proxyConfigProvider) {
        return builder.nonProxyHosts != null || proxyConfigProvider == null ? builder.nonProxyHosts :
               proxyConfigProvider.nonProxyHosts();
    }

    private static String resolveUsername(DefaultClientProxyConfigurationBuilder builder,
                                          ProxyConfigProvider proxyConfigProvider) {
        if (builder.username != null || proxyConfigProvider == null) {
            return builder.username;
        }
        return proxyConfigProvider.userName().orElseGet(() -> builder.username);
    }

    /**
     * Returns the proxy host name either from the configured endpoint or
     * from the "http.proxyHost" system property if {@link Builder#useSystemPropertyValues(Boolean)} is set to true.
     */
    public String host() {
        return host;
    }

    /**
     * Returns the proxy port either from the configured endpoint or
     * from the "http.proxyPort" system property if {@link Builder#useSystemPropertyValues(Boolean)} is set to true.
     *
     * If no value is found in neither of the above options, the default value of 0 is returned.
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
     * @see Builder#password(String)
     */
    public String username() {
        return username;
    }

    /**
     * The password to use when connecting through a proxy.
     *
     * @see Builder#password(String)
     */
    public String password() {
        return password;
    }

    /**
     * The hosts that the client is allowed to access without going through the proxy.
     *
     * If the value is not set on the object, the value represent by "http.nonProxyHosts" system property is returned.
     * If system property is also not set, an unmodifiable empty set is returned.
     *
     * @see Builder#nonProxyHosts(Set)
     */
    public Set<String> nonProxyHosts() {
        return Collections.unmodifiableSet(nonProxyHosts != null ? nonProxyHosts : Collections.emptySet());
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .endpoint(endpoint)
                .username(username)
                .password(password)
                .nonProxyHosts(nonProxyHosts)
                .useSystemPropertyValues(useSystemPropertyValues)
                .scheme(scheme)
                .useEnvironmentVariablesValues(useEnvironmentVariablesValues);
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
                       .add("nonProxyHosts", nonProxyHosts)
                       .build();
    }

    public String resolveScheme() {
        return endpoint != null ? endpoint.getScheme() : scheme;
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
         * Option whether to use system property values from {@link ProxySystemSetting} if any of the config options are missing.
         * <p>
         * This value is set to "true" by default which means SDK will automatically use system property values for options that
         * are not provided during building the {@link ProxyConfiguration} object. To disable this behavior, set this value to
         * "false".It is important to note that when this property is set to "true," all proxy settings will exclusively originate
         * from system properties, and no partial settings will be obtained from EnvironmentVariableValues
         */
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);

        /**
         * Option whether to use environment variable values from {@link ProxyEnvironmentSetting} if any of the config options are
         * missing. This value is set to "false" by default, which means SDK will not automatically use environment variable
         * values for options that are not provided during building of  {@link ProxyConfiguration} object. To enable this
         * behavior, set this value to "true".It is important to note that when this property is set to "true," all proxy
         * settings will exclusively originate from environment variableValues, and no partial settings will be obtained from
         * SystemPropertyValues.
         *
         * @param useEnvironmentVariablesValues The option whether to use environment variable values
         * @return This object for method chaining.
         */
        Builder useEnvironmentVariablesValues(Boolean useEnvironmentVariablesValues);

        /**
         * The HTTP scheme to use for connecting to the proxy. Valid values are {@code http} and {@code https}.
         * <p>
         * The client defaults to {@code http} if none is given.
         *
         * @param scheme The proxy scheme.
         * @return This object for method chaining.
         */
        Builder scheme(String scheme);
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientProxyConfigurationBuilder implements Builder {

        private URI endpoint;
        private String username;
        private String scheme = "http";
        private String password;
        private Set<String> nonProxyHosts;
        private Boolean useSystemPropertyValues = Boolean.TRUE;
        private Boolean useEnvironmentVariablesValues = Boolean.FALSE;

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
        public Builder useSystemPropertyValues(Boolean useSystemPropertyValues) {
            this.useSystemPropertyValues = useSystemPropertyValues;
            return this;
        }

        public void setUseSystemPropertyValues(Boolean useSystemPropertyValues) {
            useSystemPropertyValues(useSystemPropertyValues);
        }

        @Override
        public Builder useEnvironmentVariablesValues(Boolean useEnvironmentVariablesValues) {
            this.useEnvironmentVariablesValues = useEnvironmentVariablesValues;
            return this;
        }

        @Override
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public void setScheme(String scheme) {
            scheme(scheme);
        }

        public void setUseEnvironmentVariablesValues(Boolean useEnvironmentVariablesValues) {
            useEnvironmentVariablesValues(useEnvironmentVariablesValues);
        }

        @Override
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}
