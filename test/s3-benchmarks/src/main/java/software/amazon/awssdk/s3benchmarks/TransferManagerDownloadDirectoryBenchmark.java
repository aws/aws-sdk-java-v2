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
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public class TransferManagerDownloadDirectoryBenchmark extends BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("TransferManagerDownloadDirectoryBenchmark");
    private final TransferManagerBenchmarkConfig config;

    public TransferManagerDownloadDirectoryBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        Validate.notNull(config.filePath(), "File path must not be null");
        this.config = config;
    }

    @Override
    protected void doRunBenchmark() {

        try {
            downloadDirectory(iteration, true);
        } catch (Exception exception) {
            logger.error(() -> "Request failed: ", exception);
        }
    }

    private void downloadDirectory(int count, boolean printoutResult) throws Exception {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to download to file");
        for (int i = 0; i < count; i++) {
            downloadOnce(metrics);
        }
        if (printoutResult) {
            printOutResult(metrics, "TM v2 Download Directory");
        }
    }

    private void downloadOnce(List<Double> latencies) throws Exception {
        Path downloadPath = new File(this.path).toPath();
        long start = System.currentTimeMillis();
        DirectoryDownload download =
            transferManager.downloadDirectory(b -> b.bucket(bucket)
                                                    .destination(downloadPath)
                                                    .listObjectsV2RequestTransformer(l -> l.prefix(config.prefix())));
        CompletedDirectoryDownload completedDirectoryDownload = download.completionFuture().get(timeout.getSeconds(),
                                                                                                TimeUnit.SECONDS);
        if (completedDirectoryDownload.failedTransfers().isEmpty()) {
            long end = System.currentTimeMillis();
            latencies.add((end - start) / 1000.0);
        } else {
            logger.error(() -> "Some transfers failed: " + completedDirectoryDownload.failedTransfers());
        }

        runAndLogError(logger.logger(),
                       "Deleting directory failed " + downloadPath,
                       () -> FileUtils.cleanUpTestDirectory(downloadPath));
    }

}
