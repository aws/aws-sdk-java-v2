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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.utils.Logger;

public class V1TransferManagerUploadBenchmark extends V1BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("V1TransferManagerUploadBenchmark");
    private final File sourceFile;

    V1TransferManagerUploadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        sourceFile = new File(sourcePath);
    }

    @Override
    protected void doRunBenchmark() {
        uploadFile();
    }

    private void uploadFile() {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to upload");
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            uploadOnce(metrics);
        }
        long contentLength = sourceFile.length();
        printOutResult(metrics, "V1 Upload File", contentLength);
    }

    private void uploadOnce(List<Double> latencies) {
        long start = System.currentTimeMillis();

        try {
            transferManager.upload(bucket, key, sourceFile).waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(() -> "Thread interrupted when waiting for completion", e);
        }
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
