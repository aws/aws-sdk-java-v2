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


import java.util.zip.CRC32;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;

@SdkInternalApi
public final class Crc32Checksum implements SdkChecksum {
    private final CrcCombineOnMarkChecksum crc32;

    public Crc32Checksum() {
        // Delegates to CrcCombineOnMarkChecksum with CRC32
        this.crc32 = new CrcCombineOnMarkChecksum(
            new CRC32(),
            SdkCrc32Checksum::combine
        );
    }

    @Override
    public byte[] getChecksumBytes() {
        return crc32.getChecksumBytes();
    }

    @Override
    public void mark(int readLimit) {
        crc32.mark(readLimit);
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
        crc32.reset();
    }
}
