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
import static org.junit.Assert.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLException;
import org.junit.Test;
import software.amazon.awssdk.http.Protocol;

public class ChannelPipelineInitializerTest {

    private ChannelPipelineInitializer pipelineInitializer;

    private URI targetUri;

    @Test
    public void channelConfigOptionCheck() throws SSLException {
        targetUri = URI.create("https://some-awesome-service-1234.amazonaws.com:8080");

        SslContext sslContext = SslContextBuilder.forClient()
                                                 .sslProvider(SslProvider.JDK)
                                                 .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                                 .build();

        AtomicReference<ChannelPool> channelPoolRef = new AtomicReference<>();

        NettyConfiguration nettyConfiguration = new NettyConfiguration(GLOBAL_HTTP_DEFAULTS);

        pipelineInitializer = new ChannelPipelineInitializer(Protocol.HTTP1_1,
                                                             sslContext,
                                                             SslProvider.JDK,
                                                             100,
                                                             1024,
                                                             Duration.ZERO,
                                                             channelPoolRef,
                                                             nettyConfiguration,
                                                             targetUri);

        Channel channel = new EmbeddedChannel();

        pipelineInitializer.channelCreated(channel);

        assertThat(channel.config().getOption(ChannelOption.ALLOCATOR), is(UnpooledByteBufAllocator.DEFAULT));

    }
}