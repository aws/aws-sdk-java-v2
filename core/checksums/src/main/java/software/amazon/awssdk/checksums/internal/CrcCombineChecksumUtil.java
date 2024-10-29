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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility class that provides methods for combining CRC checksums using Galois Field arithmetic.
 * This class allows combining two CRC values into a single CRC that represents the concatenated
 * data, without recalculating the CRC from scratch.
 * <p>
 * The implementation of CRC combination was taken from the zlib source code here:
 * <a href="https://github.com/luvit/zlib/blob/master/crc32.c">https://github.com/luvit/zlib/blob/master/crc32.c</a>
 * </p>
 */
@SdkInternalApi
public final class CrcCombineChecksumUtil {

    public static final int CRC_SIZE = 32;

    private CrcCombineChecksumUtil() {
    }

    /**
     * Generates the combine matrices for CRC calculations.
     *
     * @param polynomial The CRC polynomial.
     * @return A 2D array representing the combine matrices.
     */
    public static long[][] generateCombineMatrices(long polynomial) {
        long[][] combineMatrices = new long[CRC_SIZE][CRC_SIZE];
        initializeFirstMatrix(combineMatrices, polynomial);
        deriveRemainingMatrices(combineMatrices);
        return combineMatrices;
    }

    /**
     * Combines two CRC values into a single CRC using the specified combine matrices.
     *
     * The combination is performed using Galois Field arithmetic to effectively merge
     * two CRC checksums that correspond to two separate data blocks. This method allows
     * calculating the CRC for the concatenated data without having to recompute the CRC
     * from scratch, which can significantly improve performance for large datasets.
     * <p>
     * THIS COMBINE FUNCTION HAS BEEN MODIFIED FROM THE ORIGINAL VERSION.
     * The code comes from
     * <a href="https://github.com/luvit/zlib/blob/master/crc32.c">https://github.com/luvit/zlib/blob/master/crc32.c</a>.
     * </p>
     * @param crc1                 The first CRC value.
     * @param crc2                 The second CRC value.
     * @param originalLengthOfCrc2  The length of the original data for the second CRC.
     *                             This represents the length of data used to compute {@code crc2}.
     * @param combineMatrices      The combine matrices used for combining CRCs.
     *                             These matrices are precomputed to facilitate efficient combination.
     * @return The combined CRC value representing the CRC for the concatenated data of both CRC values.
     * @throws IllegalArgumentException if {@code originalLengthOfCrc2} is negative.
     */
    public static long combine(long crc1, long crc2, long originalLengthOfCrc2, long[][] combineMatrices) {
        Validate.isNotNegative(originalLengthOfCrc2, "The length of the original data for the "
                                                    + "second CRC value must be positive.");
        if (originalLengthOfCrc2 == 0) {
            return crc1;
        }
        int matrixIndex = 2;
        while (originalLengthOfCrc2 != 0) {
            ++matrixIndex;
            if ((originalLengthOfCrc2 & 1) != 0) {
                crc1 = gf2MatrixTimes(combineMatrices[matrixIndex], crc1);
            }
            originalLengthOfCrc2 >>= 1;
        }
        crc1 ^= crc2;
        return crc1;
    }

    /**
     * Multiplies a Galois Field matrix with a vector.
     *
     * @param matrix The matrix to be multiplied.
     * @param vector The vector to be multiplied.
     * @return The result of the multiplication.
     */
    private static long gf2MatrixTimes(long[] matrix, long vector) {
        long sum = 0;
        for (long l : matrix) {
            if (vector == 0) {
                break;
            }
            if ((vector & 1) != 0) {
                sum ^= l;
            }
            vector >>= 1;
        }
        return sum;
    }

    private static void initializeFirstMatrix(long[][] combineMatrices, long polynomial) {
        combineMatrices[0][0] = polynomial;
        long row = 1;
        for (int i = 1; i < CRC_SIZE; i++) {
            combineMatrices[0][i] = row;
            row <<= 1;
        }
    }

    /**
     * Derives the remaining matrices for the combination process.
     *
     * @param combineMatrices The combine matrices to be derived.
     */
    private static void deriveRemainingMatrices(long[][] combineMatrices) {
        for (int i = 0; i < CRC_SIZE - 1; i++) {
            for (int j = 0; j < CRC_SIZE; j++) {
                combineMatrices[i + 1][j] = gf2MatrixTimes(combineMatrices[i], combineMatrices[i][j]);
            }
        }
    }

}
