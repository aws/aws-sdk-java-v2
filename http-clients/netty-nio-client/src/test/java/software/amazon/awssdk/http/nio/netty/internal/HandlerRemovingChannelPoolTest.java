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


import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.IN_USE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.RESPONSE_COMPLETE_KEY;

import software.amazon.awssdk.http.nio.netty.internal.nrs.HttpStreamsClientHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

@RunWith(MockitoJUnitRunner.class)
public class HandlerRemovingChannelPoolTest {
    @Mock
    private SdkChannelPool channelPool;

    @Mock
    private SdkAsyncHttpResponseHandler responseHandler;

    private Channel mockChannel;
    private ChannelPipeline pipeline;
    private NioEventLoopGroup nioEventLoopGroup;
    private HandlerRemovingChannelPool handlerRemovingChannelPool;

    @Before
    public void setup() throws Exception {
        mockChannel = new MockChannel();
        pipeline = mockChannel.pipeline();
        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
        nioEventLoopGroup = new NioEventLoopGroup();
        nioEventLoopGroup.register(mockChannel);
        RequestContext requestContext = new RequestContext(channelPool,
                                                           nioEventLoopGroup,
                                                           AsyncExecuteRequest.builder().responseHandler(responseHandler).build(),
                                                           null);

        mockChannel.attr(IN_USE).set(true);
        mockChannel.attr(REQUEST_CONTEXT_KEY).set(requestContext);
        mockChannel.attr(RESPONSE_COMPLETE_KEY).set(true);

        pipeline.addLast(new HttpStreamsClientHandler());
        pipeline.addLast(ResponseHandler.getInstance());
        pipeline.addLast(new ReadTimeoutHandler(10));
        pipeline.addLast(new WriteTimeoutHandler(10));
        handlerRemovingChannelPool = new HandlerRemovingChannelPool(channelPool);
    }

    @After
    public void tearDown() {
        nioEventLoopGroup.shutdownGracefully();
    }

    @Test
    public void release_openChannel_handlerShouldBeRemovedFromChannelPool() {
        assertHandlersNotRemoved();
        handlerRemovingChannelPool.release(mockChannel);

        assertHandlersRemoved();
    }

    @Test
    public void release_closedChannel_handlerShouldBeRemovedFromPipeline() {
        mockChannel.close().awaitUninterruptibly();

        // CLOSE -> INACTIVE -> UNREGISTERED: channel handlers should be removed at this point
        assertHandlersRemoved();
        handlerRemovingChannelPool.release(mockChannel);
        assertHandlersRemoved();
    }

    @Test
    public void release_deregisteredOpenChannel_handlerShouldBeRemovedFromChannelPool() {
        mockChannel.deregister().awaitUninterruptibly();
        assertHandlersNotRemoved();
        handlerRemovingChannelPool.release(mockChannel);

        assertHandlersRemoved();
    }

    private void assertHandlersRemoved() {
        assertThat(pipeline.get(HttpStreamsClientHandler.class)).isNull();
        assertThat(pipeline.get(ResponseHandler.class)).isNull();
        assertThat(pipeline.get(ReadTimeoutHandler.class)).isNull();
        assertThat(pipeline.get(WriteTimeoutHandler.class)).isNull();
    }

    private void assertHandlersNotRemoved() {
        assertThat(pipeline.get(HttpStreamsClientHandler.class)).isNotNull();
        assertThat(pipeline.get(ResponseHandler.class)).isNotNull();
        assertThat(pipeline.get(ReadTimeoutHandler.class)).isNotNull();
        assertThat(pipeline.get(WriteTimeoutHandler.class)).isNotNull();
    }
}
