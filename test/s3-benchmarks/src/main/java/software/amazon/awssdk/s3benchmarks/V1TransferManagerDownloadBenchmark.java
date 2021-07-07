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
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.printOutResult;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.utils.Logger;

public class V1TransferManagerDownloadBenchmark extends V1BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("V1TransferManagerDownloadBenchmark");

    V1TransferManagerDownloadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
    }

    @Override
    protected void doRunBenchmark() {
        downloadToFile();
    }

    private void downloadToFile() {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to download to file");
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            downloadOnceToFile(metrics);
        }
        long contentLength = s3Client.getObjectMetadata(bucket, key).getContentLength();
        printOutResult(metrics, "V1 Download to File", contentLength);
    }

    private void downloadOnceToFile(List<Double> latencies) {
        Path downloadPath = new File(this.sourcePath).toPath();
        long start = System.currentTimeMillis();

        try {
            transferManager.download(bucket, key, new File(this.sourcePath)).waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(() -> "Thread interrupted when waiting for completion", e);
        }
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
        runAndLogError(logger.logger(),
                       "Deleting file failed",
                       () -> Files.delete(downloadPath));
    }
}
