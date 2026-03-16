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

package software.amazon.awssdk.benchmark.checksum;

import static software.amazon.awssdk.benchmark.checksum.BenchmarkConstant.KB;
import static software.amazon.awssdk.benchmark.checksum.BenchmarkConstant.MB;

public enum BenchmarkSize {
    SZ_128_KB(128 * KB),
    SZ_512_KB(512 * KB),
    SZ_1_MB(1 * MB),
    SZ_2_MB(2 * MB),
    SZ_4_MB(4 * MB),
    SZ_8_MB(8 * MB),
    SZ_16_MB(16 * MB),
    SZ_32_MB(32 * MB),
    SZ_64_MB(64 * MB),
    SZ_128_MB(128 * MB),
    SZ_256_MB(256 * MB);

    private final int bytes;

    BenchmarkSize(int bytes) {
        this.bytes = bytes;
    }

    public int getBytes() {
        return bytes;
    }
}
