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
import software.amazon.awssdk.http.apache.ApacheHttpClient;
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
public class Apache4Benchmark implements CoreBenchmark {
    private static final Logger logger = Logger.getLogger(Apache4Benchmark.class.getName());

    @Param({"50","200"})
    private int maxConnections;

    @Param({"10"})
    private int threadCount;

    private S3Client s3Client;
    private S3BenchmarkImpl benchmark;
    private ExecutorService platformThreadPool;

    @Setup(Level.Trial)
    public void setup() {
        logger.info("Setting up Apache4 benchmark with maxConnections=" + maxConnections);

        // Apache 4 HTTP client
        SdkHttpClient httpClient = ApacheHttpClient.builder()
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

        // Platform thread pool for multi-threaded tests
        platformThreadPool = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r);
            t.setName("apache4-platform-worker-" + t.getId());
            return t;
        });

        logger.info("Apache4 benchmark setup complete");
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
        runMultiThreaded(platformThreadPool, threadCount, blackhole, true);
    }

    @Benchmark
    @Override
    public void multiThreadedPut(Blackhole blackhole) throws Exception {
        runMultiThreaded(platformThreadPool, threadCount, blackhole, false);
    }

    protected void runMultiThreaded(ExecutorService executor, int threads,
                                    Blackhole blackhole, boolean isGet) throws Exception {
        List<Future<?>> futures = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    if (isGet) {
                        benchmark.executeGet("medium", blackhole);
                    } else {
                        benchmark.executePut("medium", blackhole);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Operation failed", e);
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
        logger.info("Tearing down Apache4 benchmark");

        if (platformThreadPool != null) {
            platformThreadPool.shutdown();
            try {
                if (!platformThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    platformThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                platformThreadPool.shutdownNow();
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
