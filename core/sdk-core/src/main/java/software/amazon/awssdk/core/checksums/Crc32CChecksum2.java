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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.checksums.factory.SdkCrc32C;

/**
 * Implementation of {@link SdkChecksum} to calculate an CRC32 checksum.
 */
@SdkInternalApi
public class Crc32CChecksum2 implements SdkChecksum {
    private static Constructor<?> CRC32C_CLASS_CONSTRUCTOR;

    private Checksum crc32c;
    private long dataLengthForChecksum = 0;

    private Long crcAtMark;

    public Crc32CChecksum2() {
        crc32c = createCrc32c();
    }

    static {
        try {
            CRC32C_CLASS_CONSTRUCTOR = Class.forName("java.util.zip.CRC32C").getConstructor();
        } catch (Exception e) {
        }
    }

    public static Checksum createCrc32c() {
        try {
            return (Checksum) CRC32C_CLASS_CONSTRUCTOR.newInstance();
        } catch (NullPointerException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unable to load CRC32C. Are we on Java 9+?", e);
        }
    }

    @Override
    public byte[] getChecksumBytes() {
        return longToByte(getValue());
    }

    @Override
    public void mark(int readLimit) {
        if (dataLengthForChecksum > 0) {
            crcAtMark = crc32c.getValue();
            crc32c.reset();
            dataLengthForChecksum = 0;
        }
    }

    @Override
    public void update(int b) {
        crc32c.update(b);
        dataLengthForChecksum += 1;
    }

    @Override
    public void update(byte[] b, int off, int len) {
        crc32c.update(b, off, len);
        dataLengthForChecksum += len;
    }

    @Override
    public long getValue() {
        if (crcAtMark == null) {
            return crc32c.getValue();
        }

        return SdkCrc32C.combine(crcAtMark, crc32c.getValue(), dataLengthForChecksum);
    }

    @Override
    public void reset() {
        crc32c.reset();
        dataLengthForChecksum = 0;
    }
}
