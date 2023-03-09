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
 * Configuration that defines health checks for all connections established by
 * the {@link ConnectionHealthConfiguration}.
 *
 */
@SdkPublicApi
public final class ConnectionHealthConfiguration {
    private final long minimumThroughputInBps;
    private final Duration minimumThroughputTimeout;

    private ConnectionHealthConfiguration(DefaultConnectionHealthConfigurationBuilder builder) {
        this.minimumThroughputInBps = Validate.paramNotNull(builder.minimumThroughputInBps,
                                                            "minimumThroughputInBps");
        this.minimumThroughputTimeout = Validate.isPositive(builder.minimumThroughputTimeout,
                                                            "minimumThroughputTimeout");
    }

    /**
     * @return the minimum amount of throughput, in bytes per second, for a connection to be considered healthy.
     */
    public long minimumThroughputInBps() {
        return minimumThroughputInBps;
    }

    /**
     * @return How long a connection is allowed to be unhealthy before getting shut down.
     */
    public Duration minimumThroughputTimeout() {
        return minimumThroughputTimeout;
    }

    public static Builder builder() {
        return new DefaultConnectionHealthConfigurationBuilder();
    }

    /**
     * A builder for {@link ConnectionHealthConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder {

        /**
         * Sets a throughput threshold for connections. Throughput below this value will be considered unhealthy.
         *
         * @param minimumThroughputInBps minimum amount of throughput, in bytes per second, for a connection to be
         * considered healthy.
         * @return Builder
         */
        Builder minimumThroughputInBps(Long minimumThroughputInBps);

        /**
         * Sets how long a connection is allowed to be unhealthy before getting shut down.
         *
         * <p>
         * It only supports seconds precision
         *
         * @param minimumThroughputTimeout How long a connection is allowed to be unhealthy
         * before getting shut down.
         * @return Builder
         */
        Builder minimumThroughputTimeout(Duration minimumThroughputTimeout);

        ConnectionHealthConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultConnectionHealthConfigurationBuilder implements Builder {
        private Long minimumThroughputInBps;
        private Duration minimumThroughputTimeout;

        private DefaultConnectionHealthConfigurationBuilder() {
        }

        @Override
        public Builder minimumThroughputInBps(Long minimumThroughputInBps) {
            this.minimumThroughputInBps = minimumThroughputInBps;
            return this;
        }

        @Override
        public Builder minimumThroughputTimeout(Duration minimumThroughputTimeout) {
            this.minimumThroughputTimeout = minimumThroughputTimeout;
            return this;
        }

        @Override
        public ConnectionHealthConfiguration build() {
            return new ConnectionHealthConfiguration(this);
        }
    }
}
