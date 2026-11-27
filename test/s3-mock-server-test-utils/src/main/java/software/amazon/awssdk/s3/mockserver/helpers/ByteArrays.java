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

package software.amazon.awssdk.s3.mockserver.helpers;

public final class ByteArrays {

    private ByteArrays() {
    }

    public static int compareUnsigned(byte[] a, byte[] b) {
        int i = mismatch(a, b);
        if (i >= 0 && i < Math.min(a.length, b.length)) {
            return Integer.compare(a[i] & 0xFF, b[i] & 0xFF);
        }
        return a.length - b.length;
    }

    public static int mismatch(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        return a.length == b.length ? -1 : n;
    }

    public static boolean rangeEquals(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo) {
        if (aFrom < 0 || aFrom > aTo || aTo > a.length || bFrom < 0 || bFrom > bTo || bTo > b.length) {
            throw new ArrayIndexOutOfBoundsException("range outside the array");
        }
        if (aTo - aFrom != bTo - bFrom) {
            return false;
        }
        for (int i = 0; i < aTo - aFrom; i++) {
            if (a[aFrom + i] != b[bFrom + i]) {
                return false;
            }
        }
        return true;
    }

    public static String describe(byte[] bytes) {
        return bytes == null ? "null" : "byte[" + bytes.length + "]";
    }

    public static byte[] parseHex(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("odd number of hex digits: " + hex);
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(2 * i), 16);
            int lo = Character.digit(hex.charAt(2 * i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("not a hex digit in: " + hex);
            }
            out[i] = (byte) (hi << 4 | lo);
        }
        return out;
    }
}
