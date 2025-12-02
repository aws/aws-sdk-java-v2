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
import software.amazon.awssdk.benchmark.apache4.Apache4Benchmark;
import software.amazon.awssdk.benchmark.core.CoreBenchmark;
import software.amazon.awssdk.benchmark.core.ObjectSize;
import software.amazon.awssdk.benchmark.core.S3BenchmarkHelper;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class Apache5Benchmark implements CoreBenchmark {
    private static final Logger logger = Logger.loggerFor(Apache5Benchmark.class);

    @Param({"50"})
    private int maxConnections;

    @Param("SMALL")
    private ObjectSize objectSize;

    @Param({"10"})
    private int threadCount;

    private S3Client s3Client;
    private S3BenchmarkHelper benchmarkHelper;
    private ExecutorService executorService;

    @Setup(Level.Trial)
    public void setup() {
        logger.info(() -> "Setting up Apache5 benchmark with maxConnections=" + maxConnections);

        // Apache 5 HTTP client
        SdkHttpClient httpClient = Apache5HttpClient.builder()
                                                    .connectionTimeout(Duration.ofSeconds(10))
                                                    .socketTimeout(Duration.ofSeconds(30))
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                    .maxConnections(maxConnections)
                                                    .build();

        // S3 client
        s3Client = S3Client.builder()
                           .region(Region.US_WEST_2)
                           .credentialsProvider(DefaultCredentialsProvider.create())
                           .httpClient(httpClient)
                           .build();

        benchmarkHelper = new S3BenchmarkHelper(Apache5Benchmark.class.getSimpleName(), s3Client);
        benchmarkHelper.setup();

        // Always use platform threads
        executorService = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r);
            t.setName("apache5-platform-worker-" + t.getId());
            return t;
        });

        logger.info(() -> "Using platform thread executor");

        logger.info(() -> "Apache5 benchmark setup complete");
    }

    @Benchmark
    public void simpleGet(Blackhole blackhole) {
        executeGet(blackhole);
    }

    @Benchmark
    public void simplePut(Blackhole blackhole) {
        executePut(blackhole);
    }

    @Benchmark
    @Override
    public void multiThreadedGet(Blackhole blackhole) throws Exception {
        List<Future<?>> futures = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    executeGet(blackhole);
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
                    executePut(blackhole);
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

    private void executeGet(Blackhole blackhole) {
        ResponseInputStream<GetObjectResponse> object = s3Client.getObject(
            r -> r.bucket(benchmarkHelper.bucketName()).key(benchmarkHelper.objKey(objectSize)));
        blackhole.consume(object.response());
        IoUtils.drainInputStream(object);
    }

    private void executePut(Blackhole blackhole) {
        PutObjectResponse response = s3Client.putObject(
            r -> r.bucket(benchmarkHelper.bucketName()).key("Apache4Benchmark-" + Thread.currentThread().getName()),
            benchmarkHelper.requestBody(objectSize));
        blackhole.consume(response);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        logger.info(() -> "Tearing down Apache5 benchmark");

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

        if (benchmarkHelper != null) {
            benchmarkHelper.cleanup();
        }

        if (s3Client != null) {
            s3Client.close();
        }
    }
}
