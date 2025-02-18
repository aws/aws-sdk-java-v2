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
    private final Integer keepAliveMaxFailedProbes;

    private TcpKeepAliveConfiguration(DefaultTcpKeepAliveConfigurationBuilder builder) {
        this.keepAliveInterval = Validate.isPositive(builder.keepAliveInterval,
                                                     "keepAliveInterval");
        this.keepAliveTimeout = Validate.isPositive(builder.keepAliveTimeout,
                                                    "keepAliveTimeout");
        this.keepAliveMaxFailedProbes = builder.keepAliveMaxFailedProbes == null
                                        ? null
                                        : Validate.isNotNegative(builder.keepAliveMaxFailedProbes,
                                                                 "keepAliveMaxFailedProbes");
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

    /**
     * @return number of keep alive probes allowed to fail before the connection is considered lost.
     */
    public Integer keepAliveMaxFailedProbes() {
        return keepAliveMaxFailedProbes;
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

        /**
         * Sets the number of keep alive probes allowed to fail before the connection is considered lost.
         * @param keepAliveMaxFailedProbes The maximum number of keep-alive probes to send.
         * @return Builder
         */
        Builder keepAliveMaxFailedProbes(Integer keepAliveMaxFailedProbes);

        TcpKeepAliveConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultTcpKeepAliveConfigurationBuilder implements Builder {
        private Integer keepAliveMaxFailedProbes;
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

        /**
         * Sets the maximum number of TCP keep-alive probes to send before giving up and declaring the connection dead.
         * @param keepAliveMaxFailedProbes The maximum number of keep-alive probes to send.
         * @return Builder
         */
        @Override
        public Builder keepAliveMaxFailedProbes(Integer keepAliveMaxFailedProbes) {
            this.keepAliveMaxFailedProbes = keepAliveMaxFailedProbes;
            return this;
        }

        @Override
        public TcpKeepAliveConfiguration build() {
            return new TcpKeepAliveConfiguration(this);
        }
    }
}
