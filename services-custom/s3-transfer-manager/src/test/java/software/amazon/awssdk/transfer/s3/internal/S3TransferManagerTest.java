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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.UploadFileRequest;

public class S3TransferManagerTest {
    private S3CrtAsyncClient mockS3Crt;
    private S3TransferManager tm;
    private UploadDirectoryHelper uploadDirectoryManager;
    private TransferManagerConfiguration configuration;

    @BeforeEach
    public void methodSetup() {
        mockS3Crt = mock(S3CrtAsyncClient.class);
        uploadDirectoryManager = mock(UploadDirectoryHelper.class);
        configuration = mock(TransferManagerConfiguration.class);
        tm = new DefaultS3TransferManager(mockS3Crt, uploadDirectoryManager, configuration);
    }

    @AfterEach
    public void methodTeardown() {
        tm.close();
    }

    @Test
    public void defaultTransferManager_shouldNotThrowException() {
        S3TransferManager transferManager = S3TransferManager.create();
        transferManager.close();
    }

    @Test
    public void uploadFile_returnsResponse() {
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
    public void uploadFile_cancel_shouldForwardCancellation() {
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
    public void upload_cancel_shouldForwardCancellation() {
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
    public void downloadFile_returnsResponse() {
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
    public void downloadFile_cancel_shouldForwardCancellation() {
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
    public void download_cancel_shouldForwardCancellation() {
        CompletableFuture<GetObjectResponse> s3CrtFuture = new CompletableFuture<>();
        when(mockS3Crt.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(s3CrtFuture);

        CompletableFuture<CompletedDownload<ResponseBytes<GetObjectResponse>>> future =
            tm.download(d -> d
                  .getObjectRequest(g -> g.bucket("bucket")
                                          .key("key"))
                  .responseTransformer(AsyncResponseTransformer.toBytes()))
              .completionFuture();
        future.cancel(true);
        assertThat(s3CrtFuture).isCancelled();
    }

    @Test
    public void objectLambdaArnBucketProvided_shouldThrowException() {
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

        assertThatThrownBy(() -> tm.download(b -> b.getObjectRequest(p -> p.bucket(objectLambdaArn).key("key"))
                                                   .responseTransformer(AsyncResponseTransformer.toBytes()))
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.uploadDirectory(b -> b.bucket(objectLambdaArn)
                                                          .sourceDirectory(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("support S3 Object Lambda resources").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void mrapArnProvided_shouldThrowException() {
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

        assertThatThrownBy(() -> tm.download(b -> b.getObjectRequest(p -> p.bucket(mrapArn).key("key"))
                                                   .responseTransformer(AsyncResponseTransformer.toBytes()))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> tm.uploadDirectory(b -> b.bucket(mrapArn)
                                                          .sourceDirectory(Paths.get(".")))
                                   .completionFuture().join())
            .hasMessageContaining("multi-region access point ARN").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void uploadDirectory_throwException_shouldCompleteFutureExceptionally() {
        RuntimeException exception = new RuntimeException("test");
        when(uploadDirectoryManager.uploadDirectory(any(UploadDirectoryRequest.class))).thenThrow(exception);

        assertThatThrownBy(() -> tm.uploadDirectory(u -> u.sourceDirectory(Paths.get("/"))
                                                          .bucket("bucketName")).completionFuture().join())
            .hasCause(exception);
    }

    @Test
    public void close_shouldCloseUnderlyingResources() {
        S3TransferManager transferManager = new DefaultS3TransferManager(mockS3Crt, uploadDirectoryManager, configuration);
        transferManager.close();
        verify(mockS3Crt).close();
        verify(configuration).close();
    }

    @Test
    public void uploadDirectory_requestNull_shouldThrowException() {
        UploadDirectoryRequest request = null;
        assertThatThrownBy(() -> tm.uploadDirectory(request).completionFuture().join())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    public void upload_requestNull_shouldThrowException() {
        UploadFileRequest request = null;
        assertThatThrownBy(() -> tm.uploadFile(request).completionFuture().join()).isInstanceOf(NullPointerException.class)
                                                                                  .hasMessageContaining("must not be null");
    }

    @Test
    public void download_requestNull_shouldThrowException() {
        DownloadFileRequest request = null;
        assertThatThrownBy(() -> tm.downloadFile(request).completionFuture().join()).isInstanceOf(NullPointerException.class)
                                                                                    .hasMessageContaining("must not be null");
    }
}
