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

import static software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils.SUCCEEDED_FUTURE;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

/**
 * Provides access and manages a shared {@link SdkEventLoopGroup}. Uses reference counting to keep track of how many HTTP
 * clients are using the shared event loop group and will automatically close it when that count reaches zero. Event loop
 * group is lazily initialized for the first time and and subsequent requests after the count reaches zero.
 */
@SdkInternalApi
public final class SharedSdkEventLoopGroup {

    /**
     * Lazily initialized shared default event loop group.
     */
    private static SdkEventLoopGroup sharedSdkEventLoopGroup;

    /**
     * Reference count of clients using the shared event loop group.
     */
    private static int referenceCount = 0;

    private SharedSdkEventLoopGroup() {
    }

    /**
     * @return The default {@link SdkEventLoopGroup} that will be shared across all service clients.
     * This is used when the customer does not specify a custom {@link SdkEventLoopGroup} or {@link SdkEventLoopGroup.Builder}.
     * Each SdkEventLoopGroup returned is wrapped with a new {@link ReferenceCountingEventLoopGroup}.
     */
    @SdkInternalApi
    public static synchronized SdkEventLoopGroup get() {
        if (sharedSdkEventLoopGroup == null) {
            sharedSdkEventLoopGroup = SdkEventLoopGroup.builder().build();
        }

        referenceCount++;
        return SdkEventLoopGroup.create(new ReferenceCountingEventLoopGroup(sharedSdkEventLoopGroup.eventLoopGroup()),
                                        sharedSdkEventLoopGroup.channelFactory());
    }

    /**
     * Decrement the reference count and close if necessary.
     *
     * @param quietPeriod the quite period to use
     * @param timeout the timeout to use
     * @param unit the time unit
     *
     * @return the close future. If the shared event loop group is still being used, return a completed close future,
     * otherwise return the future from {@link EventLoopGroup#shutdownGracefully(long, long, TimeUnit)};
     */
    private static synchronized Future<?> decrementReference(long quietPeriod, long timeout, TimeUnit unit) {
        referenceCount--;
        if (referenceCount == 0) {
            Future<?> shutdownGracefully =
                sharedSdkEventLoopGroup.eventLoopGroup().shutdownGracefully(quietPeriod, timeout, unit);
            sharedSdkEventLoopGroup = null;
            return shutdownGracefully;
        }
        return SUCCEEDED_FUTURE;
    }

    @SdkTestInternalApi
    static synchronized int referenceCount() {
        return referenceCount;
    }

    /**
     * Special event loop group that prevents shutdown and decrements the reference count when the event loop group
     * is closed.
     */
    private static class ReferenceCountingEventLoopGroup extends DelegatingEventLoopGroup {

        private final AtomicBoolean hasBeenClosed = new AtomicBoolean(false);

        private ReferenceCountingEventLoopGroup(EventLoopGroup delegate) {
            super(delegate);
        }

        @Override
        public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
            // Only want to decrement the reference the first time it's closed. Shutdown is idempotent and may be
            // called multiple times.
            if (hasBeenClosed.compareAndSet(false, true)) {
                return decrementReference(quietPeriod, timeout, unit);
            }
            return SUCCEEDED_FUTURE;
        }
    }
}
