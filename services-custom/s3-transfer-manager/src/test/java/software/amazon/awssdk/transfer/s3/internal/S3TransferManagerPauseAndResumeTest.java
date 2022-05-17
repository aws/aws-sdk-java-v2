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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.CompletableFutureUtils;

class S3TransferManagerPauseAndResumeTest {
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
    void resumeDownloadFile_shouldSetRangeAccordingly() {
        GetObjectRequest getObjectRequest = getObjectRequest();
        GetObjectResponse response = GetObjectResponse.builder().build();
        Instant s3ObjectLastModified = Instant.now();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        HeadObjectResponse headObjectResponse = headObjectResponse(s3ObjectLastModified);

        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(file)
                                                                     .build();

        when(mockS3Crt.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        when(mockS3Crt.headObject(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(headObjectResponse));

        CompletedFileDownload completedFileDownload = tm.resumeDownloadFile(r -> r.bytesTransferred(file.length())
                                                                                  .downloadFileRequest(downloadFileRequest)
                                                                                  .fileLastModified(fileLastModified)
                                                                                  .s3ObjectLastModified(s3ObjectLastModified))
                                                        .completionFuture()
                                                        .join();
        assertThat(completedFileDownload.response()).isEqualTo(response);
        verifyActualGetObjectRequest(getObjectRequest, "bytes=1000-2000");
    }

    @Test
    void resumeDownloadFile_headObjectFailed_shouldFail() {
        GetObjectRequest getObjectRequest = getObjectRequest();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                                                                     .getObjectRequest(getObjectRequest)
                                                                     .destination(file)
                                                                     .build();
        SdkClientException sdkClientException = SdkClientException.create("failed");
        when(mockS3Crt.headObject(any(Consumer.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(sdkClientException));

        assertThatThrownBy(() -> tm.resumeDownloadFile(r -> r.bytesTransferred(1000l)
                                                             .downloadFileRequest(downloadFileRequest)
                                                             .fileLastModified(fileLastModified)
                                                             .s3ObjectLastModified(Instant.now()))
                                   .completionFuture()
                                   .join()).hasRootCause(sdkClientException);
    }

    private void verifyActualGetObjectRequest(GetObjectRequest getObjectRequest, String range) {
        ArgumentCaptor<GetObjectRequest> getObjectRequestArgumentCaptor =
            ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(mockS3Crt).getObject(getObjectRequestArgumentCaptor.capture(), any(AsyncResponseTransformer.class));
        GetObjectRequest actualRequest = getObjectRequestArgumentCaptor.getValue();
        assertThat(actualRequest.bucket()).isEqualTo(getObjectRequest.bucket());
        assertThat(actualRequest.key()).isEqualTo(getObjectRequest.key());
        assertThat(actualRequest.range()).isEqualTo(range);
    }

    private static GetObjectRequest getObjectRequest() {
        return GetObjectRequest.builder()
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
