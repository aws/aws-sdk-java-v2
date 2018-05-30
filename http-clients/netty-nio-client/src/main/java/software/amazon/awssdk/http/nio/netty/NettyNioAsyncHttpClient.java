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
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.WRITE_TIMEOUT;
import static software.amazon.awssdk.http.nio.netty.internal.utils.SocketChannelResolver.resolveSocketChannelClass;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.ChannelPipelineInitializer;
import software.amazon.awssdk.http.nio.netty.internal.HandlerRemovingChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
import software.amazon.awssdk.http.nio.netty.internal.NonManagedEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.RequestAdapter;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.RunnableRequest;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPoolMap;
import software.amazon.awssdk.http.nio.netty.internal.SharedEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.http2.HttpOrHttp2ChannelPool;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Validate;

public class NettyNioAsyncHttpClient implements SdkAsyncHttpClient {

    private final RequestAdapter requestAdapter = new RequestAdapter();
    private final EventLoopGroup group;
    private final ChannelPoolMap<URI, ChannelPool> pools;
    private final NettyConfiguration configuration;
    private final long maxStreams;
    private Protocol protocol;

    NettyNioAsyncHttpClient(DefaultBuilder builder, AttributeMap serviceDefaultsMap) {
        this.configuration = new NettyConfiguration(serviceDefaultsMap);
        this.protocol = serviceDefaultsMap.get(SdkHttpConfigurationOption.PROTOCOL);
        this.maxStreams = 200;
        this.group = eventLoopGroup(builder);
        this.pools = createChannelPoolMap();
    }

    private EventLoopGroup eventLoopGroup(DefaultBuilder builder) {
        Validate.isTrue(builder.eventLoopGroup == null || builder.eventLoopGroupFactory == null,
                        "The eventLoopGroup and the eventLoopGroupFactory can't both be configured.");
        return Either.fromNullable(builder.eventLoopGroup, builder.eventLoopGroupFactory)
                     .map(e -> e.map(NonManagedEventLoopGroup::new,
                                     EventLoopGroupFactory::create))
                     .orElseGet(SharedEventLoopGroup::get);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public AbortableRunnable prepareRequest(SdkHttpRequest sdkRequest,
                                            SdkRequestContext sdkRequestContext,
                                            SdkHttpRequestProvider requestProvider,
                                            SdkHttpResponseHandler handler) {
        RequestContext context = new RequestContext(pools.get(poolKey(sdkRequest)),
                                                    sdkRequest, requestProvider,
                                                    requestAdapter.adapt(sdkRequest),
                                                    handler, configuration);
        return new RunnableRequest(context);
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
                                    // TODO this seems to work fine with H1 too but confirm
                                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .trustManager(getTrustManager())
                                    .build();
        } catch (SSLException e) {
            // TODO is throwing the right thing here or should we notify the handler?
            throw new RuntimeException(e);
        }
    }

    private TrustManagerFactory getTrustManager() {
        return configuration.trustAllCertificates() ? InsecureTrustManagerFactory.INSTANCE : null;
    }

    private ChannelPoolMap<URI, ChannelPool> createChannelPoolMap() {
        return new SdkChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                SslContext sslContext = sslContext(key.getScheme());
                Bootstrap bootstrap =
                    new Bootstrap()
                        .group(group)
                        .channel(resolveSocketChannelClass(group))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectTimeoutMillis())
                        // TODO run some performance tests with and without this.
                        .option(ChannelOption.TCP_NODELAY, true)
                        .remoteAddress(key.getHost(), key.getPort());
                AtomicReference<ChannelPool> channelPoolRef = new AtomicReference<>();
                channelPoolRef.set(new HandlerRemovingChannelPool(
                    new HttpOrHttp2ChannelPool(bootstrap,
                                               new ChannelPipelineInitializer(protocol, sslContext, maxStreams, channelPoolRef),
                                               configuration.maxConnections(),
                                               configuration)));
                return channelPoolRef.get();
            }
        };
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return Optional.ofNullable(configuration.attribute(key));
    }

    @Override
    public void close() {
        group.shutdownGracefully();
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
         * Forces the HTTP client to trust all certificates, even invalid or self signed certificates. This should only ever
         * be used for testing purposes.
         *
         * @param trustAllCertificates Whether to trust all certificates. The default is false and only valid certificates
         *                             whose trust can be verified via the trust store will be trusted.
         * @return This builder for method chaining.
         */
        Builder trustAllCertificates(Boolean trustAllCertificates);

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
         * Forces the HTTP client to trust all certificates, even invalid or self signed certificates. This should only ever
         * be used for testing purposes.
         *
         * @param trustAllCertificates Whether to trust all certificates. The default is false and only valid certificates
         *                             whose trust can be verified via the trust store will be trusted.
         * @return This builder for method chaining.
         */
        @Override
        public Builder trustAllCertificates(Boolean trustAllCertificates) {
            standardOptions.put(TRUST_ALL_CERTIFICATES, trustAllCertificates);
            return this;
        }

        public void setTrustAllCertificates(Boolean trustAllCertificates) {
            trustAllCertificates(trustAllCertificates);
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
