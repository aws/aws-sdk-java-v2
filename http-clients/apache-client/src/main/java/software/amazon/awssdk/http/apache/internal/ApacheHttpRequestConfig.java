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

package software.amazon.awssdk.http.apache.internal;

import java.net.InetAddress;
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

/**
 * Configuration needed when building an Apache request. Note that at this time, we only support client level configuration so
 * all of these settings are supplied when creating the client.
 */
@SdkInternalApi
public final class ApacheHttpRequestConfig {

    private final Duration socketTimeout;
    private final Duration connectionTimeout;
    private final InetAddress localAddress;
    private final boolean expectContinueEnabled;
    private final ProxyConfiguration proxyConfiguration;

    private ApacheHttpRequestConfig(Builder builder) {
        this.socketTimeout = builder.socketTimeout;
        this.connectionTimeout = builder.connectionTimeout;
        this.localAddress = builder.localAddress;
        this.expectContinueEnabled = builder.expectContinueEnabled;
        this.proxyConfiguration = builder.proxyConfiguration;
    }

    public Duration socketTimeout() {
        return socketTimeout;
    }

    public Duration connectionTimeout() {
        return connectionTimeout;
    }

    public InetAddress localAddress() {
        return localAddress;
    }

    public boolean expectContinueEnabled() {
        return expectContinueEnabled;
    }

    public ProxyConfiguration proxyConfiguration() {
        return proxyConfiguration;
    }

    /**
     * @return Builder instance to construct a {@link ApacheHttpRequestConfig}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link ApacheHttpRequestConfig}.
     */
    public static final class Builder {

        private Duration socketTimeout;
        private Duration connectionTimeout;
        private InetAddress localAddress;
        private Boolean expectContinueEnabled;
        private ProxyConfiguration proxyConfiguration;

        private Builder() {
        }

        public Builder socketTimeout(Duration socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder localAddress(InetAddress localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        public Builder expectContinueEnabled(Boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        /**
         * @return An immutable {@link ApacheHttpRequestConfig} object.
         */
        public ApacheHttpRequestConfig build() {
            return new ApacheHttpRequestConfig(this);
        }
    }

}
