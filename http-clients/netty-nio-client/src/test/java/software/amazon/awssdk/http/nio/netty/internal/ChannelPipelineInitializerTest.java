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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.ssl.SslProvider;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;

public class ChannelPipelineInitializerTest {

    private final URI TARGET_URI = URI.create("https://some-awesome-service-1234.amazonaws.com:8080");
    private static final SslProvider SSL_PROVIDER = SslProvider.JDK;

    @Test
    public void channelConfigOptionCheck() {
        ChannelPipelineInitializer pipelineInitializer = createChannelPipelineInitializer(Protocol.HTTP1_1, ProtocolNegotiation.ASSUME_PROTOCOL);
        Channel channel = new EmbeddedChannel();
        pipelineInitializer.channelCreated(channel);

        assertThat(channel.config().getOption(ChannelOption.ALLOCATOR), is(UnpooledByteBufAllocator.DEFAULT));
    }

    @Test
    @EnabledIf("alpnSupported")
    public void h2AlpnEnabled_shouldUseAlpn() {
        ChannelPipelineInitializer pipelineInitializer = createChannelPipelineInitializer(Protocol.HTTP2, ProtocolNegotiation.ALPN);
        Channel channel = new EmbeddedChannel();
        pipelineInitializer.channelCreated(channel);

        assertNotNull(channel.pipeline().get(ApplicationProtocolNegotiationHandler.class));
    }

    @Test
    @EnabledIf("alpnSupported")
    public void h2AlpnEnabled_serverSupportsAlpn_shouldCreateH2Handler() throws Exception {
        ChannelPipelineInitializer pipelineInitializer = createChannelPipelineInitializer(Protocol.HTTP2, ProtocolNegotiation.ALPN);
        Channel channel = new EmbeddedChannel();
        pipelineInitializer.channelCreated(channel);

        simulateServerAlpnSuccess(channel, ApplicationProtocolNames.HTTP_2);

        assertNotNull(channel.pipeline().get(Http2MultiplexHandler.class));
    }

    @Test
    @EnabledIf("alpnSupported")
    public void h2AlpnEnabled_serverDoesNotSupportAlpn_shouldNotFallbackToH2() throws Exception {
        ChannelPipelineInitializer pipelineInitializer = createChannelPipelineInitializer(Protocol.HTTP2, ProtocolNegotiation.ALPN);
        Channel channel = new EmbeddedChannel();
        pipelineInitializer.channelCreated(channel);

        simulateServerAlpnUnsupported(channel);

        assertNull(channel.pipeline().get(Http2MultiplexHandler.class));
    }

    private void simulateServerAlpnSuccess(Channel channel, String protocol) throws Exception {
        SslHandler mockSslHandler = mock(SslHandler.class);
        when(mockSslHandler.applicationProtocol()).thenReturn(protocol);
        channel.pipeline().replace(SslHandler.class, "MockSslHandler", mockSslHandler);

        assertNotNull(channel.pipeline().get(ApplicationProtocolNegotiationHandler.class));

        ChannelHandlerContext ctx = channel.pipeline().context(ApplicationProtocolNegotiationHandler.class);
        channel.pipeline().get(ApplicationProtocolNegotiationHandler.class).userEventTriggered(ctx, SslHandshakeCompletionEvent.SUCCESS);
    }

    private void simulateServerAlpnUnsupported(Channel channel) throws Exception {
        SslHandler mockSslHandler = mock(SslHandler.class);
        when(mockSslHandler.applicationProtocol()).thenReturn(null);
        channel.pipeline().replace(SslHandler.class, "MockSslHandler", mockSslHandler);

        assertNotNull(channel.pipeline().get(ApplicationProtocolNegotiationHandler.class));

        ChannelHandlerContext ctx = channel.pipeline().context(ApplicationProtocolNegotiationHandler.class);
        channel.pipeline().get(ApplicationProtocolNegotiationHandler.class).userEventTriggered(ctx, SslHandshakeCompletionEvent.SUCCESS);
    }

    private ChannelPipelineInitializer createChannelPipelineInitializer(Protocol protocol, ProtocolNegotiation protocolNegotiation) {
        AtomicReference<ChannelPool> channelPoolRef = new AtomicReference<>();
        NettyConfiguration nettyConfiguration = new NettyConfiguration(GLOBAL_HTTP_DEFAULTS);
        SslContextProvider sslContextProvider = new SslContextProvider(nettyConfiguration,
                                                                       protocol,
                                                                       protocolNegotiation,
                                                                       SSL_PROVIDER);

        return new ChannelPipelineInitializer(protocol,
                                              protocolNegotiation,
                                              sslContextProvider.sslContext(),
                                              SSL_PROVIDER,
                                              100,
                                              1024,
                                              Duration.ZERO,
                                              channelPoolRef,
                                              nettyConfiguration,
                                              TARGET_URI);

    }

    private static boolean alpnSupported(){
        return NettyUtils.isAlpnSupported(SSL_PROVIDER);
    }
}