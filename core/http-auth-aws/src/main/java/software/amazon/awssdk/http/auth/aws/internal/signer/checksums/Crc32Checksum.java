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

package software.amazon.awssdk.http.auth.aws.internal.signer.checksums;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.checksums.CRC32;
import software.amazon.awssdk.utils.ClassLoaderHelper;

/**
 * Implementation of {@link SdkChecksum} to calculate an CRC32 checksum.
 */
@SdkInternalApi
public class Crc32Checksum implements SdkChecksum {

    private static final String CRT_CLASSPATH_FOR_CRC32 = "software.amazon.awssdk.crt.checksums.CRC32";

    private Checksum crc32;
    private Checksum lastMarkedCrc32;

    /**
     * Creates CRT Based Crc32 checksum if Crt classpath for Crc32 is loaded, else create Sdk Implemented Crc32.
     */
    public Crc32Checksum() {
        if (isCrtAvailable()) {
            crc32 = new CRC32();
        } else {
            crc32 = SdkCrc32Checksum.create();
        }
    }

    private static boolean isCrtAvailable() {
        try {
            ClassLoaderHelper.loadClass(CRT_CLASSPATH_FOR_CRC32, false);
        } catch (ClassNotFoundException e) {
            return false;
        }

        return true;
    }

    private static byte[] longToByte(Long input) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(input);
        return buffer.array();
    }

    @Override
    public byte[] getChecksumBytes() {
        return Arrays.copyOfRange(longToByte(crc32.getValue()), 4, 8);
    }

    @Override
    public void mark(int readLimit) {
        this.lastMarkedCrc32 = cloneChecksum(crc32);
    }

    @Override
    public void update(int b) {
        crc32.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        crc32.update(b, off, len);
    }

    @Override
    public long getValue() {
        return crc32.getValue();
    }

    @Override
    public void reset() {
        if (lastMarkedCrc32 == null) {
            crc32.reset();
        } else {
            crc32 = cloneChecksum(lastMarkedCrc32);
        }
    }

    private Checksum cloneChecksum(Checksum checksum) {
        if (checksum instanceof CRC32) {
            return (Checksum) ((CRC32) checksum).clone();
        }

        if (checksum instanceof SdkCrc32Checksum) {
            return (Checksum) ((SdkCrc32Checksum) checksum).clone();
        }

        throw new IllegalStateException("Unsupported checksum");
    }
}
