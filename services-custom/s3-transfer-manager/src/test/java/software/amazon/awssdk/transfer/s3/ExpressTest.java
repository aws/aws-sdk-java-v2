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

package software.amazon.awssdk.transfer.s3;

import static software.amazon.awssdk.transfer.s3.SizeConstant.GB;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.FileRequestBodyConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectAclResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.utils.AttributeMap;

public class ExpressTest {

    int maxInflightDownloads = 5;
    String bucket = "hagrid-test-3--use2-az2--x-s3";
    long bufferSize = 5L * 1000 * 1000 * 1000;
    long partSize = 5L * 1000 * 1000 * 1000;
    int chunkSize = (int) (bufferSize / maxInflightDownloads);


    S3AsyncClient s3Client;
    String testPath;
    String key;

    @BeforeEach
    void setUp() {
        this.s3Client = S3AsyncClient
            .builder()
            .region(Region.US_WEST_2)
            // .endpointOverride(URI.create(""))
            .multipartEnabled(true)
            .multipartConfiguration(c -> c
                .minimumPartSizeInBytes(partSize)
                .apiCallBufferSizeInBytes(bufferSize)
                .parallelConfiguration(p -> p.maxInFlightParts(maxInflightDownloads)))
            .httpClient(NettyNioAsyncHttpClient.builder()
                                               .connectionTimeout(Duration.ofMinutes(30))
                                               .connectionAcquisitionTimeout(Duration.ofMinutes(30))
                                               .buildWithDefaults(AttributeMap.builder()
                                                                              .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                                                                              .build()))
            .build();

        testPath = System.getProperty("testpath");
        key = System.getProperty("testkey");
    }

    @Test
    void s3_upload() throws IOException {
        CompletableFuture<PutObjectResponse> putObjectFuture = s3Client.putObject(
            put -> put.key(key + "-" + System.currentTimeMillis()).bucket(bucket),
            AsyncRequestBody.fromFile(c -> c.chunkSizeInBytes(chunkSize))
        );

        long start = System.currentTimeMillis();
        PutObjectResponse response = putObjectFuture.join();
        long end = System.currentTimeMillis();

        System.out.println(response);
        long latencyInSec = (end - start) / 1000;
        System.out.printf("total time for %d inflight: %d sec%n", maxInflightDownloads, latencyInSec);
        printOutResult(latencyInSec, Files.size(Paths.get(testPath)));

    }

    @Test
    void s3_download() throws Exception {
        CompletableFuture<GetObjectResponse> getObjectFuture = s3Client.getObject(
            get -> get.key(key).bucket(bucket),
            AsyncResponseTransformer.toFile(Paths.get(testPath))
        );

        long start = System.currentTimeMillis();
        GetObjectResponse response = getObjectFuture.join();
        long end = System.currentTimeMillis();

        System.out.println(response);
        long latencyInSec = (end - start) / 1000;
        System.out.printf("total time for %d inflight: %d sec%n", maxInflightDownloads, latencyInSec);
        printOutResult(latencyInSec, response.contentLength());

    }

    @Test
    void tm_upload() throws Exception {
        Path path = Paths.get(String.format(testPath, System.currentTimeMillis()));
        S3TransferManager manager = S3TransferManager.builder()
                                                     .s3Client(s3Client)
                                                     .build();

        CompletableFuture<CompletedFileUpload> fut =
        manager.uploadFile(
            UploadFileRequest.builder()
                             .putObjectRequest(
                                 put -> put.key(key + "-" + System.currentTimeMillis()).bucket(bucket))
                             .source(path)
                             .build())
            .completionFuture();
        long start = System.currentTimeMillis();
        CompletedFileUpload res = fut.join();
        long end = System.currentTimeMillis();
        System.out.println(res.response());
        long latencyInSec = (end - start) / 1000;
        System.out.printf("total time for %d inflight: %d sec%n", maxInflightDownloads, latencyInSec);
        printOutResult(latencyInSec, Files.size(path));
    }

    @Test
    void tm_download() {
        Path path = Paths.get(String.format(testPath, System.currentTimeMillis()));
        S3TransferManager manager = S3TransferManager.builder()
                                                     .s3Client(s3Client)
                                                     .build();
        CompletableFuture<CompletedFileDownload> fut =
            manager.downloadFile(DownloadFileRequest.builder()
                                                    .getObjectRequest(get -> get.key(key).bucket(bucket))
                                                    .destination(path)
                                                    .build())
                   .completionFuture();
        long start = System.currentTimeMillis();
        CompletedFileDownload res = fut.join();
        long end = System.currentTimeMillis();
        System.out.println(res.response());
        long latencyInSec = (end - start) / 1000;
        System.out.printf("total time for %d inflight: %d sec%n", maxInflightDownloads, latencyInSec);
        printOutResult(latencyInSec, res.response().contentLength());
    }

    public static void printOutResult(long latency, long contentLengthInByte) {
        double contentLengthInGigabit = (contentLengthInByte / (double) GB) * 8.0;
        System.out.printf("Content Length (Bytes): %d%n", contentLengthInByte);
        System.out.printf("Average latency (s): %d%n", latency);
        System.out.printf("Object size (Gigabit): %.4f%n", contentLengthInGigabit);
        System.out.printf("Average throughput (Gbps): %.4f%n", contentLengthInGigabit / latency);
        System.out.printf("==========================================================");
    }
}
