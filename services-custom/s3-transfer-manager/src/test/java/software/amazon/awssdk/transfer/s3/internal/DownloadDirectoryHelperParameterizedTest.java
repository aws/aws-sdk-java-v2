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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.transfer.s3.util.S3ApiCallMockUtils.stubSuccessfulListObjects;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;

/**
 * Testing {@link DownloadDirectoryHelper} with different file systems.
 */
public class DownloadDirectoryHelperParameterizedTest {
    private Function<DownloadFileRequest, FileDownload> singleDownloadFunction;
    private ListObjectsHelper listObjectsHelper;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private Path directory;
    private static final String DIRECTORY_NAME = "test";

    public static Collection<FileSystem> fileSystems() {
        return Sets.newHashSet(Arrays.asList(Jimfs.newFileSystem(Configuration.unix()),
                                             Jimfs.newFileSystem(Configuration.osX()),
                                             Jimfs.newFileSystem(Configuration.windows())));
    }

    @BeforeEach
    public void methodSetup() {
        singleDownloadFunction = mock(Function.class);
        listObjectsHelper = mock(ListObjectsHelper.class);
        downloadDirectoryHelper = new DownloadDirectoryHelper(TransferManagerConfiguration.builder().build(),
                                                              listObjectsHelper,
                                                              singleDownloadFunction);
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void downloadDirectory_shouldRecursivelyDownload(FileSystem jimfs) {
        directory = jimfs.getPath("test");
        String[] keys = {"1.png", "2020/1.png", "2021/1.png", "2022/1.png", "2023/1/1.png"};
        stubSuccessfulListObjects(listObjectsHelper, keys);
        ArgumentCaptor<DownloadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);

        when(singleDownloadFunction.apply(requestArgumentCaptor.capture()))
            .thenReturn(completedDownload());
        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destinationDirectory(directory)
                                                                              .bucket("bucket")
                                                                              .build());
        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().join();
        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();

        List<DownloadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.getObjectRequest().bucket()).isEqualTo("bucket"));

        assertThat(actualRequests.size()).isEqualTo(keys.length);

        verifyDestinationPathForSingleDownload(jimfs, "/", keys, actualRequests);
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void downloadDirectory_withDelimiter_shouldHonor(FileSystem jimfs) {
        directory = jimfs.getPath("test");
        String delimiter = "|";
        String[] keys = {"1.png", "2020|1.png", "2021|1.png", "2022|1.png", "2023|1|1.png"};
        stubSuccessfulListObjects(listObjectsHelper, keys);
        ArgumentCaptor<DownloadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);

        when(singleDownloadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedDownload());
        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destinationDirectory(directory)
                                                                              .bucket("bucket")
                                                                              .delimiter(delimiter)
                                                                              .build());
        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().join();
        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();

        List<DownloadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.getObjectRequest().bucket()).isEqualTo("bucket"));
        assertThat(actualRequests.size()).isEqualTo(keys.length);

        verifyDestinationPathForSingleDownload(jimfs, delimiter, keys, actualRequests);
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void downloadDirectory_notDirectory_shouldCompleteFutureExceptionally(FileSystem jimfs) throws IOException {
        directory = jimfs.getPath("test");
        Path file = jimfs.getPath("afile" + UUID.randomUUID());
        Files.write(file, "hellowrold".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder().destinationDirectory(file)
                                                                                                   .bucket("bucketName").build()).completionFuture().join())
            .hasMessageContaining("is not a directory").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static DefaultFileDownload completedDownload() {
        return new DefaultFileDownload(CompletableFuture.completedFuture(CompletedFileDownload.builder()
                                                                                              .response(GetObjectResponse.builder().build())
                                                                                              .build()),
                                       CompletableFuture.completedFuture(new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder().build())),
                                       CompletableFuture.completedFuture(DownloadFileRequest.builder().getObjectRequest(GetObjectRequest.builder().build())
                                                                   .destination(Paths.get("."))
                                                                                  .build()));
    }

    private static void verifyDestinationPathForSingleDownload(FileSystem jimfs, String delimiter, String[] keys,
                                                               List<DownloadFileRequest> actualRequests) {
        String jimfsSeparator = jimfs.getSeparator();
        List<String> destinations =
            actualRequests.stream().map(u -> u.destination().toString())
                          .collect(Collectors.toList());
        List<String> expectedPaths =
            Arrays.stream(keys).map(k -> DIRECTORY_NAME + jimfsSeparator + k.replace(delimiter, jimfsSeparator)).collect(Collectors.toList());
        assertThat(destinations).isEqualTo(expectedPaths);
    }
}
