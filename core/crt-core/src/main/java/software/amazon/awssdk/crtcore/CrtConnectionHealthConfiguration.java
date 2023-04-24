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

package software.amazon.awssdk.crtcore;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * The base class for CRT connection health configuration
 */
@SdkPublicApi
public abstract class CrtConnectionHealthConfiguration {
    private final long minimumThroughputInBps;
    private final Duration minimumThroughputTimeout;

    protected CrtConnectionHealthConfiguration(DefaultBuilder<?> builder) {
        this.minimumThroughputInBps = Validate.paramNotNull(builder.minimumThroughputInBps,
                                                            "minimumThroughputInBps");
        this.minimumThroughputTimeout = Validate.isPositive(builder.minimumThroughputTimeout,
                                                            "minimumThroughputTimeout");
    }

    /**
     * @return the minimum amount of throughput, in bytes per second, for a connection to be considered healthy.
     */
    public final long minimumThroughputInBps() {
        return minimumThroughputInBps;
    }

    /**
     * @return How long a connection is allowed to be unhealthy before getting shut down.
     */
    public final Duration minimumThroughputTimeout() {
        return minimumThroughputTimeout;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CrtConnectionHealthConfiguration that = (CrtConnectionHealthConfiguration) o;

        if (minimumThroughputInBps != that.minimumThroughputInBps) {
            return false;
        }
        return minimumThroughputTimeout.equals(that.minimumThroughputTimeout);
    }

    @Override
    public int hashCode() {
        int result = (int) (minimumThroughputInBps ^ (minimumThroughputInBps >>> 32));
        result = 31 * result + minimumThroughputTimeout.hashCode();
        return result;
    }

    /**
     * A builder for {@link CrtConnectionHealthConfiguration}.
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

        CrtConnectionHealthConfiguration build();
    }

    protected abstract static class DefaultBuilder<B extends Builder> implements Builder {
        private Long minimumThroughputInBps;
        private Duration minimumThroughputTimeout;

        protected DefaultBuilder() {
        }

        protected DefaultBuilder(CrtConnectionHealthConfiguration configuration) {
            this.minimumThroughputInBps = configuration.minimumThroughputInBps;
            this.minimumThroughputTimeout = configuration.minimumThroughputTimeout;
        }

        @Override
        public B minimumThroughputInBps(Long minimumThroughputInBps) {
            this.minimumThroughputInBps = minimumThroughputInBps;
            return (B) this;
        }

        @Override
        public B minimumThroughputTimeout(Duration minimumThroughputTimeout) {
            this.minimumThroughputTimeout = minimumThroughputTimeout;
            return (B) this;
        }

    }
}