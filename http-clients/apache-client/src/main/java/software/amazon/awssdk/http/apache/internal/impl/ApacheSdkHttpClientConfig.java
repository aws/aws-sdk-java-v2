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

package software.amazon.awssdk.http.apache.internal.impl;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.apache.internal.Defaults;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkInternalApi
public class ApacheSdkHttpClientConfig implements ToCopyableBuilder<ApacheSdkHttpClientConfig.Builder,
        ApacheSdkHttpClientConfig> {
    private AttributeMap standardOptions;
    private ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();
    private InetAddress localAddress;
    private boolean expectContinueEnabled;
    private Duration connectionTimeToLive;
    private Duration connectionMaxIdleTime;


    private ApacheSdkHttpClientConfig(DefaultBuilder builder) {
        this.standardOptions = builder.standardOptionsBuilder.build();
        this.proxyConfiguration = builder.proxyConfiguration;
        this.localAddress = builder.localAddress;
        this.expectContinueEnabled = builder.expectContinueEnabled;
        this.connectionTimeToLive = builder.connectionTimeToLive;
        this.connectionMaxIdleTime = builder.connectionMaxIdleTime;
    }

    public ProxyConfiguration proxyConfiguration() {
        return proxyConfiguration;
    }

    public Optional<Duration> socketTimeout() {
        return Optional.ofNullable(standardOptions.get(SdkHttpConfigurationOption.SOCKET_TIMEOUT));
    }

    public Optional<Duration> connectionTimeout() {
        return Optional.ofNullable(standardOptions.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT));
    }

    public Optional<Integer> maxConnections() {
        return Optional.ofNullable(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));
    }

    public Optional<InetAddress> localAddress() {
        return Optional.ofNullable(localAddress);
    }

    public boolean expectContinueEnabled() {
        return expectContinueEnabled;
    }

    Optional<Duration> connectionTimeToLive() {
        return Optional.ofNullable(connectionTimeToLive);
    }

    public Optional<Duration> connectionMaxIdleTime() {
        return Optional.ofNullable(connectionMaxIdleTime);
    }

    public boolean useStrictHostnameVerification() {
        return Optional.ofNullable(standardOptions.get(SdkHttpConfigurationOption.USE_STRICT_HOSTNAME_VERIFICATION))
                .orElse(false);
    }

    public <T> Optional<T> getOption(AttributeMap.Key<T> key) {
        return Optional.ofNullable(standardOptions.get(key));
    }

    @Override
    public Builder toBuilder() {
        return null;
    }

    public static Builder builder() {
        return builder(AttributeMap.empty());
    }

    public static Builder builder(AttributeMap serviceDefaults) {
        return new DefaultBuilder(serviceDefaults);
    }

    public interface Builder extends CopyableBuilder<ApacheSdkHttpClientConfig.Builder, ApacheSdkHttpClientConfig> {

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
        @ReviewBeforeRelease("We don't test this.")
        Builder proxyConfiguration(ProxyConfiguration proxyConfiguration);

        /**
         * Similar to {@link #proxyConfiguration(ProxyConfiguration)}, but takes a lambda to configure a new
         * {@link ProxyConfiguration.Builder}. This removes the need to called {@link ProxyConfiguration#builder()} and
         * {@link ProxyConfiguration.Builder#build()}.
         */
        default Builder proxyConfiguration(Consumer<ProxyConfiguration.Builder> proxyConfiguration) {
            return proxyConfiguration(ProxyConfiguration.builder().apply(proxyConfiguration).build());
        }

        /**
         * Configure the local address that the HTTP client should use for communication.
         */
        Builder localAddress(InetAddress localAddress);

        /**
         * Configure whether the client should send an HTTP expect-continue handshake before each request.
         */
        Builder expectContinueEnabled(boolean expectContinueEnabled);

        /**
         * The maximum amount of time that a connection should be allowed to remain open, regardless of usage frequency.
         */
        Builder connectionTimeToLive(Duration connectionTimeToLive);

        /**
         * Configure the maximum amount of time that a connection should be allowed to remain open while idle.
         */
        Builder connectionMaxIdleTime(Duration connectionMaxIdleTime);

        Builder useStrictHostnameVerification(boolean useStrictHostnameVerification);
    }

    @ReviewBeforeRelease("Review the options we expose and revisit organization of options.")
    private static final class DefaultBuilder implements Builder {
        private final AttributeMap.Builder standardOptionsBuilder;
        private ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().build();
        private InetAddress localAddress;
        private boolean expectContinueEnabled = Defaults.EXPECT_CONTINUE_ENABLED;
        private Duration connectionTimeToLive;
        private Duration connectionMaxIdleTime;


        DefaultBuilder() {
            this(AttributeMap.empty());
        }

        DefaultBuilder(AttributeMap serviceDefaults) {
            standardOptionsBuilder = serviceDefaults.toBuilder();
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptionsBuilder.put(SdkHttpConfigurationOption.SOCKET_TIMEOUT, socketTimeout);
            return this;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            socketTimeout(socketTimeout);
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptionsBuilder.put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        @Override
        public Builder maxConnections(Integer maxConnections) {
            standardOptionsBuilder.put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConnections);
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
        public Builder expectContinueEnabled(boolean expectContinueEnabled) {
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
        public Builder useStrictHostnameVerification(boolean useStrictHostnameVerification) {
            standardOptionsBuilder.put(SdkHttpConfigurationOption.USE_STRICT_HOSTNAME_VERIFICATION,
                    useStrictHostnameVerification);
            return this;
        }

        public void setUseStrictHostnameVerification(boolean useStrictHostnameVerification) {
            useStrictHostnameVerification(useStrictHostnameVerification);
        }

        @Override
        public ApacheSdkHttpClientConfig build() {
            return new ApacheSdkHttpClientConfig(this);
        }
    }
}
