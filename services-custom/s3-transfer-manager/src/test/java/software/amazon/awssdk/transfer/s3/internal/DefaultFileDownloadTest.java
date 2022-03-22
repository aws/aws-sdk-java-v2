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

import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;

class DefaultFileDownloadTest {
    private static final long OBJECT_CONTENT_LENGTH = 1024L;

    @Test
    void equals_hashcode() {
        EqualsVerifier.forClass(DefaultFileDownload.class)
                      .withNonnullFields("completionFuture", "progress")
                      .withIgnoredFields("resumableFileDownload")
                      .verify();
    }

    @Test
    void pause_shouldReturnCorrectly() {
        CompletableFuture<CompletedFileDownload> future =
            new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        GetObjectResponse sdkResponse = getObjectResponse();

        Mockito.when(transferProgress.snapshot()).thenReturn(DefaultTransferProgressSnapshot.builder()
                                                                                            .bytesTransferred(1000L)
                                                                                            .sdkResponse(sdkResponse)
                                                                                            .build());

        DownloadFileRequest request = getDownloadFileRequest();

        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   request);

        ResumableFileDownload pause = fileDownload.pause();
        assertThat(pause.downloadFileRequest()).isEqualTo(request);
        assertThat(pause.bytesTransferred()).isEqualTo(1000L);
        assertThat(pause.lastModified()).isEqualTo(sdkResponse.lastModified());
        assertThat(pause.transferSizeInBytes()).hasValue(sdkResponse.contentLength());
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
                                                                                            .bytesTransferred(OBJECT_CONTENT_LENGTH)
                                                                                            .transferSizeInBytes(OBJECT_CONTENT_LENGTH)
                                                                                            .sdkResponse(getObjectResponse)
                                                                                            .build());

        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   getDownloadFileRequest());
        ResumableFileDownload resumableFileDownload = fileDownload.pause();
        assertThat(resumableFileDownload.bytesTransferred()).isEqualTo(resumableFileDownload.transferSizeInBytes().get());
    }

    @Test
    void pauseTwice_shouldReturnTheSame() {
        CompletableFuture<CompletedFileDownload> future =
            new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        Mockito.when(transferProgress.snapshot()).thenReturn(DefaultTransferProgressSnapshot.builder()
                                                                                            .bytesTransferred(1000L)
                                                                                            .build());
        DownloadFileRequest request = getDownloadFileRequest();

        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   request);

        ResumableFileDownload resumableFileDownload = fileDownload.pause();
        ResumableFileDownload resumableFileDownload2 = fileDownload.pause();

        assertThat(resumableFileDownload).isEqualTo(resumableFileDownload2);

    }

    private DownloadFileRequest getDownloadFileRequest() {
        return DownloadFileRequest.builder()
                                  .destination(Paths.get("."))
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