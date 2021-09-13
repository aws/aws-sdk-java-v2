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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.Download;
import software.amazon.awssdk.utils.Logger;

public class TransferManagerDownloadBenchmark extends BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("TransferManagerDownloadBenchmark");

    public TransferManagerDownloadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
    }

    @Override
    protected void doRunBenchmark() {
        try {
            downloadToFile(BENCHMARK_ITERATIONS, true);
            downloadToMemory(BENCHMARK_ITERATIONS, true);
        } catch (Exception exception) {
            logger.error(() -> "Request failed: ", exception);
        }
    }

    @Override
    protected void additionalWarmup() {
        downloadToMemory(3, false);
        downloadToFile(3, false);
    }

    private void downloadToMemory(int count, boolean printoutResult) {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to download to memory");
        for (int i = 0; i < count; i++) {
            downloadOnceToMemory(metrics);
        }

        if (printoutResult) {
            printOutResult(metrics, "Download to Memory");
        }
    }

    private void downloadToFile(int count, boolean printoutResult) {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to download to file");
        for (int i = 0; i < count; i++) {
            downloadOnceToFile(metrics);
        }
        if (printoutResult) {
            printOutResult(metrics, "Download to File");
        }
    }

    private void downloadOnceToFile(List<Double> latencies) {
        Path downloadPath = new File(this.path).toPath();
        long start = System.currentTimeMillis();
        Download download =
            transferManager.download(b -> b.getObjectRequest(r -> r.bucket(bucket).key(key))
                                           .destination(downloadPath));
        download.completionFuture().join();
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
        runAndLogError(logger.logger(),
                       "Deleting file failed",
                       () -> Files.delete(downloadPath));
    }

    private void downloadOnceToMemory(List<Double> latencies) {
        long start = System.currentTimeMillis();
        s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                     new NoOpResponseTransformer()).join();
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
