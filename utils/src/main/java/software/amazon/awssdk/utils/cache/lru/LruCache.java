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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;

/**
 * An LRU (Least Recently Used) cache implementation.
 * <p>
 * The user can configure the maximum size of the cache, which is set to a default of 100.
 */
@SdkProtectedApi
public final class LruCache<K, V>  {

    private static final int DEFAULT_SIZE = 100;

    private final Map<K, CacheEntry<K, V>> cache;
    private final Function<K, V> valueSupplier;

    private CacheEntry<K, V> leastRecentlyUsed = null;
    private CacheEntry<K, V> mostRecentlyUsed = null;
    private final Lock listLock = new ReentrantLock();
    private final int maxCacheSize;

    private LruCache(Builder<K, V> b) {
        this.valueSupplier = b.supplier;
        Integer customSize = Validate.isPositiveOrNull(b.maxSize, "size");
        this.maxCacheSize = customSize != null ? customSize : DEFAULT_SIZE;
        this.cache = new ConcurrentHashMap<>();
    }

    public V get(K key) {
        CacheEntry<K, V> cachedEntry = cache.get(key);
        if (cachedEntry != null) {
            moveToFront(cachedEntry);
            return cachedEntry.value();
        }
        V value = valueSupplier.apply(key);
        CacheEntry<K, V> newEntry = new CacheEntry<>(key, value);
        addToFront(newEntry);
        return newEntry.value();
    }

    /**
     * Sets an existing entry as the most recently used.
     *
     * 1) Remove the entry from its current place in the double linked list by letting its previous neighbor point to its
     *    next neighbor, and vice versa.
     * 2) This is the new most recently used item so there is no previous entry. Point to the most recently used entry.
     * 3) Set the most recently used pointer to point to the new entry.
     *
     */
    private void moveToFront(CacheEntry<K, V> entry) {
        if (entry.equals(mostRecentlyUsed)) {
            return;
        }

        boolean lockAcquired = listLock.tryLock();

        try {
            remove(entry);
            entry.setPrevious(null);
            entry.setNext(mostRecentlyUsed);
            mostRecentlyUsed.setPrevious(entry);
            mostRecentlyUsed = entry;
        } finally {
            if (lockAcquired) {
                listLock.unlock();
            }
        }
    }

    /**
     * Removes an entry from the cache. Remove the entry from its current place in the double linked list
     * by letting its previous neighbor point to its next neighbor, and vice versa, if those exist.
     */
    private void remove(CacheEntry<K, V> entry) {
        CacheEntry<K, V> previousEntry = entry.previous();
        if (previousEntry != null) {
            previousEntry.setNext(entry.next());
        }
        CacheEntry<K, V> nextEntry = entry.next();
        if (nextEntry != null) {
            nextEntry.setPrevious(entry.previous());
        }
    }

    /**
     * Adds an entry to the cache and sets the most recently used pointer to the new entry.
     */
    private void addToFront(CacheEntry<K, V> newEntry) {
        boolean lockAcquired = listLock.tryLock();
        try {
            if (mostRecentlyUsed != null) {
                newEntry.setNext(mostRecentlyUsed);
                mostRecentlyUsed.setPrevious(newEntry);
            }
            mostRecentlyUsed = newEntry;
            if (leastRecentlyUsed == null) {
                leastRecentlyUsed = newEntry;
            }
            if (size() >= maxCacheSize) {
                evict();
            }
            cache.put(newEntry.key(), newEntry);
        } finally {
            if (lockAcquired) {
                listLock.unlock();
            }
        }
    }

    /**
     * Removes the least recently used entry from the cache.
     *
     * The pointer to the least recently used entry is updated to point to the next-but-last entry.
     * The next pointer of the new least recently used entry is updated to null, since it's last.
     *
     * @return The value of the evicted entry
     */
    private V evict() {
        CacheEntry<K, V> evictedEntry = leastRecentlyUsed;
        leastRecentlyUsed = evictedEntry.previous();
        leastRecentlyUsed.setNext(null);
        evictedEntry.setPrevious(null);
        cache.remove(evictedEntry.key());
        return evictedEntry.value();
    }

    public Integer size() {
        return cache.size();
    }

    public static <K, V> LruCache.Builder<K, V> builder(Function<K, V> supplier) {
        return new Builder<>(supplier);
    }

    public static final class Builder<K, V> {

        private final Function<K, V> supplier;
        private int maxSize;

        private Builder(Function<K, V> supplier) {
            this.supplier = supplier;
        }

        public Builder<K, V> maxSize(int maxSize) {
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
