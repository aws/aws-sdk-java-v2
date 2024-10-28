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

/**
 * Functional interface for combining two CRC values along with the data length.
 */
@SdkInternalApi
@FunctionalInterface
public interface CrcCombineFunction {
    /**
     * Combines two CRC values along with the data length to produce the resulting combined checksum.
     *
     * @param crc1 The first CRC value.
     * @param crc2 The second CRC value.
     * @param length The length of the data.
     * @return The combined CRC value.
     */
    long combine(long crc1, long crc2, long length);
}
