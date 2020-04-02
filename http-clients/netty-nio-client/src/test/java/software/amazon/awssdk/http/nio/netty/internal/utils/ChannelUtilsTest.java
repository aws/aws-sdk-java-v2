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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.HTTP2_MULTIPLEXED_CHANNEL_POOL;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.MAX_CONCURRENT_STREAMS;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.http.nio.netty.internal.MockChannel;

public class ChannelUtilsTest {

    @Test
    public void testGetAttributes() throws Exception {
        MockChannel channel = null;
        try {
            channel = new MockChannel();
            channel.attr(MAX_CONCURRENT_STREAMS).set(1L);
            assertThat(ChannelUtils.getAttribute(channel, MAX_CONCURRENT_STREAMS).get()).isEqualTo(1L);
            assertThat(ChannelUtils.getAttribute(channel, HTTP2_MULTIPLEXED_CHANNEL_POOL)).isNotPresent();
        } finally {
            Optional.ofNullable(channel).ifPresent(Channel::close);
        }
    }

    @Test
    public void removeIfExists() throws Exception {
        MockChannel channel = null;

        try {
            channel = new MockChannel();
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(new ReadTimeoutHandler(1));
            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));

            ChannelUtils.removeIfExists(pipeline, ReadTimeoutHandler.class, LoggingHandler.class);
            assertThat(pipeline.get(ReadTimeoutHandler.class)).isNull();
            assertThat(pipeline.get(LoggingHandler.class)).isNull();
        } finally {
            Optional.ofNullable(channel).ifPresent(Channel::close);
        }

    }
}
