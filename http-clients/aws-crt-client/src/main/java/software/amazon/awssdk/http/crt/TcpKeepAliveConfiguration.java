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

package software.amazon.awssdk.http.crt;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Configuration that defines keep-alive options for all connections established by
 * the {@link TcpKeepAliveConfiguration}.
 */
@SdkPublicApi
public final class TcpKeepAliveConfiguration {

    private final Duration keepAliveInterval;
    private final Duration keepAliveTimeout;

    private TcpKeepAliveConfiguration(DefaultTcpKeepAliveConfigurationBuilder builder) {
        this.keepAliveInterval = Validate.isPositive(builder.keepAliveInterval,
                                                     "keepAliveInterval");
        this.keepAliveTimeout = Validate.isPositive(builder.keepAliveTimeout,
                                                    "keepAliveTimeout");
    }

    /**
     * @return number of seconds between TCP keepalive packets being sent to the peer
     */
    public Duration keepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * @return number of seconds to wait for a keepalive response before considering the connection timed out
     */
    public Duration keepAliveTimeout() {
        return keepAliveTimeout;
    }

    public static Builder builder() {
        return new DefaultTcpKeepAliveConfigurationBuilder();
    }

    /**
     * A builder for {@link TcpKeepAliveConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder {
        /**
         * Sets the Duration between TCP keepalive packets being sent to the peer
         * @param keepAliveInterval Duration between TCP keepalive packets being sent to the peer
         * @return Builder
         */
        Builder keepAliveInterval(Duration keepAliveInterval);

        /**
         * Sets the Duration to wait for a keepalive response before considering the connection timed out
         * @param keepAliveTimeout Duration to wait for a keepalive response before considering the connection timed out
         * @return Builder
         */
        Builder keepAliveTimeout(Duration keepAliveTimeout);

        TcpKeepAliveConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultTcpKeepAliveConfigurationBuilder implements Builder {
        private Duration keepAliveInterval;
        private Duration keepAliveTimeout;

        private DefaultTcpKeepAliveConfigurationBuilder() {
        }

        /**
         * Sets the Duration between TCP keepalive packets being sent to the peer
         * @param keepAliveInterval Duration between TCP keepalive packets being sent to the peer
         * @return Builder
         */
        @Override
        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        /**
         * Sets the Duration to wait for a keepalive response before considering the connection timed out
         * @param keepAliveTimeout Duration to wait for a keepalive response before considering the connection timed out
         * @return Builder
         */
        @Override
        public Builder keepAliveTimeout(Duration keepAliveTimeout) {
            this.keepAliveTimeout = keepAliveTimeout;
            return this;
        }

        @Override
        public TcpKeepAliveConfiguration build() {
            return new TcpKeepAliveConfiguration(this);
        }
    }
}
