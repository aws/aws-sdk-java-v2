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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.checksums.SdkChecksum;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SdkChecksumBenchmark {

    @State(Scope.Benchmark)
    public static class ChunkedState {
        private SdkChecksum checksum;

        private byte[] chunkData;

        /**
         * The size of each chunk. 128KB is the chunk size used in AWS chunked encoding stream. Add other sizes as needed for
         * experiment.
         */
        @Param({"SZ_128_KB", "SZ_512_KB", "SZ_1_MB"})
        private BenchmarkSize chunkSize;

        /**
         * The size of the payload. Add other sizes as needed for
         * experiment.
         */
        @Param({"SZ_8_MB", "SZ_32_MB"})
        private BenchmarkSize payloadSize;

        @Param({"XXHASH64", "XXHASH3", "XXHASH128", "SHA512", "CRC32C", "CRC32", "CRC64NVME", "SHA1", "SHA256"})
        private ChecksumAlgorithmParam checksumAlgorithmParam;

        @Setup
        public void setup() {
            checksum = checksumAlgorithmParam.createChecksum();
            chunkData = new byte[chunkSize.getBytes()];
        }
    }

    @Benchmark
    public void chunked(ChunkedState s, Blackhole bh) {
        int chunkSize = s.chunkSize.getBytes();

        int nChunks = s.payloadSize.getBytes() / chunkSize;

        s.checksum = s.checksumAlgorithmParam.createChecksum();
        for (int i = 0; i < nChunks; ++i) {
            s.checksum.update(s.chunkData);
        }
        bh.consume(s.checksum.getChecksumBytes());
    }
}