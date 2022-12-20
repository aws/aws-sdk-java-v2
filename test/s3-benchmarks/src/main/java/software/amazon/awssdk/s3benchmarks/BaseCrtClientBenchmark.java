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
import static software.amazon.awssdk.s3benchmarks.BenchmarkUtils.printOutResult;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.crt.s3.CrtS3RuntimeException;
import software.amazon.awssdk.crt.s3.S3Client;
import software.amazon.awssdk.crt.s3.S3ClientOptions;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.internal.crt.S3NativeClientConfiguration;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

public abstract class BaseCrtClientBenchmark implements  TransferManagerBenchmark {
    private static final Logger logger = Logger.loggerFor(BaseCrtClientBenchmark.class);

    protected final String bucket;
    protected final String key;
    protected final int iteration;
    protected final S3NativeClientConfiguration s3NativeClientConfiguration;
    protected final S3Client crtS3Client;
    protected final software.amazon.awssdk.services.s3.S3Client s3Sync;
    protected final Region region;

    protected final long contentLength;

    protected BaseCrtClientBenchmark(TransferManagerBenchmarkConfig config) {
        logger.info(() -> "Benchmark config: " + config);
        Validate.isNull(config.filePath(), "File path is not supported in CrtS3ClientBenchmark");


        Long partSizeInBytes = config.partSizeInMb() == null ? null : config.partSizeInMb() * MB;
        this.s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                      .partSizeInBytes(partSizeInBytes)
                                                                      .targetThroughputInGbps(config.targetThroughput() == null ?
                                                                                              Double.valueOf(100.0) :
                                                                                              config.targetThroughput())
                                                                      .checksumValidationEnabled(true)
                                                                      .build();

        this.bucket = config.bucket();
        this.key = config.key();
        this.iteration = config.iteration() == null ? BENCHMARK_ITERATIONS : config.iteration();

        S3ClientOptions s3ClientOptions =
            new S3ClientOptions().withRegion(s3NativeClientConfiguration.signingRegion())
                                 .withEndpoint(s3NativeClientConfiguration.endpointOverride() == null ? null :
                                               s3NativeClientConfiguration.endpointOverride().toString())
                                 .withCredentialsProvider(s3NativeClientConfiguration.credentialsProvider())
                                 .withClientBootstrap(s3NativeClientConfiguration.clientBootstrap())
                                 .withPartSize(s3NativeClientConfiguration.partSizeBytes())
                                 .withComputeContentMd5(false)
                                 .withThroughputTargetGbps(s3NativeClientConfiguration.targetThroughputInGbps());

        Long readBufferSizeInMb = config.readBufferSizeInMb() == null ? null : config.readBufferSizeInMb() * MB;
        if (readBufferSizeInMb != null) {
            s3ClientOptions.withInitialReadWindowSize(readBufferSizeInMb);
            s3ClientOptions.withReadBackpressureEnabled(true);
        }

        this.crtS3Client = new S3Client(s3ClientOptions);
        s3Sync = software.amazon.awssdk.services.s3.S3Client.builder().build();
        this.contentLength = s3Sync.headObject(b -> b.bucket(bucket).key(key)).contentLength();

        AwsRegionProvider instanceProfileRegionProvider = new DefaultAwsRegionProviderChain();
        region = instanceProfileRegionProvider.getRegion();

    }

    protected abstract void sendOneRequest(List<Double> latencies) throws Exception;

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
        s3NativeClientConfiguration.close();
        crtS3Client.close();
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

    protected static final class TestS3MetaRequestResponseHandler implements S3MetaRequestResponseHandler {
        private final CompletableFuture<Void> resultFuture;

        TestS3MetaRequestResponseHandler(CompletableFuture<Void> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
            return bodyBytesIn.remaining();
        }

        @Override
        public void onFinished(S3FinishedResponseContext context) {
            if (context.getErrorCode() != 0) {
                resultFuture.completeExceptionally(
                    new CrtS3RuntimeException(context.getErrorCode(), context.getResponseStatus(),
                                              context.getErrorPayload()));
                return;
            }
            resultFuture.complete(null);
        }
    }

}
