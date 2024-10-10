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

package software.amazon.awssdk.core.checksums;

import static software.amazon.awssdk.core.internal.util.HttpChecksumUtils.longToByte;

import java.util.Arrays;
import java.util.zip.CRC32;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.checksums.factory.SdkCrc32;

/**
 * Implementation of {@link SdkChecksum} to calculate an CRC32 checksum.
 */
@SdkInternalApi
public class Crc32Checksum2 implements SdkChecksum {
    private CRC32 crc32;
    private long dataLengthForChecksum = 0;

    private Long crcAtMark;

    public Crc32Checksum2() {
        crc32 = new CRC32();
    }

    @Override
    public byte[] getChecksumBytes() {
        return Arrays.copyOfRange(longToByte(getValue()), 4, 8);
    }

    @Override
    public void mark(int readLimit) {
        if (dataLengthForChecksum > 0) {
            crcAtMark = crc32.getValue();
            crc32 = new CRC32();
            dataLengthForChecksum = 0;
        }
    }

    @Override
    public void update(int b) {
        crc32.update(b);
        dataLengthForChecksum += 1;
    }

    @Override
    public void update(byte[] b, int off, int len) {
        crc32.update(b, off, len);
        dataLengthForChecksum += len;
    }

    @Override
    public long getValue() {
        if (crcAtMark == null) {
            return crc32.getValue();
        }

        return SdkCrc32.combine(crcAtMark, crc32.getValue(), dataLengthForChecksum);
    }

    @Override
    public void reset() {
        crc32.reset();
        dataLengthForChecksum = 0;
    }
}
