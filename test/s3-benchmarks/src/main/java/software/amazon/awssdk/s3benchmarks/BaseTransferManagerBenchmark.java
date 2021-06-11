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
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.S3CrtAsyncClient;
import software.amazon.awssdk.utils.Logger;

public abstract class BaseTransferManagerBenchmark implements TransferManagerBenchmark {
    protected static final int WARMUP_ITERATIONS = 10;
    protected static final int BENCHMARK_ITERATIONS = 10;

    private static final Logger logger = Logger.loggerFor("TransferManagerBenchmark");
    private static final String WARMUP_KEY = "warmupobject";

    protected final S3TransferManager transferManager;
    protected final S3CrtAsyncClient s3;
    protected final S3Client s3Sync;
    protected final String bucket;
    protected final String key;
    protected final String path;
    private final File file;

    BaseTransferManagerBenchmark(TransferManagerBenchmarkConfig config) {
        logger.info(() -> "Benchmark config: " + config);
        Long partSizeInMb = config.partSizeInMb() == null ? null : config.partSizeInMb() * 1024 * 1024L;
        s3 = S3CrtAsyncClient.builder()
                             .targetThroughputInGbps(config.targetThroughput())
                             .minimumPartSizeInBytes(partSizeInMb)
                             .build();
        s3Sync = S3Client.builder()
                         .build();
        transferManager = S3TransferManager.builder()
                                           .s3ClientConfiguration(b -> b.targetThroughputInGbps(config.targetThroughput())
                                           .minimumPartSizeInBytes(partSizeInMb))
                                           .build();
        bucket = config.bucket();
        key = config.key();
        path = config.filePath();
        try {
            file = new RandomTempFile(1024 * 1000L);
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

    protected final void printOutResult(List<Double> metrics, String name) {
        logger.info(() -> String.format("===============  %s Result ================", name));
        logger.info(() -> "" + metrics);
        double averageLatency = metrics.stream()
                                       .mapToDouble(a -> a)
                                       .average()
                                       .orElse(0.0);

        double lowestLatency = metrics.stream()
                                      .mapToDouble(a -> a)
                                      .min().orElse(0.0);

        HeadObjectResponse headObjectResponse = s3Sync.headObject(b -> b.bucket(bucket).key(key));
        double contentLengthInGigabit = (headObjectResponse.contentLength() / (1000 * 1000 * 1000.0)) * 8.0;
        logger.info(() -> "Average latency (s): " + averageLatency);
        logger.info(() -> "Object size (Gigabit): " + contentLengthInGigabit);
        logger.info(() -> "Average throughput (Gbps): " + contentLengthInGigabit / averageLatency);
        logger.info(() -> "Highest average throughput (Gbps): " + contentLengthInGigabit / lowestLatency);
        logger.info(() -> "==========================================================");
    }

    private void cleanup() {
        s3Sync.deleteObject(b -> b.bucket(bucket).key(WARMUP_KEY));
        transferManager.close();
    }

    private void warmUp() throws InterruptedException {
        logger.info(() -> "Starting to warm up");

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            warmUpUploadBatch();
            warmUpDownloadBatch();

            Thread.sleep(500);
        }
        additionalWarmup();
        logger.info(() -> "Ending warm up");
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
