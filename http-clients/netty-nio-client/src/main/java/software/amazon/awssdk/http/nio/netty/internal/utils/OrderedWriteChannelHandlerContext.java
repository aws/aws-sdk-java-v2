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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An implementation of {@link ChannelHandlerContext} that ensures all writes are performed in the order they are invoked.
 *
 * This works around https://github.com/netty/netty/issues/7783 where writes by an event loop 'skip ahead' of writes off of the
 * event loop.
 */
@SdkInternalApi
public class OrderedWriteChannelHandlerContext extends DelegatingChannelHandlerContext {
    private OrderedWriteChannelHandlerContext(ChannelHandlerContext delegate) {
        super(delegate);
    }

    public static ChannelHandlerContext wrap(ChannelHandlerContext ctx) {
        return new OrderedWriteChannelHandlerContext(ctx);
    }

    @Override
    public ChannelFuture write(Object msg) {
        return doInOrder(promise -> super.write(msg, promise));
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        doInOrder(() -> super.write(msg, promise));
        return promise;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return doInOrder(promise -> super.writeAndFlush(msg, promise));
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        doInOrder(() -> super.writeAndFlush(msg, promise));
        return promise;
    }

    private ChannelFuture doInOrder(Consumer<ChannelPromise> task) {
        ChannelPromise promise = newPromise();
        if (!channel().eventLoop().inEventLoop()) {
            task.accept(promise);
        } else {
            // If we're in the event loop, queue a task to perform the write, so that it occurs after writes that were scheduled
            // off of the event loop.
            channel().eventLoop().execute(() -> task.accept(promise));
        }
        return promise;
    }

    private void doInOrder(Runnable task) {
        if (!channel().eventLoop().inEventLoop()) {
            task.run();
        } else {
            // If we're in the event loop, queue a task to perform the write, so that it occurs after writes that were scheduled
            // off of the event loop.
            channel().eventLoop().execute(task);
        }
    }
}
