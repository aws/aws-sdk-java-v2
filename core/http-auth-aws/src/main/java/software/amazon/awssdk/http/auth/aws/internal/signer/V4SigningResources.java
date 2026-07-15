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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.Mac;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Per-signing scratch state for the SigV4 fast path. Borrow with {@link #acquire()} at the start of a signing call,
 * return with {@link #release(V4SigningResources)} at the end. The instance is reset on acquire so callers always see a
 * fresh state.
 *
 * <p>What lives here:
 * <ul>
 *     <li>A {@link MessageDigest} (SHA-256) and {@link Mac} (HmacSHA256), instantiated once per pool entry and
 *         {@code reset()}/{@code init(...)}-ed between uses to avoid {@code Mac.getInstance} on every sign.</li>
 *     <li>Reusable {@code byte[]} buffers for the canonical request, the string-to-sign, the body and signature
 *         digests, and the hex-encoded signature.</li>
 *     <li>A reusable {@link StringBuilder} for incremental text composition (auth header, scope, dates).</li>
 *     <li>Strided {@code String[]} arrays for headers and query pairs. Even slots hold the name (lowercased), odd
 *         slots hold the value. Sorted in-place by an insertion sort, which is fast for the small N typical of HTTP
 *         requests.</li>
 *     <li>A reusable read buffer for streaming body hashing.</li>
 * </ul>
 *
 * <p>Modeled on smithy-java's {@code SigningResources}. The semantics here (Java 8 source level, V2 SDK ignore-list,
 * etc.) are tailored to match the existing V2 signer's byte-level output.
 */
@SdkInternalApi
final class V4SigningResources {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final int BUFFER_SIZE = 512;
    private static final int POOL_SIZE = 32;
    private static final int INITIAL_HEADER_CAPACITY = 16;
    private static final int READ_BUFFER_SIZE = 8192;

    private static final Pool<V4SigningResources> POOL = new Pool<>(POOL_SIZE, V4SigningResources::new);

    /** Reusable text builder. Reset before each use; kept under {@link #BUFFER_SIZE} on release. */
    final StringBuilder sb;

    /** Pooled SHA-256 digest. {@code reset()} between uses. */
    final MessageDigest sha256Digest;

    /** Pooled HmacSHA256. {@code reset()} + {@code init(key)} between uses. */
    final Mac sha256Mac;

    /** Strided header buffer: even indices hold name (lowercase), odd indices hold value. */
    String[] headers;
    int headerCount;

    /** Strided query-pair buffer: even indices hold the encoded key, odd indices hold the encoded value. */
    String[] queryPairs;
    int queryPairCount;

    /** Canonical request bytes. ASCII by construction, so chars-to-bytes can be a narrowing cast. */
    byte[] canonicalRequestBytes;

    /** String-to-sign bytes. ASCII. */
    byte[] stringToSignBytes;

    /** Body or canonical-request hash output. Always 32 bytes for SHA-256. */
    final byte[] hashBytes;

    /** Final HMAC signature output. Always 32 bytes for HmacSHA256. */
    final byte[] signatureBytes;

    /** Hex-encoded signature output. Always 64 bytes (32 bytes × 2 hex chars). */
    final byte[] signatureHexBytes;

    /** Read buffer for streaming body digest. */
    final byte[] readBuffer;

    private V4SigningResources() {
        this.sb = new StringBuilder(BUFFER_SIZE);
        this.headers = new String[INITIAL_HEADER_CAPACITY * 2];
        this.queryPairs = new String[INITIAL_HEADER_CAPACITY * 2];
        this.canonicalRequestBytes = new byte[BUFFER_SIZE];
        this.stringToSignBytes = new byte[BUFFER_SIZE];
        this.hashBytes = new byte[32];
        this.signatureBytes = new byte[32];
        this.signatureHexBytes = new byte[64];
        this.readBuffer = new byte[READ_BUFFER_SIZE];

        try {
            this.sha256Digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to fetch MessageDigest instance for SHA-256", e);
        }
        try {
            this.sha256Mac = Mac.getInstance(HMAC_SHA_256);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to fetch Mac instance for HmacSHA256", e);
        }
    }

    /**
     * Borrow an instance from the pool. The returned instance has been reset to its initial state.
     */
    static V4SigningResources acquire() {
        V4SigningResources r = POOL.acquire();
        r.reset();
        return r;
    }

    /**
     * Return an instance to the pool. The instance is shrunk to its baseline allocation if any internal buffer
     * exceeded the soft cap during use.
     */
    static void release(V4SigningResources resources) {
        if (resources == null) {
            return;
        }
        resources.shrink();
        POOL.release(resources);
    }

    /**
     * Reset all per-call state without releasing buffers. Called on acquire and again before each major phase of a
     * signing pass.
     */
    void reset() {
        sb.setLength(0);
        sha256Digest.reset();
        sha256Mac.reset();
        clearReferences();
    }

    /**
     * Drop any references the pool entry was holding so released items don't pin objects on the JVM heap.
     */
    private void clearReferences() {
        Arrays.fill(headers, 0, headerCount * 2, null);
        headerCount = 0;
        Arrays.fill(queryPairs, 0, queryPairCount * 2, null);
        queryPairCount = 0;
    }

    /**
     * Trim grown buffers back to baseline before returning to the pool, so a one-off giant request doesn't
     * permanently inflate the pool.
     */
    private void shrink() {
        if (sb.length() > BUFFER_SIZE) {
            sb.setLength(BUFFER_SIZE);
            sb.trimToSize();
        }
        sb.setLength(0);

        clearReferences();

        if (headers.length > INITIAL_HEADER_CAPACITY * 8) {
            headers = new String[INITIAL_HEADER_CAPACITY * 2];
        }
        if (queryPairs.length > INITIAL_HEADER_CAPACITY * 8) {
            queryPairs = new String[INITIAL_HEADER_CAPACITY * 2];
        }
        if (canonicalRequestBytes.length > BUFFER_SIZE * 8) {
            canonicalRequestBytes = new byte[BUFFER_SIZE];
        }
        if (stringToSignBytes.length > BUFFER_SIZE) {
            stringToSignBytes = new byte[BUFFER_SIZE];
        }
    }

    /**
     * Ensure {@link #canonicalRequestBytes} has at least {@code minLength} capacity, growing to the next power of two
     * if not. Returns the (possibly replaced) backing array.
     */
    byte[] ensureCanonicalRequestCapacity(int minLength) {
        if (canonicalRequestBytes.length < minLength) {
            int newLen = Integer.highestOneBit(minLength - 1) << 1;
            canonicalRequestBytes = new byte[newLen];
        }
        return canonicalRequestBytes;
    }

    /**
     * Ensure {@link #stringToSignBytes} has at least {@code minLength} capacity, growing to the next power of two if
     * not.
     */
    byte[] ensureStringToSignCapacity(int minLength) {
        if (stringToSignBytes.length < minLength) {
            int newLen = Integer.highestOneBit(minLength - 1) << 1;
            stringToSignBytes = new byte[newLen];
        }
        return stringToSignBytes;
    }

    /**
     * Append a header entry. The name is lowercased on demand if it contains any uppercase ASCII.
     */
    void addHeader(String name, String value) {
        addHeaderCanonical(lowercaseIfNeeded(name), value);
    }

    /**
     * Append a header entry whose name is already canonical lowercase.
     */
    void addHeaderCanonical(String name, String value) {
        int slot = headerCount * 2;
        if (slot >= headers.length) {
            String[] grown = new String[headers.length * 2];
            System.arraycopy(headers, 0, grown, 0, slot);
            headers = grown;
        }
        headers[slot] = name;
        headers[slot + 1] = value;
        headerCount++;
    }

    /**
     * Stable insertion sort of the strided header buffer by name.
     *
     * <p>Insertion sort is intentional: HTTP requests typically carry under 20 headers, where insertion sort beats
     * comparator-based array sorts by avoiding the allocation of a {@code Comparator} object and the overhead of
     * boxing {@code int} return values from {@code compareTo}. The sort must be stable so multiple values for the
     * same header name remain in insertion order, matching V2's existing canonical-header behaviour.
     */
    void sortHeadersByName() {
        for (int i = 1; i < headerCount; i++) {
            int srcSlot = i * 2;
            String name = headers[srcSlot];
            String value = headers[srcSlot + 1];

            int j = i - 1;
            while (j >= 0 && headers[j * 2].compareTo(name) > 0) {
                headers[(j + 1) * 2] = headers[j * 2];
                headers[(j + 1) * 2 + 1] = headers[j * 2 + 1];
                j--;
            }

            headers[(j + 1) * 2] = name;
            headers[(j + 1) * 2 + 1] = value;
        }
    }

    /**
     * Append a canonical query-string pair (already URL-encoded).
     */
    void addQueryPair(String encodedKey, String encodedValue) {
        int slot = queryPairCount * 2;
        if (slot >= queryPairs.length) {
            String[] grown = new String[queryPairs.length * 2];
            System.arraycopy(queryPairs, 0, grown, 0, slot);
            queryPairs = grown;
        }
        queryPairs[slot] = encodedKey;
        queryPairs[slot + 1] = encodedValue;
        queryPairCount++;
    }

    /**
     * Stable insertion sort of the query pairs by encoded key, breaking ties by encoded value.
     */
    void sortQueryPairs() {
        for (int i = 1; i < queryPairCount; i++) {
            int srcSlot = i * 2;
            String key = queryPairs[srcSlot];
            String value = queryPairs[srcSlot + 1];

            int j = i - 1;
            while (j >= 0 && compareKeyValue(queryPairs[j * 2], queryPairs[j * 2 + 1], key, value) > 0) {
                queryPairs[(j + 1) * 2] = queryPairs[j * 2];
                queryPairs[(j + 1) * 2 + 1] = queryPairs[j * 2 + 1];
                j--;
            }

            queryPairs[(j + 1) * 2] = key;
            queryPairs[(j + 1) * 2 + 1] = value;
        }
    }

    private static int compareKeyValue(String aKey, String aValue, String bKey, String bValue) {
        int cmp = aKey.compareTo(bKey);
        return cmp != 0 ? cmp : aValue.compareTo(bValue);
    }

    /**
     * Lowercase a header name only if needed. Most SDK-emitted headers are already lowercase, so the common path
     * never allocates.
     */
    static String lowercaseIfNeeded(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                return name.toLowerCase(Locale.ROOT);
            }
        }
        return name;
    }
}
