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
 * Test for S3TransferManager presigned URL download functionality.
 */
class S3TransferManagerPresignedUrlDownloadTest {
    private S3AsyncClient mockS3AsyncClient;
    private AsyncPresignedUrlExtension mockPresignedUrlExtension;
    private GenericS3TransferManager tm;
    private TransferManagerConfiguration configuration;

    @BeforeEach
    public void methodSetup() {
        mockS3AsyncClient = mock(S3AsyncClient.class);
        mockPresignedUrlExtension = mock(AsyncPresignedUrlExtension.class);
        configuration = mock(TransferManagerConfiguration.class);
        
        when(mockS3AsyncClient.presignedUrlExtension()).thenReturn(mockPresignedUrlExtension);
        
        tm = new GenericS3TransferManager(mockS3AsyncClient, 
                                         mock(UploadDirectoryHelper.class), 
                                         configuration, 
                                         mock(DownloadDirectoryHelper.class));
    }

    @AfterEach
    public void methodTeardown() {
        tm.close();
    }

    @Test
    void downloadFileWithPresignedUrl_withValidRequest_returnsResponse() throws Exception {
        GetObjectResponse response = GetObjectResponse.builder().build();
        when(mockPresignedUrlExtension.getObject(any(PresignedUrlDownloadRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        URL presignedUrl = new URL("https://test-bucket.s3.amazonaws.com/test-key?presigned=true");
        
        PresignedDownloadFileRequest request = PresignedDownloadFileRequest.builder()
            .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                .presignedUrl(presignedUrl)
                .build())
            .destination(Paths.get("/tmp/test"))
            .build();
        
        CompletedFileDownload completedFileDownload = tm.downloadFileWithPresignedUrl(request)
            .completionFuture()
            .join();

        assertThat(completedFileDownload.response()).isEqualTo(response);
    }

    @Test
    void downloadWithPresignedUrl_withValidRequest_returnsResponse() throws Exception {
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(), "test".getBytes());
        when(mockPresignedUrlExtension.getObject(any(PresignedUrlDownloadRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(responseBytes));

        URL presignedUrl = new URL("https://test-bucket.s3.amazonaws.com/test-key?presigned=true");
        
        PresignedDownloadRequest<ResponseBytes<GetObjectResponse>> request = PresignedDownloadRequest.builder()
            .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                .presignedUrl(presignedUrl)
                .build())
            .responseTransformer(AsyncResponseTransformer.toBytes())
            .build();
        
        CompletedDownload<ResponseBytes<GetObjectResponse>> completedDownload = tm.downloadWithPresignedUrl(request)
            .completionFuture()
            .join();

        assertThat(completedDownload.result()).isEqualTo(responseBytes);
    }

    @Test
    void downloadFileWithPresignedUrl_withConsumerBuilder_returnsResponse() throws Exception {
        GetObjectResponse response = GetObjectResponse.builder().build();
        when(mockPresignedUrlExtension.getObject(any(PresignedUrlDownloadRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        URL presignedUrl = new URL("https://test-bucket.s3.amazonaws.com/test-key?presigned=true");
        
        CompletedFileDownload completedFileDownload = tm.downloadFileWithPresignedUrl(
            request -> request.presignedUrlDownloadRequest(p -> p.presignedUrl(presignedUrl))
                              .destination(Paths.get("/tmp/test")))
            .completionFuture()
            .join();

        assertThat(completedFileDownload.response()).isEqualTo(response);
    }

    @Test
    void downloadFileWithPresignedUrl_whenCancelled_shouldForwardCancellation() throws Exception {
        CompletableFuture<GetObjectResponse> s3Future = new CompletableFuture<>();
        when(mockPresignedUrlExtension.getObject(any(PresignedUrlDownloadRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(s3Future);

        URL presignedUrl = new URL("https://test-bucket.s3.amazonaws.com/test-key?presigned=true");
        
        PresignedDownloadFileRequest request = PresignedDownloadFileRequest.builder()
            .presignedUrlDownloadRequest(PresignedUrlDownloadRequest.builder()
                .presignedUrl(presignedUrl)
                .build())
            .destination(Paths.get("/tmp/test"))
            .build();
        
        CompletableFuture<CompletedFileDownload> future = tm.downloadFileWithPresignedUrl(request)
            .completionFuture();

        future.cancel(true);
        assertThat(s3Future).isCancelled();
    }
}