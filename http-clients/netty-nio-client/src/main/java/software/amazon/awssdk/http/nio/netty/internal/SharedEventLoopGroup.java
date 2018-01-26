/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.nio.netty.DefaultEventLoopGroupFactory;

/**
 * Provides access and manages a shared {@link EventLoopGroup}. Uses reference counting to keep track of how many HTTP
 * clients are using the shared event loop group and will automatically close it when that count reaches zero. Event loop
 * group is lazily initialized for the first time and and subsequent requests after the count reaches zero.
 */
public final class SharedEventLoopGroup {

    /**
     * Lazily initialized shared default event loop group.
     */
    private static EventLoopGroup sharedEventLoopGroup;

    /**
     * Reference count of clients using the shared event loop group.
     */
    private static int referenceCount = 0;

    private SharedEventLoopGroup() {
    }

    /**
     * @return The default {@link EventLoopGroup} that will be shared across all service clients. This is used when the customer
     * does not specify a custom {@link EventLoopGroup} or {@link DefaultEventLoopGroupFactory}.
     */
    @SdkInternalApi
    public static synchronized EventLoopGroup get() {
        if (sharedEventLoopGroup == null) {
            sharedEventLoopGroup = DefaultEventLoopGroupFactory.builder().build().create();
        }
        referenceCount++;
        return new ReferenceCountingEventLoopGroup(sharedEventLoopGroup);
    }

    /**
     * Decrement the reference count and close if necessary.
     */
    private static synchronized void decrementReference() {
        referenceCount--;
        if (referenceCount == 0) {
            sharedEventLoopGroup.shutdownGracefully();
            sharedEventLoopGroup = null;
        }
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
        public Future<?> shutdownGracefully() {
            // Only want to decrement the reference the first time it's closed. Shutdown is idempotent and may be
            // called multiple times.
            if (hasBeenClosed.compareAndSet(false, true)) {
                decrementReference();
            }
            return null;
        }
    }
}
