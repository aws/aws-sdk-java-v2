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

package software.amazon.awssdk.http.nio.netty.internal;


import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.concurrent.Future;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.http.nio.netty.RecordingNetworkTrafficListener;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.utils.AttributeMap;

public class AwaitCloseChannelPoolMapTest {

    private final RecordingNetworkTrafficListener recorder = new RecordingNetworkTrafficListener();

    private AwaitCloseChannelPoolMap channelPoolMap;

    @Rule
    public WireMockRule mockProxy = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .networkTrafficListener(recorder));

    @After
    public void methodTeardown() {
        if (channelPoolMap != null) {
            channelPoolMap.close();
        }
        channelPoolMap = null;

        recorder.reset();
    }

    @Test
    public void close_underlyingPoolsShouldBeClosed() {
        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                .sdkChannelOptions(new SdkChannelOptions())
                .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                .protocol(Protocol.HTTP1_1)
                .maxStreams(100)
                .sslProvider(SslProvider.OPENSSL)
                .build();

        int numberOfChannelPools = 5;
        List<SimpleChannelPoolAwareChannelPool> channelPools = new ArrayList<>();

        for (int i = 0; i < numberOfChannelPools; i++) {
            channelPools.add(
                    channelPoolMap.get(URI.create("http://" + RandomStringUtils.randomAlphabetic(2) + i + "localhost:" + numberOfChannelPools)));
        }

        assertThat(channelPoolMap.pools().size()).isEqualTo(numberOfChannelPools);
        channelPoolMap.close();
        channelPools.forEach(channelPool -> {
            assertThat(channelPool.underlyingSimpleChannelPool().closeFuture()).isDone();
            assertThat(channelPool.underlyingSimpleChannelPool().closeFuture().join()).isTrue();
        });
    }

    @Test
    public void get_callsInjectedBootstrapProviderCorrectly() {
        BootstrapProvider bootstrapProvider = Mockito.spy(
            new BootstrapProvider(SdkEventLoopGroup.builder().build(),
                                  new NettyConfiguration(GLOBAL_HTTP_DEFAULTS),
                                  new SdkChannelOptions()));

        URI targetUri = URI.create("https://some-awesome-service-1234.amazonaws.com:8080");

        AwaitCloseChannelPoolMap.Builder builder =
            AwaitCloseChannelPoolMap.builder()
                                    .sdkChannelOptions(new SdkChannelOptions())
                                    .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                    .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                    .protocol(Protocol.HTTP1_1)
                                    .maxStreams(100)
                                    .sslProvider(SslProvider.OPENSSL);

        channelPoolMap = new AwaitCloseChannelPoolMap(builder, null, bootstrapProvider);
        channelPoolMap.get(targetUri);

        verify(bootstrapProvider).createBootstrap("some-awesome-service-1234.amazonaws.com", 8080);
    }

    @Test
    public void get_usingProxy_callsInjectedBootstrapProviderCorrectly() {
        BootstrapProvider bootstrapProvider = Mockito.spy(
            new BootstrapProvider(SdkEventLoopGroup.builder().build(),
                                  new NettyConfiguration(GLOBAL_HTTP_DEFAULTS),
                                  new SdkChannelOptions()));

        URI targetUri = URI.create("https://some-awesome-service-1234.amazonaws.com:8080");
        Map<URI, Boolean> shouldProxyCache =  new HashMap<>();
        shouldProxyCache.put(targetUri, true);

        ProxyConfiguration proxyConfiguration =
            ProxyConfiguration.builder()
                              .host("localhost")
                              .port(mockProxy.port())
                              .build();

        AwaitCloseChannelPoolMap.Builder builder =
            AwaitCloseChannelPoolMap.builder()
                                    .proxyConfiguration(proxyConfiguration)
                                    .sdkChannelOptions(new SdkChannelOptions())
                                    .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                                    .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                                    .protocol(Protocol.HTTP1_1)
                                    .maxStreams(100)
                                    .sslProvider(SslProvider.OPENSSL);

        channelPoolMap = new AwaitCloseChannelPoolMap(builder, shouldProxyCache, bootstrapProvider);
        channelPoolMap.get(targetUri);

        verify(bootstrapProvider).createBootstrap("localhost", mockProxy.port());
    }

    @Test
    public void usingProxy_usesCachedValueWhenPresent() {
        URI targetUri = URI.create("https://some-awesome-service-1234.amazonaws.com");

        Map<URI, Boolean> shouldProxyCache =  new HashMap<>();
        shouldProxyCache.put(targetUri, true);

        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                .host("localhost")
                .port(mockProxy.port())
                // Deliberately set the target host as a non-proxy host to see if it will check the cache first
                .nonProxyHosts(Stream.of(targetUri.getHost()).collect(Collectors.toSet()))
                .build();

        AwaitCloseChannelPoolMap.Builder builder = AwaitCloseChannelPoolMap.builder()
                .proxyConfiguration(proxyConfiguration)
                .sdkChannelOptions(new SdkChannelOptions())
                .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                .protocol(Protocol.HTTP1_1)
                .maxStreams(100)
                .sslProvider(SslProvider.OPENSSL);

        channelPoolMap = new AwaitCloseChannelPoolMap(builder, shouldProxyCache, null);

        // The target host does not exist so acquiring a channel should fail unless we're configured to connect to
        // the mock proxy host for this URI.
        SimpleChannelPoolAwareChannelPool channelPool = channelPoolMap.newPool(targetUri);
        Future<Channel> channelFuture = channelPool.underlyingSimpleChannelPool().acquire().awaitUninterruptibly();
        assertThat(channelFuture.isSuccess()).isTrue();
        channelPool.release(channelFuture.getNow()).awaitUninterruptibly();
    }

    @Test
    public void usingProxy_noSchemeGiven_defaultsToHttp() {
        ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder()
                .host("localhost")
                .port(mockProxy.port())
                .build();

        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                .proxyConfiguration(proxyConfiguration)
                .sdkChannelOptions(new SdkChannelOptions())
                .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                .configuration(new NettyConfiguration(GLOBAL_HTTP_DEFAULTS))
                .protocol(Protocol.HTTP1_1)
                .maxStreams(100)
                .sslProvider(SslProvider.OPENSSL)
                .build();

        SimpleChannelPoolAwareChannelPool simpleChannelPoolAwareChannelPool = channelPoolMap.newPool(
                URI.create("https://some-awesome-service:443"));

        simpleChannelPoolAwareChannelPool.acquire().awaitUninterruptibly();

        String requests = recorder.requests().toString();

        assertThat(requests).contains("CONNECT some-awesome-service:443");
    }

    @Test
    public void usesProvidedKeyManagersProvider() {
        TlsKeyManagersProvider provider = mock(TlsKeyManagersProvider.class);

        AttributeMap config = AttributeMap.builder()
                .put(TLS_KEY_MANAGERS_PROVIDER, provider)
                .build();

        channelPoolMap = AwaitCloseChannelPoolMap.builder()
                .sdkChannelOptions(new SdkChannelOptions())
                .sdkEventLoopGroup(SdkEventLoopGroup.builder().build())
                .configuration(new NettyConfiguration(config.merge(GLOBAL_HTTP_DEFAULTS)))
                .build();

        ChannelPool channelPool = channelPoolMap.newPool(URI.create("https://localhost:" + mockProxy.port()));
        channelPool.acquire().awaitUninterruptibly();
        verify(provider).keyManagers();
    }

}
