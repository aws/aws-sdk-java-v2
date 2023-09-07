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

package software.amazon.awssdk.s3benchmarks;

import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.BENCHMARK_ITERATIONS;
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.COPY_SUFFIX;
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.DEFAULT_TIMEOUT;
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.WARMUP_KEY;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.Logger;

public abstract class BaseTransferManagerBenchmark implements TransferManagerBenchmark {

    protected static final int WARMUP_ITERATIONS = 10;

    private static final Logger logger = Logger.loggerFor("TransferManagerBenchmark");

    protected final S3TransferManager transferManager;
    protected final S3AsyncClient s3;
    protected final S3Client s3Sync;
    protected final String bucket;
    protected final String key;
    protected final String path;
    protected final int iteration;
    protected final Duration timeout;
    private final File file;

    BaseTransferManagerBenchmark(TransferManagerBenchmarkConfig config) {
        logger.info(() -> "Benchmark config: " + config);
        Long partSizeInMb = config.partSizeInMb() == null ? null : config.partSizeInMb() * MB;
        Long readBufferSizeInMb = config.readBufferSizeInMb() == null ? null : config.readBufferSizeInMb() * MB;
        S3CrtAsyncClientBuilder builder = S3CrtAsyncClient.builder()
                                                          .targetThroughputInGbps(config.targetThroughput())
                                                          .minimumPartSizeInBytes(partSizeInMb)
                                                          .initialReadBufferSizeInBytes(readBufferSizeInMb)
                                                          .targetThroughputInGbps(config.targetThroughput() == null ?
                                                                                  Double.valueOf(100.0) :
                                                                                  config.targetThroughput());
        if (config.maxConcurrency() != null) {
            builder.maxConcurrency(config.maxConcurrency());
        }
        s3 = builder.build();
        s3Sync = S3Client.builder().build();
        transferManager = S3TransferManager.builder()
                                           .s3Client(s3)
                                           .build();
        bucket = config.bucket();
        key = config.key();
        path = config.filePath();
        iteration = config.iteration() == null ? BENCHMARK_ITERATIONS : config.iteration();
        timeout = config.timeout() == null ? DEFAULT_TIMEOUT : config.timeout();
        try {
            file = new RandomTempFile(10 * MB);
            file.deleteOnExit();
        } catch (IOException e) {
            logger.error(() -> "Failed to create the file");
            throw new RuntimeException("Failed to create the temp file", e);
        }
    }

    @Override
    public void run() {
        try {
            warmUp();
            doRunBenchmark();
        } catch (Exception e) {
            logger.error(() -> "Exception occurred", e);
        } finally {
            cleanup();
        }
    }

    /**
     * Hook method to allow subclasses to add additional warm up
     */
    protected void additionalWarmup() throws Exception {
        // default to no-op
    }

    protected abstract void doRunBenchmark();

    private void cleanup() {
        try {
            s3Sync.deleteObject(b -> b.bucket(bucket).key(WARMUP_KEY));
        } catch (Exception exception) {
            logger.error(() -> "Failed to delete object: " + WARMUP_KEY);
        }

        String copyObject = WARMUP_KEY + COPY_SUFFIX;
        try {
            s3Sync.deleteObject(b -> b.bucket(bucket).key(copyObject));
        } catch (Exception exception) {
            logger.error(() -> "Failed to delete object: " + copyObject);
        }

        s3.close();
        s3Sync.close();
        transferManager.close();
    }

    private void warmUp() throws Exception {
        logger.info(() -> "Starting to warm up");

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            warmUpUploadBatch();
            warmUpDownloadBatch();
            warmUpCopyBatch();

            Thread.sleep(500);
        }
        additionalWarmup();
        logger.info(() -> "Ending warm up");
    }

    private void warmUpCopyBatch() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(transferManager.copy(
                                           c -> c.copyObjectRequest(r -> r.sourceKey(WARMUP_KEY)
                                                                          .sourceBucket(bucket)
                                                                          .destinationKey(WARMUP_KEY + "_copy")
                                                                          .destinationBucket(bucket)))
                                       .completionFuture());
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }

    private void warmUpDownloadBatch() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Path tempFile = RandomTempFile.randomUncreatedFile().toPath();
            futures.add(s3.getObject(GetObjectRequest.builder().bucket(bucket).key(WARMUP_KEY).build(),
                                     AsyncResponseTransformer.toFile(tempFile)).whenComplete((r, t) -> runAndLogError(
                logger.logger(), "Deleting file failed", () -> Files.delete(tempFile))));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }

    private void warmUpUploadBatch() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(s3.putObject(PutObjectRequest.builder().bucket(bucket).key(WARMUP_KEY).build(),
                                     AsyncRequestBody.fromFile(file)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }
}
