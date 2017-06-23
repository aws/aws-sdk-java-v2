/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.util.concurrent.Future;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.http.async.SdkRequestChannel;

/**
 * An implementation of {@link SdkRequestChannel} that allows writing to a Netty {@link Channel}
 */
final class SdkNettyHttpRequestChannel implements SdkRequestChannel {

    private final Channel channel;
    private final Consumer<Throwable> errorHandler;
    private final Runnable abort;

    SdkNettyHttpRequestChannel(Channel channel, Consumer<Throwable> errorHandler, Runnable abort) {
        this.channel = channel;
        this.errorHandler = errorHandler;
        this.abort = abort;
    }

    @Override
    public void write(ByteBuffer data) {
        channel.writeAndFlush(new DefaultHttpContent(toByteBuf(channel.alloc(), data)))
               .addListener(this::handleFailure);
    }

    @Override
    public void complete() {
        channel.writeAndFlush(new DefaultLastHttpContent(new EmptyByteBuf(channel.alloc())))
               .addListener(this::handleFailure);
    }

    @Override
    public void abort() {
        abort.run();
    }

    @ReviewBeforeRelease("Unpooled vs channel.alloc? channel.alloc seems to be preferred but we should test" +
                         " this more thoroughly under high concurrency.")
    private ByteBuf toByteBuf(ByteBufAllocator allocator, ByteBuffer data) {
        final ByteBuf buffer = allocator.buffer(data.limit());
        buffer.writeBytes(data);
        return buffer;
    }

    private void handleFailure(Future<? super Void> future) {
        if (!future.isSuccess()) {
            errorHandler.accept(future.cause());
        }
    }
}
