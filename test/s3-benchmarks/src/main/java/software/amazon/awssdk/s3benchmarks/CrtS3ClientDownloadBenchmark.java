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

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;

public class CrtS3ClientDownloadBenchmark extends BaseCrtClientBenchmark {

    public CrtS3ClientDownloadBenchmark(TransferManagerBenchmarkConfig config) {
        super(config);
        // logger.info(() -> "Benchmark config: " + config);
        // Validate.isNull(config.filePath(), "File path is not supported in CrtS3ClientBenchmark");
        //
        // Long readBufferSizeInMb = config.readBufferSizeInMb() == null ? null : config.readBufferSizeInMb() * MB;
        //
        // Long partSizeInBytes = config.partSizeInMb() == null ? null : config.partSizeInMb() * MB;
        // s3NativeClientConfiguration = S3NativeClientConfiguration.builder()
        //                                                          .partSizeInBytes(partSizeInBytes)
        //                                                          .targetThroughputInGbps(config.targetThroughput() == null ?
        //                                                                                  Double.valueOf(100.0) :
        //                                                                                  config.targetThroughput())
        //                                                          .checksumValidationEnabled(true)
        //                                                          .build();
        //
        //
        // S3ClientOptions s3ClientOptions =
        //     new S3ClientOptions().withRegion(s3NativeClientConfiguration.signingRegion())
        //                          .withEndpoint(s3NativeClientConfiguration.endpointOverride() == null ? null :
        //                                        s3NativeClientConfiguration.endpointOverride().toString())
        //                          .withCredentialsProvider(s3NativeClientConfiguration.credentialsProvider())
        //                          .withClientBootstrap(s3NativeClientConfiguration.clientBootstrap())
        //                          .withPartSize(s3NativeClientConfiguration.partSizeBytes())
        //                          .withComputeContentMd5(false)
        //                          .withThroughputTargetGbps(s3NativeClientConfiguration.targetThroughputInGbps());
        //
        // bucket = config.bucket();
        // key = config.key();
        // iteration = config.iteration() == null ? BENCHMARK_ITERATIONS : config.iteration();
        //
        // if (readBufferSizeInMb != null) {
        //     s3ClientOptions.withInitialReadWindowSize(readBufferSizeInMb);
        //     s3ClientOptions.withReadBackpressureEnabled(true);
        // }
        //
        // crtS3Client = new S3Client(s3ClientOptions);
        // s3Sync = software.amazon.awssdk.services.s3.S3Client.builder()
        //                                                     .build();
        // this.contentLength = s3Sync.headObject(b -> b.bucket(bucket).key(key)).contentLength();
        //
        // DefaultAwsRegionProviderChain instanceProfileRegionProvider = new DefaultAwsRegionProviderChain();
        // region = instanceProfileRegionProvider.getRegion();
    }

    // @Override
    // public void run() {
    //     try {
    //         warmUp();
    //         doRunBenchmark();
    //     } catch (Exception e) {
    //         logger.error(() -> "Exception occurred", e);
    //     } finally {
    //         cleanup();
    //     }
    // }
    //
    // private void cleanup() {
    //     s3Sync.close();
    //     s3NativeClientConfiguration.close();
    //     crtS3Client.close();
    // }
    //
    // private void warmUp() throws Exception {
    //     logger.info(() -> "Starting to warm up");
    //
    //     for (int i = 0; i < 3; i++) {
    //         sendOneRequest(new ArrayList<>());
    //         Thread.sleep(500);
    //     }
    //     logger.info(() -> "Ending warm up");
    // }
    //
    // private void doRunBenchmark() {
    //     List<Double> metrics = new ArrayList<>();
    //     for (int i = 0; i < iteration; i++) {
    //         sendOneRequest(metrics);
    //     }
    //
    //     printOutResult(metrics, "Download to File", contentLength);
    // }

    @Override
    protected void sendOneRequest(List<Double> latencies) {

        CompletableFuture<Void> resultFuture = new CompletableFuture<>();

        S3MetaRequestResponseHandler responseHandler = new TestS3MetaRequestResponseHandler(resultFuture);

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
