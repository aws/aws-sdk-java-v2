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

package software.amazon.awssdk.custom.s3.transfer.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.internal.async.FileAsyncRequestBody;
import software.amazon.awssdk.custom.s3.transfer.Upload;
import software.amazon.awssdk.custom.s3.transfer.UploadObjectSpecification;
import software.amazon.awssdk.custom.s3.transfer.UploadRequest;
import software.amazon.awssdk.custom.s3.transfer.util.SizeConstant;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.testutils.RandomTempFile;

/**
 * Tests for {@link UploadManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UploadManagerTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String UPLOAD_ID = "UPLOAD_ID";
    @Mock
    private S3AsyncClient s3Client;

    private UploadManager uploadManager;

    private UploadObjectSpecification uploadObjectSpecification;
    private File file;

    @Before
    public void setup() throws IOException {
        uploadManager = UploadManager.builder()
                                     .s3Client(s3Client)
                                     .build();

        uploadObjectSpecification =
            new ApiRequestUploadObjectSpecification(PutObjectRequest.builder()
                                                                    .bucket(BUCKET)
                                                                    .key(KEY)
                                                                    .build());
        file = new RandomTempFile(1);
    }

    @Test
    public void uploadObjectApiRequest_singlePart_succeed() {
        TransferRequestBody transferRequestBody = TransferRequestBody.fromFile(file.toPath());

        PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(FileAsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(putObjectResponse));

        Upload upload = uploadManager.uploadObject(UploadRequest.builder()
                                                                .uploadSpecification(uploadObjectSpecification).build(),
                                                   transferRequestBody);

        upload.completionFuture().join();
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(FileAsyncRequestBody.class));
    }

    @Test
    public void uploadObjectApiRequest_singlePart_fail_shouldPropagateException() {
        TransferRequestBody transferRequestBody = TransferRequestBody.fromFile(file.toPath());

        AwsServiceException serviceException = S3Exception.builder().build();
        CompletableFuture<PutObjectResponse> responseCompletableFuture = new CompletableFuture<>();
        responseCompletableFuture.completeExceptionally(serviceException);

        when(s3Client.putObject(any(PutObjectRequest.class), any(FileAsyncRequestBody.class)))
            .thenReturn(responseCompletableFuture);

        Upload upload = uploadManager.uploadObject(UploadRequest.builder()
                                                                .uploadSpecification(uploadObjectSpecification).build(),
                                                   transferRequestBody);

        assertThatThrownBy(() -> upload.completionFuture().join()).hasCause(serviceException);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(FileAsyncRequestBody.class));
    }

    @Test
    public void uploadObjectApiRequest_multiplePart_succeed() {
        TransferRequestBody transferRequestBody = TransferRequestBody.fromFile(file.toPath());

        stubSuccessfulCreateMultipartUpload();
        stubSuccessfulUploadParts();
        stubSuccessfulCompleteMultipartUpload();

        Upload upload = uploadManager.uploadObject(UploadRequest.builder()
                                                                .size(16 * SizeConstant.MiB)
                                                                .uploadSpecification(uploadObjectSpecification).build(),
                                                   transferRequestBody);

        upload.completionFuture().join();
        verify(s3Client, times(3)).uploadPart(any(UploadPartRequest.class),
                                              any(FileAsyncRequestBody.class));
    }

    @Test
    public void uploadObjectApiRequest_multiplePart_createMultipartUploadFail_shouldPropagateException() {
        TransferRequestBody transferRequestBody = TransferRequestBody.fromFile(file.toPath());

        AwsServiceException exception = S3Exception.builder().message("boom").build();
        CompletableFuture<CreateMultipartUploadResponse> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(future);

        Upload upload = uploadManager.uploadObject(UploadRequest.builder()
                                                                .size(16 * SizeConstant.MiB)
                                                                .uploadSpecification(uploadObjectSpecification).build(),
                                                   transferRequestBody);

        assertThatThrownBy(() -> upload.completionFuture().join()).hasCause(exception);
    }

    @Test
    public void uploadObjectApiRequest_multiplePart_uploadPartFail_shouldPropagateException() {
        TransferRequestBody transferRequestBody = TransferRequestBody.fromFile(file.toPath());

        stubSuccessfulCreateMultipartUpload();
        AwsServiceException exception = S3Exception.builder().message("boom").build();
        CompletableFuture<UploadPartResponse> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        when(s3Client.uploadPart(any(UploadPartRequest.class), any(FileAsyncRequestBody.class)))
            .thenReturn(future);

        Upload upload = uploadManager.uploadObject(UploadRequest.builder()
                                                                .size(16 * SizeConstant.MiB)
                                                                .uploadSpecification(uploadObjectSpecification).build(),
                                                   transferRequestBody);

        assertThatThrownBy(() -> upload.completionFuture().join()).hasCause(exception);
        verify(s3Client).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }

    @Test
    public void uploadObjectApiRequest_multiplePart_completeMultiPartFail_shouldPropagateException() {
        TransferRequestBody transferRequestBody = TransferRequestBody.fromFile(file.toPath());

        stubSuccessfulCreateMultipartUpload();
        stubSuccessfulUploadParts();

        AwsServiceException exception = S3Exception.builder().message("boom").build();
        CompletableFuture<CompleteMultipartUploadResponse> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(future);


        Upload upload = uploadManager.uploadObject(UploadRequest.builder()
                                                                .size(16 * SizeConstant.MiB)
                                                                .uploadSpecification(uploadObjectSpecification).build(),
                                                   transferRequestBody);

        assertThatThrownBy(() -> upload.completionFuture().join()).hasCause(exception);
        verify(s3Client).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }

    private void stubSuccessfulCreateMultipartUpload() {
        CreateMultipartUploadResponse createMultipartUploadResponse = CreateMultipartUploadResponse
            .builder()
            .uploadId(UPLOAD_ID)
            .build();

        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(createMultipartUploadResponse));
    }

    private void stubSuccessfulUploadParts() {
        UploadPartResponse uploadPartResponse = UploadPartResponse.builder()
                                                                  .eTag("etag1")
                                                                  .build();

        when(s3Client.uploadPart(any(UploadPartRequest.class), any(FileAsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(uploadPartResponse));
    }

    private void stubSuccessfulCompleteMultipartUpload() {
        CompleteMultipartUploadResponse completeMultipartUploadResponse = CompleteMultipartUploadResponse.builder()
                                                                                                         .build();

        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(completeMultipartUploadResponse));
    }
}
