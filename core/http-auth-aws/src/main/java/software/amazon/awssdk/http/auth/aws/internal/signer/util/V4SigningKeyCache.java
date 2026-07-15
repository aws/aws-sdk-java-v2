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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Bounded cache for derived SigV4 signing keys. The cache is keyed on the (secret-access-key, region, service)
 * triple, with the entry valid for a single UTC day. The previous {@code FifoCache<SignerKey>} implementation built
 * a fresh {@code String} key per lookup; this one uses an immutable {@link CacheKey} with a precomputed hashCode so
 * lookups don't allocate.
 */
@ThreadSafe
@SdkInternalApi
public final class V4SigningKeyCache {

    /** Default max number of (secret, region, service) → signingKey entries to retain. */
    private static final int DEFAULT_MAX_SIZE = 300;

    /**
     * Shared process-wide cache used by both the legacy and fast SigV4 signing paths. Centralizing here means cached
     * entries derived by one path are reused by the other.
     */
    private static final V4SigningKeyCache SHARED = new V4SigningKeyCache(DEFAULT_MAX_SIZE);

    private final LinkedHashMap<CacheKey, CacheEntry> store;
    private final StampedLock lock = new StampedLock();
    private final int maxSize;

    public V4SigningKeyCache(int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize " + maxSize + " must be at least 1");
        }
        this.maxSize = maxSize;
        this.store = new LinkedHashMap<CacheKey, CacheEntry>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheEntry> eldest) {
                return size() > V4SigningKeyCache.this.maxSize;
            }
        };
    }

    /**
     * Look up a cached signing key in the shared cache. Returns {@code null} if missing or stale.
     */
    public static byte[] sharedGet(CacheKey key, Instant signingInstant) {
        return SHARED.get(key, signingInstant);
    }

    /**
     * Insert a freshly-derived signing key into the shared cache.
     */
    public static void sharedPut(CacheKey key, byte[] signingKey, Instant signingInstant) {
        SHARED.put(key, signingKey, signingInstant);
    }

    /**
     * Look up a cached signing key. Returns {@code null} when the key is missing or stale (different UTC day from
     * {@code signingInstant}).
     *
     * <p>The returned {@code byte[]} is shared with the cache; callers must not mutate it.
     */
    public byte[] get(CacheKey key, Instant signingInstant) {
        long stamp = lock.readLock();
        try {
            CacheEntry entry = store.get(key);
            if (entry == null) {
                return null;
            }
            return entry.daysSinceEpoch == daysSinceEpoch(signingInstant) ? entry.signingKey : null;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Insert a freshly-derived signing key. The eldest entry is evicted if the cache is at capacity.
     */
    public void put(CacheKey key, byte[] signingKey, Instant signingInstant) {
        CacheEntry entry = new CacheEntry(signingKey, daysSinceEpoch(signingInstant));
        long stamp = lock.writeLock();
        try {
            store.put(key, entry);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private static long daysSinceEpoch(Instant instant) {
        return Instant.EPOCH.until(instant, ChronoUnit.DAYS);
    }

    /**
     * Composite cache key. Holds references rather than copies because the underlying values come from
     * {@link software.amazon.awssdk.identity.spi.AwsCredentialsIdentity}, which is immutable.
     */
    public static final class CacheKey {
        private final String secretKey;
        private final String region;
        private final String service;
        private final int hashCode;

        public CacheKey(String secretKey, String region, String service) {
            this.secretKey = secretKey;
            this.region = region;
            this.service = service;
            this.hashCode = computeHashCode(secretKey, region, service);
        }

        private static int computeHashCode(String secretKey, String region, String service) {
            int result = 1;
            result = 31 * result + secretKey.hashCode();
            result = 31 * result + region.hashCode();
            result = 31 * result + service.hashCode();
            return result;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return hashCode == other.hashCode
                   && secretKey.equals(other.secretKey)
                   && region.equals(other.region)
                   && service.equals(other.service);
        }
    }

    private static final class CacheEntry {
        final byte[] signingKey;
        final long daysSinceEpoch;

        CacheEntry(byte[] signingKey, long daysSinceEpoch) {
            this.signingKey = signingKey;
            this.daysSinceEpoch = daysSinceEpoch;
        }
    }
}
