/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal;

import static software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration.CHANNEL_POOL_CLOSE_TIMEOUT_SECONDS;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.http2.HttpOrHttp2ChannelPool;
import software.amazon.awssdk.utils.Logger;

/**
 * Implementation of {@link SdkChannelPoolMap} that awaits channel pools to be closed upon closing.
 */
@SdkInternalApi
public final class AwaitCloseChannelPoolMap extends SdkChannelPoolMap<URI, SimpleChannelPoolAwareChannelPool> {

    private static final Logger log = Logger.loggerFor(AwaitCloseChannelPoolMap.class);

    private static final ChannelPoolHandler NOOP_HANDLER = new ChannelPoolHandler() {
        @Override
        public void channelReleased(Channel ch) throws Exception {
        }

        @Override
        public void channelAcquired(Channel ch) throws Exception {
        }

        @Override
        public void channelCreated(Channel ch) throws Exception {
        }
    };

    private final Map<URI, Boolean> shouldProxyForHostCache = new ConcurrentHashMap<>();


    private final SdkChannelOptions sdkChannelOptions;
    private final SdkEventLoopGroup sdkEventLoopGroup;
    private final NettyConfiguration configuration;
    private final Protocol protocol;
    private final long maxStreams;
    private final SslProvider sslProvider;
    private final ProxyConfiguration proxyConfiguration;

    private AwaitCloseChannelPoolMap(Builder builder) {
        this.sdkChannelOptions = builder.sdkChannelOptions;
        this.sdkEventLoopGroup = builder.sdkEventLoopGroup;
        this.configuration = builder.configuration;
        this.protocol = builder.protocol;
        this.maxStreams = builder.maxStreams;
        this.sslProvider = builder.sslProvider;
        this.proxyConfiguration = builder.proxyConfiguration;
    }

    @SdkTestInternalApi
    AwaitCloseChannelPoolMap(Builder builder, Map<URI, Boolean> shouldProxyForHostCache) {
        this(builder);
        this.shouldProxyForHostCache.putAll(shouldProxyForHostCache);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected SimpleChannelPoolAwareChannelPool newPool(URI key) {
        SslContext sslContext = sslContext(key);
        
        Bootstrap bootstrap = createBootstrap(key);

        AtomicReference<ChannelPool> channelPoolRef = new AtomicReference<>();

        ChannelPipelineInitializer pipelineInitializer =
            new ChannelPipelineInitializer(protocol, sslContext, maxStreams, channelPoolRef, configuration, key);

        BetterSimpleChannelPool tcpChannelPool;
        ChannelPool baseChannelPool;
        if (shouldUseProxyForHost(key)) {
            tcpChannelPool = new BetterSimpleChannelPool(bootstrap, NOOP_HANDLER);
            baseChannelPool = new Http1TunnelConnectionPool(bootstrap.config().group().next(), tcpChannelPool,
                                                            sslContext, proxyAddress(key), key, pipelineInitializer);
        } else {
            tcpChannelPool = new BetterSimpleChannelPool(bootstrap, pipelineInitializer);
            baseChannelPool = tcpChannelPool;
        }

        ChannelPool wrappedPool = wrapBaseChannelPool(bootstrap, baseChannelPool);

        channelPoolRef.set(wrappedPool);
        return new SimpleChannelPoolAwareChannelPool(wrappedPool, tcpChannelPool);
    }

    @Override
    public void close() {
        log.trace(() -> "Closing channel pools");
        // If there is a new pool being added while we are iterating the pools, there might be a
        // race condition between the close call of the newly acquired pool and eventLoopGroup.shutdown and it
        // could cause the eventLoopGroup#shutdownGracefully to hang before it times out.
        // If a new pool is being added while super.close() is running, it might be left open because
        // the underlying pool map is a ConcurrentHashMap and it doesn't guarantee strong consistency for retrieval
        // operations. See https://github.com/aws/aws-sdk-java-v2/pull/1200#discussion_r277906715
        Collection<SimpleChannelPoolAwareChannelPool> channelPools = pools().values();
        super.close();

        try {
            CompletableFuture.allOf(channelPools.stream()
                                                .map(pool -> pool.underlyingSimpleChannelPool().closeFuture())
                                                .toArray(CompletableFuture[]::new))
                             .get(CHANNEL_POOL_CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private Bootstrap createBootstrap(URI poolKey) {
        String host = bootstrapHost(poolKey);
        int port = bootstrapPort(poolKey);

        Bootstrap bootstrap =
                new Bootstrap()
                        .group(sdkEventLoopGroup.eventLoopGroup())
                        .channelFactory(sdkEventLoopGroup.channelFactory())
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectTimeoutMillis())
                        // TODO run some performance tests with and without this.
                        .remoteAddress(new InetSocketAddress(host, port));
        sdkChannelOptions.channelOptions().forEach(bootstrap::option);

        return bootstrap;
    }


    private boolean shouldUseProxyForHost(URI remoteAddr) {
        if (proxyConfiguration == null) {
            return false;
        }


        return shouldProxyForHostCache.computeIfAbsent(remoteAddr, (uri) ->
           proxyConfiguration.nonProxyHosts().stream().noneMatch(h -> uri.getHost().matches(h))
        );
    }

    private String bootstrapHost(URI remoteHost) {
        if (shouldUseProxyForHost(remoteHost)) {
            return proxyConfiguration.host();
        }
        return remoteHost.getHost();
    }

    private int bootstrapPort(URI remoteHost) {
        if (shouldUseProxyForHost(remoteHost)) {
            return proxyConfiguration.port();
        }
        return remoteHost.getPort();
    }

    private URI proxyAddress(URI remoteHost) {
        if (!shouldUseProxyForHost(remoteHost)) {
            return null;
        }

        String scheme = proxyConfiguration.scheme();
        if (scheme == null) {
            scheme = "http";
        }

        try {
            return new URI(scheme, null, proxyConfiguration.host(), proxyConfiguration.port(), null, null,
                    null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to construct proxy URI", e);
        }
    }

    private ChannelPool wrapBaseChannelPool(Bootstrap bootstrap, ChannelPool channelPool) {

        // Wrap the channel pool such that the ChannelAttributeKey.CLOSE_ON_RELEASE flag is honored.
        channelPool = new HonorCloseOnReleaseChannelPool(channelPool);

        // Wrap the channel pool such that HTTP 2 channels won't be released to the underlying pool while they're still in use.
        channelPool = new HttpOrHttp2ChannelPool(channelPool,
                                                 bootstrap.config().group(),
                                                 configuration.maxConnections(),
                                                 configuration);


        // Wrap the channel pool such that we remove request-specific handlers with each request.
        channelPool = new HandlerRemovingChannelPool(channelPool);

        // Wrap the channel pool such that an individual channel can only be released to the underlying pool once.
        channelPool = new ReleaseOnceChannelPool(channelPool);

        // Wrap the channel pool to guarantee all channels checked out are healthy, and all unhealthy channels checked in are
        // closed.
        channelPool = new HealthCheckedChannelPool(bootstrap.config().group(), configuration, channelPool);

        // Wrap the channel pool such that if the Promise given to acquire(Promise) is done when the channel is acquired
        // from the underlying pool, the channel is closed and released.
        channelPool = new CancellableAcquireChannelPool(bootstrap.config().group().next(), channelPool);

        return channelPool;
    }

    private SslContext sslContext(URI targetAddress) {
        URI proxyAddress = proxyAddress(targetAddress);

        boolean needContext = targetAddress.getScheme().equalsIgnoreCase("https")
                || proxyAddress != null && proxyAddress.getScheme().equalsIgnoreCase("https");

        if (!needContext) {
            return null;
        }

        try {
            return SslContextBuilder.forClient()
                                    .sslProvider(sslProvider)
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

    public static class Builder {

        private SdkChannelOptions sdkChannelOptions;
        private SdkEventLoopGroup sdkEventLoopGroup;
        private NettyConfiguration configuration;
        private Protocol protocol;
        private long maxStreams;
        private SslProvider sslProvider;
        private ProxyConfiguration proxyConfiguration;

        private Builder() {
        }

        public Builder sdkChannelOptions(SdkChannelOptions sdkChannelOptions) {
            this.sdkChannelOptions = sdkChannelOptions;
            return this;
        }

        public Builder sdkEventLoopGroup(SdkEventLoopGroup sdkEventLoopGroup) {
            this.sdkEventLoopGroup = sdkEventLoopGroup;
            return this;
        }

        public Builder configuration(NettyConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder maxStreams(long maxStreams) {
            this.maxStreams = maxStreams;
            return this;
        }

        public Builder sslProvider(SslProvider sslProvider) {
            this.sslProvider = sslProvider;
            return this;
        }

        public Builder proxyConfiguration(ProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        public AwaitCloseChannelPoolMap build() {
            return new AwaitCloseChannelPoolMap(this);
        }
    }
}
