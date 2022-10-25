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

package software.amazon.awssdk.utils.cache.lru;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;

/**
 * A thread-safe LRU (Least Recently Used) cache implementation that returns the value for a specified key,
 * retrieving it by either getting the stored value from the cache or using a supplied function to calculate that value
 * and add it to the cache.
 * <p>
 * When the cache is full, a new value will push out the least recently used value.
 * When the cache is queried for an already stored value (cache hit), this value is moved to the back of the queue
 * before it's returned so that the order of most recently used to least recently used can be maintained.
 * <p>
 * The user can configure the maximum size of the cache, which is set to a default of 100.
 * <p>
 * Null values are accepted.
 */
@SdkProtectedApi
@ThreadSafe
public final class LruCache<K, V>  {

    private static final int DEFAULT_SIZE = 100;

    private final Map<K, CacheEntry<K, V>> cache;
    private final Function<K, V> valueSupplier;
    private final Object listLock = new Object();
    private final int maxCacheSize;

    private CacheEntry<K, V> leastRecentlyUsed = null;
    private CacheEntry<K, V> mostRecentlyUsed = null;

    private LruCache(Builder<K, V> b) {
        this.valueSupplier = b.supplier;
        Integer customSize = Validate.isPositiveOrNull(b.maxSize, "size");
        this.maxCacheSize = customSize != null ? customSize : DEFAULT_SIZE;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Get a value based on the key. If the value exists in the cache, it's returned, and it's position in the cache is updated.
     * Otherwise, the value is calculated based on the supplied function {@link Builder#builder(Function)}.
     */
    public V get(K key) {
        while (true) {
            CacheEntry<K, V> cachedEntry = cache.computeIfAbsent(key, this::newEntry);
            synchronized (listLock) {
                if (cachedEntry.evicted()) {
                    continue;
                }
                moveToBackOfQueue(cachedEntry);
                return cachedEntry.value();
            }
        }
    }

    private CacheEntry<K, V> newEntry(K key) {
        V value = valueSupplier.apply(key);
        return new CacheEntry<>(key, value);
    }

    /**
     * Moves an entry to the back of the queue and sets it as the most recently used. If the entry is already the
     * most recently used, do nothing.
     * <p>
     * Summary of cache update:
     * <ol>
     * <li>Detach the entry from its current place in the double linked list.</li>
     * <li>Add it to the back of the queue (most recently used)</li>
     *</ol>
     */
    private void moveToBackOfQueue(CacheEntry<K, V> entry) {
        if (entry.equals(mostRecentlyUsed)) {
            return;
        }
        removeFromQueue(entry);
        addToQueue(entry);
    }

    /**
     * Detaches an entry from its neighbors in the cache. Remove the entry from its current place in the double linked list
     * by letting its previous neighbor point to its next neighbor, and vice versa, if those exist.
     * <p>
     * The least-recently-used and most-recently-used pointers are reset if needed.
     * <p>
     * <b>Note:</b> Detaching an entry does not delete it from the cache hash map.
     */
    private void removeFromQueue(CacheEntry<K, V> entry) {
        CacheEntry<K, V> previousEntry = entry.previous();
        if (previousEntry != null) {
            previousEntry.setNext(entry.next());
        }
        CacheEntry<K, V> nextEntry = entry.next();
        if (nextEntry != null) {
            nextEntry.setPrevious(entry.previous());
        }
        if (entry.equals(leastRecentlyUsed)) {
            leastRecentlyUsed = entry.previous();
        }
        if (entry.equals(mostRecentlyUsed)) {
            mostRecentlyUsed = entry.next();
        }
    }

    /**
     * Adds an entry to the queue as the most recently used, adjusts all pointers and triggers an evict
     * event if the cache is now full.
     */
    private void addToQueue(CacheEntry<K, V> entry) {
        if (mostRecentlyUsed != null) {
            mostRecentlyUsed.setPrevious(entry);
            entry.setNext(mostRecentlyUsed);
        }
        entry.setPrevious(null);
        mostRecentlyUsed = entry;
        if (leastRecentlyUsed == null) {
            leastRecentlyUsed = entry;
        }
        if (size() > maxCacheSize) {
            evict();
        }
    }

    /**
     * Removes the least recently used entry from the cache, marks it as evicted and removes it from the queue.
     */
    private void evict() {
        leastRecentlyUsed.isEvicted(true);
        cache.remove(leastRecentlyUsed.key());
        removeFromQueue(leastRecentlyUsed);
    }

    public int size() {
        return cache.size();
    }

    public static <K, V> LruCache.Builder<K, V> builder(Function<K, V> supplier) {
        return new Builder<>(supplier);
    }

    public static final class Builder<K, V> {

        private final Function<K, V> supplier;
        private Integer maxSize;

        private Builder(Function<K, V> supplier) {
            this.supplier = supplier;
        }

        public Builder<K, V> maxSize(Integer maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public LruCache<K, V> build() {
            return new LruCache<>(this);
        }
    }

    private static final class CacheEntry<K, V> {

        private final K key;
        private final V value;

        private boolean evicted = false;

        private CacheEntry<K, V> previous;
        private CacheEntry<K, V> next;

        private CacheEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        K key() {
            return key;
        }

        V value() {
            return value;
        }

        boolean evicted() {
            return evicted;
        }

        void isEvicted(boolean evicted) {
            this.evicted = evicted;
        }

        CacheEntry<K, V> next() {
            return next;
        }

        void setNext(CacheEntry<K, V> next) {
            this.next = next;
        }

        CacheEntry<K, V> previous() {
            return previous;
        }

        void setPrevious(CacheEntry<K, V> previous) {
            this.previous = previous;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || getClass() != o.getClass()) {
                return false;
            }
            CacheEntry<?, ?> that = (CacheEntry<?, ?>) o;
            return Objects.equals(key, that.key)
                   && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }
    }
}
