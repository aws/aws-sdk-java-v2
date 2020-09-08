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
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Configuration that defines health checks for for all connections established by
 * the{@link ConnectionHealthChecksConfiguration}.
 *
 * <b>NOTE:</b> This is a Preview API and is subject to change so it should not be used in production.
 */
@SdkPublicApi
@SdkPreviewApi
public final class ConnectionHealthChecksConfiguration {
    private final long minThroughputInBytesPerSecond;
    private final Duration allowableThroughputFailureInterval;

    private ConnectionHealthChecksConfiguration(DefaultConnectionHealthChecksConfigurationBuilder builder) {
        this.minThroughputInBytesPerSecond = Validate.paramNotNull(builder.minThroughputInBytesPerSecond,
                                                                   "minThroughputInBytesPerSecond");
        this.allowableThroughputFailureInterval = Validate.isPositive(builder.allowableThroughputFailureIntervalSeconds,
                                                                      "allowableThroughputFailureIntervalSeconds");
    }

    /**
     * @return the minimum amount of throughput, in bytes per second, for a connection to be considered healthy.
     */
    public long minThroughputInBytesPerSecond() {
        return minThroughputInBytesPerSecond;
    }

    /**
     * @return How long a connection is allowed to be unhealthy before getting shut down.
     */
    public Duration allowableThroughputFailureInterval() {
        return allowableThroughputFailureInterval;
    }

    public static Builder builder() {
        return new DefaultConnectionHealthChecksConfigurationBuilder();
    }

    /**
     * A builder for {@link ConnectionHealthChecksConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder {

        /**
         * Sets a throughput threshold for connections. Throughput below this value will be considered unhealthy.
         *
         * @param minThroughputInBytesPerSecond minimum amount of throughput, in bytes per second, for a connection to be
         * considered healthy.
         * @return Builder
         */
        Builder minThroughputInBytesPerSecond(Long minThroughputInBytesPerSecond);

        /**
         * Sets how long a connection is allowed to be unhealthy before getting shut down.
         *
         * <p>
         * It only supports seconds precision
         *
         * @param allowableThroughputFailureIntervalSeconds How long a connection is allowed to be unhealthy
         * before getting shut down.
         * @return Builder
         */
        Builder allowableThroughputFailureInterval(Duration allowableThroughputFailureIntervalSeconds);

        ConnectionHealthChecksConfiguration build();
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    private static final class DefaultConnectionHealthChecksConfigurationBuilder implements Builder {
        private Long minThroughputInBytesPerSecond;
        private Duration allowableThroughputFailureIntervalSeconds;

        private DefaultConnectionHealthChecksConfigurationBuilder() {
        }

        @Override
        public Builder minThroughputInBytesPerSecond(Long minThroughputInBytesPerSecond) {
            this.minThroughputInBytesPerSecond = minThroughputInBytesPerSecond;
            return this;
        }

        @Override
        public Builder allowableThroughputFailureInterval(Duration allowableThroughputFailureIntervalSeconds) {
            this.allowableThroughputFailureIntervalSeconds = allowableThroughputFailureIntervalSeconds;
            return this;
        }

        @Override
        public ConnectionHealthChecksConfiguration build() {
            return new ConnectionHealthChecksConfiguration(this);
        }
    }
}
