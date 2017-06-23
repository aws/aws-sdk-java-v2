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
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.nio.netty.internal.ChannelPipelineInitializer;
import software.amazon.awssdk.http.nio.netty.internal.RequestAdapter;
import software.amazon.awssdk.http.nio.netty.internal.RequestContext;
import software.amazon.awssdk.http.nio.netty.internal.RunnableRequest;
import software.amazon.awssdk.utils.AttributeMap;

@SdkInternalApi
final class NettyNioAsyncHttpClient implements SdkAsyncHttpClient {

    private final EventLoopGroup group = new NioEventLoopGroup();
    private final RequestAdapter requestAdapter = new RequestAdapter();
    private final ChannelPoolMap<URI, ChannelPool> pools;
    private final ServiceDefaults serviceDefaults;
    private final boolean trustAllCertificates;

    NettyNioAsyncHttpClient(NettySdkHttpClientFactory factory, AttributeMap serviceDefaultsMap) {
        this.serviceDefaults = new ServiceDefaults(serviceDefaultsMap);
        this.trustAllCertificates = factory.trustAllCertificates().orElse(Boolean.FALSE);
        this.pools = createChannelPoolMap(serviceDefaults,
                                          factory.maxConnectionsPerEndpoint().orElse(serviceDefaults.getMaxConnections()));
    }

    private AbstractChannelPoolMap<URI, ChannelPool> createChannelPoolMap(ServiceDefaults serviceDefaults,
                                                                          int maxConnectionsPerEndpoint) {
        return new AbstractChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                final Bootstrap bootstrap =
                        new Bootstrap()
                                .group(group)
                                .channel(NioSocketChannel.class)
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, serviceDefaults.getConnectionTimeout())
                                .remoteAddress(addressFor(key));
                SslContext sslContext = sslContext(key.getScheme());
                return new FixedChannelPool(bootstrap,
                                            new ChannelPipelineInitializer(sslContext), maxConnectionsPerEndpoint);
            }
        };
    }

    @Override
    public AbortableRunnable prepareRequest(SdkHttpRequest sdkRequest,
                                            SdkRequestContext requestContext,
                                            SdkHttpRequestProvider requestProvider,
                                            SdkHttpResponseHandler handler) {
        final RequestContext context = new RequestContext(pools.get(stripPath(sdkRequest.getEndpoint())),
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

    private static URI stripPath(URI uri) {
        return invokeSafely(() -> new URI(uri.getScheme(), null, uri.getHost(), port(uri), null, null, null));
    }

    private static InetSocketAddress addressFor(URI uri) {
        return new InetSocketAddress(uri.getHost(), port(uri));
    }

    private static int port(URI uri) {
        return uri.getPort() != -1 ? uri.getPort() : uri.getScheme().equalsIgnoreCase("https") ? 443 : 80;
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
