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

package software.amazon.awssdk.benchmark;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.core.checksums.Crc32Checksum;
import software.amazon.awssdk.core.checksums.Crc32Checksum2;
import software.amazon.awssdk.core.checksums.SdkChecksum;

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class Crc32Benchmark {

    @State(Scope.Thread)
    public static class ChecksumState {
        private static byte[] smallPayload = randomBytes(10);
        private static byte[] mediumPayload = randomBytes(1000);
        private static byte[] bigPayload = randomBytes(1000000);

        @Param({"small", "medium", "big"})
        public String size;

        @Param({"sdk", "crt", "java"})
        public String checksumProvider;

        private byte[] payload;
        private Supplier<SdkChecksum> sdkChecksum;

        @Setup(Level.Trial)
        public void setup() {
            switch (checksumProvider) {
                case "crt":
                    sdkChecksum = () -> new Crc32Checksum(true); // CRT implementation
                    break;
                case "sdk":
                    sdkChecksum = () -> new Crc32Checksum(false); // SDK implementation
                    break;
                case "java":
                    sdkChecksum = () -> new Crc32Checksum2(); // JDK implementation
                    break;
                default:
                    throw new IllegalArgumentException("Invalid checksumProvider: " + checksumProvider);
            }

            switch (size) {
                case "small":
                    payload = smallPayload;
                    break;
                case "medium":
                    payload = mediumPayload;
                    break;
                case "big":
                    payload = bigPayload;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid size: " + size);
            }
        }

        private static byte[] randomBytes(int i) {
            Random random = new Random(0);
            byte[] bytes = new byte[i];
            random.nextBytes(bytes);
            return bytes;
        }
    }

    // @Benchmark
    // public void performanceWithoutMarkReset(ChecksumState state, Blackhole blackhole) {
    //     SdkChecksum checksum = state.sdkChecksum.get();
    //     checksum.update(state.payload);
    //     blackhole.consume(checksum.getValue());
    // }

    @Benchmark
    public void performanceWithMarkReset(ChecksumState state, Blackhole blackhole) {
        SdkChecksum checksum = state.sdkChecksum.get();
        int midwayPoint = state.payload.length / 2;
        checksum.update(state.payload, 0, midwayPoint);
        checksum.mark(0);
        checksum.reset();
        checksum.update(state.payload, midwayPoint, state.payload.length - midwayPoint);
        blackhole.consume(checksum.getValue());
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(Crc32Benchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}
