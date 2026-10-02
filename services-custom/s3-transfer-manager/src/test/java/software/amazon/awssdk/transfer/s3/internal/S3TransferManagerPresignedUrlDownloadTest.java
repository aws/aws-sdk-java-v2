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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadRequest;

/**
 * Unit tests for S3TransferManager presigned URL download functionality.
 */
class S3TransferManagerPresignedUrlDownloadTest {
    private static final String PRESIGNED_URL = "https://test-bucket.s3.amazonaws.com/test-key"
                                                + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKID"
                                                + "&X-Amz-Date=20260101T000000Z&X-Amz-Expires=600"
                                                + "&X-Amz-SignedHeaders=host&X-Amz-Signature=abc123";

    private S3AsyncClient mockS3AsyncClient;
    private AsyncPresignedUrlExtension mockPresignedUrlExtension;
    private GenericS3TransferManager tm;
    private URL presignedUrl;

    @BeforeEach
    public void methodSetup() throws Exception {
        mockS3AsyncClient = mock(S3AsyncClient.class);
        mockPresignedUrlExtension = mock(AsyncPresignedUrlExtension.class);
        presignedUrl = new URL(PRESIGNED_URL);

        when(mockS3AsyncClient.presignedUrlExtension()).thenReturn(mockPresignedUrlExtension);

        tm = new GenericS3TransferManager(mockS3AsyncClient,
                                         mock(UploadDirectoryHelper.class),
                                         mock(TransferManagerConfiguration.class),
                                         mock(DownloadDirectoryHelper.class));
    }

    @AfterEach
    public void methodTeardown() {
        tm.close();
    }

    @Test
    void downloadFileWithPresignedUrl_withValidRequest_returnsResponse() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        stubGetObject(CompletableFuture.completedFuture(response));

        CompletedFileDownload completed = tm.downloadFileWithPresignedUrl(fileDownloadRequest())
            .completionFuture().join();

        assertThat(completed.response()).isEqualTo(response);
    }

    @Test
    void downloadWithPresignedUrl_withValidRequest_returnsResponse() {
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(), "test".getBytes());
        stubGetObject(CompletableFuture.completedFuture(responseBytes));

        CompletedDownload<ResponseBytes<GetObjectResponse>> completed =
            tm.downloadWithPresignedUrl(bytesDownloadRequest()).completionFuture().join();

        assertThat(completed.result()).isEqualTo(responseBytes);
    }

    @Test
    void downloadFileWithPresignedUrl_withConsumerBuilder_returnsResponse() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        stubGetObject(CompletableFuture.completedFuture(response));

        CompletedFileDownload completed = tm.downloadFileWithPresignedUrl(
            request -> request.presignedUrlDownloadRequest(p -> p.presignedUrl(presignedUrl))
                              .destination(Paths.get("/tmp/test")))
            .completionFuture().join();

        assertThat(completed.response()).isEqualTo(response);
    }

    @Test
    void downloadFileWithPresignedUrl_whenCancelled_shouldForwardCancellation() {
        CompletableFuture<GetObjectResponse> s3Future = new CompletableFuture<>();
        stubGetObject(s3Future);

        CompletableFuture<CompletedFileDownload> future =
            tm.downloadFileWithPresignedUrl(fileDownloadRequest()).completionFuture();

        future.cancel(true);
        assertThat(s3Future).isCancelled();
    }

    @Test
    void downloadFileWithPresignedUrl_whenRequestFails_shouldCompleteExceptionally() {
        CompletableFuture<GetObjectResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("download failed"));
        stubGetObject(failedFuture);

        assertThatThrownBy(() -> tm.downloadFileWithPresignedUrl(fileDownloadRequest()).completionFuture().join())
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("download failed");
    }

    @Test
    void downloadWithPresignedUrl_whenCancelled_shouldForwardCancellation() {
        CompletableFuture s3Future = new CompletableFuture<>();
        stubGetObject(s3Future);

        CompletableFuture<CompletedDownload<ResponseBytes<GetObjectResponse>>> future =
            tm.downloadWithPresignedUrl(bytesDownloadRequest()).completionFuture();

        future.cancel(true);
        assertThat(s3Future).isCancelled();
    }

    @Test
    void downloadFileWithPresignedUrl_withNullRequest_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> tm.downloadFileWithPresignedUrl((PresignedDownloadFileRequest) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void downloadWithPresignedUrl_withNullRequest_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> tm.downloadWithPresignedUrl(null))
            .isInstanceOf(NullPointerException.class);
    }

    private PresignedDownloadFileRequest fileDownloadRequest() {
        return PresignedDownloadFileRequest.builder()
            .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                .presignedUrl(presignedUrl)
                .build())
            .destination(Paths.get("/tmp/test"))
            .build();
    }

    private PresignedDownloadRequest<ResponseBytes<GetObjectResponse>> bytesDownloadRequest() {
        return PresignedDownloadRequest.builder()
            .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                .presignedUrl(presignedUrl)
                .build())
            .responseTransformer(AsyncResponseTransformer.toBytes())
            .build();
    }

    private void stubGetObject(CompletableFuture<?> future) {
        when(mockPresignedUrlExtension.getObject(any(PresignedUrlDownloadRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(future);
    }
}
