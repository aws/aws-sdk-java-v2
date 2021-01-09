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
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.LAST_HTTP_CONTENT_RECEIVED_KEY;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.LastHttpContent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class LastHttpContentHandlerTest {

    private MockChannel channel;
    private ChannelHandlerContext handlerContext;
    private LastHttpContentHandler contentHandler = LastHttpContentHandler.create();

    @Before
    public void setup() throws Exception {
        channel = new MockChannel();
        channel.attr(LAST_HTTP_CONTENT_RECEIVED_KEY).set(false);
        handlerContext = Mockito.mock(ChannelHandlerContext.class);
        Mockito.when(handlerContext.channel()).thenReturn(channel);
    }

    @After
    public void cleanup() {
        channel.close();
    }

    @Test
    public void lastHttpContentReceived_shouldSetAttribute() {
        LastHttpContent lastHttpContent = LastHttpContent.EMPTY_LAST_CONTENT;
        contentHandler.channelRead(handlerContext, lastHttpContent);

        assertThat(channel.attr(LAST_HTTP_CONTENT_RECEIVED_KEY).get()).isTrue();
    }

    @Test
    public void otherContentReceived_shouldNotSetAttribute() {
        String content = "some content";
        contentHandler.channelRead(handlerContext, content);

        assertThat(channel.attr(LAST_HTTP_CONTENT_RECEIVED_KEY).get()).isFalse();
    }
}
