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
import java.util.Optional;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration object to customize the Netty {@link EventLoopGroup}. Either an already constructed {@link EventLoopGroup}
 * or an {@link EventLoopGroupFactory} may be provided, but not both.
 */
@ReviewBeforeRelease("Do we want this approach to force mutual exclusion or should we flatten it out" +
                     " and do mutex checks at runtime?")
public final class EventLoopGroupConfiguration
        implements ToCopyableBuilder<EventLoopGroupConfiguration.Builder, EventLoopGroupConfiguration> {

    private final EventLoopGroup eventLoopGroup;
    private final EventLoopGroupFactory eventLoopGroupFactory;

    private EventLoopGroupConfiguration(DefaultBuilder builder) {
        this.eventLoopGroup = builder.eventLoopGroup;
        this.eventLoopGroupFactory = builder.eventLoopGroupFactory;
    }

    /**
     * @return The currently configured {@link EventLoopGroup} or an empty {@link Optional} if not present.
     */
    public Optional<EventLoopGroup> eventLoopGroup() {
        return Optional.ofNullable(eventLoopGroup);
    }

    /**
     * @return The currently configured {@link EventLoopGroupFactory} or an empty {@link Optional} if not present.
     */
    public Optional<EventLoopGroupFactory> eventLoopGroupFactory() {
        return Optional.ofNullable(eventLoopGroupFactory);
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder()
                .eventLoopGroup(eventLoopGroup)
                .eventLoopGroupFactory(eventLoopGroupFactory);
    }

    /**
     * Transforms this configuration into an {@link Either} of {@link EventLoopGroup} and {@link EventLoopGroupFactory} to
     * ease resolution of the Netty event loop group. Returns an empty {@link Optional} if neither is set.
     */
    @SdkInternalApi
    Optional<Either<EventLoopGroup, EventLoopGroupFactory>> toEither() {
        return Either.fromNullable(eventLoopGroup, eventLoopGroupFactory);
    }

    /**
     * @return Builder instance to construct a {@link EventLoopGroupConfiguration}.
     */
    public static EventLoopGroupConfiguration.Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * A builder for {@link EventLoopGroupConfiguration}.
     */
    public interface Builder extends CopyableBuilder<Builder, EventLoopGroupConfiguration> {

        /**
         * Sets the {@link EventLoopGroup} to use for the Netty HTTP client. This event loop group may be shared
         * across multiple HTTP clients for better resource and thread utilization. The preferred way to create
         * an {@link EventLoopGroup} is by using the {@link EventLoopGroupFactory#create()} method which will choose the
         * optimal implementation per the platform.
         *
         * <p>The {@link EventLoopGroup} <b>MUST</b> be closed by the caller when it is ready to
         * be disposed. The SDK will not close the {@link EventLoopGroup} when the HTTP client is closed. See
         * {@link EventLoopGroup#shutdownGracefully()} to properly close the event loop group.</p>
         *
         * <p>This configuration method is only recommended when you wish to share an {@link EventLoopGroup}
         * with multiple clients. If you do not need to share the group it is recommended to use
         * {@link #eventLoopGroupFactory(EventLoopGroupFactory)} as the SDK will handle its cleanup when
         * the HTTP client is closed.</p>
         *
         * @param eventLoopGroup Netty {@link EventLoopGroup} to use.
         * @return This builder for method chaining.
         * @see DefaultEventLoopGroupFactory
         */
        // This intentionally returns SdkBuilder so that only eventLoopGroup or eventLoopGroupFactory may be supplied.
        SdkBuilder<?, EventLoopGroupConfiguration> eventLoopGroup(EventLoopGroup eventLoopGroup);

        /**
         * Sets the {@link EventLoopGroupFactory} which will be used to create the {@link EventLoopGroup} for the Netty
         * HTTP client. This allows for custom configuration of the Netty {@link EventLoopGroup}.
         *
         * <p>The {@link EventLoopGroup} created by the factory is managed by the SDK and will be shutdown
         * when the HTTP client is closed.</p>
         *
         * <p>This is the preferred configuration method when you just want to customize the {@link EventLoopGroup}
         * but not share it across multiple HTTP clients. If you do wish to share an {@link EventLoopGroup}, see
         * {@link #eventLoopGroup(EventLoopGroup)}</p>
         *
         * @param eventLoopGroupFactory {@link EventLoopGroupFactory} to use.
         * @return This builder for method chaining.
         * @see DefaultEventLoopGroupFactory
         */
        // This intentionally returns SdkBuilder so that only eventLoopGroup or eventLoopGroupFactory may be supplied.
        SdkBuilder<?, EventLoopGroupConfiguration> eventLoopGroupFactory(EventLoopGroupFactory eventLoopGroupFactory);

    }

    private static final class DefaultBuilder implements Builder {

        private EventLoopGroup eventLoopGroup;
        private EventLoopGroupFactory eventLoopGroupFactory;

        private DefaultBuilder() {
        }

        @Override
        public DefaultBuilder eventLoopGroup(EventLoopGroup eventLoopGroup) {
            this.eventLoopGroup = eventLoopGroup;
            return this;
        }

        public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
            eventLoopGroup(eventLoopGroup);
        }

        @Override
        public DefaultBuilder eventLoopGroupFactory(EventLoopGroupFactory eventLoopGroupFactory) {
            this.eventLoopGroupFactory = eventLoopGroupFactory;
            return this;
        }

        public void setEventLoopGroupFactory(EventLoopGroupFactory eventLoopGroupFactory) {
            eventLoopGroupFactory(eventLoopGroupFactory);
        }

        @Override
        public EventLoopGroupConfiguration build() {
            return new EventLoopGroupConfiguration(this);
        }
    }
}
