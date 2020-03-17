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

package software.amazon.awssdk.utils.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A Base 16 codec implementation.
 *
 * @author Hanson Char
 */
@SdkInternalApi
public final class Base16Codec {
    private static final int OFFSET_OF_LITTLE_A = 'a' - 10;
    private static final int OFFSET_OF_A = 'A' - 10;
    private static final int MASK_4BITS = (1 << 4) - 1;
    private final byte[] alphabets;

    Base16Codec() {
        this(true);
    }

    Base16Codec(boolean upperCase) {
        this.alphabets = upperCase
                         ? CodecUtils.toBytesDirect("0123456789ABCDEF")
                         : CodecUtils.toBytesDirect("0123456789abcdef");
    }

    public byte[] encode(byte[] src) {
        byte[] dest = new byte[src.length * 2];
        byte p;

        for (int i = 0, j = 0; i < src.length; i++) {
            p = src[i];
            dest[j++] = alphabets[p >>> 4 & MASK_4BITS];
            dest[j++] = alphabets[p & MASK_4BITS];
        }
        return dest;
    }

    public byte[] decode(byte[] src, final int length) {
        if (length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Input is expected to be encoded in multiple of 2 bytes but found: "
                    + length
            );
        }
        byte[] dest = new byte[length / 2];

        for (int i = 0, j = 0; j < dest.length; j++) {
            dest[j] = (byte)
                    (
                            pos(src[i++]) << 4 | pos(src[i++])
                    )
            ;

        }
        return dest;
    }

    protected int pos(byte in) {
        int pos = LazyHolder.DECODED[in];

        if (pos > -1) {
            return pos;
        }
        throw new IllegalArgumentException("Invalid base 16 character: '" + (char) in + "'");
    }

    private static class LazyHolder {
        private static final byte[] DECODED = decodeTable();

        private static byte[] decodeTable() {
            byte[] dest = new byte['f' + 1];

            for (int i = 0; i <= 'f'; i++) {
                if (i >= '0' && i <= '9') {
                    dest[i] = (byte) (i - '0');
                } else if (i >= 'A' && i <= 'F') {
                    dest[i] = (byte) (i - OFFSET_OF_A);
                } else if (i >= 'a') {
                    dest[i] = (byte) (i - OFFSET_OF_LITTLE_A);
                } else {
                    dest[i] = -1;
                }
            }
            return dest;
        }
    }
}
