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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.transfer.s3.exception.TransferPauseException.ErrorCode.ALREADY_FINISHED;
import static software.amazon.awssdk.transfer.s3.exception.TransferPauseException.ErrorCode.PAUSE_IN_PROGRESS;

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
import software.amazon.awssdk.transfer.s3.PersistableFileDownload;
import software.amazon.awssdk.transfer.s3.exception.TransferPauseException;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.internal.progress.DownloadFileMonitor;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferListenerContext;
import software.amazon.awssdk.transfer.s3.progress.TransferProgress;

class DefaultFileDownloadTest {

    @Test
    void equals_hashcode() {
        EqualsVerifier.forClass(DefaultFileDownload.class)
                      .withNonnullFields("completionFuture", "progress")
                      .withIgnoredFields("paused")
                      .verify();
    }

    @Test
    void pause_shouldReturnCorrectly() {
        CompletableFuture<CompletedFileDownload> future =
            new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);

        Mockito.when(transferProgress.snapshot()).thenReturn(DefaultTransferProgressSnapshot.builder()
                                                                                            .bytesTransferred(1000L)
                                                                                            .build());

        DownloadFileMonitor downloadFileMonitor = monitorForInProgressTransfer();

        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   downloadFileMonitor);

        PersistableFileDownload pause = fileDownload.pause();
        assertThat(pause.downloadFileRequest()).isEqualTo(downloadFileMonitor.downloadFileRequest());
        assertThat(pause.bytesTransferred()).isEqualTo(1000L);
        assertThat(pause.lastModified()).isEqualTo(downloadFileMonitor.initialResponse().get().lastModified());
    }

    @Test
    void pause_transferAlreadyFinished_shouldThrowException() {
        CompletableFuture<CompletedFileDownload> future =
            CompletableFuture.completedFuture(CompletedFileDownload.builder()
                                                                   .response(GetObjectResponse.builder().build())
                                                                   .build());
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);


        DownloadFileMonitor downloadFileMonitor = monitorForCompletedTransfer();

        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   downloadFileMonitor);

        assertThatThrownBy(fileDownload::pause).isInstanceOf(TransferPauseException.class)
                                               .satisfies(e -> assertThat(((TransferPauseException) e).errorCode())
                                                   .isEqualTo(ALREADY_FINISHED));

    }


    @Test
    void pauseTwice_shouldThrowException() {
        CompletableFuture<CompletedFileDownload> future =
            new CompletableFuture<>();
        TransferProgress transferProgress = Mockito.mock(TransferProgress.class);
        Mockito.when(transferProgress.snapshot()).thenReturn(DefaultTransferProgressSnapshot.builder()
                                                                                            .bytesTransferred(1000L)
                                                                                            .build());
        DownloadFileMonitor downloadFileMonitor = monitorForInProgressTransfer();


        DefaultFileDownload fileDownload = new DefaultFileDownload(future,
                                                                   transferProgress,
                                                                   downloadFileMonitor);

        fileDownload.pause();


        assertThatThrownBy(fileDownload::pause).isInstanceOf(TransferPauseException.class)
                                               .satisfies(e -> assertThat(((TransferPauseException) e).errorCode())
                                                   .isEqualTo(PAUSE_IN_PROGRESS));

    }

    private DownloadFileMonitor monitorForCompletedTransfer() {
        DownloadFileRequest downloadFileRequest = getDownloadFileRequest();
        DownloadFileMonitor downloadFileMonitor = new DownloadFileMonitor(downloadFileRequest);
        downloadFileMonitor.transferComplete(TransferListenerContext.builder().build());
        return downloadFileMonitor;
    }

    private DownloadFileMonitor monitorForInProgressTransfer() {
        DownloadFileRequest downloadFileRequest = getDownloadFileRequest();
        DownloadFileMonitor downloadFileMonitor = new DownloadFileMonitor(downloadFileRequest);
        GetObjectResponse getObjectResponse = GetObjectResponse.builder()
                                                               .lastModified(Instant.now())
                                                               .build();

        downloadFileMonitor.transferInitiated(TransferListenerContext.builder()
                                                                     .initialResponse(getObjectResponse)
                                                                     .build());

        return downloadFileMonitor;
    }

    private DownloadFileRequest getDownloadFileRequest() {
        return DownloadFileRequest.builder()
                                  .destination(Paths.get("."))
                                  .getObjectRequest(GetObjectRequest.builder().key("KEY").bucket("BUCKET").build())

                                  .build();
    }

}