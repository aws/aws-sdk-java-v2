/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration that defines how to communicate via an HTTP proxy.
 */
@ReviewBeforeRelease("Review which options are required and which are optional.")
@SdkPublicApi
public final class ProxyConfiguration implements ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration> {

    private final URI endpoint;
    private final String username;
    private final String password;
    private final String ntlmDomain;
    private final String ntlmWorkstation;
    private final Set<String> nonProxyHosts;
    private final Boolean preemptiveBasicAuthenticationEnabled;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ProxyConfiguration(DefaultClientProxyConfigurationBuilder builder) {
        this.endpoint = builder.endpoint;
        this.username = builder.username;
        this.password = builder.password;
        this.ntlmDomain = builder.ntlmDomain;
        this.ntlmWorkstation = builder.ntlmWorkstation;
        this.nonProxyHosts = Collections.unmodifiableSet(new HashSet<>(builder.nonProxyHosts));
        this.preemptiveBasicAuthenticationEnabled = builder.preemptiveBasicAuthenticationEnabled == null ? Boolean.FALSE :
                builder.preemptiveBasicAuthenticationEnabled;
    }

    /**
     * The endpoint of the proxy server that the SDK should connect through.
     *
     * @see Builder#endpoint(URI)
     */
    public URI endpoint() {
        return endpoint;
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
     * @see Builder#nonProxyHosts(Set)
     */
    @ReviewBeforeRelease("Revisit the presentation of this option and support http.nonProxyHosts property")
    public Set<String> nonProxyHosts() {
        return nonProxyHosts;
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
                .preemptiveBasicAuthenticationEnabled(preemptiveBasicAuthenticationEnabled);
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
        private Set<String> nonProxyHosts = new HashSet<>();
        private Boolean preemptiveBasicAuthenticationEnabled;

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
            this.nonProxyHosts = new HashSet<>(nonProxyHosts);
            return this;
        }

        @Override
        public Builder addNonProxyHost(String nonProxyHost) {
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
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}
