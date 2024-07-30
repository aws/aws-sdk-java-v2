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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32C;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.MD5;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA1;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.checksumHeaderName;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.fromChecksumAlgorithm;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.readAll;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Crc32CChecksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Crc32Checksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Md5Checksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Sha1Checksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Sha256Checksum;

public class ChecksumUtilTest {

    @Test
    public void checksumHeaderName_shouldFormatName() {
        assertEquals("x-amz-checksum-sha256", checksumHeaderName(SHA256));
        assertEquals("x-amz-checksum-sha1", checksumHeaderName(SHA1));
        assertEquals("x-amz-checksum-crc32", checksumHeaderName(CRC32));
        assertEquals("x-amz-checksum-crc32c", checksumHeaderName(CRC32C));
        assertEquals("x-amz-checksum-md5", checksumHeaderName(MD5));
    }

    @Test
    public void fromChecksumAlgorithm_mapsToCorrectSdkChecksum() {
        assertEquals(Sha256Checksum.class, fromChecksumAlgorithm(SHA256).getClass());
        assertEquals(Sha1Checksum.class, fromChecksumAlgorithm(SHA1).getClass());
        assertEquals(Crc32Checksum.class, fromChecksumAlgorithm(CRC32).getClass());
        assertEquals(Crc32CChecksum.class, fromChecksumAlgorithm(CRC32C).getClass());
        assertEquals(Md5Checksum.class, fromChecksumAlgorithm(MD5).getClass());
    }

    @Test
    public void readAll_consumesEntireStream() {
        int bytes = 4096 * 4 + 1;
        byte[] buf = new byte[bytes];
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buf);

        readAll(inputStream);

        assertEquals(0, inputStream.available());
    }

    @Test
    public void readAll_throwIoException_shouldWrapWithIoException() throws IOException {
        IOException ioException = new IOException();
        InputStream mock = Mockito.mock(InputStream.class);
        Mockito.when(mock.read(Mockito.any(byte[].class))).thenThrow(ioException);
        assertThatThrownBy(() -> readAll(mock)).isInstanceOf(UncheckedIOException.class).hasCause(ioException);
    }

    @Test
    public void readAll_throwNonIoException_shouldNotWrap() throws IOException {
        NullPointerException npe = new NullPointerException();
        InputStream mock = Mockito.mock(InputStream.class);
        Mockito.when(mock.read(Mockito.any(byte[].class))).thenThrow(npe);
        assertThatThrownBy(() -> readAll(mock)).isEqualTo(npe);
    }
}
