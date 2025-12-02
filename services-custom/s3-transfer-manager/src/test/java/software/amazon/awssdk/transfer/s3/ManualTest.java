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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

public class ManualTest {

    @Test
    void getHagridFile() {
        int maxInflightDownloads = 50;
        String testPath = System.getProperty("testpath");
        String key = System.getProperty("testkey");
        S3AsyncClient s3AsyncClient =
            S3AsyncClient.builder()
                         .region(Region.US_WEST_2)
                         .multipartEnabled(true)
                         .multipartConfiguration(c -> c.parallelConfiguration(p -> p.maxInFlightParts(maxInflightDownloads)))
                         .httpClient(NettyNioAsyncHttpClient.builder()
                                                            .connectionTimeout(Duration.ofMinutes(30))
                                                            .connectionAcquisitionTimeout(Duration.ofMinutes(30))
                                                            .build())
                         .build();
        Path path = Paths.get(String.format(testPath, System.currentTimeMillis()));
        S3TransferManager manager = S3TransferManager.builder()
                                                     .s3Client(s3AsyncClient)
                                                     .build();
        CompletableFuture<CompletedFileDownload> fut =
            manager.downloadFile(DownloadFileRequest.builder()
                                                    .getObjectRequest(get -> get.key(key)
                                                                                .bucket("do-not-delete-java-hagrid-test"))
                                                    .destination(path)
                                                    .addTransferListener(LoggingTransferListener.create())
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

    @Test
    void uploadHagridFile() {
        int maxInflightDownloads = 50;
        String testPath = System.getProperty("testpath");
        String key = System.getProperty("testkey");
        S3AsyncClient s3AsyncClient =
            S3AsyncClient.builder()
                         .region(Region.US_WEST_2)
                         .multipartEnabled(true)
                         .multipartConfiguration(c -> c.parallelConfiguration(p -> p.maxInFlightParts(maxInflightDownloads)))
                         .httpClient(NettyNioAsyncHttpClient.builder()
                                                            .connectionTimeout(Duration.ofMinutes(30))
                                                            .connectionAcquisitionTimeout(Duration.ofMinutes(30))
                                                            .build())
                         .build();
        Path path = Paths.get(String.format(testPath, System.currentTimeMillis()));
        S3TransferManager manager = S3TransferManager.builder()
                                                     .s3Client(s3AsyncClient)
                                                     .build();

        CompletableFuture<CompletedFileUpload> fut = manager.uploadFile(
            UploadFileRequest.builder()
                             .putObjectRequest(
                                 put -> put.key(key).bucket("do-not-delete-java-hagrid-test"))
                             .source(path)
                             .addTransferListener(LoggingTransferListener.create())
                             .build())
                .completionFuture();

        long start = System.currentTimeMillis();
        CompletedFileUpload res = fut.join();
        long end = System.currentTimeMillis();
        System.out.println(res.response());
        long latencyInSec = (end - start) / 1000;
        System.out.printf("total time for %d inflight: %d sec%n", maxInflightDownloads, latencyInSec);
        printOutResult(latencyInSec, Paths.get(testPath).toFile().length());


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