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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class WriteIdleTimeoutHandlerTest {

    @Test
    void writerIdleEvent_shouldFireExceptionAndCloseChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(new WriteIdleTimeoutHandler(30000));

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.WRITER_IDLE_STATE_EVENT);

        assertThat(channel.isOpen()).isFalse();
        assertThatThrownBy(channel::checkException)
            .isInstanceOf(IOException.class)
            .hasMessageContaining("No data was written to the request body within 30000ms");
    }

    @Test
    void readerIdleEvent_shouldBeIgnored() {
        EmbeddedChannel channel = new EmbeddedChannel(new WriteIdleTimeoutHandler(30000));

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.READER_IDLE_STATE_EVENT);

        assertThat(channel.isOpen()).isTrue();
    }

    @Test
    void duplicateWriterIdleEvent_shouldNotFireTwice() {
        EmbeddedChannel channel = new EmbeddedChannel(new WriteIdleTimeoutHandler(30000));

        channel.pipeline().fireUserEventTriggered(IdleStateEvent.WRITER_IDLE_STATE_EVENT);

        // Channel is already closed; second event should not throw
        channel.pipeline().fireUserEventTriggered(IdleStateEvent.WRITER_IDLE_STATE_EVENT);
    }
}
