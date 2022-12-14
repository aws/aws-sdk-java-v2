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

import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.internal.progress.ResumeTransferProgress;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;

class DefaultFileDownloadTest {
    private static final long OBJECT_CONTENT_LENGTH = 1024L;
    private static FileSystem fileSystem;
    private static File file;

    @BeforeAll
    public static void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem();
        file = File.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.random(2000).getBytes(StandardCharsets.UTF_8));
    }

    @AfterAll
    public static void tearDown() throws IOException {
        file.delete();
    }

    @Test
    void pause_shouldReturnCorrectly() {
        CompletableFuture<CompletedFileDownload> future =
            new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        GetObjectResponse sdkResponse = getObjectResponse();

        Mockito.when(transferProgress.snapshot()).thenReturn(DefaultTransferProgressSnapshot.builder()
                                                                                            .transferredBytes(1000L)
                                                                                            .sdkResponse(sdkResponse)
                                                                                            .build());

        DownloadFileRequest request = getDownloadFileRequest();

        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   () -> request,
                                                                   null);

        ResumableFileDownload pause = fileDownload.pause();
        assertThat(pause.downloadFileRequest()).isEqualTo(request);
        assertThat(pause.bytesTransferred()).isEqualTo(file.length());
        assertThat(pause.s3ObjectLastModified()).hasValue(sdkResponse.lastModified());
        assertThat(pause.totalSizeInBytes()).hasValue(sdkResponse.contentLength());
    }

    @Test
    void pause_transferAlreadyFinished_shouldReturnNormally() {
        GetObjectResponse getObjectResponse = GetObjectResponse.builder()
                                                               .contentLength(OBJECT_CONTENT_LENGTH)
                                                               .build();
        CompletableFuture<CompletedFileDownload> future =
            CompletableFuture.completedFuture(CompletedFileDownload.builder()
                                                                   .response(getObjectResponse)
                                                                   .build());
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        Mockito.when(transferProgress.snapshot()).thenReturn(DefaultTransferProgressSnapshot.builder()
                                                                                            .transferredBytes(OBJECT_CONTENT_LENGTH)
                                                                                            .totalBytes(OBJECT_CONTENT_LENGTH)
                                                                                            .sdkResponse(getObjectResponse)
                                                                                            .build());

        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   this::getDownloadFileRequest,
                                                                   null);
        ResumableFileDownload resumableFileDownload = fileDownload.pause();
        assertThat(resumableFileDownload.bytesTransferred()).isEqualTo(file.length());
        assertThat(resumableFileDownload.totalSizeInBytes()).hasValue(OBJECT_CONTENT_LENGTH);
    }

    @Test
    void pauseTwice_shouldReturnTheSame() {
        CompletableFuture<CompletedFileDownload> future =
            new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        Mockito.when(transferProgress.snapshot()).thenReturn(DefaultTransferProgressSnapshot.builder()
                                                                                            .transferredBytes(1000L)
                                                                                            .build());
        DownloadFileRequest request = getDownloadFileRequest();

        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   () -> request,
                                                                   null);

        ResumableFileDownload resumableFileDownload = fileDownload.pause();
        ResumableFileDownload resumableFileDownload2 = fileDownload.pause();

        assertThat(resumableFileDownload).isEqualTo(resumableFileDownload2);
    }

    @Test
    void progress_progressNotFinished_shouldReturnDefaultProgress() {
        CompletableFuture<CompletedFileDownload> completedFileDownloadFuture =
            new CompletableFuture<>();

        CompletableFuture<TransferProgress> progressFuture =
            new CompletableFuture<>();

        CompletableFuture<DownloadFileRequest> requestFuture =
            new CompletableFuture<>();
        DefaultTransferProgressSnapshot snapshot = DefaultTransferProgressSnapshot.builder()
                                                                               .transferredBytes(1000L)
                                                                               .build();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        Mockito.when(transferProgress.snapshot()).thenReturn(snapshot);
        DefaultFileDownload fileDownload = new DefaultFileDownload(completedFileDownloadFuture,
                                                                   new ResumeTransferProgress(progressFuture),
                                                                   () -> requestFuture.getNow(null),
                                                                   null);

        assertThat(fileDownload.progress().snapshot()).isEqualTo(DefaultTransferProgressSnapshot.builder()
                                                                                                .transferredBytes(0L)
                                                                                                .build());

        progressFuture.complete(transferProgress);
        assertThat(fileDownload.progress().snapshot()).isEqualTo(snapshot);
    }

    private DownloadFileRequest getDownloadFileRequest() {
        return DownloadFileRequest.builder()
                                  .destination(file)
                                  .getObjectRequest(GetObjectRequest.builder().key("KEY").bucket("BUCKET").build())
                                  .build();
    }

    private GetObjectResponse getObjectResponse() {
        return GetObjectResponse.builder()
                                .lastModified(Instant.now())
                                .contentLength(OBJECT_CONTENT_LENGTH)
                                .build();
    }

}