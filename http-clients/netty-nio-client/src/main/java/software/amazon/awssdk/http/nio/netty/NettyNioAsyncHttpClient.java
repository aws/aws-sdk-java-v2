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
import static software.amazon.awssdk.http.nio.netty.internal.utils.SocketChannelResolver.resolveSocketChannelClass;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.util.Optional;
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
import software.amazon.awssdk.http.nio.netty.internal.NettyConfiguration;
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
    private final NettyConfiguration configuration;

    NettyNioAsyncHttpClient(NettySdkHttpClientFactory factory, AttributeMap serviceDefaultsMap) {
        this.configuration = new NettyConfiguration(serviceDefaultsMap, factory);
        this.group = factory.eventLoopGroupConfiguration().toEither()
                            .map(e -> e.map(NonManagedEventLoopGroup::new,
                                            EventLoopGroupFactory::create))
                            .orElseGet(SharedEventLoopGroup::get);
        this.pools = createChannelPoolMap();
    }

    private ChannelPoolMap<URI, ChannelPool> createChannelPoolMap() {
        return new SdkChannelPoolMap<URI, ChannelPool>() {
            @Override
            protected ChannelPool newPool(URI key) {
                Bootstrap bootstrap =
                        new Bootstrap()
                                .group(group)
                                .channel(resolveSocketChannelClass(group))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectionTimeout())
                                .option(ChannelOption.TCP_NODELAY, true)
                                .remoteAddress(key.getHost(), key.getPort());
                ChannelPipelineInitializer pipelineInitializer = new ChannelPipelineInitializer(sslContext(key.getScheme()));
                return BetterFixedChannelPool.builder()
                                             .channelPool(new SimpleChannelPool(bootstrap,
                                                                                pipelineInitializer,
                                                                                ChannelHealthChecker.ACTIVE))
                                             .executor(bootstrap.config().group().next())
                                             .acquireTimeoutAction(BetterFixedChannelPool.AcquireTimeoutAction.FAIL)
                                             // TODO expose better options for this
                                             .acquireTimeoutMillis(configuration.connectionAcquisitionTimeout())
                                             .maxConnections(configuration.maxConnectionsPerEndpoint())
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
                                                          handler, configuration);
        return new RunnableRequest(context);
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return configuration.getConfigurationValue(key);
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
                builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            }
            return invokeSafely(builder::build);
        }
        return null;
    }
}
