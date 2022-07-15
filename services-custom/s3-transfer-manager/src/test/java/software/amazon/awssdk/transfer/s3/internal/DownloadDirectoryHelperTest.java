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
import static software.amazon.awssdk.transfer.s3.util.S3ApiCallMockUtils.stubSuccessfulListObjects;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.EncodingType;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.config.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

public class DownloadDirectoryHelperTest {
    private static FileSystem jimfs;
    private static Path directory;
    private Function<DownloadFileRequest, FileDownload> singleDownloadFunction;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private ListObjectsHelper listObjectsHelper;

    @BeforeAll
    public static void setup() {
        jimfs = Jimfs.newFileSystem();
        directory = jimfs.getPath("test");
    }

    @AfterAll
    public static void tearDown() {
        try {
            jimfs.close();
        } catch (IOException e) {
            // no-op
        }
    }

    @BeforeEach
    public void methodSetup() {
        listObjectsHelper = mock(ListObjectsHelper.class);
        singleDownloadFunction = mock(Function.class);
        downloadDirectoryHelper = new DownloadDirectoryHelper(TransferManagerConfiguration.builder().build(),
                                                              listObjectsHelper,
                                                              singleDownloadFunction);
    }

    @Test
    void downloadDirectory_allDownloadsSucceed_failedDownloadsShouldBeEmpty() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        FileDownload fileDownload = newSuccessfulDownload();

        FileDownload fileDownload2 = newSuccessfulDownload();

        when(singleDownloadFunction.apply(any(DownloadFileRequest.class))).thenReturn(fileDownload, fileDownload2);

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<DownloadFileRequest> argumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        verify(singleDownloadFunction, times(2)).apply(argumentCaptor.capture());

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertThat(argumentCaptor.getAllValues()).element(0).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key1"));
        assertThat(argumentCaptor.getAllValues()).element(1).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key2"));
    }

    @Test
    void downloadDirectory_cancel_shouldCancelAllFutures() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        CompletableFuture<CompletedFileDownload> future = new CompletableFuture<>();
        FileDownload fileDownload = newDownload(future);

        CompletableFuture<CompletedFileDownload> future2 = new CompletableFuture<>();
        FileDownload fileDownload2 = newDownload(future2);

        when(singleDownloadFunction.apply(any(DownloadFileRequest.class))).thenReturn(fileDownload, fileDownload2);

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build());
        downloadDirectory.completionFuture().cancel(true);

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);

        assertThatThrownBy(() -> future2.get(1, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);
    }

    @Test
    void downloadDirectory_partialSuccess_shouldProvideFailedDownload() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        FileDownload fileDownload = newSuccessfulDownload();

        SdkClientException exception = SdkClientException.create("failed");
        FileDownload fileDownload2 = newFailedDownload(exception);

        when(singleDownloadFunction.apply(any(DownloadFileRequest.class))).thenReturn(fileDownload, fileDownload2);

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completedDirectoryDownload.failedTransfers()).hasSize(1)
                                                                .element(0).satisfies(failedFileDownload -> assertThat(failedFileDownload.exception()).isEqualTo(exception));
    }

    @Test
    void downloadDirectory_withFilter_shouldHonorFilter() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        FileDownload fileDownload = newSuccessfulDownload();

        FileDownload fileDownload2 = newSuccessfulDownload();

        when(singleDownloadFunction.apply(any(DownloadFileRequest.class))).thenReturn(fileDownload, fileDownload2);

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .filter(s3Object -> "key2".equals(s3Object.key()))
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<DownloadFileRequest> argumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        verify(singleDownloadFunction, times(1)).apply(argumentCaptor.capture());

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertThat(argumentCaptor.getAllValues()).element(0).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key2"));
    }

    @Test
    void downloadDirectory_withDownloadRequestTransformer_shouldTransform() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        FileDownload fileDownload = newSuccessfulDownload();
        FileDownload fileDownload2 = newSuccessfulDownload();

        when(singleDownloadFunction.apply(any(DownloadFileRequest.class))).thenReturn(fileDownload, fileDownload2);
        Path newDestination = Paths.get("/new/path");
        GetObjectRequest newGetObjectRequest = GetObjectRequest.builder().build();
        TransferRequestOverrideConfiguration newOverrideConfiguration = TransferRequestOverrideConfiguration.builder()
                                                                                                         .addListener(LoggingTransferListener.create())
                                                                                                         .build();
        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .downloadFileRequestTransformer(d -> d.destination(newDestination)
                                                                                                                    .getObjectRequest(newGetObjectRequest)
                                                                                                                    .overrideConfiguration(newOverrideConfiguration))
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<DownloadFileRequest> argumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        verify(singleDownloadFunction, times(2)).apply(argumentCaptor.capture());

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertThat(argumentCaptor.getAllValues()).allSatisfy(d -> {
            assertThat(d.getObjectRequest()).isEqualTo(newGetObjectRequest);
            assertThat(d.overrideConfiguration()).hasValue(newOverrideConfiguration);
            assertThat(d.destination()).isEqualTo(newDestination);
        });
    }

    @Test
    void downloadDirectory_withListObjectsRequestTransformer_shouldTransform() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        FileDownload fileDownload = newSuccessfulDownload();
        FileDownload fileDownload2 = newSuccessfulDownload();

        EncodingType newEncodingType = EncodingType.URL;
        int newMaxKeys = 10;

        when(singleDownloadFunction.apply(any(DownloadFileRequest.class))).thenReturn(fileDownload, fileDownload2);
        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .listObjectsV2RequestTransformer(l -> l.encodingType(newEncodingType)
                                                                                  .maxKeys(newMaxKeys))
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<ListObjectsV2Request> argumentCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(listObjectsHelper, times(1)).listS3ObjectsRecursively(argumentCaptor.capture());

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertThat(argumentCaptor.getValue()).satisfies(l -> {
            assertThat(l.encodingType()).isEqualTo(newEncodingType);
            assertThat(l.maxKeys()).isEqualTo(newMaxKeys);
        });
    }

    private FileDownload newSuccessfulDownload() {
        GetObjectResponse getObjectResponse = GetObjectResponse.builder().eTag(UUID.randomUUID().toString()).build();
        CompletedFileDownload completedFileDownload = CompletedFileDownload.builder().response(getObjectResponse).build();
        CompletableFuture<CompletedFileDownload> successfulFuture = new CompletableFuture<>();
        FileDownload fileDownload = newDownload(successfulFuture);
        successfulFuture.complete(completedFileDownload);
        return fileDownload;
    }

    private FileDownload newFailedDownload(SdkClientException exception) {
        CompletableFuture<CompletedFileDownload> failedFuture = new CompletableFuture<>();
        FileDownload fileDownload2 = newDownload(failedFuture);
        failedFuture.completeExceptionally(exception);
        return fileDownload2;
    }

    private FileDownload newDownload(CompletableFuture<CompletedFileDownload> future) {
        return new DefaultFileDownload(future,
                                       new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder()
                                                                                                  .bytesTransferred(0L)
                                                                                                  .build()),
                                       () -> DownloadFileRequest.builder().destination(Paths.get(
                                           ".")).getObjectRequest(GetObjectRequest.builder().build()).build(),
                                       null);
    }
}
