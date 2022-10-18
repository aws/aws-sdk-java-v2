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

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.s3.CrtS3RuntimeException;
import software.amazon.awssdk.crt.s3.S3Client;
import software.amazon.awssdk.crt.s3.S3ClientOptions;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.internal.crt.S3NativeClientConfiguration;
import software.amazon.awssdk.utils.Logger;

public class CrtS3ClientBenchmark implements TransferManagerBenchmark {
    private final String bucket;
    private final String key;
    private final String path;
    private final int iteration;
    private final S3NativeClientConfiguration s3NativeClientConfiguration;
    private final S3Client crtS3Client;
    private final software.amazon.awssdk.services.s3.S3Client s3Sync;
    private final Region region;

    private final long contentLength;

    private static final Logger logger = Logger.loggerFor("CrtS3ClientBenchmark");

    public CrtS3ClientBenchmark(TransferManagerBenchmarkConfig config) {

        logger.info(() -> "Benchmark config: " + config);

        Long readBufferSizeInMb = config.readBufferSizeInMb() == null ? null : config.readBufferSizeInMb() * MB;

        Long partSizeInBytes = config.partSizeInMb() == null ? null : config.partSizeInMb() * MB;
        s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
                                                                 .partSizeInBytes(partSizeInBytes)
                                                                 .targetThroughputInGbps(config.targetThroughput())
                                                                 .build();


        S3ClientOptions s3ClientOptions =
            new S3ClientOptions().withRegion(s3NativeClientConfiguration.signingRegion())
                                 .withEndpoint(s3NativeClientConfiguration.endpointOverride() == null ? null :
                                               s3NativeClientConfiguration.endpointOverride().toString())
                                 .withCredentialsProvider(s3NativeClientConfiguration.credentialsProvider())
                                 .withClientBootstrap(s3NativeClientConfiguration.clientBootstrap())
                                 .withPartSize(s3NativeClientConfiguration.partSizeBytes())
                                 .withComputeContentMd5(false)
                                 .withThroughputTargetGbps(s3NativeClientConfiguration.targetThroughputInGbps());

        bucket = config.bucket();
        key = config.key();
        path = config.filePath();
        iteration = config.iteration() == null ? BENCHMARK_ITERATIONS : config.iteration();

        if (readBufferSizeInMb != null) {
            s3ClientOptions.withInitialReadWindowSize(readBufferSizeInMb);
            s3ClientOptions.withReadBackpressureEnabled(true);
        }

        crtS3Client = new S3Client(s3ClientOptions);
        s3Sync = software.amazon.awssdk.services.s3.S3Client.builder()
                                                            .build();
        this.contentLength = s3Sync.headObject(b -> b.bucket(bucket).key(key)).contentLength();

        DefaultAwsRegionProviderChain instanceProfileRegionProvider = new DefaultAwsRegionProviderChain();
        region = instanceProfileRegionProvider.getRegion();
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

    private void doRunBenchmark() {
        List<Double> metrics = new ArrayList<>();
        for (int i = 0; i < iteration; i++) {
            sendOneRequest(metrics);
        }

        printOutResult(metrics, "Download to File", contentLength);
    }

    private void sendOneRequest(List<Double> latencies) {

        CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        S3MetaRequestResponseHandler responseHandler = new S3MetaRequestResponseHandler() {
            @Override
            public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
                return 0;
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
        };

        String endpoint = bucket + ".s3." + region + ".amazonaws.com";

        HttpHeader[] headers = {new HttpHeader("Host", endpoint)};
        HttpRequest httpRequest = new HttpRequest("GET", "/" + key, headers, null);

        S3MetaRequestOptions metaRequestOptions = new S3MetaRequestOptions()
            .withEndpoint(URI.create("https://" + endpoint))
            .withMetaRequestType(S3MetaRequestOptions.MetaRequestType.GET_OBJECT).withHttpRequest(httpRequest)
            .withResponseHandler(responseHandler);

        long start = System.currentTimeMillis();
        try (S3MetaRequest metaRequest = crtS3Client.makeMetaRequest(metaRequestOptions)) {
            resultFuture.get(10, TimeUnit.MINUTES);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        long end = System.currentTimeMillis();
        latencies.add((end - start) / 1000.0);
    }
}
