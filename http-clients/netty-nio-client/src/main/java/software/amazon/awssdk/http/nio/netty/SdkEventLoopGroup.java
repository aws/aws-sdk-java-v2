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

package software.amazon.awssdk.http.nio.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.nio.netty.internal.utils.SocketChannelResolver;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;

/**
 * Provides {@link EventLoopGroup} and {@link ChannelFactory} for {@link NettyNioAsyncHttpClient}.
 * <p>
 * There are two ways to create a new instance.
 *
 * <ul>
 * <li>Using {@link #create(EventLoopGroup, ChannelFactory)} to provide a custom {@link EventLoopGroup} and
 * {@link ChannelFactory}
 * <p>
 * The {@link EventLoopGroup} <b>MUST</b> be closed by the caller when it is ready to
 * be disposed. The SDK will not close the {@link EventLoopGroup} when the HTTP client is closed. See
 * {@link EventLoopGroup#shutdownGracefully()} to properly close the event loop group.
 * </li>
 *
 * <li>using {@link #builder()} to provide custom configuration of {@link EventLoopGroup}. The {@link EventLoopGroup} created by
 * the builder is managed by the SDK and will be shutdown when the HTTP client is closed.
 * </li>
 * </ul>
 */
@SdkPublicApi
public final class SdkEventLoopGroup {

    private final EventLoopGroup eventLoopGroup;
    private final ChannelFactory<? extends Channel> channelFactory;

    private SdkEventLoopGroup(EventLoopGroup eventLoopGroup, ChannelFactory<? extends Channel> channelFactory) {
        Validate.paramNotNull(eventLoopGroup, "eventLoopGroup");
        Validate.paramNotNull(channelFactory, "channelFactory");
        this.eventLoopGroup = eventLoopGroup;
        this.channelFactory = channelFactory;
    }

    /**
     * Create an instance of {@link SdkEventLoopGroup} from the builder
     */
    private SdkEventLoopGroup(DefaultBuilder builder) {
        this.eventLoopGroup = resolveEventLoopGroup(builder);
        this.channelFactory = resolveChannelFactory();
    }

    /**
     * @return the {@link EventLoopGroup} to be used with Netty Http client.
     */
    public EventLoopGroup eventLoopGroup() {
        return eventLoopGroup;
    }

    /**
     * @return the {@link ChannelFactory} to be used with Netty Http Client.
     */
    public ChannelFactory<? extends Channel> channelFactory() {
        return channelFactory;
    }

    /**
     * Creates a new instance of SdkEventLoopGroup with {@link EventLoopGroup} and {@link ChannelFactory}
     * to be used with {@link NettyNioAsyncHttpClient}.
     *
     * @param eventLoopGroup the EventLoopGroup to be used
     * @param channelFactory the channel factor to be used
     * @return a new instance of SdkEventLoopGroup
     */
    public static SdkEventLoopGroup create(EventLoopGroup eventLoopGroup, ChannelFactory<? extends Channel> channelFactory) {
        return new SdkEventLoopGroup(eventLoopGroup, channelFactory);
    }

    /**
     * Creates a new instance of SdkEventLoopGroup with {@link EventLoopGroup}.
     *
     * <p>
     * {@link ChannelFactory} will be resolved based on the type of {@link EventLoopGroup} provided. IllegalArgumentException will
     * be thrown for any unknown EventLoopGroup type.
     *
     * @param eventLoopGroup the EventLoopGroup to be used
     * @return a new instance of SdkEventLoopGroup
     */
    public static SdkEventLoopGroup create(EventLoopGroup eventLoopGroup) {
        return create(eventLoopGroup, SocketChannelResolver.resolveSocketChannelFactory(eventLoopGroup));
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private EventLoopGroup resolveEventLoopGroup(DefaultBuilder builder) {
        int numThreads = Optional.ofNullable(builder.numberOfThreads).orElse(0);
        ThreadFactory threadFactory = Optional.ofNullable(builder.threadFactory)
                                              .orElse(new ThreadFactoryBuilder().threadNamePrefix("aws-java-sdk-NettyEventLoop")
                                                                                .build());
        return new NioEventLoopGroup(numThreads, threadFactory);
        /*
        Need to investigate why epoll is raising channel inactive after succesful response that causes
        problems with retries.

        if (Epoll.isAvailable() && isNotAwsLambda()) {
            return new EpollEventLoopGroup(numThreads, resolveThreadFactory());
        } else {

        }*/
    }

    private ChannelFactory<? extends Channel> resolveChannelFactory() {
        // Currently we only support NioEventLoopGroup
        return NioSocketChannel::new;
    }

    /**
     * A builder for {@link SdkEventLoopGroup}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.
     */
    public interface Builder {

        /**
         * Number of threads to use for the {@link EventLoopGroup}. If not set, the default
         * Netty thread count is used (which is double the number of available processors unless the io.netty.eventLoopThreads
         * system property is set.
         *
         * @param numberOfThreads Number of threads to use.
         * @return This builder for method chaining.
         */
        Builder numberOfThreads(Integer numberOfThreads);

        /**
         * {@link ThreadFactory} to create threads used by the {@link EventLoopGroup}. If not set,
         * a generic thread factory is used.
         *
         * @param threadFactory ThreadFactory to use.
         * @return This builder for method chaining.
         */
        Builder threadFactory(ThreadFactory threadFactory);

        SdkEventLoopGroup build();
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
        public SdkEventLoopGroup build() {
            return new SdkEventLoopGroup(this);
        }
    }
}
