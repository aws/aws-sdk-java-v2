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
import static software.amazon.awssdk.transfer.s3.SizeConstant.KB;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.utils.Logger;

public class HagridTest {
    private static final Logger log = Logger.loggerFor(HagridTest.class);
    String bucket = "do-not-delete-java-hagrid-test";

    @Test
    void getHagridFile() throws IOException {
        int maxInflightDownloads = 200;
        String testPath = System.getProperty("testpath");
        String key = System.getProperty("testkey");
        S3AsyncClient s3AsyncClient =
            S3AsyncClient.builder()
                         .region(Region.US_WEST_2)
                         .multipartEnabled(true)
                         .multipartConfiguration(c -> c.maxInflightDownloads(maxInflightDownloads))
                         .httpClient(NettyNioAsyncHttpClient.builder()
                                                            .connectionTimeout(Duration.ofMinutes(30))
                                                            .connectionAcquisitionTimeout(Duration.ofMinutes(30))
                                                            .maxConcurrency(maxInflightDownloads)
                                                            .build())
                         .build();
        Path path = Paths.get(String.format(testPath, System.currentTimeMillis()));
        S3TransferManager manager = S3TransferManager.builder()
                                                     .s3Client(s3AsyncClient)
                                                     .build();
        CompletableFuture<CompletedFileDownload> fut =
            manager.downloadFile(DownloadFileRequest.builder()
                                                    .getObjectRequest(get -> get.key(key)
                                                                                .bucket(bucket))
                                                    .destination(path)
                                                    // .addTransferListener(LoggingTransferListener.create())
                                                    .build())
                   .completionFuture();
        long start = System.currentTimeMillis();
        CompletedFileDownload res = fut.join();
        long end = System.currentTimeMillis();
        System.out.println(res.response());
        long latencyInSec = (end - start) / 1000;
        printOutResult(latencyInSec, Files.size(path));
    }

    @Test
    void uploadHagridFile() throws IOException {
        long chunkSize = 16 * KB;
        int concurrency = 100;
        String testPath = System.getProperty("testpath");
        String key = System.getProperty("testkey");
        S3AsyncClient s3AsyncClient =
            S3AsyncClient.builder()
                         .region(Region.US_WEST_2)
                         .multipartEnabled(true)
                         .multipartConfiguration(c -> c
                             .minimumPartSizeInBytes(50 * MB)
                             .apiCallBufferSizeInBytes(chunkSize * concurrency))
                         .httpClient(NettyNioAsyncHttpClient.builder()
                                                            .connectionTimeout(Duration.ofMinutes(30))
                                                            .connectionAcquisitionTimeout(Duration.ofMillis(Integer.MAX_VALUE))
                                                            .build())
                         .overrideConfiguration(c -> c.retryStrategy(r -> r.maxAttempts(1)))
                         .build();
        Path path = Paths.get(String.format(testPath, System.currentTimeMillis()));
        S3TransferManager manager = S3TransferManager.builder()
                                                     .s3Client(s3AsyncClient)
                                                     .build();

        FileUpload fileUpload = manager.uploadFile(
            UploadFileRequest.builder()
                             .addTransferListener(LoggingTransferListener.create())
                             .putObjectRequest(
                                 put -> put.key(key).bucket(bucket))
                             .source(path)
                             .build());

        long start = System.currentTimeMillis();
        CompletedFileUpload upload = fileUpload.completionFuture().join();
        long end = System.currentTimeMillis();
        System.out.println(upload.response());
        long latencyInSec = (end - start) / 1000;
        printOutResult(latencyInSec, Files.size(path));
    }

    public static void printOutResult(long latency, long contentLengthInByte) {
        double contentLengthInGigabit = (contentLengthInByte / (double) GB) * 8.0;
        System.out.printf("Content Length (Bytes): %d%n", contentLengthInByte);
        System.out.printf("Average latency (s): %d%n", latency);
        System.out.printf("Object size (Gigabit): %.4f%n", contentLengthInGigabit);
        System.out.printf("Average throughput (Gbps): %.4f%n", contentLengthInGigabit / latency);
    }

}
