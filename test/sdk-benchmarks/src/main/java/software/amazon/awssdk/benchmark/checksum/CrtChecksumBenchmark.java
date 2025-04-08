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
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.internal.CrcCloneOnMarkChecksum;
import software.amazon.awssdk.crt.checksums.CRC32C;

/**
 * Benchmarks for testing CRT implementations of checksums.
 * <p>
 * There are pitfalls with passing buffers to and from native code since it could lead to lots of copying.
 */
public class CrtChecksumBenchmark {
    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    private static final int PAYLOAD_SIZE = 512 * MB;

    public enum Algorithm {
        CRC32C,
        CRC64,
        ;
    }

    public enum Size {
        SZ_512_KB(512 * KB),
        SZ_1_MB(MB),
        SZ_2_MB(2 * MB),
        SZ_4_MB(4 * MB),
        SZ_8_MB(8 * MB),
        SZ_16_MB(16 * MB),
        SZ_32_MB(32 * MB),
        SZ_64_MB(64 * MB),
        SZ_128_MB(128 * MB),
        SZ_256_MB(256 * MB);

        private int numBytes;

        private Size(int bytes) {

            this.numBytes = bytes;
        }

        public int getNumBytes() {
            return numBytes;
        }
    }

    @State(Scope.Benchmark)
    public static class ChunkedState {
        private SdkChecksum crc;

        private byte[] chunkData;

        @Param({"SZ_512_KB", "SZ_1_MB", "SZ_2_MB", "SZ_4_MB", "SZ_8_MB", "SZ_16_MB", "SZ_32_MB", "SZ_64_MB", "SZ_128_MB", "SZ_256_MB"})
        private Size chunkSize;

        @Param({"CRC64", "CRC32C"})
        private Algorithm algorithm;

        @Setup
        public void setup () {
            crc = getSdkChecksum(algorithm);
            chunkData = new byte[chunkSize.getNumBytes()];
        }
    }

    /**
     * Approximates where we feed a fixed number of bytes (PAYLOAD_BYTES), using different chunk sizes.
     */
    @Benchmark
    public void chunked(ChunkedState s, Blackhole bh) {
        int chunkSize = s.chunkSize.getNumBytes();

        int nChunks = PAYLOAD_SIZE / chunkSize;

        s.crc.reset();
        for (int i = 0; i < nChunks; ++i) {
            s.crc.update(s.chunkData);
        }
        bh.consume(s.crc.getChecksumBytes());
    }

    /**
     * This benchmark approximates use cases where crc64.update(byte[], offset, length) is called where length is
     * smaller than the length of byte[]. For example, the application might have a *large* read buffer, but only fills
     * a small portion of it and calls crc64.update() with only that portion.
     */
    @State(Scope.Benchmark)
    public static class FixedInputBufferSizeState {
        @Param({"SZ_32_MB", "SZ_64_MB", "SZ_128_MB", "SZ_256_MB"})
        private Size fixedBufferSize;

        @Param({"SZ_512_KB", "SZ_1_MB", "SZ_2_MB"})
        private Size chunkSize;

        @Param({"CRC64", "CRC32C"})
        private Algorithm algorithm;

        private byte[] buffer;

        private SdkChecksum crc;

        @Setup
        public void setup() {
            buffer = new byte[fixedBufferSize.getNumBytes()];
            crc = getSdkChecksum(algorithm);
        }
    }

    @Benchmark
    public void fixedBufferSize(FixedInputBufferSizeState s, Blackhole bh) {
        int chunkSize = s.chunkSize.getNumBytes();

        int nChunks = PAYLOAD_SIZE / chunkSize;

        s.crc.reset();
        for (int i = 0; i < nChunks; ++i) {
            s.crc.update(s.buffer, 0, chunkSize);
        }
        bh.consume(s.crc.getChecksumBytes());
    }

    private static SdkChecksum getSdkChecksum(Algorithm algorithm) {
        switch (algorithm) {
            case CRC32C:
                // Construct directly instead of using forAlgorithm because it
                // will pick up the JVM provided one if it's available.
                return new CrcCloneOnMarkChecksum(new CRC32C());
            case CRC64:
                return SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC64NVME);
            default:
                throw new RuntimeException("Unsupported algorithm: " + algorithm);
        }
    }
}