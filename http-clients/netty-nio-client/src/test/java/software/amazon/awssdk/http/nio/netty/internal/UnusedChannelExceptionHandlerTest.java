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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UnusedChannelExceptionHandlerTest {
    private Throwable exception = new Throwable();
    private IOException ioException = new IOException();

    private ChannelHandlerContext ctx;
    private Channel channel;
    private Attribute<Boolean> inUseAttribute;
    private Attribute<CompletableFuture<Void>> futureAttribute;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setUp() {
        ctx = Mockito.mock(ChannelHandlerContext.class);
        channel = Mockito.mock(Channel.class);

        inUseAttribute = Mockito.mock(Attribute.class);
        futureAttribute = Mockito.mock(Attribute.class);

        Mockito.when(ctx.channel()).thenReturn(channel);
        Mockito.when(channel.attr(ChannelAttributeKey.IN_USE)).thenReturn(inUseAttribute);
        Mockito.when(channel.attr(ChannelAttributeKey.EXECUTE_FUTURE_KEY)).thenReturn(futureAttribute);
    }

    @Test
    public void inUseDoesNothing() {
        Mockito.when(inUseAttribute.get()).thenReturn(true);

        UnusedChannelExceptionHandler.getInstance().exceptionCaught(ctx, exception);

        Mockito.verify(ctx).fireExceptionCaught(exception);
        Mockito.verify(ctx, new Times(0)).close();
    }

    @Test
    public void notInUseNonIoExceptionCloses() {
        notInUseCloses(exception);
    }

    @Test
    public void notInUseIoExceptionCloses() {
        notInUseCloses(ioException);
    }

    @Test
    public void notInUseHasIoExceptionCauseCloses() {
        notInUseCloses(new RuntimeException(ioException));
    }


    private void notInUseCloses(Throwable exception) {
        Mockito.when(inUseAttribute.get()).thenReturn(false);
        Mockito.when(futureAttribute.get()).thenReturn(CompletableFuture.completedFuture(null));

        UnusedChannelExceptionHandler.getInstance().exceptionCaught(ctx, exception);

        Mockito.verify(ctx).close();
    }

    @Test
    public void notInUseFutureCompletes() {
        CompletableFuture<Void> incompleteFuture = new CompletableFuture<>();

        Mockito.when(inUseAttribute.get()).thenReturn(false);
        Mockito.when(futureAttribute.get()).thenReturn(incompleteFuture);

        UnusedChannelExceptionHandler.getInstance().exceptionCaught(ctx, exception);

        Mockito.verify(ctx).close();
        assertThat(incompleteFuture.isDone()).isTrue();
    }
}
