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
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.DEFAULT_TIMEOUT;
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.printOutResult;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public abstract class BaseJavaS3ClientBenchmark implements TransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor(BaseJavaS3ClientBenchmark.class);

    protected final S3Client s3Client;

    protected final S3AsyncClient s3AsyncClient;
    protected final String bucket;
    protected final String key;
    protected final Duration timeout;
    private final ChecksumAlgorithm checksumAlgorithm;
    private final int iteration;

    protected BaseJavaS3ClientBenchmark(TransferManagerBenchmarkConfig config) {
        this.bucket = Validate.paramNotNull(config.bucket(), "bucket");
        this.key = Validate.paramNotNull(config.key(), "key");
        this.timeout = Validate.getOrDefault(config.timeout(), () -> DEFAULT_TIMEOUT);
        this.iteration = Validate.getOrDefault(config.iteration(), () -> BENCHMARK_ITERATIONS);
        this.checksumAlgorithm = config.checksumAlgorithm();

        this.s3Client = S3Client.create();

        long partSizeInMb = Validate.paramNotNull(config.partSizeInMb(), "partSize");
        long readBufferInMb = Validate.paramNotNull(config.readBufferSizeInMb(), "readBufferSizeInMb");
        Validate.mutuallyExclusive("cannot use forceCrtHttpClient and connectionAcquisitionTimeoutInSec",
                                   config.forceCrtHttpClient(), config.connectionAcquisitionTimeoutInSec());
        this.s3AsyncClient = S3AsyncClient.builder()
                                          .multipartEnabled(true)
                                          .multipartConfiguration(c -> c.minimumPartSizeInBytes(partSizeInMb * MB)
                                                                        .thresholdInBytes(partSizeInMb * 2 * MB)
                                                                        .apiCallBufferSizeInBytes(readBufferInMb * MB))
                                          .httpClientBuilder(httpClient(config))
                                          .build();
    }

    private SdkAsyncHttpClient.Builder httpClient(TransferManagerBenchmarkConfig config) {
        if (config.forceCrtHttpClient()) {
            logger.info(() -> "Using CRT HTTP client");
            AwsCrtAsyncHttpClient.Builder builder = AwsCrtAsyncHttpClient.builder();
            if (config.readBufferSizeInMb() != null) {
                builder.readBufferSizeInBytes(config.readBufferSizeInMb() * MB);
            }
            if (config.maxConcurrency() != null) {
                builder.maxConcurrency(config.maxConcurrency());
            }
            return builder;
        }
        NettyNioAsyncHttpClient.Builder builder = NettyNioAsyncHttpClient.builder();
        if (config.connectionAcquisitionTimeoutInSec() != null) {
            Duration connAcqTimeout = Duration.ofSeconds(config.connectionAcquisitionTimeoutInSec());
            builder.connectionAcquisitionTimeout(connAcqTimeout);
        }
        if (config.maxConcurrency() != null) {
            builder.maxConcurrency(config.maxConcurrency());
        }
        return builder;
    }

    protected abstract void sendOneRequest(List<Double> latencies) throws Exception;

    protected abstract long contentLength() throws Exception;

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
        s3Client.close();
        s3AsyncClient.close();
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
        printOutResult(metrics, "S3 Async client", contentLength());
    }

}
