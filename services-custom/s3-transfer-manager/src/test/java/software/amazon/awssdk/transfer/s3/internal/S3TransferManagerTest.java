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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.CompletedDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

class S3TransferManagerTest {
    private S3CrtAsyncClient mockS3Crt;
    private S3TransferManager tm;
    private UploadDirectoryHelper uploadDirectoryHelper;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private TransferManagerConfiguration configuration;

    @BeforeEach
    public void methodSetup() {
        mockS3Crt = mock(S3CrtAsyncClient.class);
        uploadDirectoryHelper = mock(UploadDirectoryHelper.class);
        configuration = mock(TransferManagerConfiguration.class);
        downloadDirectoryHelper = mock(DownloadDirectoryHelper.class);
        tm = new DefaultS3TransferManager(mockS3Crt, uploadDirectoryHelper, configuration, downloadDirectoryHelper);
    }

    @AfterEach
    public void methodTeardown() {
        tm.close();
    }

    @Test
    void defaultTransferManager_shouldNotThrowException() {
        S3TransferManager transferManager = S3TransferManager.create();
        transferManager.close();
    }

    @Test
    void uploadFile_returnsResponse() {
        PutObjectResponse response = PutObjectResponse.builder().build();
        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletedFileUpload completedFileUpload = tm.uploadFile(u -> u.putObjectRequest(p -> p.bucket("bucket")
                                                                                              .key("key"))
                                                                      .source(Paths.get(".")))
                                                    .completionFuture()
                                                    .join();

        assertThat(completedFileUpload.response()).isEqualTo(response);
    }

    @Test
    public void upload_returnsResponse() {
        PutObjectResponse response = PutObjectResponse.builder().build();
        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletedUpload completedUpload = tm.upload(u -> u.putObjectRequest(p -> p.bucket("bucket")
                                                                                  .key("key"))
                                                          .requestBody(AsyncRequestBody.fromString("foo")))
                                            .completionFuture()
                                            .join();

        assertThat(completedUpload.response()).isEqualTo(response);
    }

    @Test
    public void copy_returnsResponse() {
        CopyObjectResponse response = CopyObjectResponse.builder().build();
        when(mockS3Crt.copyObject(any(CopyObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletedCopy completedCopy = tm.copy(u -> u.copyObjectRequest(p -> p.sourceBucket("bucket")
                                                                             .sourceKey("sourceKey")
                                                                             .destinationBucket("bucket")
                                                                             .destinationKey("destKey")))
                                        .completionFuture()
                                        .join();

        assertThat(completedCopy.response()).isEqualTo(response);
    }

    @Test
    void uploadFile_cancel_shouldForwardCancellation() {
        CompletableFuture<PutObjectResponse> s3CrtFuture = new CompletableFuture<>();
        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(s3CrtFuture);

        CompletableFuture<CompletedFileUpload> future = tm.uploadFile(u -> u.putObjectRequest(p -> p.bucket("bucket")
                                                                                                    .key("key"))
                                                                            .source(Paths.get(".")))
                                                          .completionFuture();

        future.cancel(true);
        assertThat(s3CrtFuture).isCancelled();
    }

    @Test
    void upload_cancel_shouldForwardCancellation() {
        CompletableFuture<PutObjectResponse> s3CrtFuture = new CompletableFuture<>();
        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(s3CrtFuture);

        CompletableFuture<CompletedUpload> future = tm.upload(u -> u.putObjectRequest(p -> p.bucket("bucket")
                                                                                            .key("key"))
                                                                    .requestBody(AsyncRequestBody.fromString("foo")))
                                                      .completionFuture();

        future.cancel(true);
        assertThat(s3CrtFuture).isCancelled();
    }

    @Test
    void copy_cancel_shouldForwardCancellation() {
        CompletableFuture<CopyObjectResponse> s3CrtFuture = new CompletableFuture<>();
        when(mockS3Crt.copyObject(any(CopyObjectRequest.class)))
            .thenReturn(s3CrtFuture);
        

        CompletableFuture<CompletedCopy> future = tm.copy(u -> u.copyObjectRequest(p -> p.sourceBucket("bucket")
                                                                                         .sourceKey("sourceKey")
                                                                                         .destinationBucket("bucket")
                                                                                         .destinationKey("destKey")))
                                                    .completionFuture();

        future.cancel(true);
        assertThat(s3CrtFuture).isCancelled();
    }

    @Test
    void downloadFile_returnsResponse() {
        GetObjectResponse response = GetObjectResponse.builder().build();
        when(mockS3Crt.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletedFileDownload completedFileDownload = tm.downloadFile(d -> d.getObjectRequest(g -> g.bucket("bucket")
                                                                                                    .key("key"))
                                                                            .destination(Paths.get(".")))
                                                        .completionFuture()
                                                        .join();
        assertThat(completedFileDownload.response()).isEqualTo(response);
    }

    @Test
    void downloadFile_cancel_shouldForwardCancellation() {
        CompletableFuture<GetObjectResponse> s3CrtFuture = new CompletableFuture<>();
        when(mockS3Crt.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(s3CrtFuture);

        CompletableFuture<CompletedFileDownload> future = tm.downloadFile(d -> d
                                                                .getObjectRequest(g -> g.bucket(
                                                                                            "bucket")
                                                                                        .key("key"))
                                                                .destination(Paths.get(".")))
                                                            .completionFuture();
        future.cancel(true);
        assertThat(s3CrtFuture).isCancelled();
    }

    @Test
    void download_cancel_shouldForwardCancellation() {
        CompletableFuture<GetObjectResponse> s3CrtFuture = new CompletableFuture<>();
        when(mockS3Crt.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(s3CrtFuture);
        DownloadRequest<ResponseBytes<GetObjectResponse>> downloadRequest =
            DownloadRequest.builder()
                           .getObjectRequest(g -> g.bucket("bucket").key("key"))
                           .responseTransformer(AsyncResponseTransformer.toBytes()).build();

        CompletableFuture<CompletedDownload<ResponseBytes<GetObjectResponse>>> future =
            tm.download(downloadRequest).completionFuture();
        future.cancel(true);
        assertThat(s3CrtFuture).isCancelled();
    }

    @Test
    void objectLambdaArnBucketProvided_shouldThrowException() {
        String objectLambdaArn = "arn:xxx:s3-object-lambda";

        assertThatThrownBy(() -> tm.uploadFile(b -> b.putObjectRequest(p -> p.bucket(objectLambdaArn).key("key"))
                                                     .source(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.upload(b -> b.putObjectRequest(p -> p.bucket(objectLambdaArn).key("key"))
                                                 .requestBody(AsyncRequestBody.fromString("foo")))
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.downloadFile(b -> b.getObjectRequest(p -> p.bucket(objectLambdaArn).key("key"))
                                                       .destination(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);


        DownloadRequest<ResponseBytes<GetObjectResponse>> downloadRequest =
            DownloadRequest.builder()
                           .getObjectRequest(g -> g.bucket(objectLambdaArn).key("key"))
                           .responseTransformer(AsyncResponseTransformer.toBytes())
                           .build();

        assertThatThrownBy(() -> tm.download(downloadRequest)
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.uploadDirectory(b -> b.bucket(objectLambdaArn)
                                                          .source(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> tm.copy(b -> b.copyObjectRequest(p -> p.sourceBucket(objectLambdaArn)
                                                                        .sourceKey("sourceKey")
                                                                        .destinationBucket("bucket")
                                                                        .destinationKey("destKey")))
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.copy(b -> b.copyObjectRequest(p -> p.sourceBucket("bucket")
                                                                        .sourceKey("sourceKey")
                                                                        .destinationBucket(objectLambdaArn)
                                                                        .destinationKey("destKey")))
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mrapArnProvided_shouldThrowException() {
        String mrapArn = "arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap";

        assertThatThrownBy(() -> tm.uploadFile(b -> b.putObjectRequest(p -> p.bucket(mrapArn).key("key"))
                                                     .source(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.upload(b -> b.putObjectRequest(p -> p.bucket(mrapArn).key("key"))
                                                 .requestBody(AsyncRequestBody.fromString("foo")))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.downloadFile(b -> b.getObjectRequest(p -> p.bucket(mrapArn).key("key"))
                                                       .destination(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);

        DownloadRequest<ResponseBytes<GetObjectResponse>> downloadRequest =
            DownloadRequest.builder()
                           .getObjectRequest(g -> g.bucket(mrapArn).key("key"))
                           .responseTransformer(AsyncResponseTransformer.toBytes()).build();

        assertThatThrownBy(() -> tm.download(downloadRequest).completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.uploadDirectory(b -> b.bucket(mrapArn)
                                                          .source(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.downloadDirectory(b -> b.bucket(mrapArn)
                                                            .destination(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.copy(b -> b.copyObjectRequest(p -> p.sourceBucket(mrapArn)
                                                                        .sourceKey("sourceKey")
                                                                        .destinationBucket("bucket")
                                                                        .destinationKey("destKey")))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.copy(b -> b.copyObjectRequest(p -> p.sourceBucket("bucket")
                                                                        .sourceKey("sourceKey")
                                                                        .destinationBucket(mrapArn)
                                                                        .destinationKey("destKey")))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadDirectory_throwException_shouldCompleteFutureExceptionally() {
        RuntimeException exception = new RuntimeException("test");
        when(uploadDirectoryHelper.uploadDirectory(any(UploadDirectoryRequest.class))).thenThrow(exception);

        assertThatThrownBy(() -> tm.uploadDirectory(u -> u.source(Paths.get("/"))
                                                          .bucket("bucketName")).completionFuture().join())
            .hasCause(exception);
    }

    @Test
    void downloadDirectory_throwException_shouldCompleteFutureExceptionally() {
        RuntimeException exception = new RuntimeException("test");
        when(downloadDirectoryHelper.downloadDirectory(any(DownloadDirectoryRequest.class))).thenThrow(exception);

        assertThatThrownBy(() -> tm.downloadDirectory(u -> u.destination(Paths.get("/"))
                                                          .bucket("bucketName")).completionFuture().join())
            .hasCause(exception);
    }

    @Test
    void close_shouldCloseUnderlyingResources() {
        S3TransferManager transferManager = new DefaultS3TransferManager(mockS3Crt, uploadDirectoryHelper, configuration, downloadDirectoryHelper);
        transferManager.close();
        verify(mockS3Crt, times(0)).close();
        verify(configuration).close();
    }

    @Test
    void close_shouldNotCloseCloseS3AsyncClientPassedInBuilder_when_transferManagerClosed() {
        S3TransferManager transferManager =
            DefaultS3TransferManager.builder().s3Client(mockS3Crt).build();
        transferManager.close();
        verify(mockS3Crt, times(0)).close();
    }

    @Test
    void uploadDirectory_requestNull_shouldThrowException() {
        UploadDirectoryRequest request = null;
        assertThatThrownBy(() -> tm.uploadDirectory(request).completionFuture().join())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    void upload_requestNull_shouldThrowException() {
        UploadFileRequest request = null;
        assertThatThrownBy(() -> tm.uploadFile(request).completionFuture().join()).isInstanceOf(NullPointerException.class)
                                                                                  .hasMessageContaining("must not be null");
    }

    @Test
    void download_requestNull_shouldThrowException() {
        DownloadFileRequest request = null;
        assertThatThrownBy(() -> tm.downloadFile(request).completionFuture().join()).isInstanceOf(NullPointerException.class)
                                                                                    .hasMessageContaining("must not be null");
    }

    @Test
    void copy_requestNull_shouldThrowException() {
        CopyRequest request = null;
        assertThatThrownBy(() -> tm.copy(request).completionFuture().join()).isInstanceOf(NullPointerException.class)
                                                                                  .hasMessageContaining("must not be null");
    }

    @Test
    void downloadDirectory_requestNull_shouldThrowException() {
        DownloadDirectoryRequest request = null;
        assertThatThrownBy(() -> tm.downloadDirectory(request).completionFuture().join())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("must not be null");
    }
}
