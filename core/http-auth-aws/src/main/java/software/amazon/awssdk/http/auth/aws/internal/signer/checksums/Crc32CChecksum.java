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
import software.amazon.awssdk.crt.checksums.CRC32C;
import software.amazon.awssdk.utils.ClassLoaderHelper;

/**
 * Implementation of {@link SdkChecksum} to calculate an CRC32C checksum.
 */
@SdkInternalApi
public class Crc32CChecksum implements SdkChecksum {

    private static final String CRT_CLASSPATH_FOR_CRC32C = "software.amazon.awssdk.crt.checksums.CRC32C";

    private Checksum crc32c;
    private Checksum lastMarkedCrc32C;

    /**
     * Creates CRT Based Crc32C checksum if Crt classpath for Crc32c is loaded, else create Sdk Implemented Crc32c
     */
    public Crc32CChecksum() {
        if (isCrtAvailable()) {
            crc32c = new CRC32C();
        } else {
            crc32c = SdkCrc32CChecksum.create();
        }
    }

    private static boolean isCrtAvailable() {
        try {
            ClassLoaderHelper.loadClass(CRT_CLASSPATH_FOR_CRC32C, false);
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
        return Arrays.copyOfRange(longToByte(crc32c.getValue()), 4, 8);
    }

    @Override
    public void mark(int readLimit) {
        this.lastMarkedCrc32C = cloneChecksum(crc32c);
    }

    @Override
    public void update(int b) {
        crc32c.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        crc32c.update(b, off, len);
    }

    @Override
    public long getValue() {
        return crc32c.getValue();
    }

    @Override
    public void reset() {
        if (lastMarkedCrc32C == null) {
            crc32c.reset();
        } else {
            crc32c = cloneChecksum(lastMarkedCrc32C);
        }
    }

    private Checksum cloneChecksum(Checksum checksum) {
        if (checksum instanceof CRC32C) {
            return (Checksum) ((CRC32C) checksum).clone();
        }

        if (checksum instanceof SdkCrc32CChecksum) {
            return (Checksum) ((SdkCrc32CChecksum) checksum).clone();
        }

        throw new IllegalStateException("Unsupported checksum");
    }
}
