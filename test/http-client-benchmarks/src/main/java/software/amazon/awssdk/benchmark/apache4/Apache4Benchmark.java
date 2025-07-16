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

package software.amazon.awssdk.benchmark.apache4;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import software.amazon.awssdk.benchmark.core.CoreBenchmark;
import software.amazon.awssdk.benchmark.core.S3BenchmarkImpl;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.Logger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class Apache4Benchmark implements CoreBenchmark {
    private static final Logger logger = Logger.loggerFor(Apache4Benchmark.class);

    @Param({"50"})
    private int maxConnections;

    @Param({"20"})
    private int threadCount;

    private S3Client s3Client;
    private S3BenchmarkImpl s3Benchmark;
    private ExecutorService executor;

    @Setup(Level.Trial)
    public void setup() {
        logger.info(() -> "Setting up Apache4 benchmark with " + threadCount + " threads");

        SdkHttpClient httpClient = ApacheHttpClient.builder()
                                                   .connectionTimeout(Duration.ofSeconds(10))
                                                   .socketTimeout(Duration.ofSeconds(30))
                                                   .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                   .maxConnections(maxConnections)
                                                   .build();

        s3Client = S3Client.builder()
                           .region(Region.US_WEST_2)
                           .httpClient(httpClient)
                           .build();

        s3Benchmark = new S3BenchmarkImpl(s3Client);
        s3Benchmark.setup();

        executor = Executors.newFixedThreadPool(threadCount);
    }

    @TearDown(Level.Trial)
    public void teardown() {
        logger.info(() -> "Tearing down Apache4 benchmark");
        s3Benchmark.cleanup();
        s3Client.close();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Benchmark
    @Override
    public void simpleGet(Blackhole blackhole) throws Exception {
        s3Benchmark.executeGet("5MB", blackhole);
    }

    @Benchmark
    @Override
    public void simplePut(Blackhole blackhole) {
        s3Benchmark.executePut("5MB", blackhole);
    }

    @Benchmark
    @Override
    public void multiThreadedGet(Blackhole blackhole) throws Exception {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    s3Benchmark.executeGet("5MB", blackhole);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
    }

    @Benchmark
    @Override
    public void multiThreadedPut(Blackhole blackhole) throws Exception {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> s3Benchmark.executePut("5MB", blackhole)));
        }

        for (Future<?> future : futures) {
            future.get();
        }
    }
}
