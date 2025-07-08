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

package software.amazon.awssdk.utils.cache.bounded;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;

/**
 * A thread-safe cache implementation that returns the value for a specified key, retrieving it by either getting the stored
 * value from the cache or using a supplied function to calculate that value and add it to the cache.
 * <p>
 * When the cache is full, batch eviction of random values will be performed, with a default evictionBatchSize of 10.
 * <p>
 * The user can configure the maximum size of the cache, which is set to a default of 150.
 * <p>
 * Keys must not be null, otherwise an error will be thrown. Null values are not cached.
 */
@SdkProtectedApi
@ThreadSafe
public final class BoundedCache<K, V>  {
    private static final int DEFAULT_CACHE_SIZE = 150;
    private static final int DEFAULT_EVICTION_BATCH_SIZE = 10;

    private final ConcurrentHashMap<K, V> cache;
    private final Function<K, V> valueMappingFunction;
    private final int maxCacheSize;
    private final int evictionBatchSize;
    private final Object cacheLock;
    private final AtomicInteger cacheSize;

    private BoundedCache(Builder<K, V> b) {
        this.valueMappingFunction = b.mappingFunction;
        this.maxCacheSize = b.maxSize != null ? Validate.isPositive(b.maxSize, "maxSize") : DEFAULT_CACHE_SIZE;
        this.evictionBatchSize = b.evictionBatchSize != null ?
                                 Validate.isPositive(b.evictionBatchSize, "evictionBatchSize") :
                                 DEFAULT_EVICTION_BATCH_SIZE;
        this.cache = new ConcurrentHashMap<>();
        this.cacheLock = new Object();
        this.cacheSize = new AtomicInteger();
    }

    /**
     * Get a value based on the key. The key must not be null, otherwise an error is thrown.
     * If the value exists in the cache, it's returned.
     * Otherwise, the value is calculated based on the supplied function {@link Builder#builder(Function)}.
     */
    public V get(K key) {
        Validate.paramNotNull(key, "key");
        V value = cache.get(key);
        if (value != null) {
            return value;
        }

        V newValue = valueMappingFunction.apply(key);

        // If the value is null, just return it without caching
        if (newValue == null) {
            return null;
        }

        synchronized (cacheLock) {
            // Check again inside the synchronized block in case another thread added the value
            value = cache.get(key);
            if (value != null) {
                return value;
            }

            if (cacheSize.get() >= maxCacheSize) {
                cleanup();
            }

            cache.put(key, newValue);
            cacheSize.incrementAndGet();
            return newValue;
        }
    }

    /**
     * Clean up the cache by batch removing random entries of evictionBatchSize
     */
    private void cleanup() {
        Iterator<K> iterator = cache.keySet().iterator();
        int count = 0;
        while (iterator.hasNext() && count < evictionBatchSize) {
            iterator.next();
            iterator.remove();
            count++;
            cacheSize.decrementAndGet();
        }
    }

    public int size() {
        return cacheSize.get();
    }

    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    public static <K, V> BoundedCache.Builder<K, V> builder(Function<K, V> mappingFunction) {
        return new Builder<>(mappingFunction);
    }

    public static final class Builder<K, V> {

        private final Function<K, V> mappingFunction;
        private Integer maxSize;
        private Integer evictionBatchSize;

        private Builder(Function<K, V> mappingFunction) {
            this.mappingFunction = mappingFunction;
        }

        public Builder<K, V> maxSize(Integer maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder<K, V> evictionBatchSize(Integer evictionBatchSize) {
            this.evictionBatchSize = evictionBatchSize;
            return this;
        }

        public BoundedCache<K, V> build() {
            return new BoundedCache<>(this);
        }
    }
}