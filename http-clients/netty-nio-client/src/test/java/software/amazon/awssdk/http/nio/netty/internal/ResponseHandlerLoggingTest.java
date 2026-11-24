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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.EXECUTE_FUTURE_KEY;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.KEEP_ALIVE;
import static software.amazon.awssdk.http.nio.netty.internal.ChannelAttributeKey.REQUEST_CONTEXT_KEY;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.testutils.LogCaptor;

class ResponseHandlerLoggingTest {

    private EmbeddedChannel channel;
    private ChannelHandlerContext ctx;

    @BeforeEach
    void setUp() {
        SdkHttpRequest request = mock(SdkHttpRequest.class);
        when(request.toString()).thenReturn("request\r\nvalue");
        AsyncExecuteRequest executeRequest = AsyncExecuteRequest.builder()
                                                                .request(request)
                                                                .responseHandler(mock(SdkAsyncHttpResponseHandler.class))
                                                                .build();

        channel = new EmbeddedChannel();
        channel.attr(REQUEST_CONTEXT_KEY).set(new RequestContext(mock(SdkChannelPool.class), null, executeRequest, null));
        channel.attr(EXECUTE_FUTURE_KEY).set(new CompletableFuture<>());
        channel.attr(KEEP_ALIVE).set(true);

        ctx = mock(ChannelHandlerContext.class);
        when(ctx.channel()).thenReturn(channel);
    }

    @AfterEach
    void tearDown() {
        channel.close();
    }

    @Test
    void exceptionCaught_requestAndCauseContainLineBreaks_logsEscapedMessageAndThrowable() throws Exception {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            ResponseHandler.getInstance().exceptionCaught(ctx, new IOException("cause\r\nvalue"));

            LogEvent event = logCaptor.loggedEvents().stream()
                                      .filter(e -> e.getMessage().getFormattedMessage()
                                                    .contains("Exception processing request"))
                                      .findFirst()
                                      .orElseThrow(AssertionError::new);
            assertThat(event.getMessage().getFormattedMessage())
                .contains("Exception processing request: request\\r\\nvalue")
                .doesNotContain("\r", "\n");
            assertThat(event.getThrown()).hasToString("java.io.IOException: cause\\r\\nvalue");
            assertThat(event.getThrown().getMessage()).doesNotContain("\r", "\n");
        }
    }
}
