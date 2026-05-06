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
import java.util.concurrent.CompletableFuture;
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
import software.amazon.awssdk.benchmark.core.ObjectSize;
import software.amazon.awssdk.benchmark.core.S3BenchmarkHelper;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.apache5.Apache5NioAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.Logger;

/**
 * Async benchmark comparing Apache5NioAsyncHttpClient vs NettyNioAsyncHttpClient.
 *
 * <p>True async benchmarking: each benchmark method fires {@code concurrency} requests
 * concurrently as CompletableFutures and waits for all to complete. This measures
 * real async throughput rather than sequential request latency.
 *
 * <p>Compare with {@link Apache5Benchmark} (sync) to see async vs sync throughput.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class Apache5NioAsyncBenchmark {
    private static final Logger logger = Logger.loggerFor(Apache5NioAsyncBenchmark.class);

    public enum Client {
        Apache5Nio,
        Netty
    }

    @Param({"Apache5Nio", "Netty"})
    private Client client;

    @Param({"50"})
    private int maxConnections;

    @Param("SMALL")
    private ObjectSize objectSize;

    /**
     * Number of concurrent async requests fired per benchmark invocation.
     * Higher values stress the async pipeline more.
     */
    @Param({"10"})
    private int concurrency;

    private S3AsyncClient s3AsyncClient;
    private S3BenchmarkHelper benchmarkHelper;

    @Setup(Level.Trial)
    public void setup() {
        logger.info(() -> "Setting up async benchmark: client=" + client
                          + " maxConnections=" + maxConnections
                          + " concurrency=" + concurrency);

        SdkAsyncHttpClient httpClient = buildHttpClient();

        s3AsyncClient = S3AsyncClient.builder()
                                     .region(Region.US_WEST_2)
                                     .credentialsProvider(DefaultCredentialsProvider.create())
                                     .httpClient(httpClient)
                                     .build();

        benchmarkHelper = new S3BenchmarkHelper(
            Apache5NioAsyncBenchmark.class.getSimpleName() + "-" + client.name(), s3AsyncClient);
        benchmarkHelper.setup();

        logger.info(() -> "Async benchmark setup complete");
    }

    /**
     * Fires {@code concurrency} GET requests concurrently and waits for all.
     * This is true async benchmarking — the HTTP client handles all requests
     * in parallel on its own NIO threads.
     */
    @Benchmark
    public void concurrentGet(Blackhole blackhole) throws Exception {
        List<CompletableFuture<GetObjectResponse>> futures = new ArrayList<>(concurrency);

        for (int i = 0; i < concurrency; i++) {
            futures.add(
                s3AsyncClient.getObject(
                    r -> r.bucket(benchmarkHelper.bucketName()).key(benchmarkHelper.objKey(objectSize)),
                    AsyncResponseTransformer.toBytes()
                ).thenApply(bytes -> {
                    blackhole.consume(bytes.asByteArray());
                    return bytes.response();
                })
            );
        }

        // Wait for all concurrent requests — this is the key difference from sync:
        // all requests are in-flight simultaneously on the async client's NIO threads.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Fires {@code concurrency} PUT requests concurrently and waits for all.
     */
    @Benchmark
    public void concurrentPut(Blackhole blackhole) throws Exception {
        List<CompletableFuture<PutObjectResponse>> futures = new ArrayList<>(concurrency);
        String keyPrefix = client.name() + "-put-" + Thread.currentThread().getName() + "-";

        for (int i = 0; i < concurrency; i++) {
            int idx = i;
            futures.add(
                s3AsyncClient.putObject(
                    r -> r.bucket(benchmarkHelper.bucketName()).key(keyPrefix + idx),
                    benchmarkHelper.asyncRequestBody(objectSize)
                ).thenApply(resp -> {
                    blackhole.consume(resp);
                    return resp;
                })
            );
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Single async GET — equivalent to Apache5Benchmark.simpleGet() but async.
     * Useful for direct latency comparison with the sync benchmark.
     */
    @Benchmark
    public void simpleGet(Blackhole blackhole) throws Exception {
        GetObjectResponse response = s3AsyncClient.getObject(
            r -> r.bucket(benchmarkHelper.bucketName()).key(benchmarkHelper.objKey(objectSize)),
            AsyncResponseTransformer.toBytes()
        ).thenApply(bytes -> {
            blackhole.consume(bytes.asByteArray());
            return bytes.response();
        }).join();
        blackhole.consume(response);
    }

    /**
     * Single async PUT — equivalent to Apache5Benchmark.simplePut() but async.
     */
    @Benchmark
    public void simplePut(Blackhole blackhole) throws Exception {
        PutObjectResponse response = s3AsyncClient.putObject(
            r -> r.bucket(benchmarkHelper.bucketName()).key(client.name() + "-simple-put"),
            benchmarkHelper.asyncRequestBody(objectSize)
        ).join();
        blackhole.consume(response);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        logger.info(() -> "Tearing down async benchmark");

        if (benchmarkHelper != null) {
            benchmarkHelper.cleanup();
        }

        if (s3AsyncClient != null) {
            s3AsyncClient.close();
        }
    }

    private SdkAsyncHttpClient buildHttpClient() {
        switch (client) {
            case Apache5Nio:
                return Apache5NioAsyncHttpClient.builder()
                                                .maxConnections(maxConnections)
                                                .socketTimeout(Duration.ofSeconds(30))
                                                .connectionTimeout(Duration.ofSeconds(10))
                                                .build();
            case Netty:
                return NettyNioAsyncHttpClient.builder()
                                              .maxConcurrency(maxConnections)
                                              .connectionTimeout(Duration.ofSeconds(10))
                                              .readTimeout(Duration.ofSeconds(30))
                                              .build();
            default:
                throw new IllegalArgumentException("Unknown client: " + client);
        }
    }
}
