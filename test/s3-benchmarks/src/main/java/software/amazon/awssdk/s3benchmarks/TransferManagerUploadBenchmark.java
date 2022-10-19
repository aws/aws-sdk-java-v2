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

import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.printOutResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public class TransferManagerUploadBenchmark extends BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("TransferManagerUploadBenchmark");
    private final TransferManagerBenchmarkConfig config;

    public TransferManagerUploadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        Validate.notNull(config.filePath(), "File path must not be null");
        this.config = config;
    }

    @Override
    protected void doRunBenchmark() {
        try {
            uploadFromFile(iteration, true);
        } catch (Exception exception) {
            logger.error(() -> "Request failed: ", exception);
        }
    }

    @Override
    protected void additionalWarmup() {
        try {
            uploadFromFile(3, false);
        } catch (Exception exception) {
            logger.error(() -> "Warmup failed: ", exception);
        }
    }

    private void uploadFromFile(int count, boolean printOutResult) throws IOException {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to upload from file");
        for (int i = 0; i < count; i++) {
            uploadOnceFromFile(metrics);
        }
        if (printOutResult) {
            printOutResult(metrics, "Upload from File", Files.size(Paths.get(path)));
        }
    }

    private void uploadOnceFromFile(List<Double> latencies) {
        File sourceFile = new File(path);
        long start = System.currentTimeMillis();
        transferManager.uploadFile(b -> b.putObjectRequest(r -> r.bucket(bucket)
                                                                 .key(key)
                                                                 .checksumAlgorithm(config.checksumAlgorithm()))
                                         .source(sourceFile.toPath()))
                       .completionFuture().join();
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
