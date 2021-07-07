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

import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.DEFAULT_WARMUP_CONCURRENCY;
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.PRE_WARMUP_ITERATIONS;
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.PRE_WARMUP_RUNS;
import static software.amazon.awssdk.transfer.s3.SizeConstant.KB;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.internal.S3CrtAsyncClient;
import software.amazon.awssdk.utils.Logger;

public abstract class BaseTransferManagerBenchmark implements TransferManagerBenchmark {

    private static final Logger logger = Logger.loggerFor("TransferManagerBenchmark");
    private static final String WARMUP_KEY = "warmupobject";

    protected final S3CrtAsyncClient s3;
    protected final S3Client s3Sync;
    protected final String bucket;
    protected final String key;
    protected final String path;
    protected final int warmupConcurrency;
    protected final File tmpFile;

    BaseTransferManagerBenchmark(TransferManagerBenchmarkConfig config) {
        logger.info(() -> "Benchmark config: " + config);
        Long partSizeInMb = config.partSizeInMb() == null ? null : config.partSizeInMb() * MB;
        s3 = S3CrtAsyncClient.builder()
                             .targetThroughputInGbps(config.targetThroughput())
                             .minimumPartSizeInBytes(partSizeInMb)
                             .build();
        s3Sync = S3Client.builder()
                         .build();
        bucket = config.bucket();
        key = config.key();
        path = config.filePath();
        warmupConcurrency = config.warmupConcurrency() == null ? DEFAULT_WARMUP_CONCURRENCY : config.warmupConcurrency();
        try {
            tmpFile = new RandomTempFile(1000 * KB);
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
    protected void additionalWarmup() {
        // default to no-op
    }

    protected abstract void doRunBenchmark();

    private void cleanup() {
        s3Sync.deleteObject(b -> b.bucket(bucket).key(WARMUP_KEY));
        s3.close();
    }

    private void warmUp() throws InterruptedException {
        logger.info(() -> "Starting to warm up");

        for (int i = 0; i < PRE_WARMUP_ITERATIONS; i++) {
            warmUpUploadBatch();
            warmUpDownloadBatch();

            Thread.sleep(500);
        }
        additionalWarmup();
        logger.info(() -> "Ending warm up");
    }

    private void warmUpDownloadBatch() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < PRE_WARMUP_RUNS; i++) {
            Path tempFile = RandomTempFile.randomUncreatedFile().toPath();
            futures.add(s3.getObject(GetObjectRequest.builder().bucket(bucket).key(WARMUP_KEY).build(),
                                     AsyncResponseTransformer.toFile(tempFile)).whenComplete((r, t) -> runAndLogError(
                logger.logger(), "Deleting file failed", () -> Files.delete(tempFile))));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }

    private void warmUpUploadBatch() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < PRE_WARMUP_RUNS; i++) {
            futures.add(s3.putObject(PutObjectRequest.builder().bucket(bucket).key(WARMUP_KEY).build(),
                                     AsyncRequestBody.fromFile(tmpFile)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    }
}
