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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ToBuilderIgnoreField;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.lru.LruCache;

/**
 * A store to keep token buckets per scope.
 */
@SdkInternalApi
public final class RateLimiterTokenBucketStore
    implements ToCopyableBuilder<RateLimiterTokenBucketStore.Builder, RateLimiterTokenBucketStore> {
    private static final int MAX_ENTRIES = 128;
    private static final RateLimiterClock DEFAULT_CLOCK = new SystemClock();
    private final LruCache<String, RateLimiterTokenBucket> scopeToTokenBucket;
    private final RateLimiterClock clock;

    private RateLimiterTokenBucketStore(Builder builder) {
        this.clock = Validate.paramNotNull(builder.clock, "clock");
        this.scopeToTokenBucket = LruCache.<String, RateLimiterTokenBucket>builder(x -> new RateLimiterTokenBucket(clock))
                                          .maxSize(MAX_ENTRIES)
                                          .build();
    }

    public RateLimiterTokenBucket tokenBucketForScope(String scope) {
        return scopeToTokenBucket.get(scope);
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

        Builder() {
            this.clock = DEFAULT_CLOCK;
        }

        Builder(RateLimiterTokenBucketStore store) {
            this.clock = store.clock;
        }

        public Builder clock(RateLimiterClock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public RateLimiterTokenBucketStore build() {
            return new RateLimiterTokenBucketStore(this);
        }
    }
}
