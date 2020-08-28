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

package software.amazon.awssdk.http.crt;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.utils.Validate;

/**
 * Provides {@link EventLoopGroup} for {@link AwsCrtAsyncHttpClient}.
 * <p>
 * There are three ways to create a new instance.
 *
 * <ul>
 * <li>using {@link #builder()} to provide custom configuration of {@link EventLoopGroup}.
 * This is the preferred configuration method when you just want to customize the {@link EventLoopGroup}</li>
 *
 * <li>Using {@link #create(EventLoopGroup)} to provide a custom {@link EventLoopGroup}
 * </ul>
 *
 * <p>
 * When configuring the {@link EventLoopGroup} of {@link AwsCrtAsyncHttpClient}, if {@link Builder} is
 * passed to {@link AwsCrtAsyncHttpClient.Builder#eventLoopGroupBuilder},
 * the {@link EventLoopGroup} is managed by the SDK and will be shutdown when the HTTP client is closed. Otherwise,
 * if an instance of {@link SdkEventLoopGroup} is passed to {@link AwsCrtAsyncHttpClient.Builder#eventLoopGroup},
 * the {@link EventLoopGroup} <b>MUST</b> be closed by the caller when it is ready to be disposed. The SDK will not
 * close the {@link EventLoopGroup} when the HTTP client is closed. See {@link EventLoopGroup#close()} ()} to
 * properly close the event loop group.
 *
 * @see AwsCrtAsyncHttpClient.Builder#eventLoopGroupBuilder(Builder)
 * @see AwsCrtAsyncHttpClient.Builder#eventLoopGroup(SdkEventLoopGroup)
 */
@SdkPublicApi
public final class SdkEventLoopGroup {

    private final EventLoopGroup eventLoopGroup;

    SdkEventLoopGroup(EventLoopGroup eventLoopGroup) {
        Validate.paramNotNull(eventLoopGroup, "eventLoopGroup");
        this.eventLoopGroup = eventLoopGroup;
    }

    /**
     * Create an instance of {@link SdkEventLoopGroup} from the builder
     */
    private SdkEventLoopGroup(DefaultBuilder builder) {
        Validate.isPositiveOrNull(builder.numberOfThreads, "numOfThreads");
        this.eventLoopGroup = resolveEventLoopGroup(builder);
    }

    /**
     * @return the {@link EventLoopGroup} to be used with Netty Http client.
     */
    public EventLoopGroup eventLoopGroup() {
        return eventLoopGroup;
    }

    /**
     * Creates a new instance of SdkEventLoopGroup with {@link EventLoopGroup}
     * to be used with {@link AwsCrtAsyncHttpClient}.
     *
     * @param eventLoopGroup the EventLoopGroup to be used
     * @return a new instance of SdkEventLoopGroup
     */
    public static SdkEventLoopGroup create(EventLoopGroup eventLoopGroup) {
        return new SdkEventLoopGroup(eventLoopGroup);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private EventLoopGroup resolveEventLoopGroup(DefaultBuilder builder) {
        int numThreads = builder.numberOfThreads != null ? builder.numberOfThreads : Runtime.getRuntime().availableProcessors();
        return new EventLoopGroup(numThreads);
    }

    /**
     * A builder for {@link SdkEventLoopGroup}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.
     */
    public interface Builder {

        /**
         * Number of threads to use for the {@link EventLoopGroup}.
         *
         * @param numberOfThreads Number of threads to use.
         * @return This builder for method chaining.
         */
        Builder numberOfThreads(Integer numberOfThreads);

        SdkEventLoopGroup build();
    }

    public static final class DefaultBuilder implements Builder {

        private Integer numberOfThreads;

        private DefaultBuilder() {
        }

        @Override
        public Builder numberOfThreads(Integer numberOfThreads) {
            this.numberOfThreads = numberOfThreads;
            return this;
        }

        @Override
        public SdkEventLoopGroup build() {
            return new SdkEventLoopGroup(this);
        }
    }
}
