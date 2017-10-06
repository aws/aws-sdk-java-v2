/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.SOCKET_TIMEOUT;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.Defaults;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Factory for creating an instance of {@link SdkHttpClient}. The factory can be configured through the builder {@link
 * #builder()}, once built it  can create a {@link SdkHttpClient} via {@link #createHttpClient()} or can be passed to the SDK
 * client builders directly to have the SDK create and manage the HTTP client. See documentation on the service's respective
 * client builder for more information on configuring the HTTP layer.
 *
 * <pre class="brush: java">
 * SdkHttpClient httpClient = ApacheSdkHttpClientFactory.builder()
 * .socketTimeout(Duration.ofSeconds(10))
 * .build()
 * .createHttpClient();
 * </pre>
 */
public final class ApacheSdkHttpClientFactory
        implements SdkHttpClientFactory, ToCopyableBuilder<ApacheSdkHttpClientFactory.Builder, ApacheSdkHttpClientFactory> {

    private final AttributeMap standardOptions;
    private final ProxyConfiguration proxyConfiguration;
    private final Optional<InetAddress> localAddress;
    private final Optional<Boolean> expectContinueEnabled;
    private final Optional<Duration> connectionPoolTtl;
    private final Optional<Duration> maxIdleConnectionTimeout;

    private ApacheSdkHttpClientFactory(DefaultBuilder builder) {
        this.standardOptions = builder.standardOptions.build();
        this.proxyConfiguration = builder.proxyConfiguration;
        this.localAddress = Optional.ofNullable(builder.localAddress);
        this.expectContinueEnabled = Optional.ofNullable(builder.expectContinueEnabled);
        this.connectionPoolTtl = Optional.ofNullable(builder.connectionTimeToLive);
        this.maxIdleConnectionTimeout = Optional.ofNullable(builder.connectionMaxIdleTime);
    }

    public ProxyConfiguration proxyConfiguration() {
        return proxyConfiguration;
    }

    @ReviewBeforeRelease("This isn't currently used. Remove or implement.")
    public Optional<InetAddress> localAddress() {
        return localAddress;
    }

    @ReviewBeforeRelease("This isn't currently used. Remove or implement.")
    public Optional<Boolean> expectContinueEnabled() {
        return expectContinueEnabled;
    }

    public Optional<Duration> connectionTimeToLive() {
        return connectionPoolTtl;
    }

    public Optional<Duration> connectionMaxIdleTime() {
        return maxIdleConnectionTimeout;
    }

    public SdkHttpClient createHttpClient() {
        return createHttpClientWithDefaults(AttributeMap.empty());
    }

    @Override
    public SdkHttpClient createHttpClientWithDefaults(AttributeMap serviceDefaults) {
        AttributeMap resolvedOptions = standardOptions.merge(serviceDefaults).merge(GLOBAL_HTTP_DEFAULTS);
        return new ApacheHttpClientFactory().create(this, resolvedOptions, createRequestConfig(resolvedOptions));
    }

    private ApacheHttpRequestConfig createRequestConfig(AttributeMap resolvedOptions) {
        return ApacheHttpRequestConfig.builder()
                                      .socketTimeout(resolvedOptions.get(SOCKET_TIMEOUT))
                                      .connectionTimeout(resolvedOptions.get(CONNECTION_TIMEOUT))
                                      .proxyConfiguration(proxyConfiguration)
                                      .localAddress(localAddress.orElse(null))
                                      .expectContinueEnabled(expectContinueEnabled.orElse(Defaults.EXPECT_CONTINUE_ENABLED))
                                      .build();
    }

    /**
     * @return Builder instance to construct a {@link ApacheSdkHttpClientFactory}.
     */
    public static Builder builder() {
        return new DefaultBuilder(AttributeMap.builder());
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(standardOptions.toBuilder())
                .proxyConfiguration(proxyConfiguration)
                .localAddress(localAddress.orElse(null))
                .expectContinueEnabled(expectContinueEnabled.orElse(null))
                .connectionTimeToLive(connectionPoolTtl.orElse(null))
                .connectionMaxIdleTime(maxIdleConnectionTimeout.orElse(null));
    }

    /**
     * Builder for {@link ApacheSdkHttpClientFactory}.
     */
    public interface Builder extends CopyableBuilder<Builder, ApacheSdkHttpClientFactory> {

        /**
         * The amount of time to wait for data to be transferred over an established, open connection before the connection is
         * timed out. A duration of 0 means infinity, and is not recommended.
         */
        Builder socketTimeout(Duration socketTimeout);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out. A duration of 0
         * means infinity, and is not recommended.
         */
        Builder connectionTimeout(Duration connectionTimeout);

        /**
         * The maximum number of connections allowed in the connection pool. Each built HTTP client has it's own private
         * connection pool.
         */
        Builder maxConnections(Integer maxConnections);

        /**
         * Configuration that defines how to communicate via an HTTP proxy.
         */
        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /**
         * Configure the local address that the HTTP client should use for communication.
         */
        Builder localAddress(InetAddress localAddress);

        /**
         * Configure whether the client should send an HTTP expect-continue handshake before each request.
         */
        Builder expectContinueEnabled(Boolean expectContinueEnabled);

        /**
         * The maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
         */
        Builder connectionTimeToLive(Duration connectionTimeToLive);

        /**
         * Configure the maximum amount of time that a connection should be allowed to remain open while idle.
         */
        Builder connectionMaxIdleTime(Duration connectionMaxIdleTime);

    }

    /**
     * Builder for a {@link ApacheSdkHttpClientFactory}.
     */
    @ReviewBeforeRelease("Review the options we expose and revisit organization of options.")
    private static final class DefaultBuilder implements Builder {

        private final AttributeMap.Builder standardOptions;
        private ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();
        private InetAddress localAddress;
        private Boolean expectContinueEnabled;
        private Duration connectionTimeToLive;
        private Duration connectionMaxIdleTime;

        private DefaultBuilder(AttributeMap.Builder standardOptions) {
            this.standardOptions = standardOptions;
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptions.put(SOCKET_TIMEOUT, socketTimeout);
            return this;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            socketTimeout(socketTimeout);
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.put(CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            standardOptions.put(MAX_CONNECTIONS, maxConnections);
            return this;
        }

        public void setMaxConnections(Integer maxConnections) {
            maxConnections(maxConnections);
        }

        @Override
        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
            proxyConfiguration(proxyConfiguration);
        }

        @Override
        public Builder localAddress(InetAddress localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        public void setLocalAddress(InetAddress localAddress) {
            localAddress(localAddress);
        }

        @Override
        public Builder expectContinueEnabled(Boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        public void setExpectContinueEnabled(Boolean useExpectContinue) {
            this.expectContinueEnabled = useExpectContinue;
        }

        @Override
        public Builder connectionTimeToLive(Duration connectionTimeToLive) {
            this.connectionTimeToLive = connectionTimeToLive;
            return this;
        }

        public void setConnectionTimeToLive(Duration connectionTimeToLive) {
            connectionTimeToLive(connectionTimeToLive);
        }

        @Override
        public Builder connectionMaxIdleTime(Duration maxIdleConnectionTimeout) {
            this.connectionMaxIdleTime = maxIdleConnectionTimeout;
            return this;
        }

        public void setConnectionMaxIdleTime(Duration connectionMaxIdleTime) {
            connectionMaxIdleTime(connectionMaxIdleTime);
        }

        @Override
        public ApacheSdkHttpClientFactory build() {
            return new ApacheSdkHttpClientFactory(this);
        }
    }
}

