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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.utils.BinaryUtils;

class XxHashChecksumTest {

    private static Stream<Arguments> xxHashAlgorithms() {
        return Stream.of(
            Arguments.of(DefaultChecksumAlgorithm.XXHASH64, "xQCwyRKzdtg="),
            Arguments.of(DefaultChecksumAlgorithm.XXHASH3, "tqy52Eo4/3Q="),
            Arguments.of(DefaultChecksumAlgorithm.XXHASH128, "c1H4mBL5c4K5HQWzHgTdfw==")
        );
    }

    @ParameterizedTest
    @MethodSource("xxHashAlgorithms")
    void getChecksumBytes_withByteArray_returnsExpectedChecksum(ChecksumAlgorithm algorithm, String expectedBase64) {
        SdkChecksum checksum = ChecksumProvider.crtXxHash(algorithm);
        checksum.update("Hello world".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedBase64, BinaryUtils.toBase64(checksum.getChecksumBytes()));
    }

    @ParameterizedTest
    @MethodSource("xxHashAlgorithms")
    void mark_throwsUnsupportedOperationException(ChecksumAlgorithm algorithm, String expectedBase64) {
        SdkChecksum checksum = ChecksumProvider.crtXxHash(algorithm);
        assertThrows(UnsupportedOperationException.class, () -> checksum.mark(1));
    }

    @ParameterizedTest
    @MethodSource("xxHashAlgorithms")
    void reset_throwsUnsupportedOperationException(ChecksumAlgorithm algorithm, String expectedBase64) {
        SdkChecksum checksum = ChecksumProvider.crtXxHash(algorithm);
        assertThrows(UnsupportedOperationException.class, () -> checksum.reset());
    }

    @ParameterizedTest
    @MethodSource("xxHashAlgorithms")
    void getChecksumBytes_withSingleByteUpdates_returnsExpectedChecksum(ChecksumAlgorithm algorithm, String expectedBase64) {
        SdkChecksum checksum = ChecksumProvider.crtXxHash(algorithm);
        byte[] bytes = "Hello world".getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            checksum.update(b & 0xFF);
        }
        assertEquals(expectedBase64, BinaryUtils.toBase64(checksum.getChecksumBytes()));
    }

    @ParameterizedTest
    @MethodSource("xxHashAlgorithms")
    void getChecksumBytes_withOffsetAndLength_returnsExpectedChecksum(ChecksumAlgorithm algorithm, String expectedBase64) {
        SdkChecksum checksum = ChecksumProvider.crtXxHash(algorithm);
        byte[] bytes = "Hello world".getBytes(StandardCharsets.UTF_8);
        checksum.update(bytes, 0, bytes.length);
        assertEquals(expectedBase64, BinaryUtils.toBase64(checksum.getChecksumBytes()));
    }

    @Test
    void getChecksumBytes_closesResource_subsequentGetChecksumBytesThrows() {
        SdkChecksum checksum = ChecksumProvider.crtXxHash(DefaultChecksumAlgorithm.XXHASH64);
        checksum.update("Hello".getBytes(StandardCharsets.UTF_8));
        checksum.getChecksumBytes();

        // Second call should fail since resource is closed
        assertThatThrownBy(() -> checksum.getChecksumBytes()).hasMessageContaining("failed to finalize hash");
    }
}
