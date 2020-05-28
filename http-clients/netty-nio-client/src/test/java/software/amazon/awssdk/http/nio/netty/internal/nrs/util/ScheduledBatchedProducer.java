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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
public class ScheduledBatchedProducer extends BatchedProducer {

    private final ScheduledExecutorService executor;
    private final long delay;

    public ScheduledBatchedProducer(long eofOn, int batchSize, long sequence, ScheduledExecutorService executor, long delay) {
        super(eofOn, batchSize, sequence, executor);
        this.executor = executor;
        this.delay = delay;
    }

    protected boolean complete;

    @Override
    public void read(final ChannelHandlerContext ctx) throws Exception {
        executor.schedule(() -> {
            for (int i = 0; i < batchSize && sequence != eofOn; i++) {
                ctx.fireChannelRead(sequence++);
            }
            complete = eofOn == sequence;
            executor.schedule(() -> {
                if (complete) {
                    ctx.fireChannelInactive();
                } else {
                    ctx.fireChannelReadComplete();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }, delay, TimeUnit.MILLISECONDS);
    }
}
