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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.checksums.factory.CrtBasedChecksumProvider;
import software.amazon.awssdk.core.internal.checksums.factory.SdkCrc32C;

/**
 * Implementation of {@link SdkChecksum} to calculate an CRC32C checksum.
 */
@SdkInternalApi
public class Crc32CChecksum implements SdkChecksum {

    private Checksum crc32c;
    private Checksum lastMarkedCrc32C;
    private final boolean isCrtBasedChecksum;

    /**
     * Creates CRT Based Crc32C checksum if Crt classpath for Crc32c is loaded, else create Sdk Implemented Crc32c
     */
    public Crc32CChecksum() {
        crc32c = CrtBasedChecksumProvider.createCrc32C();
        isCrtBasedChecksum = crc32c != null;
        if (!isCrtBasedChecksum) {
            crc32c = SdkCrc32C.create();
        }
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
        if (isCrtBasedChecksum) {
            try {
                Method method = checksum.getClass().getDeclaredMethod("clone");
                return (Checksum) method.invoke(checksum);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not clone checksum class " + checksum.getClass(), e);
            }
        } else {
            return (Checksum) ((SdkCrc32C) checksum).clone();

        }
    }
}