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

import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.COPY_SUFFIX;
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.printOutResult;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public class V1TransferManagerCopyBenchmark extends V1BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("V1TransferManagerCopyBenchmark");

    V1TransferManagerCopyBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        Validate.notNull(config.key(), "Key must not be null");
    }

    @Override
    protected void doRunBenchmark() {
        copy();
    }

    @Override
    protected void additionalWarmup() {
        for (int i = 0; i < 3; i++) {
            copyOnce(new ArrayList<>());
        }
    }

    private void copy() {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to copy");
        for (int i = 0; i < iteration; i++) {
            copyOnce(metrics);
        }
        long contentLength = s3Client.getObjectMetadata(bucket, key).getContentLength();
        printOutResult(metrics, "V1 copy", contentLength);
    }

    private void copyOnce(List<Double> latencies) {
        long start = System.currentTimeMillis();

        try {
            transferManager.copy(bucket, key, bucket, key + COPY_SUFFIX).waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(() -> "Thread interrupted when waiting for completion", e);
        }
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
