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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.security.PublicKey;
import org.junit.jupiter.api.Test;

class CertificateCacheTest {
    private static final URI URL_A = URI.create("https://sns.us-east-1.amazonaws.com/a.pem");
    private static final URI URL_B = URI.create("https://sns.us-east-1.amazonaws.com/b.pem");
    private static final URI URL_C = URI.create("https://sns.us-east-1.amazonaws.com/c.pem");

    private final PublicKey keyA = mock(PublicKey.class, "keyA");
    private final PublicKey keyB = mock(PublicKey.class, "keyB");
    private final PublicKey keyC = mock(PublicKey.class, "keyC");

    @Test
    void candidates_hintPresent_returnsHintFirst() {
        CertificateCache cache = new CertificateCache(10, 16);
        cache.putIfAbsent(URL_A, "fp-a", keyA);
        cache.putIfAbsent(URL_B, "fp-b", keyB);
        cache.putIfAbsent(URL_C, "fp-c", keyC);

        assertThat(cache.candidates(URL_B)).containsExactly(keyB, keyA, keyC);
        assertThat(cache.candidates(URI.create("https://sns.us-east-1.amazonaws.com/unknown.pem")))
            .containsExactlyInAnyOrder(keyA, keyB, keyC);
    }

    @Test
    void candidates_emptyCache_returnsEmpty() {
        CertificateCache cache = new CertificateCache(10, 16);

        assertThat(cache.candidates(URL_A)).isEmpty();
        assertThat(cache.lookup(URL_A)).isNull();
        assertThat(cache.size()).isZero();
    }

    @Test
    void putIfAbsent_sameFingerprint_keepsFirstInstanceAndSize() {
        CertificateCache cache = new CertificateCache(10, 16);
        PublicKey sameContentDifferentInstance = mock(PublicKey.class, "keyA2");

        PublicKey first = cache.putIfAbsent(URL_A, "fp-a", keyA);
        PublicKey second = cache.putIfAbsent(URL_B, "fp-a", sameContentDifferentInstance);

        assertThat(first).isSameAs(keyA);
        assertThat(second).isSameAs(keyA);
        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.lookup(URL_A)).isSameAs(keyA);
        assertThat(cache.lookup(URL_B)).isSameAs(keyA);
        assertThat(cache.candidates(URL_B)).containsExactly(keyA);
    }

    @Test
    void putIfAbsent_overCapacity_removesLeastRecentlyUsed() {
        CertificateCache cache = new CertificateCache(2, 16);
        cache.putIfAbsent(URL_A, "fp-a", keyA);
        cache.putIfAbsent(URL_B, "fp-b", keyB);

        // Touch A so that B becomes the least recently used entry.
        assertThat(cache.lookup(URL_A)).isSameAs(keyA);
        cache.putIfAbsent(URL_C, "fp-c", keyC);

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.candidates(URL_C)).containsExactly(keyC, keyA);
        assertThat(cache.lookup(URL_B)).isNull();
    }

    @Test
    void lookup_hintForRemovedCertificate_returnsNullAndDropsHint() {
        CertificateCache cache = new CertificateCache(1, 16);
        cache.putIfAbsent(URL_A, "fp-a", keyA);
        cache.putIfAbsent(URL_B, "fp-b", keyB);

        assertThat(cache.lookup(URL_A)).isNull();
        assertThat(cache.candidates(URL_A)).containsExactly(keyB);

        // The stale hint is gone: re-inserting the certificate under another URL does not revive it for URL_A.
        cache.putIfAbsent(URL_C, "fp-a", keyA);
        assertThat(cache.lookup(URL_A)).isNull();
        assertThat(cache.lookup(URL_C)).isSameAs(keyA);
    }

    @Test
    void urlHints_overCapacity_removesOldestHintOnly() {
        CertificateCache cache = new CertificateCache(10, 2);
        cache.putIfAbsent(URL_A, "fp-a", keyA);
        cache.putIfAbsent(URL_B, "fp-a", keyA);
        cache.putIfAbsent(URL_C, "fp-a", keyA);

        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.lookup(URL_A)).isNull();
        assertThat(cache.lookup(URL_B)).isSameAs(keyA);
        assertThat(cache.lookup(URL_C)).isSameAs(keyA);
        assertThat(cache.candidates(URL_A)).containsExactly(keyA);
    }
}
