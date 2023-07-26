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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.utils.Logger;

public abstract class BaseJavaS3ClientBenchmark implements TransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor(BaseJavaS3ClientBenchmark.class);

    private final

    public BaseJavaS3ClientBenchmark(TransferManagerBenchmarkConfig config) {

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

    private void cleanup() {
        s3Sync.close();
        s3Async.close();
    }

    private void warmUp() throws Exception {
        logger.info(() -> "Starting to warm up");
        for (int i = 0; i < 3; i++) {
            sendOneRequest(new ArrayList<>());
            Thread.sleep(500);
        }
        logger.info(() -> "Ending warm up");
    }

    private void doRunBenchmark() throws Exception {
        List<Double> metrics = new ArrayList<>();
        for (int i = 0; i < iteration; i++) {
            sendOneRequest(metrics);
        }
        printOutResult(metrics, "Download to File", contentLength);
    }


}
