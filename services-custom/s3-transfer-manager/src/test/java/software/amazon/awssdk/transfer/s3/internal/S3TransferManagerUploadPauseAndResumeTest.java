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
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.RESUME_TOKEN;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

class S3TransferManagerUploadPauseAndResumeTest {
    private S3CrtAsyncClient mockS3Crt;
    private S3AsyncClient mockS3;
    private S3TransferManager tmCrt;
    private S3TransferManager tmJava;
    private UploadDirectoryHelper uploadDirectoryHelper;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private TransferManagerConfiguration configuration;
    private File file;

    @BeforeEach
    public void methodSetup() throws IOException {
        file = RandomTempFile.createTempFile("test", UUID.randomUUID().toString());
        Files.write(file.toPath(), RandomStringUtils.randomAlphanumeric(1000).getBytes(StandardCharsets.UTF_8));
        uploadDirectoryHelper = mock(UploadDirectoryHelper.class);
        configuration = mock(TransferManagerConfiguration.class);
        downloadDirectoryHelper = mock(DownloadDirectoryHelper.class);
        mockS3Crt = mock(S3CrtAsyncClient.class);
        mockS3 = mock(S3AsyncClient.class);
        tmCrt = new CrtS3TransferManager(configuration, mockS3Crt, false);
        tmJava = new GenericS3TransferManager(mockS3, uploadDirectoryHelper, configuration, downloadDirectoryHelper);
    }

    @AfterEach
    public void methodTeardown() {
        file.delete();
        tmCrt.close();
        tmJava.close();
    }

    enum TmType{
        JAVA, CRT
    }

    private static Stream<Arguments> transferManagers() {
        return Stream.of(
            Arguments.of(TmType.JAVA),
            Arguments.of(TmType.CRT)
        );
    }

    private S3TransferManager configureTestBehavior(TmType tmType, PutObjectResponse response) {
        if (tmType == TmType.JAVA) {
            when(mockS3.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
            when(mockS3.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(AbortMultipartUploadResponse.builder().build()));
            return tmJava;
        } else {
            when(mockS3Crt.putObject(any(PutObjectRequest.class), any(Path.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
            when(mockS3Crt.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(AbortMultipartUploadResponse.builder().build()));
            return tmCrt;
        }
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void resumeUploadFile_noResumeToken_shouldUploadFromBeginning(TmType tmType) {
        PutObjectRequest putObjectRequest = putObjectRequest();
        PutObjectResponse response = PutObjectResponse.builder().build();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        long fileLength = file.length();

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(putObjectRequest)
                                                               .source(file)
                                                               .build();

        S3TransferManager tm = configureTestBehavior(tmType, response);

        CompletedFileUpload completedFileUpload = tm.resumeUploadFile(r -> r.fileLength(fileLength)
                                                                            .uploadFileRequest(uploadFileRequest)
                                                                            .fileLastModified(fileLastModified))
                                                    .completionFuture()
                                                    .join();
        assertThat(completedFileUpload.response()).isEqualTo(response);

        if (tmType == TmType.JAVA) {
            verifyActualPutObjectRequestNotResumed_tmJava();
        } else {
            verifyActualPutObjectRequestNotResumed_tmCrt();
        }
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void resumeUploadFile_fileModified_shouldAbortExistingAndUploadFromBeginning(TmType tmType) {
        PutObjectRequest putObjectRequest = putObjectRequest();
        PutObjectResponse response = PutObjectResponse.builder().build();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        long fileLength = file.length();

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(putObjectRequest)
                                                               .source(file)
                                                               .build();

        S3TransferManager tm = configureTestBehavior(tmType, response);

        String multipartId = "someId";
        CompletedFileUpload completedFileUpload = tm.resumeUploadFile(r -> r.fileLength(fileLength + 10L)
                                                                            .partSizeInBytes(8 * MB)
                                                                            .totalParts(10L)
                                                                            .multipartUploadId(multipartId)
                                                                            .uploadFileRequest(uploadFileRequest)
                                                                            .fileLastModified(fileLastModified))
                                                    .completionFuture()
                                                    .join();
        assertThat(completedFileUpload.response()).isEqualTo(response);

        if (tmType == TmType.JAVA) {
            verifyActualPutObjectRequestNotResumed_tmJava();
        } else {
            verifyActualPutObjectRequestNotResumed_tmCrt();
        }

        ArgumentCaptor<AbortMultipartUploadRequest> abortMultipartUploadRequestArgumentCaptor =
            ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);

        if (tmType == TmType.JAVA) {
            verify(mockS3).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        } else {
            verify(mockS3Crt).abortMultipartUpload(abortMultipartUploadRequestArgumentCaptor.capture());
        }

        AbortMultipartUploadRequest actualRequest = abortMultipartUploadRequestArgumentCaptor.getValue();
        assertThat(actualRequest.uploadId()).isEqualTo(multipartId);
    }

    @ParameterizedTest
    @MethodSource("transferManagers")
    void resumeUploadFile_hasValidResumeToken_shouldResumeUpload(TmType tmType) {
        PutObjectRequest putObjectRequest = putObjectRequest();
        PutObjectResponse response = PutObjectResponse.builder().build();
        Instant fileLastModified = Instant.ofEpochMilli(file.lastModified());
        long fileLength = file.length();

        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                                               .putObjectRequest(putObjectRequest)
                                                               .source(file)
                                                               .build();

        S3TransferManager tm = configureTestBehavior(tmType, response);

        String multipartId = "someId";
        long totalParts = 10L;
        long partSizeInBytes = 8 * MB;
        CompletedFileUpload completedFileUpload = tm.resumeUploadFile(r -> r.fileLength(fileLength)
                                                                            .partSizeInBytes(partSizeInBytes)
                                                                            .totalParts(totalParts)
                                                                            .multipartUploadId(multipartId)
                                                                            .uploadFileRequest(uploadFileRequest)
                                                                            .fileLastModified(fileLastModified))
                                                    .completionFuture()
                                                    .join();
        assertThat(completedFileUpload.response()).isEqualTo(response);

        if (tmType == TmType.JAVA) {
            verifyActualPutObjectRequestResumedAndCorrectTokenReturned_tmJava(multipartId, partSizeInBytes, totalParts);
        } else {
            verifyActualPutObjectRequestResumedAndCorrectTokenReturned_tmCrt(multipartId, partSizeInBytes, totalParts);
        }
    }

    private void verifyActualPutObjectRequestNotResumed_tmCrt() {
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(mockS3Crt).putObject(putObjectRequestArgumentCaptor.capture(), any(Path.class));
        PutObjectRequest actualRequest = putObjectRequestArgumentCaptor.getValue();
        AwsRequestOverrideConfiguration awsRequestOverrideConfiguration = actualRequest.overrideConfiguration().get();
        SdkHttpExecutionAttributes attribute =
            awsRequestOverrideConfiguration.executionAttributes().getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES);

        assertThat(attribute.getAttribute(CRT_PAUSE_RESUME_TOKEN)).isNull();
    }

    private void verifyActualPutObjectRequestNotResumed_tmJava() {
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(mockS3).putObject(putObjectRequestArgumentCaptor.capture(), any(AsyncRequestBody.class));
        PutObjectRequest actualRequest = putObjectRequestArgumentCaptor.getValue();

        assertThat(actualRequest.overrideConfiguration()).isEmpty();
    }

    private void verifyActualPutObjectRequestResumedAndCorrectTokenReturned_tmJava(String multipartId, long partSizeInBytes,
                                                                                   long totalParts) {
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(mockS3).putObject(putObjectRequestArgumentCaptor.capture(), any(AsyncRequestBody.class));
        PutObjectRequest actualRequest = putObjectRequestArgumentCaptor.getValue();
        AwsRequestOverrideConfiguration awsRequestOverrideConfiguration = actualRequest.overrideConfiguration().get();

        assertThat(awsRequestOverrideConfiguration.executionAttributes().getAttribute(RESUME_TOKEN)).isNotNull();
        S3ResumeToken s3ResumeToken = awsRequestOverrideConfiguration.executionAttributes().getAttribute(RESUME_TOKEN);

        assertThat(s3ResumeToken.uploadId()).isEqualTo(multipartId);
        assertThat(s3ResumeToken.partSize()).isEqualTo(partSizeInBytes);
        assertThat(s3ResumeToken.totalNumParts()).isEqualTo(totalParts);
    }

    private void verifyActualPutObjectRequestResumedAndCorrectTokenReturned_tmCrt(String multipartId, long partSizeInBytes,
                                                                                  long totalParts) {
        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(mockS3Crt).putObject(putObjectRequestArgumentCaptor.capture(), any(Path.class));
        PutObjectRequest actualRequest = putObjectRequestArgumentCaptor.getValue();
        AwsRequestOverrideConfiguration awsRequestOverrideConfiguration = actualRequest.overrideConfiguration().get();
        SdkHttpExecutionAttributes attribute =
            awsRequestOverrideConfiguration.executionAttributes().getAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES);

        assertThat(attribute.getAttribute(CRT_PAUSE_RESUME_TOKEN)).satisfies(token -> {
            assertThat(token.getUploadId()).isEqualTo(multipartId);
            assertThat(token.getPartSize()).isEqualTo(partSizeInBytes);
            assertThat(token.getTotalNumParts()).isEqualTo(totalParts);
        });
    }

    private static PutObjectRequest putObjectRequest() {
        return PutObjectRequest.builder()
                               .key("key")
                               .bucket("bucket")
                               .build();
    }

}
