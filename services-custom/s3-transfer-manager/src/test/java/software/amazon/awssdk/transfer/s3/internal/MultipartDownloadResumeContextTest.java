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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadResumeContext;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;

public class MultipartDownloadResumeContextTest {

    S3AsyncClient s3;
    S3TransferManager tm;

    @BeforeEach
    void init() {
        this.s3 = mock(S3AsyncClient.class);
        this.tm = new GenericS3TransferManager(s3,
                                               mock(UploadDirectoryHelper.class),
                                               mock(TransferManagerConfiguration.class),
                                               mock(DownloadDirectoryHelper.class));
    }

    @Test
    void pauseAndResume_shouldKeepMultipartContext() {
        CompletableFuture<GetObjectResponse> future = new CompletableFuture<>();
        when(s3.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(future);
        when(s3.headObject(any(Consumer.class)))
            .thenReturn(new CompletableFuture<>());

        GetObjectRequest req = GetObjectRequest.builder().key("key").bucket("bucket").build();

        FileDownload dl = tm.downloadFile(
            DownloadFileRequest.builder()
                               .destination(Paths.get("some", "path"))
                               .getObjectRequest(req)
                               .build());
        ResumableFileDownload resume = dl.pause();

        assertThat(resume.downloadFileRequest().getObjectRequest())
            .matches(hasMultipartContextAttribute(), "[1] hasMultipartContextAttribute");

        FileDownload dl2 = tm.resumeDownloadFile(resume);
        ResumableFileDownload resume2 = dl2.pause();

        assertThat(resume2.downloadFileRequest().getObjectRequest())
            .matches(hasMultipartContextAttribute(), "[2] hasMultipartContextAttribute");
    }

    @Test
    void resumeAfterObjectReplaced_thenPauseAfterWriting_shouldNotKeepPausedDownloadContext() throws IOException {
        File file = RandomTempFile.createTempFile("test", UUID.randomUUID().toString());
        try {
            Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric(1024).getBytes(StandardCharsets.UTF_8));
            MultipartDownloadResumeContext pausedContext = new MultipartDownloadResumeContext();
            pausedContext.addCompletedPart(1);
            pausedContext.totalParts(2);
            pausedContext.response(GetObjectResponse.builder().eTag("\"original\"").partsCount(2).build());
            GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                                .bucket("bucket")
                                .key("key")
                                .overrideConfiguration(c -> c.putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT,
                                                                                    pausedContext))
                                .build();
            Instant originalLastModified = Instant.parse("2026-09-28T12:00:00Z");
            ResumableFileDownload token =
                ResumableFileDownload.builder()
                                     .downloadFileRequest(DownloadFileRequest.builder()
                                                                             .getObjectRequest(getObjectRequest)
                                                                             .destination(file)
                                                                             .build())
                                     .bytesTransferred(file.length())
                                     .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
                                     .s3ObjectLastModified(originalLastModified)
                                     .s3ObjectEtag("\"original\"")
                                     .totalSizeInBytes(2048L)
                                     .build();

            when(s3.headObject(any(Consumer.class)))
                .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder()
                                                                                .eTag("\"replacement\"")
                                                                                .lastModified(originalLastModified.plusSeconds(60))
                                                                                .contentLength(2048L)
                                                                                .build()));
            when(s3.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
                .thenReturn(new CompletableFuture<>());

            FileDownload restarted = tm.resumeDownloadFile(token);
            Files.write(file.toPath(), new byte[100]);
            ResumableFileDownload secondToken = restarted.pause();

            GetObjectRequest pausedRequest = secondToken.downloadFileRequest().getObjectRequest();
            assertThat(pausedRequest.overrideConfiguration().get().executionAttributes()
                                    .getAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT))
                .isNotSameAs(pausedContext)
                .satisfies(ctx -> assertThat(ctx.completedParts()).isEmpty());
            assertThat(pausedRequest.ifUnmodifiedSince()).isNull();
            assertThat(pausedRequest.ifMatch()).isNull();
        } finally {
            file.delete();
        }
    }

    private Predicate<GetObjectRequest> hasMultipartContextAttribute() {
        return getObjectRequest -> {
            if (!getObjectRequest.overrideConfiguration().isPresent()) {
                return false;
            }

            return getObjectRequest.overrideConfiguration()
                                   .get()
                                   .executionAttributes()
                                   .getAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT)
                   != null;
        };
    }

    @Test
    void resumeRangedDownload_pauseAfterHead_secondResumeShouldUseCorrectRange() throws IOException {
        long originalRangeStart = 2048;
        long originalRangeEnd = 6143;
        String originalRange = "bytes=" + originalRangeStart + "-" + originalRangeEnd;

        File file = RandomTempFile.createTempFile("test", UUID.randomUUID().toString());
        try {
            Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric(1000)
                                                        .getBytes(StandardCharsets.UTF_8));

            GetObjectRequest rangedGetRequest = GetObjectRequest.builder()
                                                                .bucket("bucket")
                                                                .key("key")
                                                                .range(originalRange)
                                                                .build();

            DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                         .getObjectRequest(rangedGetRequest)
                                                                         .destination(file)
                                                                         .build();

            CompletableFuture<GetObjectResponse> getFuture = new CompletableFuture<>();
            when(s3.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
                .thenReturn(getFuture);

            Instant s3LastModified = Instant.now();
            HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                                                                       .contentLength(8192L)
                                                                       .lastModified(s3LastModified)
                                                                       .build();
            when(s3.headObject(any(Consumer.class)))
                .thenReturn(CompletableFuture.completedFuture(headObjectResponse));

            ResumableFileDownload firstToken = ResumableFileDownload.builder()
                                                                    .bytesTransferred(file.length())
                                                                    .downloadFileRequest(downloadFileRequest)
                                                                    .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
                                                                    .s3ObjectLastModified(s3LastModified)
                                                                    .totalSizeInBytes(8192L)
                                                                    .build();

            FileDownload firstDownload = tm.resumeDownloadFile(firstToken);
            ResumableFileDownload pauseToken = firstDownload.pause();
            assertThat(pauseToken.downloadFileRequest().getObjectRequest().range()).isEqualTo(originalRange);

            Mockito.reset(s3);
            GetObjectResponse response = GetObjectResponse.builder().build();
            when(s3.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
            when(s3.headObject(any(Consumer.class)))
                .thenReturn(CompletableFuture.completedFuture(headObjectResponse));

            tm.resumeDownloadFile(pauseToken).completionFuture().join();

            // file.length() is 1000. Expected: bytes=(2048+1000)-6143 = bytes=3048-6143
            ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
            verify(s3).getObject(captor.capture(), any(AsyncResponseTransformer.class));
            GetObjectRequest actualRequest = captor.getValue();

            String expectedRange = "bytes=" + (originalRangeStart + file.length()) + "-" + originalRangeEnd;
            assertThat(actualRequest.range()).isEqualTo(expectedRange);
        } finally {
            file.delete();
        }
    }

}
