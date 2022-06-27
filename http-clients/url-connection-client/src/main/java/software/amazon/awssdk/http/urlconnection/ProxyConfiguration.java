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
import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsProperty;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
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

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ProxyConfiguration(DefaultClientProxyConfigurationBuilder builder) {
        this.endpoint = builder.endpoint;
        this.username = builder.username;
        this.password = builder.password;
        this.nonProxyHosts = builder.nonProxyHosts;
        this.useSystemPropertyValues = builder.useSystemPropertyValues;
        this.host = resolveHost();
        this.port = resolvePort();
        this.scheme = resolveScheme();
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
        return resolveValue(username, ProxySystemSetting.PROXY_USERNAME);
    }

    /**
     * The password to use when connecting through a proxy.
     *
     * @see Builder#password(String)
     */
    public String password() {
        return resolveValue(password, ProxySystemSetting.PROXY_PASSWORD);
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
        Set<String> hosts = nonProxyHosts == null && useSystemPropertyValues ? parseNonProxyHostsProperty()
                                                                             : nonProxyHosts;

        return Collections.unmodifiableSet(hosts != null ? hosts : Collections.emptySet());
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .endpoint(endpoint)
                .username(username)
                .password(password)
                .nonProxyHosts(nonProxyHosts)
                .useSystemPropertyValues(useSystemPropertyValues);
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


    private String resolveHost() {
        return endpoint != null ? endpoint.getHost()
                                : resolveValue(null, ProxySystemSetting.PROXY_HOST);
    }

    private int resolvePort() {
        if (endpoint != null) {
            return endpoint.getPort();
        } else if (useSystemPropertyValues) {
            return ProxySystemSetting.PROXY_PORT.getStringValue().map(Integer::parseInt).orElse(0);
        }
        return 0;
    }

    public String resolveScheme() {
        return endpoint != null ? endpoint.getScheme() : null;
    }

    /**
     * Uses the configuration options, system setting property and returns the final value of the given member.
     */
    private String resolveValue(String value, ProxySystemSetting systemSetting) {
        return value == null && useSystemPropertyValues ? systemSetting.getStringValue().orElse(null)
                                                        : value;
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
         *
         * This value is set to "true" by default which means SDK will automatically use system property values
         * for options that are not provided during building the {@link ProxyConfiguration} object. To disable this behavior,
         * set this value to "false".
         */
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);

    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultClientProxyConfigurationBuilder implements Builder {

        private URI endpoint;
        private String username;
        private String password;
        private Set<String> nonProxyHosts;
        private Boolean useSystemPropertyValues = Boolean.TRUE;

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
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}
