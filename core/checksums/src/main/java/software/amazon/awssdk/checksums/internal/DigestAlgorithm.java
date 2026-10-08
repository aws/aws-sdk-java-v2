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

package software.amazon.awssdk.checksums.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@SdkInternalApi
public enum DigestAlgorithm {
    SHA1("SHA-1"),

    MD5("MD5"),
    SHA256("SHA-256"),
    SHA512("SHA-512")
    ;

    private static final Supplier<MessageDigest> CLOSED_DIGEST = () -> {
        throw new IllegalStateException("This message digest is closed.");
    };

    private static final int MAX_CACHED_DIGESTS = 10_000;
    private final String algorithmName;
    // ConcurrentLinkedDeque avoids ReentrantLock used by LinkedBlockingDeque, which BlockHound
    // flags as blocking on async SDK paths (see #6590). Capacity is enforced with AtomicInteger.
    private final ConcurrentLinkedDeque<MessageDigest> digestCache = new ConcurrentLinkedDeque<>();
    private final AtomicInteger cachedDigestCount = new AtomicInteger();

    DigestAlgorithm(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Returns a {@link CloseableMessageDigest} to use for this algorithm.
     */
    public CloseableMessageDigest getDigest() {
        MessageDigest digest = digestCache.pollFirst();
        if (digest != null) {
            cachedDigestCount.decrementAndGet();
            digest.reset();
            return new CloseableMessageDigest(digest);
        }
        return new CloseableMessageDigest(newDigest());
    }

    private MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to fetch message digest instance for Algorithm "
                                       + algorithmName + ": " + e.getMessage(), e);
        }
    }

    @SdkTestInternalApi
    static void clearCaches() {
        for (DigestAlgorithm value : values()) {
            value.digestCache.clear();
            value.cachedDigestCount.set(0);
        }
    }

    public final class CloseableMessageDigest implements SdkAutoCloseable, Cloneable {

        private Supplier<MessageDigest> digest;
        private byte[] messageDigest;

        private CloseableMessageDigest(MessageDigest digest) {
            this.digest = () -> digest;
        }

        /**
         * Retrieve the message digest instance.
         */
        public MessageDigest messageDigest() {
            return digest.get();
        }

        /**
         * Retrieve the message digest bytes. This will close the message digest when invoked. This is because the underlying
         * message digest is reset on read, and we'd rather fail future interactions with the digest than act on the wrong data.
         */
        public byte[] digest() {
            if (messageDigest != null) {
                return messageDigest;
            }
            messageDigest = messageDigest().digest();
            close();
            return messageDigest;
        }

        /**
         * Release this message digest back to the cache. Once released, you must not use the digest anymore.
         */
        @Override
        public void close() {
            if (digest == CLOSED_DIGEST) {
                return;
            }

            MessageDigest toCache = digest.get();
            digest = CLOSED_DIGEST;

            // Soft capacity limit without locking/blocking. Drop the digest if the cache is full.
            if (cachedDigestCount.incrementAndGet() <= MAX_CACHED_DIGESTS) {
                digestCache.offerFirst(toCache);
            } else {
                cachedDigestCount.decrementAndGet();
            }
        }

        @Override
        public CloseableMessageDigest clone() {
            try {
                return new CloseableMessageDigest((MessageDigest) digest.get().clone());
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException("Clone was not supported by this digest type.", e);
            }
        }
    }
}
