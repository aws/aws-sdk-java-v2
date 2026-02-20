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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * Benchmarks for testing CRT implementations of checksums.
 * <p>
 * There are pitfalls with passing buffers to and from native code since it could lead to lots of copying.
 */
public class CrtChecksumBenchmark {
    private static final int PAYLOAD_SIZE = 512 * BenchmarkConstant.MB;

    @State(Scope.Benchmark)
    public static class ChunkedState {
        private SdkChecksum crc;

        private byte[] chunkData;

        @Param({"SZ_512_KB",
                "SZ_1_MB",
                "SZ_2_MB",
                "SZ_4_MB",
                "SZ_8_MB",
                "SZ_16_MB",
                "SZ_32_MB",
                "SZ_64_MB",
                "SZ_128_MB",
                "SZ_256_MB"})
        private BenchmarkSize chunkSize;

        @Param({"CRC64NVME", "CRC32C_CRT"})
        private ChecksumAlgorithmParam algorithm;

        @Setup
        public void setup() {
            crc = algorithm.createChecksum();
            chunkData = new byte[chunkSize.getBytes()];
        }
    }

    /**
     * This benchmark approximates use cases where crc64.update(byte[], offset, length) is called where length is
     * smaller than the length of byte[]. For example, the application might have a *large* read buffer, but only fills
     * a small portion of it and calls crc64.update() with only that portion.
     */
    @State(Scope.Benchmark)
    public static class FixedInputBufferSizeState {
        @Param({"SZ_32_MB", "SZ_64_MB", "SZ_128_MB", "SZ_256_MB"})
        private BenchmarkSize fixedBufferSize;

        @Param({"SZ_512_KB", "SZ_1_MB", "SZ_2_MB"})
        private BenchmarkSize chunkSize;

        @Param({"CRC64NVME", "CRC32C_CRT", "XXHASH64", "XXHASH3", "XXHASH128"})
        private ChecksumAlgorithmParam algorithm;

        private byte[] buffer;

        private SdkChecksum checksum;

        @Setup
        public void setup() {
            buffer = new byte[fixedBufferSize.getBytes()];
            checksum = algorithm.createChecksum();
        }
    }

    @Benchmark
    public void fixedBufferSize(FixedInputBufferSizeState s, Blackhole bh) {
        int chunkSize = s.chunkSize.getBytes();

        int nChunks = PAYLOAD_SIZE / chunkSize;

        s.checksum = s.algorithm.createChecksum();
        for (int i = 0; i < nChunks; ++i) {
            s.checksum.update(s.buffer, 0, chunkSize);
        }
        bh.consume(s.checksum.getChecksumBytes());
    }
}