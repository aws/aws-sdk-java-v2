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

package software.amazon.awssdk.retries.internal.ratelimiter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ToBuilderIgnoreField;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.lru.LruCache;

/**
 * A store to keep token buckets per scope.
 */
@SdkInternalApi
public final class RateLimiterTokenBucketStore
    implements ToCopyableBuilder<RateLimiterTokenBucketStore.Builder, RateLimiterTokenBucketStore>, SdkAutoCloseable {
    private static final int MAX_ENTRIES = 128;
    private static final String THREAD_NAME_PREFIX = "sdk-adaptive-rate-limiter-";

    private static final RateLimiterClock DEFAULT_CLOCK = new SystemClock();
    private final LruCache<String, RateLimiterTokenBucket> scopeToTokenBucket;
    private final RateLimiterClock clock;
    private final ScheduledExecutorService scheduler;
    private final boolean closeScheduler;

    private RateLimiterTokenBucketStore(Builder builder) {
        this(builder.clock,
             resolveScheduler(builder),
             builder.scheduler == null);
    }

    private RateLimiterTokenBucketStore(RateLimiterClock clock, ScheduledExecutorService scheduler, boolean closeScheduler) {
        this.clock = Validate.paramNotNull(clock, "clock");
        this.scheduler = Validate.paramNotNull(scheduler, "scheduler");
        this.closeScheduler = closeScheduler;
        this.scopeToTokenBucket = LruCache.<String, RateLimiterTokenBucket>builder(
                                              x -> new RateLimiterTokenBucket(clock, scheduler))
                                          .maxSize(MAX_ENTRIES)
                                          .build();
    }

    @Override
    public void close() {
        scopeToTokenBucket.evictAll();
        if (closeScheduler) {
            scheduler.shutdownNow();
        }
    }

    public RateLimiterTokenBucket tokenBucketForScope(String scope) {
        return scopeToTokenBucket.get(scope);
    }

    @SdkTestInternalApi
    ScheduledExecutorService scheduler() {
        return scheduler;
    }

    private static ScheduledExecutorService resolveScheduler(Builder b) {
        if (b.scheduler != null) {
            return b.scheduler;
        }
        return Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                                                              .daemonThreads(true)
                                                              .threadNamePrefix(THREAD_NAME_PREFIX)
                                                              .build());
    }

    @Override
    @ToBuilderIgnoreField("scopeToTokenBucket")
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static RateLimiterTokenBucketStore.Builder builder() {
        return new Builder();
    }

    public static class Builder implements CopyableBuilder<Builder, RateLimiterTokenBucketStore> {
        private RateLimiterClock clock;
        private ScheduledExecutorService scheduler;

        Builder() {
            this.clock = DEFAULT_CLOCK;
        }

        Builder(RateLimiterTokenBucketStore store) {
            this.clock = store.clock;
            this.scheduler = store.scheduler;
        }

        public Builder clock(RateLimiterClock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * The scheduler used by the {@link RateLimiterTokenBucket rate limter buckets} to perform async notifications.
         * The configured scheduler <strong>will not</strong> be closed when {@link #close() closing} this bucket store.
         *
         * @return This object for method chaining.
         */
        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        @Override
        public RateLimiterTokenBucketStore build() {
            return new RateLimiterTokenBucketStore(this);
        }
    }
}
