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

package software.amazon.awssdk.authcrt.signer.internal.checksum;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.zip.Checksum;
import org.junit.Test;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.internal.checksums.factory.CrtBasedChecksumProvider;
import software.amazon.awssdk.crt.checksums.CRC32;
import software.amazon.awssdk.crt.checksums.CRC32C;
import software.amazon.awssdk.utils.BinaryUtils;

public class CrtBasedChecksumTest {

    static final String TEST_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Test
    public void crtBasedCrc32ChecksumValues(){
        Checksum checksum = CrtBasedChecksumProvider.createCrc32();
        assertThat(checksum).isNotNull().isInstanceOf(CRC32.class);
    }

    @Test
    public void crtBasedCrc32_C_ChecksumValues(){
        Checksum checksum = CrtBasedChecksumProvider.createCrc32C();
        assertThat(checksum).isNotNull().isInstanceOf(CRC32C.class);
    }

    @Test
    public void crc32CheckSumValues() throws UnsupportedEncodingException {
        final SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(Algorithm.CRC32);
        final byte[] bytes = TEST_STRING.getBytes("UTF-8");
        sdkChecksum.update(bytes, 0, bytes.length);
        assertThat(getAsString(sdkChecksum.getChecksumBytes())).isEqualTo("000000000000000000000000000000001fc2e6d2");
    }

    @Test
    public void crc32_C_CheckSumValues() throws UnsupportedEncodingException {
        final SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(Algorithm.CRC32C);
        final byte[] bytes = TEST_STRING.getBytes("UTF-8");
        sdkChecksum.update(bytes, 0, bytes.length);
        assertThat(getAsString(sdkChecksum.getChecksumBytes())).isEqualTo("00000000000000000000000000000000a245d57d");
    }

    @Test
    public void validateEncodedBase64ForCrc32C()  {
        SdkChecksum crc32c = SdkChecksum.forAlgorithm(Algorithm.CRC32C);
        crc32c.update("abc".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(crc32c.getChecksumBytes());
        assertThat(toBase64).isEqualTo("Nks/tw==");
    }

    @Test
    public void validateEncodedBase64ForCrc32()  {
        SdkChecksum crc32 = SdkChecksum.forAlgorithm(Algorithm.CRC32);
        crc32.update("abc".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(crc32.getChecksumBytes());
        assertThat(toBase64).isEqualTo("NSRBwg==");
    }

    @Test
    public void validateMarkAndResetForCrc32()  {
        SdkChecksum crc32 = SdkChecksum.forAlgorithm(Algorithm.CRC32);
        crc32.update("ab".getBytes(StandardCharsets.UTF_8));
        crc32.mark(3);
        crc32.update("xyz".getBytes(StandardCharsets.UTF_8));
        crc32.reset();
        crc32.update("c".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(crc32.getChecksumBytes());
        assertThat(toBase64).isEqualTo("NSRBwg==");
    }

    @Test
    public void validateMarkAndResetForCrc32C()  {
        SdkChecksum crc32c = SdkChecksum.forAlgorithm(Algorithm.CRC32C);
        crc32c.update("ab".getBytes(StandardCharsets.UTF_8));
        crc32c.mark(3);
        crc32c.update("xyz".getBytes(StandardCharsets.UTF_8));
        crc32c.reset();
        crc32c.update("c".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(crc32c.getChecksumBytes());
        assertThat(toBase64).isEqualTo("Nks/tw==");
    }

    @Test
    public void validateMarkForCrc32C()  {
        SdkChecksum crc32c = SdkChecksum.forAlgorithm(Algorithm.CRC32C);
        crc32c.update("Hello ".getBytes(StandardCharsets.UTF_8));
        crc32c.mark(3);
        crc32c.update("world".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(crc32c.getChecksumBytes());
        assertThat(toBase64).isEqualTo("crUfeA==");
    }

    @Test
    public void validateMarkForCrc32()  {
        SdkChecksum crc32 = SdkChecksum.forAlgorithm(Algorithm.CRC32);
        crc32.update("Hello ".getBytes(StandardCharsets.UTF_8));
        crc32.mark(3);
        crc32.update("world".getBytes(StandardCharsets.UTF_8));
        String toBase64 = BinaryUtils.toBase64(crc32.getChecksumBytes());
        assertThat(toBase64).isEqualTo("i9aeUg==");
    }

    private String getAsString(byte[] checksumBytes) {
        return String.format("%040x", new BigInteger(1, checksumBytes));
    }
}
