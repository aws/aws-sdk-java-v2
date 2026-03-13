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

import static software.amazon.awssdk.benchmark.apache5.utility.BenchmarkUtilities.isJava21OrHigher;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.benchmark.core.ObjectSize;
import software.amazon.awssdk.benchmark.core.S3BenchmarkHelper;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.JavaSystemSetting;

/**
 * Async http client benchmark using virtual threads. This class requires Java 21+.
 */
@Fork(jvmArgsAppend = "-Djdk.tracePinnedThreads=full")
@State(Scope.Benchmark)
public class AsyncVirtualThreadBenchmark {
    // We redirect standard out to a file for the -Djdk.tracePinnedThreads=full option. When virtual threads become pinned,
    // the JDK will print out the stacktrace through standard out. However, because JMH runs benchmarks in a forked JVM
    // (unless you specify -f 0, which is not recommended by JMH), that output is lost. Redirect standard out to a file so
    // that any time a thread is pinned, the stack trace is written to the file instead,which can be inspected after the
    // benchmark run.
    static {
        try {
            Path tmp = Paths.get(AsyncVirtualThreadBenchmark.class.getSimpleName() + "-stdout-" + UUID.randomUUID() + ".log");
            PrintStream fileOut = new PrintStream(
                Files.newOutputStream(tmp, StandardOpenOption.APPEND, StandardOpenOption.CREATE));
            System.setOut(fileOut);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create STDOUT file", e);
        }
    }

    public enum Client {
        Netty,
        Crt
    }

    @Param("50")
    private int maxConnections;

    @Param("SMALL")
    private ObjectSize objectSize;

    @Param({"Netty", "Crt"})
    private Client client;

    private S3AsyncClient s3AsyncClient;
    private S3BenchmarkHelper benchmark;
    private ExecutorService virtualThreadExecutor;
    private String putKeyPrefix;

    @Setup(Level.Trial)
    public void setup() {
        if (!isJava21OrHigher()) {
            throw new UnsupportedOperationException(
                "Virtual threads require Java 21 or higher. Current version: " + JavaSystemSetting.JAVA_VERSION);
        }

        SdkAsyncHttpClient.Builder<?> httpClientBuilder = httpClientBuilder();

        s3AsyncClient = S3AsyncClient.builder()
                           .region(Region.US_WEST_2)
                           .credentialsProvider(DefaultCredentialsProvider.create())
                           .httpClient(configure(httpClientBuilder))
                           .build();

        String benchmarkName = AsyncVirtualThreadBenchmark.class.getSimpleName();

        benchmark = new S3BenchmarkHelper(benchmarkName, s3AsyncClient);
        benchmark.setup();

        virtualThreadExecutor = createVirtualThreadExecutor();

        putKeyPrefix = benchmarkName + "-";
    }

    private SdkAsyncHttpClient configure(SdkAsyncHttpClient.Builder<?> builder) {
        AttributeMap config = AttributeMap.builder()
            .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, maxConnections)
            .build();

        return builder.buildWithDefaults(config);
    }

    private ExecutorService createVirtualThreadExecutor() {
        try {
            // Use reflection to call Executors.newVirtualThreadPerTaskExecutor()
            Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            return (ExecutorService) method.invoke(null);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(
                "Virtual threads are not available in this Java version. " +
                "This benchmark requires Java 21 or higher.", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create virtual thread executor", e);
        }
    }

    @Benchmark
    public void getObject(Blackhole blackhole) {
        safeExecute(() -> {
            ResponseInputStream<GetObjectResponse> object = s3AsyncClient.getObject(
                r -> r.bucket(benchmark.bucketName()).key(benchmark.objKey(objectSize)),
                AsyncResponseTransformer.toBlockingInputStream()).join();
            blackhole.consume(object.response());
            IoUtils.drainInputStream(object);
        });
    }

    @Benchmark
    public void putObject(Blackhole blackhole) {
        String jmhThreadName = Thread.currentThread().getName();
        safeExecute(() -> {
            PutObjectResponse response = s3AsyncClient.putObject(
                r -> r.bucket(benchmark.bucketName()).key(putKeyPrefix + jmhThreadName),
                benchmark.asyncRequestBody(objectSize)).join();
            blackhole.consume(response);
        });
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (virtualThreadExecutor != null) {
            virtualThreadExecutor.shutdown();
            try {
                if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    virtualThreadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                virtualThreadExecutor.shutdownNow();
            }
        }

        if (benchmark != null) {
            benchmark.cleanup();
        }

        if (s3AsyncClient != null) {
            s3AsyncClient.close();
        }
    }

    private void safeExecute(Runnable runnable) {
        try {
            virtualThreadExecutor.submit(runnable).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error during execution", e);
        }
    }

    private SdkAsyncHttpClient.Builder<?> httpClientBuilder() {
        switch (client) {
            case Netty:
                return NettyNioAsyncHttpClient.builder();
            case Crt:
                return AwsCrtAsyncHttpClient.builder();
            default:
                throw new IllegalArgumentException("Unknown HTTP client: " + client);
        }
    }
}
