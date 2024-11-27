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

import java.util.Locale;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ChecksumBenchmark {
    @State(Scope.Thread)
    public static class ChecksumState {

        @Param( {"128B", "4KB", "128KB", "1MB"})
        public String size;

        @Param( {"MD5", "CRC32", "CRC32C", "SHA1", "SHA256","CRC64NVME"})
        public String checksumProvider;

        private byte[] payload;
        private SdkChecksum sdkChecksum;

        @Setup(Level.Trial)
        public void setup() {
            // Initialize the correct checksum provider based on the parameter
            switch (checksumProvider.toUpperCase(Locale.ROOT)) {
                case "MD5":
                    sdkChecksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.MD5);
                    break;
                case "CRC32":
                    sdkChecksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);
                    break;
                case "CRC32C":
                    sdkChecksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32C);
                    break;
                case "SHA1":
                    sdkChecksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.SHA1);
                    break;
                case "SHA256":
                    sdkChecksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.SHA256);
                    break;
                case "CRC64NVME":
                    sdkChecksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC64NVME);
                    break;

                default:
                    throw new IllegalArgumentException("Invalid checksumProvider: " + checksumProvider);
            }
            // Initialize the payload based on the size parameter
            switch (size) {
                case "128B":
                    payload = generateStringOfSize(128).getBytes(StandardCharsets.UTF_8);
                    break;
                case "4KB":
                    payload = generateStringOfSize(4 * 1024).getBytes(StandardCharsets.UTF_8);
                    break;
                case "128KB":
                    payload = generateStringOfSize(128 * 1024).getBytes(StandardCharsets.UTF_8);
                    break;
                case "1MB":
                    payload = generateStringOfSize(1000 * 1024).getBytes(StandardCharsets.UTF_8);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid size: " + size);
            }
        }
    }

    @Benchmark
    public void updateEntireByteArrayChecksum(ChecksumState state, Blackhole blackhole) {
        state.sdkChecksum.update(state.payload);
        state.sdkChecksum.getChecksumBytes();
        blackhole.consume(state.sdkChecksum);
    }

    @TearDown
    public void tearDown(ChecksumState state) {
        state.sdkChecksum.reset();
    }

    @Benchmark
    public void updateIndividualByteChecksumOneByteATime(ChecksumState state, Blackhole blackhole) {
        for (byte b : state.payload) {
            state.sdkChecksum.update(b);
        }
        state.sdkChecksum.getChecksumBytes();
        blackhole.consume(state.sdkChecksum);
    }

    private static String generateStringOfSize(int byteSize) {
        String result = new String(new char[byteSize]).replace('\0', 'A'); // Approximate
        while (result.getBytes(StandardCharsets.UTF_8).length > byteSize) {
            result = result.substring(0, result.length() - 1); // Adjust to exact size
        }
        return result;
    }
}
