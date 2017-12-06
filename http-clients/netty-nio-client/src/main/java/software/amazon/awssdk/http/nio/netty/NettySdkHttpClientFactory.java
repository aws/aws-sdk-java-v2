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

package software.amazon.awssdk.http.nio.netty;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;

import io.netty.channel.EventLoopGroup;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClientFactory;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Factory that allows more advanced configuration of the Netty NIO HTTP implementation. Use {@link #builder()} to
 * configure and construct an immutable instance of the factory.
 */
@Immutable
public final class NettySdkHttpClientFactory
        implements SdkAsyncHttpClientFactory, ToCopyableBuilder<NettySdkHttpClientFactory.Builder, NettySdkHttpClientFactory> {

    private final AttributeMap standardOptions;
    private final Optional<Boolean> trustAllCertificates;
    private final EventLoopGroupConfiguration eventLoopGroupConfiguration;
    private final Duration readTimeout;
    private final Duration writeTimeout;
    private final Duration connectionAquisitionTimeout;

    private NettySdkHttpClientFactory(DefaultBuilder builder) {
        this.standardOptions = builder.standardOptions.build();
        this.trustAllCertificates = Optional.ofNullable(builder.trustAllCertificates);
        this.eventLoopGroupConfiguration = builder.eventLoopGroupConfiguration;
        this.readTimeout = validateIsWholeSecond(builder.readTimeout, "readTimeout");
        this.writeTimeout = validateIsWholeSecond(builder.writeTimeout, "writeTimeout");
        this.connectionAquisitionTimeout = builder.connectionAcquisitionTimeout;
    }

    /**
     * @return Optional of the maxConnectionsPerEndpoint setting.
     * @see Builder#maxConnectionsPerEndpoint(Integer)
     */
    public Optional<Integer> maxConnectionsPerEndpoint() {
        return Optional.ofNullable(standardOptions.get(MAX_CONNECTIONS));
    }

    /**
     * @return Optional of the writeTimeout setting.
     * @see Builder#writeTimeout(Duration)
     */
    public Optional<Duration> writeTimeout() {
        return Optional.ofNullable(writeTimeout);
    }

    /**
     * @return Optional of the readTimeout setting.
     * @see Builder#readTimeout(Duration)
     */
    public Optional<Duration> readTimeout() {
        return Optional.ofNullable(readTimeout);
    }

    /**
     * @return Optional of the connectionTimeout setting.
     * @see Builder#connectionTimeout(Duration)
     */
    public Optional<Duration> connectionTimeout() {
        return Optional.ofNullable(standardOptions.get(CONNECTION_TIMEOUT));
    }

    /**
     * @return Optional of the connectionAquisitionTimeout setting.
     * @see Builder#connectionTimeout(Duration)
     */
    public Optional<Duration> connectionAquisitionTimeout() {
        return Optional.ofNullable(connectionAquisitionTimeout);
    }

    /**
     * @return Optional of the trustAllCertificates setting.
     * @see Builder#trustAllCertificates(Boolean)
     */
    public Optional<Boolean> trustAllCertificates() {
        return trustAllCertificates;
    }

    /**
     * @return The current {@link EventLoopGroupConfiguration} which is a container for either an {@link EventLoopGroup} or an
     * {@link DefaultEventLoopGroupFactory}.
     * @see Builder#eventLoopGroupConfiguration(EventLoopGroupConfiguration)
     */
    public EventLoopGroupConfiguration eventLoopGroupConfiguration() {
        return eventLoopGroupConfiguration;
    }

    /**
     * Create an HTTP client instance with global defaults applied. This client instance can be shared
     * across multiple SDK clients for better resource utilization. Note that if sharing is not needed then it is
     * recommended to pass this factory into the SDK client builders so that service defaults may be applied.
     *
     * @return Created client.
     */
    public SdkAsyncHttpClient createHttpClient() {
        return createHttpClientWithDefaults(AttributeMap.empty());
    }

    @Override
    public SdkAsyncHttpClient createHttpClientWithDefaults(AttributeMap serviceDefaults) {
        return new NettyNioAsyncHttpClient(this, standardOptions.merge(serviceDefaults)
                                                                .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(standardOptions.toBuilder())
                .trustAllCertificates(trustAllCertificates.orElse(null));
    }

    /**
     * @return A {@link Builder} for creating an immutable {@link NettySdkHttpClientFactory}.
     */
    public static Builder builder() {
        return new DefaultBuilder(AttributeMap.builder());
    }

    private Duration validateIsWholeSecond(Duration duration, String param) {
        if (duration != null && duration.toMillis() % 1000 != 0) {
            throw new IllegalArgumentException(param + "must be a whole second, got: " + duration);
        }
        return duration;
    }

    /**
     * Builder interface for {@link NettySdkHttpClientFactory}.
     *
     * @see NettySdkHttpClientFactory#builder()
     */
    public interface Builder extends CopyableBuilder<Builder, NettySdkHttpClientFactory> {

        /**
         * Max allowed connections per endpoint allowed in the connection pool.
         *
         * @param maxConnectionsPerEndpoint New value for max connections per endpoint.
         * @return This builder for method chaining.
         */
        Builder maxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint);

        /**
         * The amount of time to wait for a read on a socket before an exception is thrown.
         * <br/>
         * <strong>note: minimum supported granularity is seconds, if {@link Duration} cannot be converted
         * to a whole second an exception will be thrown</strong>
         *
         * @param timeout timeout duration
         * @return this builder for method chaining.
         */
        Builder readTimeout(Duration timeout);

        /**
         * The amount of time to wait for a write on a socket before an exception is thrown.
         * <br/>
         * <strong>note: minimum supported granularity is seconds, if {@link Duration} cannot be converted
         * to a whole second an exception will be thrown</strong>
         *
         * @param timeout timeout duration
         * @return this builder for method chaining.
         */
        Builder writeTimeout(Duration timeout);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out.
         *
         * Defaults to {@link #connectionTimeout()} if not set.
         * @param timeout the timeout duration
         * @return this builder for method chaining.
         */
        Builder connectionTimeout(Duration timeout);

        /**
         * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
         * @param timeout the timeout duration
         * @return this builder for method chaining.
         */
        @ReviewBeforeRelease("Does it make sense to separate 'connection acquisition' timeout from 'socket connection' timeout?")
        Builder connectionAcquisitionTimeout(Duration timeout);

        /**
         * Forces the HTTP client to trust all certificates, even invalid or self signed certificates. This should only ever
         * be used for testing purposes.
         *
         * @param trustAllCertificates Whether to trust all certificates. The default is false and only valid certificates
         *                             whose trust can be verified via the trust store will be trusted.
         * @return This builder for method chaining.
         */
        Builder trustAllCertificates(Boolean trustAllCertificates);

        /**
         * Configuration for the Netty {@link EventLoopGroup} which multiplexes IO events.
         *
         * <p>
         * If none is provided then a default {@link EventLoopGroup} will be used. This default {@link EventLoopGroup} will be
         * shared across all HTTP client instances and will be automatically shutdown by the SDK when no references to it remain.
         * </p>
         *
         * @param eventLoopGroupConfiguration New configuration object.
         * @return This builder for method chaining.
         */
        Builder eventLoopGroupConfiguration(EventLoopGroupConfiguration eventLoopGroupConfiguration);

        /**
         * Configuration for the Netty {@link EventLoopGroup} which multiplexes IO events.
         *
         * @param eventLoopGroupConfiguration consumer that allows a {@link EventLoopGroupConfiguration.Builder} to be mutated.
         * @return This builder for method chaining.
         * @see #eventLoopGroupConfiguration(EventLoopGroupConfiguration)
         */
        default Builder eventLoopGroupConfiguration(Consumer<EventLoopGroupConfiguration.Builder> eventLoopGroupConfiguration) {
            return eventLoopGroupConfiguration(EventLoopGroupConfiguration.builder().apply(eventLoopGroupConfiguration).build());
        }
    }

    private static final class DefaultBuilder implements Builder {

        private final AttributeMap.Builder standardOptions;
        private Boolean trustAllCertificates;
        private EventLoopGroupConfiguration eventLoopGroupConfiguration = EventLoopGroupConfiguration.builder().build();
        private Duration readTimeout;
        private Duration writeTimeout;
        private Duration connectionAcquisitionTimeout;

        private DefaultBuilder(AttributeMap.Builder standardOptions) {
            this.standardOptions = standardOptions;
        }

        @Override
        public Builder maxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint) {
            standardOptions.put(MAX_CONNECTIONS, maxConnectionsPerEndpoint);
            return this;
        }

        @Override
        public Builder readTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }

        @Override
        public Builder writeTimeout(Duration timeout) {
            this.writeTimeout = timeout;
            return this;
        }

        public void setMaxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint) {
            maxConnectionsPerEndpoint(maxConnectionsPerEndpoint);
        }

        @Override
        public Builder connectionTimeout(Duration timeout) {
            standardOptions.put(CONNECTION_TIMEOUT, timeout);
            return this;
        }

        @Override
        public Builder connectionAcquisitionTimeout(Duration timeout) {
            this.connectionAcquisitionTimeout = timeout;
            return this;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        @Override
        public Builder trustAllCertificates(Boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
            return this;
        }

        public void setTrustAllCertificates(Boolean trustAllCertificates) {
            trustAllCertificates(trustAllCertificates);
        }

        @Override
        public DefaultBuilder eventLoopGroupConfiguration(EventLoopGroupConfiguration eventLoopGroupConfiguration) {
            this.eventLoopGroupConfiguration = eventLoopGroupConfiguration;
            return this;
        }

        public void setEventLoopGroupConfiguration(EventLoopGroupConfiguration eventLoopGroupConfiguration) {
            eventLoopGroupConfiguration(eventLoopGroupConfiguration);
        }

        @Override
        public NettySdkHttpClientFactory build() {
            return new NettySdkHttpClientFactory(this);
        }
    }
}
