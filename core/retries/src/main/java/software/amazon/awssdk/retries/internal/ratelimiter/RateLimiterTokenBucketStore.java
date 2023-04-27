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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ToBuilderIgnoreField;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A store to keep token buckets per scope.
 */
@SdkInternalApi
public final class RateLimiterTokenBucketStore
    implements ToCopyableBuilder<RateLimiterTokenBucketStore.Builder, RateLimiterTokenBucketStore> {
    private static final int MAX_ENTRIES = 128;
    private static final RateLimiterTokenBucketStore.Clock DEFAULT_CLOCK = new SystemClock();
    private final Map<String, RateLimiterTokenBucket> scopeToTokenBucket;
    private final RateLimiterTokenBucketStore.Clock clock;

    private RateLimiterTokenBucketStore(Builder builder) {
        this.clock = Validate.paramNotNull(builder.clock, "clock");
        this.scopeToTokenBucket = new ConcurrentHashMap<>(new LruMap<>());
    }

    public RateLimiterTokenBucket tokenBucketForScope(String scope) {
        return scopeToTokenBucket.computeIfAbsent(scope, x -> new RateLimiterTokenBucket(clock));
    }

    @Override
    @ToBuilderIgnoreField("scopeToTokenBucket")
    public Builder toBuilder() {
        return new Builder(this);
    }

    public interface Clock {
        /**
         * Returns the current time in seconds, and should include sub second resolution.
         *
         * @return the current time in seconds, and should include sub second resolution
         */
        double time();
    }

    static class SystemClock implements RateLimiterTokenBucketStore.Clock {
        @Override
        public double time() {
            // The value returned by this method is expected to
            // be in seconds with fractional value. We make the
            // conversion here.
            return System.nanoTime() / 1_000_000_000.0;
        }
    }

    /**
     * A map that limits the number of entries it holds to at most {@link RateLimiterTokenBucketStore#MAX_ENTRIES}. If the limit
     * is exceeded then the last recently used entry is removed to make room for the new one.
     */
    @SuppressWarnings("serial")
    static final class LruMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;

        LruMap() {
            super(MAX_ENTRIES, 1.0f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    }

    public static RateLimiterTokenBucketStore.Builder builder() {
        return new Builder();
    }

    public static class Builder implements CopyableBuilder<Builder, RateLimiterTokenBucketStore> {
        private RateLimiterTokenBucketStore.Clock clock;

        Builder() {
            this.clock = DEFAULT_CLOCK;
        }

        Builder(RateLimiterTokenBucketStore store) {
            this.clock = store.clock;
        }

        public Builder clock(RateLimiterTokenBucketStore.Clock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public RateLimiterTokenBucketStore build() {
            return new RateLimiterTokenBucketStore(this);
        }
    }
}
