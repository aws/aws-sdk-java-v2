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
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public class TransferManagerCopyBenchmark extends BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("TransferManagerCopyBenchmark");
    private final long contentLength;

    public TransferManagerCopyBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        Validate.notNull(config.key(), "Key must not be null");
        this.contentLength = s3Sync.headObject(b -> b.bucket(bucket).key(key)).contentLength();
    }

    @Override
    protected void doRunBenchmark() {
        try {
            copy(iteration, true);
        } catch (Exception exception) {
            logger.error(() -> "Request failed: ", exception);
        }
    }

    @Override
    protected void additionalWarmup() throws Exception {
        copy(5, false);
    }


    private void copy(int count, boolean printoutResult) throws Exception {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to copy");
        for (int i = 0; i < count; i++) {
            copyOnce(metrics);
        }
        if (printoutResult) {
            printOutResult(metrics, "TM v2 copy", contentLength);
        }
    }

    private void copyOnce(List<Double> latencies) throws Exception {
        long start = System.currentTimeMillis();
        Copy copy =
            transferManager.copy(b -> b.copyObjectRequest(c -> c.sourceBucket(bucket).sourceKey(key)
                                                                .destinationBucket(bucket).destinationKey(key + COPY_SUFFIX)));
        copy.completionFuture().get(timeout.getSeconds(), TimeUnit.SECONDS);
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
