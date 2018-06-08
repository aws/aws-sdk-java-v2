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

import static io.netty.handler.ssl.SslContext.defaultClientProvider;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.READ_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.WRITE_TIMEOUT;
import static software.amazon.awssdk.http.nio.netty.internal.utils.SocketChannelResolver.resolveSocketChannelClass;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.ChannelPipelineInitializer;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.http.nio.netty.internal.NonManagedEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.RequestAdapter;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.RunnableRequest;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPoolMap;
import software.amazon.awssdk.http.nio.netty.internal.SharedEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link SdkAsyncHttpClient} that uses a Netty non-blocking HTTP client to communicate with the service.
 *
 * <p>This can be created via {@link #builder()}</p>
 */
@SdkPublicApi
public final class NettyNioAsyncHttpClient implements SdkAsyncHttpClient {
    private static final Logger log = Logger.loggerFor(NettyNioAsyncHttpClient.class);
    private final EventLoopGroup group;
    private final RequestAdapter requestAdapter = new RequestAdapter();
    private final ChannelPoolMap<URI, ChannelPool> pools;
    private final NettyConfiguration configuration;

    private NettyNioAsyncHttpClient(DefaultBuilder factory, AttributeMap configuration) {
        this.configuration = new NettyConfiguration(configuration);
        this.group = eventLoopGroup(factory);
        this.pools = createChannelPoolMap();
    }

    private EventLoopGroup eventLoopGroup(DefaultBuilder factory) {
        Validate.isTrue(factory.eventLoopGroup == null || factory.eventLoopGroupFactory == null,
                        "The eventLoopGroup and the eventLoopGroupFactory can't both be configured.");
        return Either.fromNullable(factory.eventLoopGroup, factory.eventLoopGroupFactory)
                     .map(e -> e.map(NonManagedEventLoopGroup::new,
                                     EventLoopGroupFactory::create))
                     .orElseGet(SharedEventLoopGroup::get);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private ChannelPoolMap<URI, ChannelPool> createChannelPoolMap() {
        return new SdkChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                Bootstrap bootstrap =
                        new Bootstrap()
                                .group(group)
                                .channel(resolveSocketChannelClass(group))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectTimeoutMillis())
                                .option(ChannelOption.TCP_NODELAY, true)
                                .remoteAddress(key.getHost(), key.getPort());
                SslContext sslContext = sslContext(key.getScheme());
                return new FixedChannelPool(bootstrap,
                                            // TODO expose better options for this
                                            new ChannelPipelineInitializer(sslContext),
                                            ChannelHealthChecker.ACTIVE,
                                            FixedChannelPool.AcquireTimeoutAction.FAIL,
                                            configuration.connectionAcquireTimeoutMillis(),
                                            configuration.maxConnections(),
                                            configuration.maxPendingConnectionAcquires());
            }
        };
    }

    @Override
    public AbortableRunnable prepareRequest(SdkHttpRequest sdkRequest,
                                            SdkRequestContext requestContext,
                                            SdkHttpRequestProvider requestProvider,
                                            SdkHttpResponseHandler handler) {
        final RequestContext context = new RequestContext(pools.get(poolKey(sdkRequest)),
                                                          sdkRequest, requestProvider,
                                                          requestAdapter.adapt(sdkRequest),
                                                          handler, configuration);
        return new RunnableRequest(context);
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return Optional.ofNullable(configuration.attribute(key));
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }

    private static URI poolKey(SdkHttpRequest sdkRequest) {
        return invokeSafely(() -> new URI(sdkRequest.protocol(), null, sdkRequest.host(),
                                          sdkRequest.port(), null, null, null));
    }

    private SslContext sslContext(String scheme) {
        if (scheme.equalsIgnoreCase("https")) {
            SslContextBuilder builder = SslContextBuilder.forClient().sslProvider(defaultClientProvider());
            if (configuration.trustAllCertificates()) {
                log.warn(() -> "SSL Certificate verification is disabled. This is not a safe setting and should only be "
                               + "used for testing.");
                builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }
            return invokeSafely(builder::build);
        }
        return null;
    }

    /**
     * Builder that allows configuration of the Netty NIO HTTP implementation. Use {@link #builder()} to configure and construct
     * a Netty HTTP client.
     */
    public interface Builder extends SdkAsyncHttpClient.Builder {
        default Builder apply(Consumer<Builder> mutator) {
            mutator.accept(this);
            return this;
        }

        /**
         * Max allowed connections per endpoint allowed in the connection pool.
         *
         * @param maxConnectionsPerEndpoint New value for max connections per endpoint.
         * @return This builder for method chaining.
         */
        Builder maxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint);

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
         * Sets the {@link EventLoopGroup} to use for the Netty HTTP client. This event loop group may be shared
         * across multiple HTTP clients for better resource and thread utilization. The preferred way to create
         * an {@link EventLoopGroup} is by using the {@link EventLoopGroupFactory#create()} method which will choose the
         * optimal implementation per the platform.
         *
         * <p>The {@link EventLoopGroup} <b>MUST</b> be closed by the caller when it is ready to
         * be disposed. The SDK will not close the {@link EventLoopGroup} when the HTTP client is closed. See
         * {@link EventLoopGroup#shutdownGracefully()} to properly close the event loop group.</p>
         *
         * <p>This configuration method is only recommended when you wish to share an {@link EventLoopGroup}
         * with multiple clients. If you do not need to share the group it is recommended to use
         * {@link #eventLoopGroupFactory(EventLoopGroupFactory)} as the SDK will handle its cleanup when
         * the HTTP client is closed.</p>
         *
         * @param eventLoopGroup Netty {@link EventLoopGroup} to use.
         * @return This builder for method chaining.
         * @see DefaultEventLoopGroupFactory
         */
        Builder eventLoopGroup(EventLoopGroup eventLoopGroup);

        /**
         * Sets the {@link EventLoopGroupFactory} which will be used to create the {@link EventLoopGroup} for the Netty
         * HTTP client. This allows for custom configuration of the Netty {@link EventLoopGroup}.
         *
         * <p>The {@link EventLoopGroup} created by the factory is managed by the SDK and will be shutdown
         * when the HTTP client is closed.</p>
         *
         * <p>This is the preferred configuration method when you just want to customize the {@link EventLoopGroup}
         * but not share it across multiple HTTP clients. If you do wish to share an {@link EventLoopGroup}, see
         * {@link #eventLoopGroup(EventLoopGroup)}</p>
         *
         * @param eventLoopGroupFactory {@link EventLoopGroupFactory} to use.
         * @return This builder for method chaining.
         * @see DefaultEventLoopGroupFactory
         */
        Builder eventLoopGroupFactory(EventLoopGroupFactory eventLoopGroupFactory);
    }

    /**
     * Factory that allows more advanced configuration of the Netty NIO HTTP implementation. Use {@link #builder()} to
     * configure and construct an immutable instance of the factory.
     */
    private static final class DefaultBuilder implements Builder {

        private final AttributeMap.Builder standardOptions = AttributeMap.builder();
        private EventLoopGroup eventLoopGroup;
        private EventLoopGroupFactory eventLoopGroupFactory;

        private DefaultBuilder() {
        }

        /**
         * Max allowed connections per endpoint allowed in the connection pool.
         *
         * @param maxConnectionsPerEndpoint New value for max connections per endpoint.
         * @return This builder for method chaining.
         */
        @Override
        public Builder maxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint) {
            standardOptions.put(MAX_CONNECTIONS, maxConnectionsPerEndpoint);
            return this;
        }

        public void setMaxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint) {
            maxConnectionsPerEndpoint(maxConnectionsPerEndpoint);
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
            standardOptions.put(CONNECTION_ACQUIRE_TIMEOUT, connectionAcquisitionTimeout);
            return this;
        }

        public void setConnectionAcquisitionTimeout(Duration connectionAcquisitionTimeout) {
            connectionAcquisitionTimeout(connectionAcquisitionTimeout);
        }

        /**
         * Sets the {@link EventLoopGroup} to use for the Netty HTTP client. This event loop group may be shared
         * across multiple HTTP clients for better resource and thread utilization. The preferred way to create
         * an {@link EventLoopGroup} is by using the {@link EventLoopGroupFactory#create()} method which will choose the
         * optimal implementation per the platform.
         *
         * <p>The {@link EventLoopGroup} <b>MUST</b> be closed by the caller when it is ready to
         * be disposed. The SDK will not close the {@link EventLoopGroup} when the HTTP client is closed. See
         * {@link EventLoopGroup#shutdownGracefully()} to properly close the event loop group.</p>
         *
         * <p>This configuration method is only recommended when you wish to share an {@link EventLoopGroup}
         * with multiple clients. If you do not need to share the group it is recommended to use
         * {@link #eventLoopGroupFactory(EventLoopGroupFactory)} as the SDK will handle its cleanup when
         * the HTTP client is closed.</p>
         *
         * <p>Setting this will un-set anything set by {@link #eventLoopGroupFactory(EventLoopGroupFactory)}</p>
         *
         * @param eventLoopGroup Netty {@link EventLoopGroup} to use.
         * @return This builder for method chaining.
         * @see DefaultEventLoopGroupFactory
         */
        @Override
        public Builder eventLoopGroup(EventLoopGroup eventLoopGroup) {
            this.eventLoopGroup = eventLoopGroup;
            return this;
        }

        public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
            eventLoopGroup(eventLoopGroup);
        }

        /**
         * Sets the {@link EventLoopGroupFactory} which will be used to create the {@link EventLoopGroup} for the Netty
         * HTTP client. This allows for custom configuration of the Netty {@link EventLoopGroup}.
         *
         * <p>The {@link EventLoopGroup} created by the factory is managed by the SDK and will be shutdown
         * when the HTTP client is closed.</p>
         *
         * <p>This is the preferred configuration method when you just want to customize the {@link EventLoopGroup}
         * but not share it across multiple HTTP clients. If you do wish to share an {@link EventLoopGroup}, see
         * {@link #eventLoopGroup(EventLoopGroup)}</p>
         *
         * <p>Setting this will un-set anything set by {@link #eventLoopGroup(EventLoopGroup)}</p>
         *
         * @param eventLoopGroupFactory {@link EventLoopGroupFactory} to use.
         * @return This builder for method chaining.
         * @see DefaultEventLoopGroupFactory
         */
        @Override
        public Builder eventLoopGroupFactory(EventLoopGroupFactory eventLoopGroupFactory) {
            this.eventLoopGroupFactory = eventLoopGroupFactory;
            return this;
        }

        public void setEventLoopGroupFactory(EventLoopGroupFactory eventLoopGroupFactory) {
            eventLoopGroupFactory(eventLoopGroupFactory);
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            return new NettyNioAsyncHttpClient(this, standardOptions.build()
                                                                    .merge(serviceDefaults)
                                                                    .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
        }
    }
}
