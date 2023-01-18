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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public class TransferManagerDownloadBenchmark extends BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("TransferManagerDownloadBenchmark");
    private final long contentLength;

    public TransferManagerDownloadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        Validate.notNull(config.key(), "Key must not be null");
        this.contentLength = s3Sync.headObject(b -> b.bucket(bucket).key(key)).contentLength();
    }

    @Override
    protected void doRunBenchmark() {
        if (path == null) {
            try {
                downloadToMemory(iteration, true);
            } catch (Exception exception) {
                logger.error(() -> "Request failed: ", exception);
            }
            return;
        }

        try {
            downloadToFile(iteration, true);
        } catch (Exception exception) {
            logger.error(() -> "Request failed: ", exception);
        }
    }

    private void downloadToMemory(int count, boolean printoutResult) throws Exception {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to download to memory");
        for (int i = 0; i < count; i++) {
            downloadOnceToMemory(metrics);
        }

        if (printoutResult) {
            printOutResult(metrics, "TM v2 Download to Memory", contentLength);
        }
    }

    private void downloadToFile(int count, boolean printoutResult) throws Exception {
        List<Double> metrics = new ArrayList<>();
        logger.info(() -> "Starting to download to file");
        for (int i = 0; i < count; i++) {
            downloadOnceToFile(metrics);
        }
        if (printoutResult) {
            printOutResult(metrics, "TM v2 Download to File", contentLength);
        }
    }

    private void downloadOnceToFile(List<Double> latencies) throws Exception {
        Path downloadPath = new File(this.path).toPath();
        long start = System.currentTimeMillis();
        FileDownload download =
            transferManager.downloadFile(b -> b.getObjectRequest(r -> r.bucket(bucket).key(key))
                                               .destination(downloadPath));
        download.completionFuture().get(10, TimeUnit.MINUTES);
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
        runAndLogError(logger.logger(),
                       "Deleting file failed",
                       () -> Files.delete(downloadPath));
    }

    private void downloadOnceToMemory(List<Double> latencies) throws Exception {
        long start = System.currentTimeMillis();
        AsyncResponseTransformer<GetObjectResponse, Void> responseTransformer = new NoOpResponseTransformer<>();
        transferManager.download(DownloadRequest.builder()
                                                .getObjectRequest(req -> req.bucket(bucket).key(key))
                                                .responseTransformer(responseTransformer)
                                                .build())
                       .completionFuture()
                       .get(timeout.getSeconds(), TimeUnit.SECONDS);
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
