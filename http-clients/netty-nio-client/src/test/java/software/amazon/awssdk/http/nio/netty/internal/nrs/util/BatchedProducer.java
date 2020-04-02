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
 *
 * Original source licensed under the Apache License 2.0 by playframework.
 */

package software.amazon.awssdk.http.nio.netty.internal.nrs.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.util.concurrent.Executor;

/**
 * A batched producer.
 *
 * Responds to read requests with batches of elements according to batch size. When eofOn is reached, it closes the
 * channel.
 *
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
public class BatchedProducer extends ChannelOutboundHandlerAdapter {

    protected final long eofOn;
    protected final int batchSize;
    private final Executor executor;
    protected long sequence;

    public BatchedProducer(long eofOn, int batchSize, long sequence, Executor executor) {
        this.eofOn = eofOn;
        this.batchSize = batchSize;
        this.sequence = sequence;
        this.executor = executor;
    }

    private boolean cancelled = false;


    @Override
    public void read(final ChannelHandlerContext ctx) throws Exception {
        if (cancelled) {
            throw new IllegalStateException("Received demand after being cancelled");
        }
        executor.execute(() -> {
            for (int i = 0; i < batchSize && sequence != eofOn; i++) {
                ctx.fireChannelRead(sequence++);
            }
            if (eofOn == sequence) {
                ctx.fireChannelInactive();
            } else {
                ctx.fireChannelReadComplete();
            }
        });
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (cancelled) {
            throw new IllegalStateException("Cancelled twice");
        }
        cancelled = true;
    }
}
