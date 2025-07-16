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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.benchmark.core.CoreBenchmark;
import software.amazon.awssdk.benchmark.core.S3BenchmarkImpl;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.Logger;

/**
 * Apache5 benchmark using virtual threads. This class requires Java 21+.
 */

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class Apache5VirtualBenchmark implements CoreBenchmark {
    private static final Logger logger = Logger.loggerFor(Apache5VirtualBenchmark.class);

    @Param({"50"})
    private int maxConnections;

    @Param({"10"})
    private int threadCount;

    private S3Client s3Client;
    private S3BenchmarkImpl s3Benchmark;
    private ExecutorService executor;

    @Setup(Level.Trial)
    public void setup() {
        // CHECKSTYLE:OFF
        String version = System.getProperty("java.version");
        // CHECKSTYLE:ON
        logger.info(() -> "Running on Java version: " + version);
        
        if (!isJava21OrHigher()) {
            throw new UnsupportedOperationException("Virtual threads benchmark requires Java 21 or higher");
        }

        logger.info(() -> "Setting up Apache5 virtual threads benchmark with " + threadCount + " threads");

        SdkHttpClient httpClient = Apache5HttpClient.builder()
                                                    .connectionTimeout(Duration.ofSeconds(10))
                                                    .socketTimeout(Duration.ofSeconds(30))
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                    .maxConnections(maxConnections)
                                                    .build();

        s3Client = S3Client.builder()
                           .region(Region.US_WEST_2)
                           .credentialsProvider(DefaultCredentialsProvider.create())
                           .httpClient(httpClient)
                           .build();

        s3Benchmark = new S3BenchmarkImpl(s3Client);
        s3Benchmark.setup();

        // Create virtual thread executor using reflection to maintain compatibility with Java < 21
        executor = createVirtualThreadExecutor();
    }

    @TearDown(Level.Trial)
    public void teardown() {
        logger.info(() -> "Tearing down Apache5 virtual threads benchmark");
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

    private boolean isJava21OrHigher() {
        // CHECKSTYLE:OFF
        String version = System.getProperty("java.version");
        // CHECKSTYLE:ON
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dotPos = version.indexOf('.');
        int majorVersion;
        if (dotPos != -1) {
            majorVersion = Integer.parseInt(version.substring(0, dotPos));
        } else {
            majorVersion = Integer.parseInt(version);
        }
        return majorVersion >= 21;
    }

    private ExecutorService createVirtualThreadExecutor() {
        try {
            // Use reflection to access Java 21 virtual thread factory
            Class<?> executorsClass = Executors.class;
            Method newVirtualThreadPerTaskExecutor = executorsClass.getMethod("newVirtualThreadPerTaskExecutor");
            return (ExecutorService) newVirtualThreadPerTaskExecutor.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.error(() -> "Failed to create virtual thread executor: " + e.getMessage(), e);
            throw new UnsupportedOperationException("Virtual threads not supported in this Java version", e);
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Override
    public void simpleGet(Blackhole blackhole) throws Exception {
        s3Benchmark.executeGet("5MB", blackhole);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Override
    public void simplePut(Blackhole blackhole) {
        s3Benchmark.executePut("5MB", blackhole);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
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
    @OutputTimeUnit(TimeUnit.SECONDS)
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
