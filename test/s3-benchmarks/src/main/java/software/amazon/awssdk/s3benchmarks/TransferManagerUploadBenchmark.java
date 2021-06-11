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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.utils.Logger;

public class TransferManagerUploadBenchmark extends BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("TransferManagerUploadBenchmark");

    public TransferManagerUploadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
    }

    @Override
    protected void doRunBenchmark() {
        try {
            uploadFromFile(BENCHMARK_ITERATIONS, true);
        } catch (Exception exception) {
            logger.error(() -> "Request failed: ", exception);
        }
    }

    @Override
    protected void additionalWarmup() {
        uploadFromFile(3, false);
    }

    private void uploadFromFile(int count, boolean printOutResult) {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to upload from file");
        for (int i = 0; i < count; i++) {
            uploadOnceFromFile(metrics);
        }
        if (printOutResult) {
            printOutResult(metrics, "Upload from File");
        }
    }

    private void uploadOnceFromFile(List<Double> latencies) {
        File sourceFile = new File(path);
        long start = System.currentTimeMillis();
        transferManager.upload(b -> b.putObjectRequest(r -> r.bucket(bucket).key(key))
                                     .source(sourceFile.toPath()))
                       .completionFuture().join();
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
