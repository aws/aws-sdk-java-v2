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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.utils.BinaryUtils;

class Crc64NvmeChecksumTest {

    private SdkChecksum sdkChecksum;
    private static final String TEST_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @BeforeEach
    public void setUp() {
        sdkChecksum = new Crc64NvmeChecksum();
    }

    @Test
    void validateCrcChecksumValues() {
        byte[] bytes = TEST_STRING.getBytes(StandardCharsets.UTF_8);
        sdkChecksum.update(bytes, 0, bytes.length);
        assertEquals("0000000000000000000000008b8f30cfc6f16409", getAsString(sdkChecksum.getChecksumBytes()));
    }

    @Test
    void validateEncodedBase64ForCrc() {
        sdkChecksum.update("abc".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertEquals("BeXKuz/B+us=", toBase64);
    }

    @Test
    void validateMarkAndResetForCrc() {
        sdkChecksum.update("ab".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(3);
        sdkChecksum.update("xyz".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        sdkChecksum.update("c".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertEquals("BeXKuz/B+us=", toBase64);
    }

    @Test
    void validateMarkForCrc() {
        sdkChecksum.update("Hello ".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(4);
        sdkChecksum.update("world".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertEquals("OOJZ0D8xKts=", toBase64);
    }

    @Test
    void validateSingleMarksForCrc() {
        sdkChecksum.update("alpha".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(3);
        sdkChecksum.update("beta".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        assertEquals("Ehnh98TMQlQ=", toBase64);
    }

    @Test
    void validateMultipleMarksForCrc() {
        sdkChecksum.update("alpha".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(3);
        sdkChecksum.update("beta".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.mark(5);
        sdkChecksum.update("gamma".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        sdkChecksum.update("delta".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        // Final checksum of "alphabetadelta"
        assertEquals("ugWp+3k2NgA=", toBase64);
    }

    @Test
    void validateResetWithoutMarkForCrc() {
        sdkChecksum.update("beta".getBytes(StandardCharsets.UTF_8));
        sdkChecksum.reset();
        sdkChecksum.update("alpha".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(sdkChecksum.getChecksumBytes());
        // Checksum of "alpha"
        assertEquals("Ehnh98TMQlQ=", toBase64);
    }

    private String getAsString(byte[] checksumBytes) {
        return String.format("%040x", new BigInteger(1, checksumBytes));
    }
}