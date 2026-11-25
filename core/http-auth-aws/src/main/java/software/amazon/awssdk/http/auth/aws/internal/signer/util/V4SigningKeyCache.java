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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;

/**
 * Bounded cache for derived SigV4 signing keys, keyed on the (secret access key, region, service) triple. An entry is
 * valid for the UTC day it was derived on. When the cache is full, the oldest entry is evicted first.
 *
 * <p>Lookups use a {@link CacheKey} with a precomputed hash code, so they do not build a string key per request.
 *
 * <p>The cache copies signing keys on the way in and on the way out, so no caller can modify a cached entry. A modified
 * entry would break every signature for its secret, region, and service in the process until it expired.
 */
@ThreadSafe
@SdkInternalApi
public final class V4SigningKeyCache {

    /** Default max number of (secret, region, service) → signingKey entries to retain. */
    private static final int DEFAULT_MAX_SIZE = 300;
    private static final long SECONDS_PER_DAY = 86_400L;

    /**
     * Process-wide cache used by {@link SignerUtils#deriveSigningKey}.
     */
    private static final V4SigningKeyCache SHARED = new V4SigningKeyCache(DEFAULT_MAX_SIZE);

    private final LinkedHashMap<CacheKey, CacheEntry> store;
    private final StampedLock lock = new StampedLock();
    private final int maxSize;

    public V4SigningKeyCache(int maxSize) {
        this.maxSize = Validate.isPositive(maxSize, "maxSize");
        this.store = new LinkedHashMap<CacheKey, CacheEntry>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheEntry> eldest) {
                return size() > V4SigningKeyCache.this.maxSize;
            }
        };
    }

    /**
     * Look up a cached signing key in the shared cache. Returns a copy, or {@code null} if missing or stale.
     */
    public static byte[] sharedGet(CacheKey key, Instant signingInstant) {
        return SHARED.get(key, signingInstant);
    }

    /**
     * Insert a copy of a freshly-derived signing key into the shared cache.
     */
    public static void sharedPut(CacheKey key, byte[] signingKey, Instant signingInstant) {
        SHARED.put(key, signingKey, signingInstant);
    }

    /**
     * Look up a cached signing key. Returns {@code null} when the key is missing or stale (different UTC day from
     * {@code signingInstant}). The returned array is a copy that the caller owns.
     */
    public byte[] get(CacheKey key, Instant signingInstant) {
        CacheEntry entry;
        long stamp = lock.readLock();
        try {
            entry = store.get(key);
        } finally {
            lock.unlockRead(stamp);
        }
        if (entry == null || entry.daysSinceEpoch != daysSinceEpoch(signingInstant)) {
            return null;
        }
        return entry.signingKey.clone();
    }

    /**
     * Insert a copy of a freshly-derived signing key. The eldest entry is evicted if the cache is at capacity.
     */
    public void put(CacheKey key, byte[] signingKey, Instant signingInstant) {
        CacheEntry entry = new CacheEntry(signingKey.clone(), daysSinceEpoch(signingInstant));
        long stamp = lock.writeLock();
        try {
            store.put(key, entry);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns the number of the UTC day containing {@code instant}, counted from 1970-01-01. Rounds down, so instants
     * before the epoch land on negative days instead of sharing day 0 with 1970-01-01.
     */
    private static long daysSinceEpoch(Instant instant) {
        return Math.floorDiv(instant.getEpochSecond(), SECONDS_PER_DAY);
    }

    /**
     * Composite cache key. Holds references rather than copies because the underlying values come from
     * {@link software.amazon.awssdk.identity.spi.AwsCredentialsIdentity}, which is immutable.
     *
     * <p>This class must never get a {@code toString()}: it holds the secret access key, which must not appear in logs,
     * exception messages, or debugger output.
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
            this.hashCode = computeHashCode();
        }

        private int computeHashCode() {
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
