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

package software.amazon.awssdk.http.nio.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configurable factory for creating {@link EventLoopGroup} instances. Will choose the optimal implementation of
 * {@link EventLoopGroup} per the platform (i.e. if on Linux then {@link EpollEventLoopGroup} will be used).
 */
public final class EventLoopGroupFactory
        implements ToCopyableBuilder<EventLoopGroupFactory.Builder, EventLoopGroupFactory> {

    private final Integer numberOfThreads;
    private final ThreadFactory threadFactory;

    private EventLoopGroupFactory(DefaultBuilder builder) {
        this.numberOfThreads = builder.numberOfThreads;
        this.threadFactory = builder.threadFactory;
    }

    /**
     * @return The number of threads currently configured for the {@link EventLoopGroup} or an empty {@link Optional} if not set.
     */
    public Optional<Integer> numberOfThreads() {
        return Optional.ofNullable(numberOfThreads);
    }

    /**
     * @return The currently configured {@link ThreadFactory} or an empty {@link Optional} if not set.
     */
    public Optional<ThreadFactory> threadFactory() {
        return Optional.ofNullable(threadFactory);
    }

    /**
     * Create a new {@link EventLoopGroup} using the current configuration of the factory. Will use the optimal
     * implementation per the platform.
     */
    public EventLoopGroup create() {
        int numThreads = numberOfThreads == null ? 0 : numberOfThreads;
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(numThreads, threadFactory);
        } else {
            return new NioEventLoopGroup(numThreads, threadFactory);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .threadFactory(threadFactory)
                .numberOfThreads(numberOfThreads);
    }

    /**
     * @return Builder instance to construct a {@link EventLoopGroupFactory}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }


    /**
     * A builder for {@link EventLoopGroupFactory}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, EventLoopGroupFactory> {

        /**
         * Number of threads to use for the {@link io.netty.channel.EventLoopGroup}. If not set, the default
         * Netty thread count is used (which is double the number of available processors unless the io.netty.eventLoopThreads
         * system property is set.
         *
         * @param numberOfThreads Number of threads to use.
         * @return This builder for method chaining.
         */
        Builder numberOfThreads(Integer numberOfThreads);

        /**
         * {@link ThreadFactory} to create threads used by the {@link io.netty.channel.EventLoopGroup}. If not set,
         * a generic thread factory is used.
         *
         * @param threadFactory ThreadFactory to use.
         * @return This builder for method chaining.
         */
        Builder threadFactory(ThreadFactory threadFactory);

    }

    private static final class DefaultBuilder implements Builder {

        private Integer numberOfThreads;
        private ThreadFactory threadFactory;

        private DefaultBuilder() {
        }

        @Override
        public Builder numberOfThreads(Integer numberOfThreads) {
            this.numberOfThreads = numberOfThreads;
            return this;
        }

        public void setNumberOfThreads(Integer numberOfThreads) {
            numberOfThreads(numberOfThreads);
        }

        @Override
        public Builder threadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public void setThreadFactory(ThreadFactory threadFactory) {
            threadFactory(threadFactory);
        }

        @Override
        public EventLoopGroupFactory build() {
            return new EventLoopGroupFactory(this);
        }
    }
}
