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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.DrainingSubscriber;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

public class S3TransferManagerListenerTest {
    private final FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    private S3CrtAsyncClient s3Crt;
    private S3TransferManager tm;
    private long contentLength;

    @BeforeEach
    public void methodSetup() {
        s3Crt = mock(S3CrtAsyncClient.class);
        tm = new DefaultS3TransferManager(s3Crt, mock(UploadDirectoryHelper.class), mock(TransferManagerConfiguration.class),
                                          mock(DownloadDirectoryHelper.class));
        contentLength = 1024L;
        when(s3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenAnswer(drainPutRequestBody());
        when(s3Crt.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenAnswer(randomGetResponseBody(contentLength));
    }

    @AfterEach
    public void methodTeardown() {
        tm.close();
    }

    @Test
    public void uploadFile_success_shouldInvokeListener() throws Exception {
        TransferListener listener = mock(TransferListener.class);

        Path path = newTempFile();
        Files.write(path, randomBytes(contentLength));

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(r -> r.bucket("bucket")
                                                                                       .key("key"))
                                                               .source(path)
                                                               .addTransferListener(listener)
                                                               .build();
        FileUpload fileUpload = tm.uploadFile(uploadFileRequest);

        ArgumentCaptor<TransferListener.Context.TransferInitiated> captor1 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferInitiated.class);
        verify(listener, timeout(1000).times(1)).transferInitiated(captor1.capture());
        TransferListener.Context.TransferInitiated ctx1 = captor1.getValue();
        assertThat(ctx1.request()).isSameAs(uploadFileRequest);
        assertThat(ctx1.progressSnapshot().totalBytes()).hasValue(contentLength);
        assertThat(ctx1.progressSnapshot().transferredBytes()).isZero();

        ArgumentCaptor<TransferListener.Context.BytesTransferred> captor2 =
            ArgumentCaptor.forClass(TransferListener.Context.BytesTransferred.class);
        verify(listener, timeout(1000).times(1)).bytesTransferred(captor2.capture());
        TransferListener.Context.BytesTransferred ctx2 = captor2.getValue();
        assertThat(ctx2.request()).isSameAs(uploadFileRequest);
        assertThat(ctx2.progressSnapshot().totalBytes()).hasValue(contentLength);
        assertThat(ctx2.progressSnapshot().transferredBytes()).isPositive();

        ArgumentCaptor<TransferListener.Context.TransferComplete> captor3 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferComplete.class);
        verify(listener, timeout(1000).times(1)).transferComplete(captor3.capture());
        TransferListener.Context.TransferComplete ctx3 = captor3.getValue();
        assertThat(ctx3.request()).isSameAs(uploadFileRequest);
        assertThat(ctx3.progressSnapshot().totalBytes()).hasValue(contentLength);
        assertThat(ctx3.progressSnapshot().transferredBytes()).isEqualTo(contentLength);
        assertThat(ctx3.completedTransfer()).isSameAs(fileUpload.completionFuture().get());

        fileUpload.completionFuture().join();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void upload_success_shouldInvokeListener() throws Exception {
        TransferListener listener = mock(TransferListener.class);

        UploadRequest uploadRequest = UploadRequest.builder()
                                                   .putObjectRequest(r -> r.bucket("bucket")
                                                                           .key("key"))
                                                   .requestBody(AsyncRequestBody.fromString("foo"))
                                                   .addTransferListener(listener)
                                                   .build();
        Upload upload = tm.upload(uploadRequest);

        ArgumentCaptor<TransferListener.Context.TransferInitiated> captor1 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferInitiated.class);
        verify(listener, timeout(1000).times(1)).transferInitiated(captor1.capture());
        TransferListener.Context.TransferInitiated ctx1 = captor1.getValue();
        assertThat(ctx1.request()).isSameAs(uploadRequest);
        assertThat(ctx1.progressSnapshot().totalBytes()).hasValue(3L);
        assertThat(ctx1.progressSnapshot().transferredBytes()).isZero();

        ArgumentCaptor<TransferListener.Context.BytesTransferred> captor2 =
            ArgumentCaptor.forClass(TransferListener.Context.BytesTransferred.class);
        verify(listener, timeout(1000).times(1)).bytesTransferred(captor2.capture());
        TransferListener.Context.BytesTransferred ctx2 = captor2.getValue();
        assertThat(ctx2.request()).isSameAs(uploadRequest);
        assertThat(ctx2.progressSnapshot().totalBytes()).hasValue(3L);
        assertThat(ctx2.progressSnapshot().transferredBytes()).isPositive();

        ArgumentCaptor<TransferListener.Context.TransferComplete> captor3 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferComplete.class);
        verify(listener, timeout(1000).times(1)).transferComplete(captor3.capture());
        TransferListener.Context.TransferComplete ctx3 = captor3.getValue();
        assertThat(ctx3.request()).isSameAs(uploadRequest);
        assertThat(ctx3.progressSnapshot().totalBytes()).hasValue(3L);
        assertThat(ctx3.progressSnapshot().transferredBytes()).isEqualTo(3L);
        assertThat(ctx3.completedTransfer()).isSameAs(upload.completionFuture().get());

        upload.completionFuture().join();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void downloadFile_success_shouldInvokeListener() throws Exception {
        TransferListener listener = mock(TransferListener.class);

        DownloadFileRequest downloadRequest = DownloadFileRequest.builder()
                                                                 .getObjectRequest(r -> r.bucket("bucket")
                                                                                         .key("key"))
                                                                 .destination(newTempFile())
                                                                 .addTransferListener(listener)
                                                                 .build();
        FileDownload download = tm.downloadFile(downloadRequest);

        ArgumentCaptor<TransferListener.Context.TransferInitiated> captor1 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferInitiated.class);
        verify(listener, timeout(1000).times(1)).transferInitiated(captor1.capture());
        TransferListener.Context.TransferInitiated ctx1 = captor1.getValue();
        assertThat(ctx1.request()).isSameAs(downloadRequest);
        // transferSize is not known until we receive GetObjectResponse header
        assertThat(ctx1.progressSnapshot().totalBytes()).isNotPresent();
        assertThat(ctx1.progressSnapshot().transferredBytes()).isZero();

        ArgumentCaptor<TransferListener.Context.BytesTransferred> captor2 =
            ArgumentCaptor.forClass(TransferListener.Context.BytesTransferred.class);
        verify(listener, timeout(1000).times(1)).bytesTransferred(captor2.capture());
        TransferListener.Context.BytesTransferred ctx2 = captor2.getValue();
        assertThat(ctx2.request()).isSameAs(downloadRequest);
        // transferSize should now be known
        assertThat(ctx2.progressSnapshot().totalBytes()).hasValue(contentLength);
        assertThat(ctx2.progressSnapshot().transferredBytes()).isPositive();

        ArgumentCaptor<TransferListener.Context.TransferComplete> captor3 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferComplete.class);
        verify(listener, timeout(1000).times(1)).transferComplete(captor3.capture());
        TransferListener.Context.TransferComplete ctx3 = captor3.getValue();
        assertThat(ctx3.request()).isSameAs(downloadRequest);
        assertThat(ctx3.progressSnapshot().totalBytes()).hasValue(contentLength);
        assertThat(ctx3.progressSnapshot().transferredBytes()).isEqualTo(contentLength);
        assertThat(ctx3.completedTransfer()).isSameAs(download.completionFuture().get());

        download.completionFuture().join();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void download_success_shouldInvokeListener() throws Exception {
        TransferListener listener = mock(TransferListener.class);

        DownloadRequest<ResponseBytes<GetObjectResponse>> downloadRequest =
            DownloadRequest.builder()
                           .getObjectRequest(r -> r.bucket(
                                                       "bucket")
                                                   .key("key"))
                           .responseTransformer(AsyncResponseTransformer.toBytes())
                           .addTransferListener(listener)
                           .build();
        Download<ResponseBytes<GetObjectResponse>> download = tm.download(downloadRequest);

        ArgumentCaptor<TransferListener.Context.TransferInitiated> captor1 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferInitiated.class);
        verify(listener, timeout(1000).times(1)).transferInitiated(captor1.capture());
        TransferListener.Context.TransferInitiated ctx1 = captor1.getValue();
        assertThat(ctx1.request()).isSameAs(downloadRequest);
        // transferSize is not known until we receive GetObjectResponse header
        assertThat(ctx1.progressSnapshot().totalBytes()).isNotPresent();
        assertThat(ctx1.progressSnapshot().transferredBytes()).isZero();

        ArgumentCaptor<TransferListener.Context.BytesTransferred> captor2 =
            ArgumentCaptor.forClass(TransferListener.Context.BytesTransferred.class);
        verify(listener, timeout(1000).times(1)).bytesTransferred(captor2.capture());
        TransferListener.Context.BytesTransferred ctx2 = captor2.getValue();
        assertThat(ctx2.request()).isSameAs(downloadRequest);
        // transferSize should now be known
        assertThat(ctx2.progressSnapshot().totalBytes()).hasValue(contentLength);
        assertThat(ctx2.progressSnapshot().transferredBytes()).isPositive();

        ArgumentCaptor<TransferListener.Context.TransferComplete> captor3 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferComplete.class);
        verify(listener, timeout(1000).times(1)).transferComplete(captor3.capture());
        TransferListener.Context.TransferComplete ctx3 = captor3.getValue();
        assertThat(ctx3.request()).isSameAs(downloadRequest);
        assertThat(ctx3.progressSnapshot().totalBytes()).hasValue(contentLength);
        assertThat(ctx3.progressSnapshot().transferredBytes()).isEqualTo(contentLength);
        assertThat(ctx3.completedTransfer()).isSameAs(download.completionFuture().get());

        download.completionFuture().join();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void uploadFile_failure_shouldInvokeListener() throws Exception {
        TransferListener listener = mock(TransferListener.class);

        Path path = newTempFile();
        Files.write(path, randomBytes(contentLength));

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(r -> r.bucket("bucket")
                                                                                       .key("key"))
                                                               .source(path)
                                                               .addTransferListener(listener)
                                                               .build();
        SdkClientException sdkClientException = SdkClientException.create("");
        when(s3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenThrow(sdkClientException);
        FileUpload fileUpload = tm.uploadFile(uploadFileRequest);

        CompletableFuture<CompletedFileUpload> future = fileUpload.completionFuture();
        assertThatThrownBy(future::join)
            .isInstanceOf(CompletionException.class)
            .hasCause(sdkClientException);

        ArgumentCaptor<TransferListener.Context.TransferInitiated> captor1 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferInitiated.class);
        verify(listener, timeout(1000).times(1)).transferInitiated(captor1.capture());
        TransferListener.Context.TransferInitiated ctx1 = captor1.getValue();
        assertThat(ctx1.request()).isSameAs(uploadFileRequest);
        assertThat(ctx1.progressSnapshot().transferredBytes()).isZero();

        ArgumentCaptor<TransferListener.Context.TransferFailed> captor2 =
            ArgumentCaptor.forClass(TransferListener.Context.TransferFailed.class);
        verify(listener, timeout(1000).times(1)).transferFailed(captor2.capture());
        TransferListener.Context.TransferFailed ctx2 = captor2.getValue();
        assertThat(ctx2.request()).isSameAs(uploadFileRequest);
        assertThat(ctx2.progressSnapshot().transferredBytes()).isZero();
        assertThat(ctx2.exception()).isEqualTo(sdkClientException);

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void listener_exception_shouldBeSuppressed() throws Exception {
        TransferListener listener = throwingListener();

        Path path = newTempFile();
        Files.write(path, randomBytes(contentLength));

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(r -> r.bucket("bucket")
                                                                                       .key("key"))
                                                               .source(path)
                                                               .addTransferListener(listener)
                                                               .build();
        FileUpload fileUpload = tm.uploadFile(uploadFileRequest);

        verify(listener, timeout(1000).times(1)).transferInitiated(any());
        verify(listener, timeout(1000).times(1)).bytesTransferred(any());
        verify(listener, timeout(1000).times(1)).transferComplete(any());

        fileUpload.completionFuture().join();
        verifyNoMoreInteractions(listener);
    }

    private static TransferListener throwingListener() {
        TransferListener listener = mock(TransferListener.class);
        RuntimeException e = new RuntimeException("Intentional exception for testing purposes");
        doThrow(e).when(listener).transferInitiated(any());
        doThrow(e).when(listener).bytesTransferred(any());
        doThrow(e).when(listener).transferComplete(any());
        doThrow(e).when(listener).transferFailed(any());
        return listener;
    }

    private static Answer<CompletableFuture<PutObjectResponse>> drainPutRequestBody() {
        return invocationOnMock -> {
            AsyncRequestBody requestBody = invocationOnMock.getArgument(1, AsyncRequestBody.class);
            CompletableFuture<PutObjectResponse> cf = new CompletableFuture<>();
            requestBody.subscribe(new DrainingSubscriber<ByteBuffer>() {
                @Override
                public void onError(Throwable t) {
                    cf.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    cf.complete(PutObjectResponse.builder().build());
                }
            });
            return cf;
        };
    }

    private static Answer<CompletableFuture<GetObjectResponse>> randomGetResponseBody(long contentLength) {
        return invocationOnMock -> {
            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
                invocationOnMock.getArgument(1, AsyncResponseTransformer.class);
            CompletableFuture<GetObjectResponse> cf = responseTransformer.prepare();
            responseTransformer.onResponse(GetObjectResponse.builder()
                                                            .contentLength(contentLength)
                                                            .build());
            responseTransformer.onStream(AsyncRequestBody.fromBytes(randomBytes(contentLength)));
            return cf;
        };
    }

    private Path newTempFile() {
        return fs.getPath("/", UUID.randomUUID().toString());
    }

    private static byte[] randomBytes(long size) {
        byte[] bytes = new byte[Math.toIntExact(size)];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}
