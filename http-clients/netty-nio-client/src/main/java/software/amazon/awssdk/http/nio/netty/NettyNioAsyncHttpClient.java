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

import static io.netty.handler.ssl.SslContext.defaultClientProvider;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.SOCKET_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.USE_STRICT_HOSTNAME_VERIFICATION;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.h2.BetterFixedChannelPool;
import software.amazon.awssdk.http.nio.netty.internal.ChannelPipelineInitializer;
import software.amazon.awssdk.http.nio.netty.internal.DelegatingEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.NonManagedEventLoopGroup;
import software.amazon.awssdk.http.nio.netty.internal.RequestAdapter;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.RunnableRequest;
import software.amazon.awssdk.http.nio.netty.internal.SdkChannelPoolMap;
import software.amazon.awssdk.http.nio.netty.internal.SharedEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;

@SdkInternalApi
final class NettyNioAsyncHttpClient implements SdkAsyncHttpClient {

    private final EventLoopGroup group;
    private final RequestAdapter requestAdapter = new RequestAdapter();
    private final ChannelPoolMap<URI, ChannelPool> pools;
    private final ServiceDefaults serviceDefaults;
    private final boolean trustAllCertificates;

    NettyNioAsyncHttpClient(NettySdkHttpClientFactory factory, AttributeMap serviceDefaultsMap) {
        this.serviceDefaults = new ServiceDefaults(serviceDefaultsMap);
        this.trustAllCertificates = factory.trustAllCertificates().orElse(Boolean.FALSE);
        this.group = factory.eventLoopGroupConfiguration().toEither()
                            .map(e -> e.map(NonManagedEventLoopGroup::new,
                                            EventLoopGroupFactory::create))
                            .orElseGet(SharedEventLoopGroup::get);
        this.pools = createChannelPoolMap(serviceDefaults,
                                          factory.maxConnectionsPerEndpoint().orElse(serviceDefaults.getMaxConnections()));
    }

    private ChannelPoolMap<URI, ChannelPool> createChannelPoolMap(ServiceDefaults serviceDefaults,
                                                                  int maxConnectionsPerEndpoint) {
        return new SdkChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                Bootstrap bootstrap =
                        new Bootstrap()
                                .group(group)
                                .channel(resolveSocketChannelClass())
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, serviceDefaults.getConnectionTimeout())
                                .option(ChannelOption.TCP_NODELAY, true)
                                .remoteAddress(key.getHost(), key.getPort());
                return BetterFixedChannelPool.builder()
                                             .channelPool(new SimpleChannelPool(bootstrap,
                                                                                new ChannelPipelineInitializer(sslContext(key.getScheme())),
                                                                                ChannelHealthChecker.ACTIVE))
                                             .executor(bootstrap.config().group().next())
                                             .acquireTimeoutAction(BetterFixedChannelPool.AcquireTimeoutAction.FAIL)
                                             // TODO expose better options for this
                                             .acquireTimeoutMillis(1000)
                                             .maxConnections(maxConnectionsPerEndpoint)
                                             .maxPendingAcquires(1000)
                                             .build();
            }
        };
    }

    @Override
    public AbortableRunnable prepareRequest(SdkHttpRequest sdkRequest,
                                            SdkRequestContext sdkRequestContext,
                                            SdkHttpRequestProvider requestProvider,
                                            SdkHttpResponseHandler handler) {
        RequestContext context = new RequestContext(pools.get(poolKey(sdkRequest)),
                                                          sdkRequest, requestProvider,
                                                          requestAdapter.adapt(sdkRequest),
                                                          handler);
        return new RunnableRequest(context);
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return serviceDefaults.getConfigurationValue(key);
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }

    /**
     * Depending on the EventLoopGroup used we may need to use a different socket channel.
     */
    @ReviewBeforeRelease("Perhaps we should make the customer provide both event loop group" +
                         "and channel in some kind of wrapper class to avoid having to do this.")
    private Class<? extends Channel> resolveSocketChannelClass() {
        EventLoopGroup unwrapped = group;
        // Keep unwrapping until it's not a DelegatingEventLoopGroup
        while (unwrapped instanceof DelegatingEventLoopGroup) {
            unwrapped = ((DelegatingEventLoopGroup) unwrapped).getDelegate();
        }
        return unwrapped instanceof EpollEventLoopGroup ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    private static URI poolKey(SdkHttpRequest sdkRequest) {
        return invokeSafely(() -> new URI(sdkRequest.protocol(), null, sdkRequest.host(),
                                          sdkRequest.port(), null, null, null));
    }

    private SslContext sslContext(String scheme) {
        if (scheme.equalsIgnoreCase("https")) {
            SslContextBuilder builder = SslContextBuilder.forClient().sslProvider(defaultClientProvider());
            if (trustAllCertificates) {
                builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }
            return invokeSafely(builder::build);
        }
        return null;
    }

    /**
     * Helper class to unwrap and convert service defaults.
     */
    private static class ServiceDefaults {
        private final AttributeMap serviceDefaults;

        private ServiceDefaults(AttributeMap serviceDefaults) {
            this.serviceDefaults = serviceDefaults;
        }

        @ReviewBeforeRelease("Not sure if Netty supports setting socket timeout. There's a ReadTimeoutHandler but that" +
                             "fires if the connection is just idle which is not what we want.")
        public int getSocketTimeout() {
            return saturatedCast(serviceDefaults.get(SOCKET_TIMEOUT).toMillis());
        }

        public int getConnectionTimeout() {
            return saturatedCast(serviceDefaults.get(CONNECTION_TIMEOUT).toMillis());
        }

        @ReviewBeforeRelease("Does it make sense to use this value? Netty's implementation is max connections" +
                             " per endpoint so if it's a shared client it doesn't mean quite the same thing.")
        public int getMaxConnections() {
            return serviceDefaults.get(MAX_CONNECTIONS);
        }

        @ReviewBeforeRelease("Support disabling strict hostname verification")
        public <T> Optional<T> getConfigurationValue(AttributeMap.Key<T> key) {
            return key == USE_STRICT_HOSTNAME_VERIFICATION ? Optional.empty() :
                    Optional.ofNullable(serviceDefaults.get(key));
        }
    }
}
