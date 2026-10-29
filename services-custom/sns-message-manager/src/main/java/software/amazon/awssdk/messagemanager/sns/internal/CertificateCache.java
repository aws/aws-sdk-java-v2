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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.net.URI;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;

/**
 * Bounded cache of validated SNS signing certificates, keyed by the certificate's content.
 * <p>
 * Certificates are stored once per fingerprint (the SHA-256 of the DER encoding). A second, independently bounded map
 * remembers which fingerprint each URL last resolved to; it is a hint that lets the caller try the most likely certificate
 * first and skip a fetch for a URL that is already known.
 * <p>
 * Both maps are least-recently-used; every method holds one lock for the few map operations it performs and does no I/O
 * or cryptography.
 */
@SdkInternalApi
@ThreadSafe
final class CertificateCache {
    private final Object lock = new Object();
    private final LinkedHashMap<String, PublicKey> byFingerprint;
    private final LinkedHashMap<URI, String> fingerprintByUrl;

    CertificateCache(int maxCertificates, int maxUrlHints) {
        Validate.isPositive(maxCertificates, "maxCertificates");
        Validate.isPositive(maxUrlHints, "maxUrlHints");
        this.byFingerprint = new LruMap<>(maxCertificates);
        this.fingerprintByUrl = new LruMap<>(maxUrlHints);
    }

    /**
     * The cached certificates a signature naming {@code url} should be verified against: the one last fetched from this URL
     * first, if it is still cached, followed by every other cached certificate. Returns a snapshot that is never null.
     */
    List<PublicKey> candidates(URI url) {
        synchronized (lock) {
            List<PublicKey> result = new ArrayList<>(byFingerprint.size());
            PublicKey hinted = lookupLocked(url);
            if (hinted != null) {
                result.add(hinted);
            }
            for (PublicKey key : byFingerprint.values()) {
                if (key != hinted) {
                    result.add(key);
                }
            }
            return result;
        }
    }

    /**
     * The certificate last fetched from {@code url}, or null if this URL has no hint or the certificate it pointed at is no
     * longer cached. A stale hint is dropped.
     */
    PublicKey lookup(URI url) {
        synchronized (lock) {
            return lookupLocked(url);
        }
    }

    /**
     * Inserts the certificate unless one with the same fingerprint is already cached, records that {@code url} resolved to
     * this fingerprint, and returns the cached instance (the existing one or the one just inserted).
     */
    PublicKey putIfAbsent(URI url, String fingerprint, PublicKey publicKey) {
        synchronized (lock) {
            PublicKey cached = byFingerprint.get(fingerprint);
            if (cached == null) {
                byFingerprint.put(fingerprint, publicKey);
                cached = publicKey;
            }
            fingerprintByUrl.put(url, fingerprint);
            return cached;
        }
    }

    /**
     * The number of distinct certificates currently cached.
     */
    int size() {
        synchronized (lock) {
            return byFingerprint.size();
        }
    }

    private PublicKey lookupLocked(URI url) {
        String fingerprint = fingerprintByUrl.get(url);
        if (fingerprint == null) {
            return null;
        }
        PublicKey key = byFingerprint.get(fingerprint);
        if (key == null) {
            fingerprintByUrl.remove(url);
        }
        return key;
    }

    private static final class LruMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int maxSize;

        private LruMap(int maxSize) {
            super(16, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}
