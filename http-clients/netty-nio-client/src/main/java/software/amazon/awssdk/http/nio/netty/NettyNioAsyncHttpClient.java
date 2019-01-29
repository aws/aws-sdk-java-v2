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

package software.amazon.awssdk.http.nio.netty;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.READ_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.WRITE_TIMEOUT;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.internal.ChannelPipelineInitializer;
import software.amazon.awssdk.http.nio.netty.internal.HandlerRemovingChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.http.nio.netty.internal.NettyRequestExecutor;
import software.amazon.awssdk.http.nio.netty.internal.NonManagedEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.ReleaseOnceChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelOptions;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPoolMap;
import software.amazon.awssdk.http.nio.netty.internal.SharedSdkEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.http2.HttpOrHttp2ChannelPool;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link SdkAsyncHttpClient} that uses a Netty non-blocking HTTP client to communicate with the service.
 *
 * <p>This can be created via {@link #builder()}</p>
 */
@SdkPublicApi
public final class NettyNioAsyncHttpClient implements SdkAsyncHttpClient {
    private static final Logger log = LoggerFactory.getLogger(NettyNioAsyncHttpClient.class);

    private final SdkEventLoopGroup sdkEventLoopGroup;
    private final SdkChannelPoolMap<URI, ChannelPool> pools;
    private final SdkChannelOptions sdkChannelOptions;
    private final NettyConfiguration configuration;
    private final long maxStreams;
    private Protocol protocol;

    NettyNioAsyncHttpClient(DefaultBuilder builder, AttributeMap serviceDefaultsMap) {
        this.configuration = new NettyConfiguration(serviceDefaultsMap);
        this.protocol = serviceDefaultsMap.get(SdkHttpConfigurationOption.PROTOCOL);
        this.maxStreams = builder.maxHttp2Streams == null ? Integer.MAX_VALUE : builder.maxHttp2Streams;
        this.sdkEventLoopGroup = eventLoopGroup(builder);
        this.pools = createChannelPoolMap();
        this.sdkChannelOptions = channelOptions(builder);
    }

    @SdkTestInternalApi
    NettyNioAsyncHttpClient(SdkEventLoopGroup sdkEventLoopGroup,
                            SdkChannelPoolMap<URI, ChannelPool> pools,
                            SdkChannelOptions sdkChannelOptions,
                            NettyConfiguration configuration,
                            long maxStreams) {
        this.sdkEventLoopGroup = sdkEventLoopGroup;
        this.pools = pools;
        this.sdkChannelOptions = sdkChannelOptions;
        this.configuration = configuration;
        this.maxStreams = maxStreams;
    }

    private SdkChannelOptions channelOptions(DefaultBuilder builder) {
        return builder.sdkChannelOptions;
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        RequestContext ctx = createRequestContext(request);
        return new NettyRequestExecutor(ctx).execute();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private RequestContext createRequestContext(AsyncExecuteRequest request) {
        ChannelPool pool = pools.get(poolKey(request.request()));
        return new RequestContext(pool, request, configuration);
    }

    private SdkEventLoopGroup eventLoopGroup(DefaultBuilder builder) {
        Validate.isTrue(builder.eventLoopGroup == null || builder.eventLoopGroupBuilder == null,
                "The eventLoopGroup and the eventLoopGroupFactory can't both be configured.");
        return Either.fromNullable(builder.eventLoopGroup, builder.eventLoopGroupBuilder)
                .map(e -> e.map(this::nonManagedEventLoopGroup, SdkEventLoopGroup.Builder::build))
                .orElseGet(SharedSdkEventLoopGroup::get);
    }

    private static URI poolKey(SdkHttpRequest sdkRequest) {
        return invokeSafely(() -> new URI(sdkRequest.protocol(), null, sdkRequest.host(),
                                          sdkRequest.port(), null, null, null));
    }

    private SslContext sslContext(String protocol) {
        if (!protocol.equalsIgnoreCase("https")) {
            return null;
        }
        try {
            return SslContextBuilder.forClient()
                                    .sslProvider(SslContext.defaultClientProvider())
                                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .trustManager(getTrustManager())
                                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private TrustManagerFactory getTrustManager() {
        return configuration.trustAllCertificates() ? InsecureTrustManagerFactory.INSTANCE : null;
    }

    private SdkChannelPoolMap<URI, ChannelPool> createChannelPoolMap() {
        return new SdkChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                SslContext sslContext = sslContext(key.getScheme());
                Bootstrap bootstrap =
                    new Bootstrap()
                        .group(sdkEventLoopGroup.eventLoopGroup())
                        .channelFactory(sdkEventLoopGroup.channelFactory())
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectTimeoutMillis())
                        // TODO run some performance tests with and without this.
                        .remoteAddress(key.getHost(), key.getPort());
                sdkChannelOptions.channelOptions().forEach(bootstrap::option);
                AtomicReference<ChannelPool> channelPoolRef = new AtomicReference<>();
                ChannelPipelineInitializer handler =
                    new ChannelPipelineInitializer(protocol, sslContext, maxStreams, channelPoolRef);
                channelPoolRef.set(new ReleaseOnceChannelPool(
                    new HandlerRemovingChannelPool(
                        new HttpOrHttp2ChannelPool(bootstrap, handler,
                                                   configuration.maxConnections(), configuration))));
                return channelPoolRef.get();
            }
        };
    }

    private SdkEventLoopGroup nonManagedEventLoopGroup(SdkEventLoopGroup eventLoopGroup) {
        return SdkEventLoopGroup.create(new NonManagedEventLoopGroup(eventLoopGroup.eventLoopGroup()),
                                        eventLoopGroup.channelFactory());
    }

    @Override
    public void close() {
        runAndLogError(log, "Unable to close channel pools", pools::close);
        runAndLogError(log, "Unable to shutdown event loop", sdkEventLoopGroup.eventLoopGroup()::shutdownGracefully);
    }

    /**
     * Builder that allows configuration of the Netty NIO HTTP implementation. Use {@link #builder()} to configure and construct
     * a Netty HTTP client.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder<NettyNioAsyncHttpClient.Builder> {

        /**
         * Maximum number of allowed concurrent requests. For HTTP/1.1 this is the same as max connections. For HTTP/2
         * the number of connections that will be used depends on the max streams allowed per connection.
         *
         * <p>
         * If the maximum number of concurrent requests is exceeded they may be queued in the HTTP client (see
         * {@link #maxPendingConnectionAcquires(Integer)}</p>) and can cause increased latencies. If the client is overloaded
         * enough such that the pending connection queue fills up, subsequent requests may be rejected or time out
         * (see {@link #connectionAcquisitionTimeout(Duration)}).
         * </p>
         *
         * @param maxConcurrency New value for max concurrency.
         * @return This builder for method chaining.
         */
        Builder maxConcurrency(Integer maxConcurrency);

        /**
         * The maximum number of pending acquires allowed. Once this exceeds, acquire tries will be failed.
         *
         * @param maxPendingAcquires Max number of pending acquires
         * @return This builder for method chaining.
         */
        Builder maxPendingConnectionAcquires(Integer maxPendingAcquires);

        /**
         * The amount of time to wait for a read on a socket before an exception is thrown.
         *
         * @param readTimeout timeout duration
         * @return this builder for method chaining.
         */
        Builder readTimeout(Duration readTimeout);

        /**
         * The amount of time to wait for a write on a socket before an exception is thrown.
         *
         * @param writeTimeout timeout duration
         * @return this builder for method chaining.
         */
        Builder writeTimeout(Duration writeTimeout);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out.
         *
         * @param timeout the timeout duration
         * @return this builder for method chaining.
         */
        Builder connectionTimeout(Duration timeout);

        /**
         * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
         * @param connectionAcquisitionTimeout the timeout duration
         * @return this builder for method chaining.
         */
        Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout);

        /**
         * Sets the {@link SdkEventLoopGroup} to use for the Netty HTTP client. This event loop group may be shared
         * across multiple HTTP clients for better resource and thread utilization. The preferred way to create
         * an {@link EventLoopGroup} is by using the {@link SdkEventLoopGroup#builder()})} method which will choose the
         * optimal implementation per the platform.
         *
         * <p>The {@link EventLoopGroup} <b>MUST</b> be closed by the caller when it is ready to
         * be disposed. The SDK will not close the {@link EventLoopGroup} when the HTTP client is closed. See
         * {@link EventLoopGroup#shutdownGracefully()} to properly close the event loop group.</p>
         *
         * <p>This configuration method is only recommended when you wish to share an {@link EventLoopGroup}
         * with multiple clients. If you do not need to share the group it is recommended to use
         * {@link #eventLoopGroupBuilder(SdkEventLoopGroup.Builder)} as the SDK will handle its cleanup when
         * the HTTP client is closed.</p>
         *
         * @param eventLoopGroup Netty {@link SdkEventLoopGroup} to use.
         * @return This builder for method chaining.
         * @see SdkEventLoopGroup
         */
        Builder eventLoopGroup(SdkEventLoopGroup eventLoopGroup);

        /**
         * Sets the {@link SdkEventLoopGroup.Builder} which will be used to create the {@link SdkEventLoopGroup} for the Netty
         * HTTP client. This allows for custom configuration of the Netty {@link EventLoopGroup}.
         *
         * <p>The {@link EventLoopGroup} created by the builder is managed by the SDK and will be shutdown
         * when the HTTP client is closed.</p>
         *
         * <p>This is the preferred configuration method when you just want to customize the {@link EventLoopGroup}
         * but not share it across multiple HTTP clients. If you do wish to share an {@link EventLoopGroup}, see
         * {@link #eventLoopGroup(SdkEventLoopGroup)}</p>
         *
         * @param eventLoopGroupBuilder {@link SdkEventLoopGroup.Builder} to use.
         * @return This builder for method chaining.
         * @see SdkEventLoopGroup.Builder
         */
        Builder eventLoopGroupBuilder(SdkEventLoopGroup.Builder eventLoopGroupBuilder);

        /**
         * Sets the HTTP protocol to use (i.e. HTTP/1.1 or HTTP/2). Not all services support HTTP/2.
         *
         * @param protocol Protocol to use.
         * @return This builder for method chaining.
         */
        Builder protocol(Protocol protocol);

        /**
         * Add new socket channel option which will be used to create Netty Http client. This allows custom configuration
         * for Netty.
         * @param channelOption {@link ChannelOption} to set
         * @param value See {@link ChannelOption} to find the type of value for each option
         * @return This builder for method chaining.
         * @see SdkEventLoopGroup.Builder
         */
        Builder putChannelOption(ChannelOption channelOption, Object value);

        /**
         * Sets the max number of concurrent streams for an HTTP/2 connection. This setting is only respected when the HTTP/2
         * protocol is used.
         *
         * <p>Note that this cannot exceed the value of the MAX_CONCURRENT_STREAMS setting returned by the service. If it
         * does the service setting is used instead.</p>
         *
         * @param maxHttp2Streams Max concurrent HTTP/2 streams per connection.
         * @return This builder for method chaining.
         */
        Builder maxHttp2Streams(Integer maxHttp2Streams);
    }

    /**
     * Factory that allows more advanced configuration of the Netty NIO HTTP implementation. Use {@link #builder()} to
     * configure and construct an immutable instance of the factory.
     */
    private static final class DefaultBuilder implements Builder {

        private final AttributeMap.Builder standardOptions = AttributeMap.builder();

        private SdkChannelOptions sdkChannelOptions = new SdkChannelOptions();

        private SdkEventLoopGroup eventLoopGroup;
        private SdkEventLoopGroup.Builder eventLoopGroupBuilder;
        private Integer maxHttp2Streams;

        private DefaultBuilder() {
        }

        /**
         * Max allowed connections per endpoint allowed in the connection pool.
         *
         * @param maxConcurrency New value for max connections per endpoint.
         * @return This builder for method chaining.
         */
        @Override
        public Builder maxConcurrency(Integer maxConcurrency) {
            standardOptions.put(MAX_CONNECTIONS, maxConcurrency);
            return this;
        }

        public void setMaxConcurrency(Integer maxConnectionsPerEndpoint) {
            maxConcurrency(maxConnectionsPerEndpoint);
        }

        /**
         * The maximum number of pending acquires allowed. Once this exceeds, acquire tries will be failed.
         *
         * @param maxPendingAcquires Max number of pending acquires
         * @return This builder for method chaining.
         */
        @Override
        public Builder maxPendingConnectionAcquires(Integer maxPendingAcquires) {
            standardOptions.put(MAX_PENDING_CONNECTION_ACQUIRES, maxPendingAcquires);
            return this;
        }

        public void setMaxPendingConnectionAcquires(Integer maxPendingAcquires) {
            maxPendingConnectionAcquires(maxPendingAcquires);
        }

        /**
         * The amount of time to wait for a read on a socket before an exception is thrown.
         *
         * @param readTimeout timeout duration
         * @return this builder for method chaining.
         */
        @Override
        public Builder readTimeout(Duration readTimeout) {
            Validate.isPositive(readTimeout, "readTimeout");
            standardOptions.put(READ_TIMEOUT, readTimeout);
            return this;
        }

        public void setReadTimeout(Duration readTimeout) {
            readTimeout(readTimeout);
        }

        /**
         * The amount of time to wait for a write on a socket before an exception is thrown.
         *
         * @param writeTimeout timeout duration
         * @return this builder for method chaining.
         */
        @Override
        public Builder writeTimeout(Duration writeTimeout) {
            Validate.isPositive(writeTimeout, "writeTimeout");
            standardOptions.put(WRITE_TIMEOUT, writeTimeout);
            return this;
        }

        public void setWriteTimeout(Duration writeTimeout) {
            writeTimeout(writeTimeout);
        }

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out.
         *
         * @param timeout the timeout duration
         * @return this builder for method chaining.
         */
        @Override
        public Builder connectionTimeout(Duration timeout) {
            Validate.isPositive(timeout, "connectionTimeout");
            standardOptions.put(CONNECTION_TIMEOUT, timeout);
            return this;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        /**
         * The amount of time to wait when acquiring a connection from the pool before giving up and timing out.
         * @param connectionAcquisitionTimeout the timeout duration
         * @return this builder for method chaining.
         */
        @Override
        public Builder connectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            Validate.isPositive(connectionAcquisitionTimeout, "connectionAcquisitionTimeout");
            standardOptions.put(CONNECTION_ACQUIRE_TIMEOUT, connectionAcquisitionTimeout);
            return this;
        }

        public void setConnectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            connectionAcquisitionTimeout(connectionAcquisitionTimeout);
        }

        @Override
        public Builder eventLoopGroup(SdkEventLoopGroup eventLoopGroup) {
            this.eventLoopGroup = eventLoopGroup;
            return this;
        }

        public void setEventLoopGroup(SdkEventLoopGroup eventLoopGroup) {
            eventLoopGroup(eventLoopGroup);
        }

        @Override
        public Builder eventLoopGroupBuilder(SdkEventLoopGroup.Builder eventLoopGroupBuilder) {
            this.eventLoopGroupBuilder = eventLoopGroupBuilder;
            return this;
        }

        public void setEventLoopGroupBuilder(SdkEventLoopGroup.Builder eventLoopGroupBuilder) {
            eventLoopGroupBuilder(eventLoopGroupBuilder);
        }

        @Override
        public Builder protocol(Protocol protocol) {
            standardOptions.put(SdkHttpConfigurationOption.PROTOCOL, protocol);
            return this;
        }

        public void setProtocol(Protocol protocol) {
            protocol(protocol);
        }

        @Override
        public Builder putChannelOption(ChannelOption channelOption, Object value) {
            this.sdkChannelOptions.putOption(channelOption, value);
            return this;
        }

        @Override
        public Builder maxHttp2Streams(Integer maxHttp2Streams) {
            this.maxHttp2Streams = maxHttp2Streams;
            return this;
        }

        public void setMaxHttp2Streams(Integer maxHttp2Streams) {
            maxHttp2Streams(maxHttp2Streams);
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new NettyNioAsyncHttpClient(this, standardOptions.build()
                                                                    .merge(serviceDefaults)
                                                                    .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));

        }
    }
}
