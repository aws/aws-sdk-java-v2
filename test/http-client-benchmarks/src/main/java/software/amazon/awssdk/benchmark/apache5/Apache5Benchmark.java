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

package software.amazon.awssdk.benchmark.apache5;


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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.benchmark.core.CoreBenchmark;
import software.amazon.awssdk.benchmark.core.S3BenchmarkImpl;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class Apache5Benchmark implements CoreBenchmark {
    private static final Logger logger = Logger.getLogger(Apache5Benchmark.class.getName());

    @Param({"50"})
    private int maxConnections;

    @Param({"10"})
    private int threadCount;

    @Param({"platform", "virtual"})
    private String executorType;

    private S3Client s3Client;
    private S3BenchmarkImpl benchmark;
    private ExecutorService executorService;

    @Setup(Level.Trial)
    public void setup() {
        logger.info("Setting up Apache5 benchmark with maxConnections=" + maxConnections +
                    ", executorType=" + executorType);

        // Apache 5 HTTP client
        SdkHttpClient httpClient = Apache5HttpClient.builder()
                                                    .maxConnections(maxConnections)
                                                    .connectionTimeout(Duration.ofSeconds(10))
                                                    .socketTimeout(Duration.ofSeconds(30))
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                    .connectionMaxIdleTime(Duration.ofSeconds(60))
                                                    .connectionTimeToLive(Duration.ofMinutes(5))
                                                    .useIdleConnectionReaper(true)
                                                    .build();

        // S3 client
        s3Client = S3Client.builder()
                           .region(Region.US_WEST_2)
                           .credentialsProvider(DefaultCredentialsProvider.create())
                           .httpClient(httpClient)
                           .build();

        // Initialize benchmark implementation
        benchmark = new S3BenchmarkImpl(s3Client);
        benchmark.setup();

        // Create appropriate executor based on parameter
        if ("virtual".equals(executorType)) {
            executorService = Executors.newVirtualThreadPerTaskExecutor();
            logger.info("Using virtual thread executor");
        } else {
            executorService = Executors.newFixedThreadPool(threadCount, r -> {
                Thread t = new Thread(r);
                t.setName("apache5-platform-worker-" + t.getId());
                return t;
            });
            logger.info("Using platform thread executor");
        }

        logger.info("Apache5 benchmark setup complete");
    }

    @Benchmark
    @Override
    public void simpleGet(Blackhole blackhole) throws Exception {
        benchmark.executeGet("medium", blackhole);
    }

    @Benchmark
    @Override
    public void simplePut(Blackhole blackhole) throws Exception {
        benchmark.executePut("medium", blackhole);
    }

    @Benchmark
    @Override
    public void multiThreadedGet(Blackhole blackhole) throws Exception {
        List<Future<?>> futures = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    benchmark.executeGet("medium", blackhole);
                } catch (Exception e) {
                    throw new RuntimeException("GET operation failed", e);
                }
            }));
        }

        // Wait for all operations to complete
        for (Future<?> future : futures) {
            future.get();
        }
    }

    @Benchmark
    @Override
    public void multiThreadedPut(Blackhole blackhole) throws Exception {
        List<Future<?>> futures = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    benchmark.executePut("medium", blackhole);
                } catch (Exception e) {
                    throw new RuntimeException("PUT operation failed", e);
                }
            }));
        }

        // Wait for all operations to complete
        for (Future<?> future : futures) {
            future.get();
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        logger.info("Tearing down Apache5 benchmark");

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        if (benchmark != null) {
            benchmark.cleanup();
        }

        if (s3Client != null) {
            s3Client.close();
        }
    }
}
