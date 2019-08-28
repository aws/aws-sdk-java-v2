/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Proxy configuration for {@link NettyNioAsyncHttpClient}. This class is used to configure an HTTP proxy to be used by
 * the {@link NettyNioAsyncHttpClient}.
 *
 * @see NettyNioAsyncHttpClient.Builder#proxyConfiguration(ProxyConfiguration)
 */
@SdkPublicApi
public final class ProxyConfiguration implements ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration> {
    private final String scheme;
    private final String host;
    private final int port;
    private final Set<String> nonProxyHosts;

    private ProxyConfiguration(BuilderImpl builder) {
        this.scheme = builder.scheme;
        this.host = builder.host;
        this.port = builder.port;
        this.nonProxyHosts = Collections.unmodifiableSet(builder.nonProxyHosts);
    }

    /**
     * @return The proxy scheme.
     */
    public String scheme() {
        return scheme;
    }

    /**
     * @return The proxy host.
     */
    public String host() {
        return host;
    }

    /**
     * @return The proxy port.
     */
    public int port() {
        return port;
    }

    /**
     * @return The set of hosts that should not be proxied.
     */
    public Set<String> nonProxyHosts() {
        return nonProxyHosts;
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

        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) {
            return false;
        }

        if (host != null ? !host.equals(that.host) : that.host != null) {
            return false;
        }

        return nonProxyHosts.equals(that.nonProxyHosts);

    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + nonProxyHosts.hashCode();
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
         * Set the set of hosts that should not be proxied. Any request whose host portion matches any of the patterns
         * given in the set will be sent to the remote host directly instead of through the proxy.
         *
         * @param nonProxyHosts The set of hosts that should not be proxied.
         * @return This object for method chaining.
         */
        Builder nonProxyHosts(Set<String> nonProxyHosts);
    }

    private static final class BuilderImpl implements Builder {
        private String scheme;
        private String host;
        private int port;
        private Set<String> nonProxyHosts = Collections.emptySet();

        private BuilderImpl() {
        }

        private BuilderImpl(ProxyConfiguration proxyConfiguration) {
            this.scheme = proxyConfiguration.scheme;
            this.host = proxyConfiguration.host;
            this.port = proxyConfiguration.port;
            this.nonProxyHosts = new HashSet<>(proxyConfiguration.nonProxyHosts);
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
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}
