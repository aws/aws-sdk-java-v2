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
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SDK_HTTP_EXECUTION_ATTRIBUTES;
import static software.amazon.awssdk.services.s3.internal.crt.S3InternalSdkHttpExecutionAttribute.CRT_PAUSE_RESUME_TOKEN;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

class S3TransferManagerUploadPauseAndResumeTest {
    private S3CrtAsyncClient mockS3Crt;
    private S3TransferManager tm;
    private UploadDirectoryHelper uploadDirectoryHelper;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private TransferManagerConfiguration configuration;
    private File file;

    @BeforeEach
    public void methodSetup() throws IOException {
        file = RandomTempFile.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric(1000).getBytes(StandardCharsets.UTF_8));
        mockS3Crt = mock(S3CrtAsyncClient.class);
        uploadDirectoryHelper = mock(UploadDirectoryHelper.class);
        configuration = mock(TransferManagerConfiguration.class);
        downloadDirectoryHelper = mock(DownloadDirectoryHelper.class);
        tm = new DefaultS3TransferManager(mockS3Crt, uploadDirectoryHelper, configuration, downloadDirectoryHelper);
    }

    @AfterEach
    public void methodTeardown() {
        file.delete();
        tm.close();
    }

    @Test
    void resumeUploadFile_noResumeToken_shouldUploadFromBeginning() {
        PutObjectRequest putObjectRequest = putObjectRequest();
        PutObjectResponse response = PutObjectResponse.builder().build();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        long fileLength = file.length();

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(putObjectRequest)
                                                               .source(file)
                                                               .build();


        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        CompletedFileUpload completedFileUpload = tm.resumeUploadFile(r -> r.fileLength(fileLength)
                                                                            .uploadFileRequest(uploadFileRequest)
                                                                            .fileLastModified(fileLastModified))
                                                    .completionFuture()
                                                    .join();
        assertThat(completedFileUpload.response()).isEqualTo(response);
        verifyActualPutObjectRequestNotResumed();
    }

    @Test
    void resumeUploadFile_fileModified_shouldUploadFromBeginning() {
        PutObjectRequest putObjectRequest = putObjectRequest();
        PutObjectResponse response = PutObjectResponse.builder().build();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        long fileLength = file.length();

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(putObjectRequest)
                                                               .source(file)
                                                               .build();


        when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        String multipartId = "someId";
        CompletedFileUpload completedFileUpload = tm.resumeUploadFile(r -> r.fileLength(fileLength + 10L)
                                                                            .partSizeInBytes(8 * MB)
                                                                            .totalNumOfParts(10L)
                                                                            .multipartUploadId(multipartId)
                                                                            .uploadFileRequest(uploadFileRequest)
                                                                            .fileLastModified(fileLastModified))
                                                    .completionFuture()
                                                    .join();
        assertThat(completedFileUpload.response()).isEqualTo(response);
        verifyActualPutObjectRequestNotResumed();

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor =
            ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(mockS3Crt).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());

        AbortMultipartUploadRequest actualRequest = abortMultipartUploadRequestArgumentCaptor.getValue();
        assertThat(actualRequest.uploadId()).isEqualTo(multipartId);
    }


    // @Test
    // void resumeUploadFile_headObjectFailed_shouldFail() {
    //     PutObjectRequest putObjectRequest = putObjectRequest();
    //     Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
    //     UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
    //                                                            .putObjectRequest(putObjectRequest)
    //                                                            .source(file)
    //                                                            .build();
    //     SdkClientException sdkClientException = SdkClientException.create("failed");
    //     when(mockS3Crt.headObject(any(Consumer.class)))
    //         .thenReturn(CompletableFutureUtils.failedFuture(sdkClientException));
    //
    //     assertThatThrownBy(() -> tm.resumeUploadFile(r -> r.bytesTransferred(1000l)
    //                                                        .uploadFileRequest(uploadFileRequest)
    //                                                        .fileLastModified(fileLastModified)
    //                                                        .s3ObjectLastModified(Instant.now()))
    //                                .completionFuture()
    //                                .join()).hasRootCause(sdkClientException);
    // }
    //
    // @Test
    // public void pauseAfterResumeBeforeHeadSucceeds() throws InterruptedException {
    //     UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
    //                                                            .putObjectRequest(putObjectRequest())
    //                                                            .source(file)
    //                                                            .build();
    //
    //     CompletableFuture<?> headFuture = new CompletableFuture<>();
    //     when(mockS3Crt.headObject(any(Consumer.class))).thenReturn(headFuture);
    //
    //     ResumableFileUpload originalResumable =
    //         ResumableFileUpload.builder()
    //                            .bytesTransferred(file.length())
    //                            .uploadFileRequest(uploadFileRequest)
    //                            .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
    //                            .s3ObjectLastModified(Instant.now())
    //                            .totalSizeInBytes(2000L)
    //                            .build();
    //
    //     FileUpload fileDownload = tm.resumeUploadFile(originalResumable);
    //     ResumableFileUpload newResumable = fileDownload.pause();
    //
    //     assertThat(newResumable).isEqualTo(originalResumable);
    //     assertThat(fileDownload.completionFuture()).isCancelled();
    //     assertThat(headFuture).isCancelled();
    // }
    //
    // @Test
    // public void pauseAfterResumeAfterHeadBeforeGetSucceeds() throws InterruptedException {
    //     UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
    //                                                            .putObjectRequest(putObjectRequest())
    //                                                            .source(file)
    //                                                            .build();
    //
    //     CompletableFuture<?> getFuture = new CompletableFuture<>();
    //     when(mockS3Crt.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class))).thenReturn(getFuture);
    //
    //     Instant s3LastModified = Instant.now();
    //     when(mockS3Crt.headObject(any(Consumer.class)))
    //         .thenReturn(CompletableFuture.completedFuture(headObjectResponse(s3LastModified)));
    //
    //     ResumableFileUpload originalResumable =
    //         ResumableFileUpload.builder()
    //                            .bytesTransferred(file.length())
    //                            .uploadFileRequest(uploadFileRequest)
    //                            .fileLastModified(Instant.ofEpochMilli(file.lastModified()))
    //                            .s3ObjectLastModified(s3LastModified)
    //                            .totalSizeInBytes(2000L)
    //                            .build();
    //
    //     FileUpload fileDownload = tm.resumeUploadFile(originalResumable);
    //     ResumableFileUpload newResumable = fileDownload.pause();
    //
    //     assertThat(newResumable.s3ObjectLastModified()).isEqualTo(originalResumable.s3ObjectLastModified());
    //     assertThat(newResumable.bytesTransferred()).isEqualTo(originalResumable.bytesTransferred());
    //     assertThat(newResumable.totalSizeInBytes()).isEqualTo(originalResumable.totalSizeInBytes());
    //     assertThat(newResumable.fileLastModified()).isEqualTo(originalResumable.fileLastModified());
    //
    //     // Download will be modified now that we finished the head request
    //     assertThat(newResumable.uploadFileRequest()).isNotEqualTo(originalResumable.uploadFileRequest());
    //
    //     assertThat(fileDownload.completionFuture()).isCancelled();
    //     assertThat(getFuture).isCancelled();
    // }


    private void verifyActualPutObjectRequestNotResumed() {
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor =
            ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockS3Crt).putObject(putObjectRequestArgumentCaptor.capture(), any(AsyncRequestBody.class));
        PutObjectRequest actualRequest = putObjectRequestArgumentCaptor.getValue();
        AwsRequestOverrideConfiguration awsRequestOverrideConfiguration = actualRequest.overrideConfiguration().get();
        SdkHttpExecutionAttributes attribute =
            awsRequestOverrideConfiguration.executionAttributes().getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES);

        assertThat(attribute.getAttribute(CRT_PAUSE_RESUME_TOKEN)).isNull();

    }

    private static PutObjectRequest putObjectRequest() {
        return PutObjectRequest.builder()
                               .key("key")
                               .bucket("bucket")
                               .build();
    }

    private static HeadObjectResponse headObjectResponse(Instant s3ObjectLastModified) {
        return HeadObjectResponse
            .builder()
            .contentLength(2000L)
            .lastModified(s3ObjectLastModified)
            .build();
    }

}
