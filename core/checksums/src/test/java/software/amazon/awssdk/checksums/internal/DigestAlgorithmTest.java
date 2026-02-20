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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.checksums.internal.DigestAlgorithm.CloseableMessageDigest;
import software.amazon.awssdk.utils.BinaryUtils;

class DigestAlgorithmTest {
    @BeforeEach
    void clearCache() {
        DigestAlgorithm.clearCaches();
    }

    @Test
    void getAlgorithmName_returnsCorrectValue() {
        assertThat(DigestAlgorithm.SHA1.getAlgorithmName()).isEqualTo("SHA-1");
        assertThat(DigestAlgorithm.MD5.getAlgorithmName()).isEqualTo("MD5");
        assertThat(DigestAlgorithm.SHA256.getAlgorithmName()).isEqualTo("SHA-256");
    }

    @Test
    void getDigest_returnsMessageDigest() {
        CloseableMessageDigest digest = DigestAlgorithm.SHA1.getDigest();
        assertThat(digest).isNotNull();
        assertThat(digest.messageDigest()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("digestAlgorithmTestCases")
    void digestAlgorithms_useCorrectImplementation(DigestAlgorithm algorithm, String expectedBase64) {
        String input = "Hello, World!";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        CloseableMessageDigest digest = algorithm.getDigest();
        digest.messageDigest().update(data);
        byte[] hash = digest.digest();
        
        assertThat(hash).isNotNull();
        assertThat(BinaryUtils.toBase64(hash)).isEqualTo(expectedBase64);
    }

    static Stream<Arguments> digestAlgorithmTestCases() {
        return Stream.of(
            Arguments.of(DigestAlgorithm.SHA1, "CgqfKmdylCVXq1NV12r0Qvj2XgE="),
            Arguments.of(DigestAlgorithm.MD5, "ZajifYh5KDgxtmS9i38K1A=="),
            Arguments.of(DigestAlgorithm.SHA256, "3/1gIbsr1bCvZ2KQgJ7DpTGR3YHH9wpLKGiKNiGCmG8="),
            Arguments.of(DigestAlgorithm.SHA512, "N015SpXNz9izWZMYX++bo2jxYNja9DLQi6nx7R5avmzGkpHg+i/gAGpSVw7xjBne9OYXwzzlLvCm5fvjGMsDhw==")
        );
    }

    @Test
    void closedDigests_areClearedAndReused() {
        CloseableMessageDigest digest1 = DigestAlgorithm.SHA1.getDigest();
        MessageDigest messageDigest1 = digest1.messageDigest();
        messageDigest1.update((byte) 'a');
        byte[] aDigest = digest1.digest();
        digest1.close();

        CloseableMessageDigest digest2 = DigestAlgorithm.SHA1.getDigest();
        assertThat(digest2.messageDigest()).isSameAs(messageDigest1);
        assertThat(digest2.digest()).isNotEqualTo(aDigest);
        digest2.close();
    }

    @Test
    void digestClone_clonesDigestContent() {
        CloseableMessageDigest original = DigestAlgorithm.SHA1.getDigest();
        original.messageDigest().update((byte) 'a');

        CloseableMessageDigest cloned = original.clone();

        assertThat(cloned).isNotNull();
        assertThat(cloned.messageDigest()).isNotSameAs(original.messageDigest());
        assertThat(original.digest()).isEqualTo(cloned.digest());
    }

    @Test
    void digestClones_behaveIndependently() {
        CloseableMessageDigest original = DigestAlgorithm.SHA1.getDigest();
        CloseableMessageDigest cloned = original.clone();

        assertThat(cloned).isNotNull();
        assertThat(cloned.messageDigest()).isNotSameAs(original.messageDigest());

        // Test that both digests work independently
        original.messageDigest().update((byte) 'a');

        assertThat(original.digest()).isNotEqualTo(cloned.digest());
    }

    @Test
    void readingDigest_closesDigest() {
        CloseableMessageDigest digest = DigestAlgorithm.SHA1.getDigest();

        digest.digest();

        assertThatThrownBy(() -> digest.messageDigest())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void closeDigest_canBeDoneMultipleTimes() {
        CloseableMessageDigest digest = DigestAlgorithm.SHA1.getDigest();
        digest.close();
        digest.close();
        digest.close();
    }

    @Test
    void closedDigests_failMethodCalls() {
        CloseableMessageDigest digest = DigestAlgorithm.SHA1.getDigest();
        digest.close();

        assertThatThrownBy(() -> digest.messageDigest())
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> digest.clone())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void digestsCanBeRetrievedMultipleTimes() {
        CloseableMessageDigest digest = DigestAlgorithm.SHA1.getDigest();
        String input = "Test Data";
        byte[] data = input.getBytes(StandardCharsets.UTF_8);

        digest.messageDigest().update(data);
        byte[] firstHash = digest.digest();
        byte[] secondHash = digest.digest();

        assertThat(firstHash).isEqualTo(secondHash);
    }

    @Test
    void testCacheLimit() {
        // Test that we can release more than the size of the cache back to the cache.
        List<CloseableMessageDigest> digests = new ArrayList<>();
        for (int i = 0; i < 10_001; i++) {
            CloseableMessageDigest digest = DigestAlgorithm.SHA1.getDigest();
            digest.close();
        }

        // Still able to get new digests
        CloseableMessageDigest digest = DigestAlgorithm.SHA1.getDigest();
        assertThat(digest).isNotNull();
    }
}