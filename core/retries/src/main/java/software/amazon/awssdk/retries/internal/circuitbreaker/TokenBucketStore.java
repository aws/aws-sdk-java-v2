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

package software.amazon.awssdk.retries.internal.circuitbreaker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * A store to keep token buckets per scope.
 */
@SdkInternalApi
public final class TokenBucketStore {
    private static final int DEFAULT_MAX_TOKENS = 500;
    private static final int MAX_ENTRIES = 128;
    private final int tokenBucketMaxCapacity;
    private final Map<String, TokenBucket> scopeToTokenBucket;

    @SuppressWarnings("serial")
    private TokenBucketStore(Builder builder) {
        this.tokenBucketMaxCapacity = builder.tokenBucketMaxCapacity;
        this.scopeToTokenBucket = new ConcurrentHashMap<>(new LruMap<>());
    }

    /**
     * Returns the {@link TokenBucket} for the given scope.
     */
    public TokenBucket tokenBucketForScope(String scope) {
        Validate.paramNotNull(scope, "scope");
        return scopeToTokenBucket.computeIfAbsent(scope,
                                                  key -> new TokenBucket(tokenBucketMaxCapacity));
    }

    /**
     * Returns a new builder to create a new store.
     */
    public static TokenBucketStore.Builder builder() {
        return new Builder();
    }

    /**
     * A map that limits the number of entries it holds to at most {@link TokenBucketStore#MAX_ENTRIES}. If the limit is exceeded
     * then the last recently used entry is removed to make room for the new one.
     */
    @SuppressWarnings("serial")
    static final class LruMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 885024284016559479L;

        LruMap() {
            super(MAX_ENTRIES, 1.0f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    }

    public static class Builder {
        private int tokenBucketMaxCapacity;

        Builder() {
            tokenBucketMaxCapacity = DEFAULT_MAX_TOKENS;
        }

        Builder(TokenBucketStore store) {
            this.tokenBucketMaxCapacity = store.tokenBucketMaxCapacity;
        }

        public Builder tokenBucketMaxCapacity(int tokenBucketMaxCapacity) {
            this.tokenBucketMaxCapacity = tokenBucketMaxCapacity;
            return this;
        }

        public TokenBucketStore build() {
            return new TokenBucketStore(this);
        }
    }
}
