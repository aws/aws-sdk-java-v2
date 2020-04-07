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

package software.amazon.awssdk.http.nio.netty;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration specific to HTTP/2 connections.
 */
@SdkPublicApi
public final class Http2Configuration implements ToCopyableBuilder<Http2Configuration.Builder, Http2Configuration> {
    private final Long maxStreams;
    private final Integer initialWindowSize;
    private final Duration healthCheckPingPeriod;

    private Http2Configuration(DefaultBuilder builder) {
        this.maxStreams = builder.maxStreams;
        this.initialWindowSize = builder.initialWindowSize;
        this.healthCheckPingPeriod = builder.healthCheckPingPeriod;
    }

    /**
     * @return The maximum number of streams to be created per HTTP/2 connection.
     */
    public Long maxStreams() {
        return maxStreams;
    }

    /**
     * @return The initial window size for an HTTP/2 stream.
     */
    public Integer initialWindowSize() {
        return initialWindowSize;
    }

    /**
     * @return The health check period for an HTTP/2 connection.
     */
    public Duration healthCheckPingPeriod() {
        return healthCheckPingPeriod;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Http2Configuration that = (Http2Configuration) o;

        if (maxStreams != null ? !maxStreams.equals(that.maxStreams) : that.maxStreams != null) {
            return false;
        }

        return initialWindowSize != null ? initialWindowSize.equals(that.initialWindowSize) : that.initialWindowSize == null;

    }

    @Override
    public int hashCode() {
        int result = maxStreams != null ? maxStreams.hashCode() : 0;
        result = 31 * result + (initialWindowSize != null ? initialWindowSize.hashCode() : 0);
        return result;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public interface Builder extends CopyableBuilder<Builder, Http2Configuration> {

        /**
         * Sets the max number of concurrent streams per connection.
         *
         * <p>Note that this cannot exceed the value of the MAX_CONCURRENT_STREAMS setting returned by the service. If it
         * does the service setting is used instead.</p>
         *
         * @param maxStreams Max concurrent HTTP/2 streams per connection.
         * @return This builder for method chaining.
         */
        Builder maxStreams(Long maxStreams);

        /**
         * Sets initial window size of a stream. This setting is only respected when the HTTP/2 protocol is used.
         *
         * See <a href="https://tools.ietf.org/html/rfc7540#section-6.5.2">https://tools.ietf.org/html/rfc7540#section-6.5.2</a>
         * for more information about this parameter.
         *
         * @param initialWindowSize The initial window size of a stream.
         * @return This builder for method chaining.
         */
        Builder initialWindowSize(Integer initialWindowSize);

        /**
         * Sets the period that the Netty client will send {@code PING} frames to the remote endpoint to check the
         * health of the connection. The default value is {@link
         * software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration#HTTP2_CONNECTION_PING_TIMEOUT_SECONDS}. To
         * disable this feature, set a duration of 0.
         *
         * @param healthCheckPingPeriod The ping period.
         * @return This builder for method chaining.
         */
        Builder healthCheckPingPeriod(Duration healthCheckPingPeriod);
    }

    private static final class DefaultBuilder implements Builder {
        private Long maxStreams;
        private Integer initialWindowSize;
        private Duration healthCheckPingPeriod;

        private DefaultBuilder() {
        }

        private DefaultBuilder(Http2Configuration http2Configuration) {
            this.maxStreams = http2Configuration.maxStreams;
            this.initialWindowSize = http2Configuration.initialWindowSize;
            this.healthCheckPingPeriod = http2Configuration.healthCheckPingPeriod;
        }

        @Override
        public Builder maxStreams(Long maxStreams) {
            this.maxStreams = Validate.isPositiveOrNull(maxStreams, "maxStreams");
            return this;
        }

        public void setMaxStreams(Long maxStreams) {
            maxStreams(maxStreams);
        }

        @Override
        public Builder initialWindowSize(Integer initialWindowSize) {
            this.initialWindowSize = Validate.isPositiveOrNull(initialWindowSize, "initialWindowSize");
            return this;
        }

        public void setInitialWindowSize(Integer initialWindowSize) {
            initialWindowSize(initialWindowSize);
        }

        @Override
        public Builder healthCheckPingPeriod(Duration healthCheckPingPeriod) {
            this.healthCheckPingPeriod = healthCheckPingPeriod;
            return this;
        }

        public void setHealthCheckPingPeriod(Duration healthCheckPingPeriod) {
            healthCheckPingPeriod(healthCheckPingPeriod);
        }

        @Override
        public Http2Configuration build() {
            return new Http2Configuration(this);
        }
    }
}
