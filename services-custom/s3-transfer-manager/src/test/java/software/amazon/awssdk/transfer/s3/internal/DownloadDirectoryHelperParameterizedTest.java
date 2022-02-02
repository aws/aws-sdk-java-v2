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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.assertj.core.util.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.transfer.s3.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Testing {@link DownloadDirectoryHelper} with different file systems.
 */
@RunWith(Parameterized.class)
public class DownloadDirectoryHelperParameterizedTest {
    private static final Set<Configuration> FILE_SYSTEMS = Sets.newHashSet(Arrays.asList(Configuration.unix(),
                                                                                         Configuration.osX(),
                                                                                         Configuration.windows()));
    private Function<DownloadFileRequest, FileDownload> singleDownloadFunction;
    private ListObjectsRecursivelyHelper listObjectsHelper;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private Path directory;
    private static final String DIRECTORY_NAME = "test";

    @Parameterized.Parameter
    public Configuration configuration;

    private FileSystem jimfs;

    @Parameterized.Parameters
    public static Collection<Configuration> fileSystems() {
        return FILE_SYSTEMS;
    }

    @Before
    public void methodSetup() throws IOException {
        singleDownloadFunction = mock(Function.class);
        listObjectsHelper = mock(ListObjectsRecursivelyHelper.class);
        jimfs = Jimfs.newFileSystem(configuration);
        downloadDirectoryHelper = new DownloadDirectoryHelper(TransferManagerConfiguration.builder().build(),
                                                              jimfs,
                                                              singleDownloadFunction,
                                                              listObjectsHelper);
        directory = jimfs.getPath("test");
    }

    @After
    public void tearDown() {
        if (jimfs != null) {
            IoUtils.closeQuietly(jimfs, null);
        } else {
            FileUtils.cleanUpTestDirectory(directory);
        }
    }

    @Test
    public void downloadDirectory_shouldRecursivelyDownload() {
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

        verifyDestinationPathForSingleDownload("/", keys, actualRequests);
    }

    @Test
    public void downloadDirectory_withDelimiter_shouldHonor() {
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

        verifyDestinationPathForSingleDownload(delimiter, keys, actualRequests);
    }

    @Test
    public void downloadDirectory_notDirectory_shouldCompleteFutureExceptionally() throws IOException {
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
                                       new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder().build()));
    }

    private void verifyDestinationPathForSingleDownload(String delimiter, String[] keys,
                                                        List<DownloadFileRequest> actualRequests) {
        String jimfsSeparator = this.jimfs.getSeparator();
        List<String> destinations =
            actualRequests.stream().map(u -> u.destination().toString())
                          .collect(Collectors.toList());
        List<String> expectedPaths =
            Arrays.stream(keys).map(k -> DIRECTORY_NAME + jimfsSeparator + k.replace(delimiter, jimfsSeparator)).collect(Collectors.toList());
        assertThat(destinations).isEqualTo(expectedPaths);
    }
}
