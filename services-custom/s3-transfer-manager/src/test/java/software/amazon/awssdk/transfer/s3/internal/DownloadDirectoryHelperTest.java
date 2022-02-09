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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.transfer.s3.util.S3ApiCallMockUtils.stubSuccessfulListObjects;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;

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
                                                              singleDownloadFunction,
                                                              listObjectsHelper);
    }

    @Test
    void downloadDirectory_allDownloadsSucceed_failedDownloadsShouldBeEmpty() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        GetObjectResponse getObjectResponse = GetObjectResponse.builder().eTag("1234").build();
        CompletedFileDownload completedFileDownload = CompletedFileDownload.builder().response(getObjectResponse).build();
        CompletableFuture<CompletedFileDownload> successfulFuture = new CompletableFuture<>();

        FileDownload fileDownload = newDownload(successfulFuture);
        successfulFuture.complete(completedFileDownload);

        GetObjectResponse getObjectResponse2 = GetObjectResponse.builder().eTag("5678").build();
        CompletedFileDownload completedFileDownload2 = CompletedFileDownload.builder().response(getObjectResponse2).build();
        CompletableFuture<CompletedFileDownload> successfulFuture2 = new CompletableFuture<>();
        FileDownload fileDownload2 = newDownload(successfulFuture2);
        successfulFuture2.complete(completedFileDownload2);

        when(singleDownloadFunction.apply(any(DownloadFileRequest.class))).thenReturn(fileDownload, fileDownload2);

        DirectoryDownload DownloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destinationDirectory(directory)
                                                                              .bucket("bucket")
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = DownloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<DownloadFileRequest> argumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        verify(singleDownloadFunction, times(2)).apply(argumentCaptor.capture());

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertThat(argumentCaptor.getAllValues()).element(0).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key1"));
        assertThat(argumentCaptor.getAllValues()).element(1).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key2"));
    }

    @Test
    void downloadDirectory_partialSuccess_shouldProvideFailedDownload() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        GetObjectResponse getObjectResponse = GetObjectResponse.builder().eTag("1234").build();
        CompletedFileDownload completedFileDownload = CompletedFileDownload.builder().response(getObjectResponse).build();
        CompletableFuture<CompletedFileDownload> successfulFuture = new CompletableFuture<>();
        FileDownload fileDownload = newDownload(successfulFuture);
        successfulFuture.complete(completedFileDownload);

        SdkClientException exception = SdkClientException.create("failed");
        CompletableFuture<CompletedFileDownload> failedFuture = new CompletableFuture<>();
        FileDownload fileDownload2 = newDownload(failedFuture);
        failedFuture.completeExceptionally(exception);

        when(singleDownloadFunction.apply(any(DownloadFileRequest.class))).thenReturn(fileDownload, fileDownload2);

        DirectoryDownload DownloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destinationDirectory(directory)
                                                                              .bucket("bucket")
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = DownloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completedDirectoryDownload.failedTransfers()).hasSize(1)
                                                                .element(0).satisfies(failedFileDownload -> assertThat(failedFileDownload.exception()).isEqualTo(exception));
    }

    private FileDownload newDownload(CompletableFuture<CompletedFileDownload> future) {
        return new DefaultFileDownload(future,
                                       new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder().build())
        );
    }
}
