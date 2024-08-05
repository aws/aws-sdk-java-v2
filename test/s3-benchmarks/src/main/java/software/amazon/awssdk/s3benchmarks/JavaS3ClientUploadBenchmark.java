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

import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.async.SimplePublisher;

public class JavaS3ClientUploadBenchmark extends BaseJavaS3ClientBenchmark {

    private final String filePath;
    private final Long contentLengthInMb;
    private final Long partSizeInMb;
    private final ChecksumAlgorithm checksumAlgorithm;

    public JavaS3ClientUploadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        this.filePath = config.filePath();
        this.contentLengthInMb = config.contentLengthInMb();
        this.partSizeInMb = config.partSizeInMb();
        this.checksumAlgorithm = config.checksumAlgorithm();
    }

    @Override
    protected void sendOneRequest(List<Double> latencies) throws Exception {
        if (filePath == null) {
            double latency = uploadFromMemory();
            latencies.add(latency);
            return;
        }
        Double latency = runWithTime(
            s3AsyncClient.putObject(req -> req.key(key).bucket(bucket).checksumAlgorithm(checksumAlgorithm),
                                    Paths.get(filePath))::join).latency();
        latencies.add(latency);
    }

    private double uploadFromMemory() throws Exception {
        if (contentLengthInMb == null) {
            throw new UnsupportedOperationException("Java upload benchmark - contentLengthInMb required for upload from memory");
        }
        long partSizeInBytes = partSizeInMb * MB;
        // upload using known content length
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        byte[] bytes = new byte[(int) partSizeInBytes];
        Thread uploadThread = Executors.defaultThreadFactory().newThread(() -> {
            long remaining = contentLengthInMb * MB;
            while (remaining > 0) {
                publisher.send(ByteBuffer.wrap(bytes));
                remaining -= partSizeInBytes;
            }
            publisher.complete();
        });
        CompletableFuture<PutObjectResponse> responseFuture =
            s3AsyncClient.putObject(r -> r.bucket(bucket)
                                          .key(key)
                                          .contentLength(contentLengthInMb * MB)
                                          .checksumAlgorithm(checksumAlgorithm),
                                    AsyncRequestBody.fromPublisher(publisher));
        uploadThread.start();
        long start = System.currentTimeMillis();
        responseFuture.get(timeout.getSeconds(), TimeUnit.SECONDS);
        long end = System.currentTimeMillis();
        return (end - start) / 1000.0;
    }

    @Override
    protected long contentLength() throws Exception {
        return filePath != null
               ? Files.size(Paths.get(filePath))
               : contentLengthInMb * MB;
    }
}
