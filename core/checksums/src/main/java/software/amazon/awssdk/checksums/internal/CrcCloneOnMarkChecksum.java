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

import static software.amazon.awssdk.utils.NumericUtils.longToByte;

import java.lang.reflect.Method;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Class that provides functionality for combining CRC checksums with mark and reset capabilities.
 *
 * <p>
 * This class is intended for use with checksums that do not provide a cloneable method. It uses
 * combine methods to handle mark and reset operations efficiently.
 * </p>
 */
@SdkInternalApi
public final class CrcCloneOnMarkChecksum extends BaseCrcChecksum {

    public CrcCloneOnMarkChecksum(Checksum checksum) {
        super(checksum);
    }

    @Override
    Checksum cloneChecksum(Checksum checksum) {
        return cloneIfCloneable(checksum);
    }

    @Override
    public byte[] getChecksumBytes() {
        byte[] valueBytes = longToByte(getValue());
        return new byte[] { valueBytes[4], valueBytes[5], valueBytes[6], valueBytes[7] };
    }

    // Function to clone any Checksum that implements Cloneable
    private Checksum cloneIfCloneable(Checksum checksum) {
        if (checksum instanceof Cloneable) {
            try {
                Method cloneMethod = checksum.getClass().getMethod("clone");
                return (Checksum) cloneMethod.invoke(checksum);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to clone the checksum", e);
            }
        }
        throw new UnsupportedOperationException("Checksum does not support cloning");
    }
}
