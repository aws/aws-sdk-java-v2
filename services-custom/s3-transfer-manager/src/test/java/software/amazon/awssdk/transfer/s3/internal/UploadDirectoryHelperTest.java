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
import static org.mockito.Mockito.when;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.CompletedUploadDirectory;
import software.amazon.awssdk.transfer.s3.Upload;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.UploadDirectoryTransfer;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;

public class UploadDirectoryHelperTest {
    private static FileSystem jimfs;
    private static Path directory;
    private Function<UploadRequest, Upload> singleUploadFunction;
    private UploadDirectoryHelper uploadDirectoryHelper;

    @BeforeClass
    public static void setUp() throws IOException {
        jimfs = Jimfs.newFileSystem();
        directory = jimfs.getPath("test");
        Files.createDirectory(directory);
        Files.createFile(jimfs.getPath("test/1"));
        Files.createFile(jimfs.getPath("test/2"));
    }

    @AfterClass
    public static void tearDown() throws IOException {
        jimfs.close();
    }

    @Before
    public void methodSetup() {
        singleUploadFunction = mock(Function.class);
        uploadDirectoryHelper = new UploadDirectoryHelper(TransferManagerConfiguration.builder().build(), singleUploadFunction);
    }

    @Test
    public void uploadDirectory_cancel_shouldCancelAllFutures() {
        CompletableFuture<CompletedUpload> future = new CompletableFuture<>();
        Upload upload = newUpload(future);

        CompletableFuture<CompletedUpload> future2 = new CompletableFuture<>();
        Upload upload2 = newUpload(future2);

        when(singleUploadFunction.apply(any(UploadRequest.class))).thenReturn(upload, upload2);

        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .build());

        uploadDirectory.completionFuture().cancel(true);

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);

        assertThatThrownBy(() -> future2.get(1, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);
    }

    @Test
    public void uploadDirectory_allUploadsSucceed_failedUploadsShouldBeEmpty() throws ExecutionException, InterruptedException,
                                                                                    TimeoutException {
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().eTag("1234").build();
        CompletedUpload completedUpload = CompletedUpload.builder().response(putObjectResponse).build();
        CompletableFuture<CompletedUpload> successfulFuture = new CompletableFuture<>();

        Upload upload = newUpload(successfulFuture);
        successfulFuture.complete(completedUpload);

        PutObjectResponse putObjectResponse2 = PutObjectResponse.builder().eTag("5678").build();
        CompletedUpload completedUpload2 = CompletedUpload.builder().response(putObjectResponse2).build();
        CompletableFuture<CompletedUpload> failedFuture = new CompletableFuture<>();
        Upload upload2 = newUpload(failedFuture);
        failedFuture.complete(completedUpload2);

        when(singleUploadFunction.apply(any(UploadRequest.class))).thenReturn(upload, upload2);

        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .build());

        CompletedUploadDirectory completedUploadDirectory = uploadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completedUploadDirectory.failedUploads()).isEmpty();
    }

    @Test
    public void uploadDirectory_partialSuccess_shouldProvideFailedUploads() throws ExecutionException, InterruptedException,
                                                                                   TimeoutException {
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().eTag("1234").build();
        CompletedUpload completedUpload = CompletedUpload.builder().response(putObjectResponse).build();
        CompletableFuture<CompletedUpload> successfulFuture = new CompletableFuture<>();
        Upload upload = newUpload(successfulFuture);
        successfulFuture.complete(completedUpload);

        SdkClientException exception = SdkClientException.create("failed");
        CompletableFuture<CompletedUpload> failedFuture = new CompletableFuture<>();
        Upload upload2 = newUpload(failedFuture);
        failedFuture.completeExceptionally(exception);

        when(singleUploadFunction.apply(any(UploadRequest.class))).thenReturn(upload, upload2);

        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .build());

        CompletedUploadDirectory completedUploadDirectory = uploadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completedUploadDirectory.failedUploads()).hasSize(1);
        assertThat(completedUploadDirectory.failedUploads().iterator().next().exception()).isEqualTo(exception);
        assertThat(completedUploadDirectory.failedUploads().iterator().next().request().source().toString()).isEqualTo("test/2");
    }

    private Upload newUpload(CompletableFuture<CompletedUpload> future) {
        return new DefaultUpload(future,
                                 new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder().build())
        );
    }
}
