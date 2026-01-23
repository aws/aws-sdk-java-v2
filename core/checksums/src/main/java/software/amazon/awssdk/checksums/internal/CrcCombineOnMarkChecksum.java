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

import java.util.function.BiFunction;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * Class that provides functionality for combining CRC checksums with mark and reset capabilities.
 *
 * <p>
 * This class allows combining two CRC values, making it efficient to update and restore the checksum
 * during mark and reset operations. It is particularly useful in scenarios where a checksum needs to
 * be marked, potentially reset to a previous state, and combined with additional data.
 * </p>
 *
 * <p>
 * The class maintains an internal {@link Checksum} instance and uses a {@link BiFunction} to combine
 * two CRC values when necessary. The combine function is applied to a pair of CRC values, along with
 * the length of the data, to produce the resulting combined checksum.
 * </p>
 */
@SdkInternalApi
public class CrcCombineOnMarkChecksum implements SdkChecksum {
    private final Checksum crc;
    private long dataLengthForChecksum = 0;
    private Long crcAtMark;
    private long markedDataLength = 0;
    private boolean isResetDone = false;

    private final CrcCombineFunction crcCombineFunction;


    public CrcCombineOnMarkChecksum(Checksum checksum,
                                    CrcCombineFunction crcCombineFunction) {
        this.crc = checksum;
        this.crcCombineFunction = crcCombineFunction;
    }

    @Override
    public byte[] getChecksumBytes() {
        byte[] valueBytes = longToByte(getValue());
        return new byte[] { valueBytes[4], valueBytes[5], valueBytes[6], valueBytes[7] };
    }

    @Override
    public void mark(int readLimit) {
        if (dataLengthForChecksum > 0) {
            saveMarkState();
        }
    }

    @Override
    public void update(int b) {
        crc.update(b);
        dataLengthForChecksum += 1;
    }

    @Override
    public void update(byte[] b, int off, int len) {
        crc.update(b, off, len);
        dataLengthForChecksum += len;
    }

    @Override
    public long getValue() {
        if (canRestoreMarkedState()) {
            return crcCombineFunction.combine(crcAtMark, crc.getValue(), dataLengthForChecksum - markedDataLength);
        }
        return crc.getValue();
    }

    @Override
    public void reset() {
        if (crcAtMark != null) {
            crc.reset();
            dataLengthForChecksum = markedDataLength;
        } else {
            crc.reset();
            dataLengthForChecksum = 0;
        }
        isResetDone = true;
    }

    private void saveMarkState() {
        crcAtMark = crc.getValue();
        markedDataLength = dataLengthForChecksum;
    }

    private boolean canRestoreMarkedState() {
        return crcAtMark != null && isResetDone;
    }

}
