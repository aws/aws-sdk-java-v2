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
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

public class TransferManagerUploadBenchmark extends BaseTransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor("TransferManagerUploadBenchmark");
    private final TransferManagerBenchmarkConfig config;

    public TransferManagerUploadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        Validate.notNull(config.key(), "Key must not be null");
        Validate.mutuallyExclusive("Only one of --file or --contentLengthInMB option must be specified, but both were.",
                                   config.filePath(), config.contentLengthInMb());

        if (config.filePath() == null && config.contentLengthInMb() == null) {
            throw new IllegalArgumentException("Either --file or --contentLengthInMB must be specified, but none were.");
        }
        this.config = config;
    }

    @Override
    protected void doRunBenchmark() {
        try {
            doUpload(iteration, true);
        } catch (Exception exception) {
            logger.error(() -> "Request failed: ", exception);
        }
    }

    @Override
    protected void additionalWarmup() {
        try {
            doUpload(3, false);
        } catch (Exception exception) {
            logger.error(() -> "Warmup failed: ", exception);
        }
    }

    private void doUpload(int count, boolean printOutResult) throws Exception {
        List<Double> metrics = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (config.contentLengthInMb() == null) {
                logger.info(() -> "Starting to upload from file");
                uploadOnceFromFile(metrics);
            } else {
                logger.info(() -> "Starting to upload from memory");
                uploadOnceFromMemory(metrics);
            }
        }
        if (printOutResult) {
            if (config.contentLengthInMb() == null) {
                printOutResult(metrics, "Upload from File", Files.size(Paths.get(path)));
            } else {
                printOutResult(metrics, "Upload from Memory", config.contentLengthInMb() * MB);
            }
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

    private void uploadOnceFromMemory(List<Double> latencies) throws Exception {
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        long partSizeInBytes = config.partSizeInMb() * MB;
        byte[] bytes = new byte[(int) partSizeInBytes];
        UploadRequest uploadRequest = UploadRequest
            .builder()
            .putObjectRequest(r -> r.bucket(bucket)
                                    .key(key)
                                    .contentLength(config.contentLengthInMb() * MB)
                                    .checksumAlgorithm(config.checksumAlgorithm()))
            .requestBody(AsyncRequestBody.fromPublisher(publisher))
            .build();
        Thread uploadThread = Executors.defaultThreadFactory().newThread(() -> {
            long remaining = config.contentLengthInMb() * MB;
            while (remaining > 0) {
                publisher.send(ByteBuffer.wrap(bytes));
                remaining -= partSizeInBytes;
            }
            publisher.complete();
        });
        Upload upload = transferManager.upload(uploadRequest);
        uploadThread.start();

        long start = System.currentTimeMillis();
        upload.completionFuture().get(timeout.getSeconds(), TimeUnit.SECONDS);
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
