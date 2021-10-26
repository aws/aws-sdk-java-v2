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
import java.nio.file.Paths;
import java.util.List;
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
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.FileUpload;
import software.amazon.awssdk.transfer.s3.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.UploadDirectoryOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;

public class UploadDirectoryHelperTest {
    private static FileSystem jimfs;
    private static Path directory;
    private Function<UploadFileRequest, FileUpload> singleUploadFunction;
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
        CompletableFuture<CompletedFileUpload> future = new CompletableFuture<>();
        FileUpload fileUpload = newUpload(future);

        CompletableFuture<CompletedFileUpload> future2 = new CompletableFuture<>();
        FileUpload fileUpload2 = newUpload(future2);

        when(singleUploadFunction.apply(any(UploadFileRequest.class))).thenReturn(fileUpload, fileUpload2);

        DirectoryUpload uploadDirectory =
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
        CompletedFileUpload completedFileUpload = CompletedFileUpload.builder().response(putObjectResponse).build();
        CompletableFuture<CompletedFileUpload> successfulFuture = new CompletableFuture<>();

        FileUpload fileUpload = newUpload(successfulFuture);
        successfulFuture.complete(completedFileUpload);

        PutObjectResponse putObjectResponse2 = PutObjectResponse.builder().eTag("5678").build();
        CompletedFileUpload completedFileUpload2 = CompletedFileUpload.builder().response(putObjectResponse2).build();
        CompletableFuture<CompletedFileUpload> failedFuture = new CompletableFuture<>();
        FileUpload fileUpload2 = newUpload(failedFuture);
        failedFuture.complete(completedFileUpload2);

        when(singleUploadFunction.apply(any(UploadFileRequest.class))).thenReturn(fileUpload, fileUpload2);

        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .build());

        CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completedDirectoryUpload.failedTransfers()).isEmpty();
    }

    @Test
    public void uploadDirectory_partialSuccess_shouldProvideFailedUploads() throws ExecutionException, InterruptedException,
                                                                                   TimeoutException {
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().eTag("1234").build();
        CompletedFileUpload completedFileUpload = CompletedFileUpload.builder().response(putObjectResponse).build();
        CompletableFuture<CompletedFileUpload> successfulFuture = new CompletableFuture<>();
        FileUpload fileUpload = newUpload(successfulFuture);
        successfulFuture.complete(completedFileUpload);

        SdkClientException exception = SdkClientException.create("failed");
        CompletableFuture<CompletedFileUpload> failedFuture = new CompletableFuture<>();
        FileUpload fileUpload2 = newUpload(failedFuture);
        failedFuture.completeExceptionally(exception);

        when(singleUploadFunction.apply(any(UploadFileRequest.class))).thenReturn(fileUpload, fileUpload2);

        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .build());

        CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completedDirectoryUpload.failedTransfers()).hasSize(1);
        assertThat(completedDirectoryUpload.failedTransfers().iterator().next().exception()).isEqualTo(exception);
        assertThat(completedDirectoryUpload.failedTransfers().iterator().next().request().source().toString()).isEqualTo("test/2");
    }

    @Test
    public void uploadDirectory_withRequestTransformer_usesRequestTransformer() throws Exception {
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().eTag("1234").build();
        CompletedFileUpload completedFileUpload = CompletedFileUpload.builder().response(putObjectResponse).build();
        CompletableFuture<CompletedFileUpload> successfulFuture = new CompletableFuture<>();

        FileUpload upload = newUpload(successfulFuture);
        successfulFuture.complete(completedFileUpload);

        PutObjectResponse putObjectResponse2 = PutObjectResponse.builder().eTag("5678").build();
        CompletedFileUpload completedFileUpload2 = CompletedFileUpload.builder().response(putObjectResponse2).build();
        CompletableFuture<CompletedFileUpload> failedFuture = new CompletableFuture<>();
        FileUpload upload2 = newUpload(failedFuture);
        failedFuture.complete(completedFileUpload2);

        ArgumentCaptor<UploadFileRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(uploadRequestCaptor.capture())).thenReturn(upload, upload2);

        Path newSource = Paths.get("/new/path");
        PutObjectRequest newPutObjectRequest = PutObjectRequest.builder().build();
        TransferRequestOverrideConfiguration newOverrideConfig = TransferRequestOverrideConfiguration.builder()
                                                                                                     .build();

        UploadDirectoryOverrideConfiguration uploadConfig =
            UploadDirectoryOverrideConfiguration.builder()
                                                .uploadFileRequestTransformer(r -> r.source(newSource)
                                                                                    .putObjectRequest(newPutObjectRequest)
                                                                                    .overrideConfiguration(newOverrideConfig))
                                                .build();

        uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                    .sourceDirectory(directory)
                                                                    .bucket("bucket")
                                                                    .overrideConfiguration(uploadConfig)
                                                                    .build())
                             .completionFuture()
                             .get(5, TimeUnit.SECONDS);

        List<UploadFileRequest> uploadRequests = uploadRequestCaptor.getAllValues();
        assertThat(uploadRequests).hasSize(2);
        assertThat(uploadRequests).element(0).satisfies(r -> {
            assertThat(r.source()).isEqualTo(newSource);
            assertThat(r.putObjectRequest()).isEqualTo(newPutObjectRequest);
            assertThat(r.overrideConfiguration()).hasValue(newOverrideConfig);
        });
        assertThat(uploadRequests).element(1).satisfies(r -> {
            assertThat(r.source()).isEqualTo(newSource);
            assertThat(r.putObjectRequest()).isEqualTo(newPutObjectRequest);
            assertThat(r.overrideConfiguration()).hasValue(newOverrideConfig);
        });
    }

    private FileUpload newUpload(CompletableFuture<CompletedFileUpload> future) {
        return new DefaultFileUpload(future,
                                 new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder().build())
        );
    }
}
