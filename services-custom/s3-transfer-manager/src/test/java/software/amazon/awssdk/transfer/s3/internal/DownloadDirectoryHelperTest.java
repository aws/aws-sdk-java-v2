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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.EncodingType;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

public class DownloadDirectoryHelperTest {
    private static final String DIRECTORY_NAME = "test";
    private FileSystem fs;
    private Path directory;
    private Function<DownloadFileRequest, FileDownload> singleDownloadFunction;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private ListObjectsHelper listObjectsHelper;

    @BeforeEach
    public void methodSetup() {
        fs = Jimfs.newFileSystem();
        directory = fs.getPath("test");
        listObjectsHelper = mock(ListObjectsHelper.class);
        singleDownloadFunction = mock(Function.class);
        downloadDirectoryHelper = new DownloadDirectoryHelper(TransferManagerConfiguration.builder().build(),
                                                              listObjectsHelper,
                                                              singleDownloadFunction);
    }

    @AfterEach
    public void methodCleanup() throws IOException {
        fs.close();
    }

    public static Collection<FileSystem> fileSystems() {
        return Sets.newHashSet(Arrays.asList(Jimfs.newFileSystem(Configuration.unix()),
                                             Jimfs.newFileSystem(Configuration.osX()),
                                             Jimfs.newFileSystem(Configuration.windows())));
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

    @ParameterizedTest
    @ValueSource(strings = {"/blah",
                            "../blah/object.dat",
                            "blah/../../object.dat",
                            "blah/../object/../../blah/another/object.dat",
                            "../{directory-name}-2/object.dat"})
    void invalidKey_shouldThrowException(String testingString) throws Exception {
        assertExceptionThrownForInvalidKeys(testingString);
    }

    private void assertExceptionThrownForInvalidKeys(String key) throws IOException {
        Path destinationDirectory = Files.createTempDirectory("test");
        String lastElement = destinationDirectory.getName(destinationDirectory.getNameCount() - 1).toString();
        key = key.replace("{directory-name}", lastElement);
        stubSuccessfulListObjects(listObjectsHelper, key);
        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(destinationDirectory)
                                                                              .bucket("bucket")
                                                                              .build());

        assertThatThrownBy(() -> downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS))
            .hasCauseInstanceOf(SdkClientException.class).getRootCause().hasMessageContaining("Cannot download key");
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

        List<TransferListener> newTransferListener = Arrays.asList(LoggingTransferListener.create());

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .downloadFileRequestTransformer(d -> d.destination(newDestination)
                                                                                                                    .getObjectRequest(newGetObjectRequest)
                                                                                                                    .transferListeners(newTransferListener))
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<DownloadFileRequest> argumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        verify(singleDownloadFunction, times(2)).apply(argumentCaptor.capture());

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertThat(argumentCaptor.getAllValues()).allSatisfy(d -> {
            assertThat(d.getObjectRequest()).isEqualTo(newGetObjectRequest);
            assertThat(d.transferListeners()).isEqualTo(newTransferListener);
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
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build());
        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().join();
        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();

        List<DownloadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.getObjectRequest().bucket()).isEqualTo("bucket"));

        assertThat(actualRequests.size()).isEqualTo(keys.length);

        verifyDestinationPathForSingleDownload(jimfs, "/", keys, actualRequests);
    }

    /**
     * The S3 bucket has the following keys:
     * abc/def/image.jpg
     * abc/def/title.jpg
     * abc/def/ghi/xyz.txt
     *
     * if the prefix is "abc/def/", the structure should like this:
     * image.jpg
     * title.jpg
     * ghi
     *  - xyz.txt
     */
    @ParameterizedTest
    @MethodSource("fileSystems")
    void downloadDirectory_withPrefix_shouldStripPrefixInDestinationPath(FileSystem jimfs) {
        directory = jimfs.getPath("test");
        String[] keys = {"abc/def/image.jpg", "abc/def/title.jpg", "abc/def/ghi/xyz.txt"};
        stubSuccessfulListObjects(listObjectsHelper, keys);
        ArgumentCaptor<DownloadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);

        when(singleDownloadFunction.apply(requestArgumentCaptor.capture()))
            .thenReturn(completedDownload());
        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .listObjectsV2RequestTransformer(l -> l.prefix(
                                                                                  "abc/def/"))
                                                                              .build());
        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().join();
        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();

        List<DownloadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();

        assertThat(actualRequests.size()).isEqualTo(keys.length);

        List<String> destinations =
            actualRequests.stream().map(u -> u.destination().toString())
                          .collect(Collectors.toList());

        String jimfsSeparator = jimfs.getSeparator();

        List<String> expectedPaths =
            Arrays.asList("image.jpg", "title.jpg", "ghi/xyz.txt").stream()
                  .map(k -> DIRECTORY_NAME + jimfsSeparator + k.replace("/",jimfsSeparator)).collect(Collectors.toList());
        assertThat(destinations).isEqualTo(expectedPaths);
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void downloadDirectory_containsObjectWithPrefixInIt_shouldInclude(FileSystem jimfs) {
        String prefix = "abc";
        directory = jimfs.getPath("test");
        String[] keys = {"abc/def/image.jpg", "abc/def/title.jpg", "abcd"};
        stubSuccessfulListObjects(listObjectsHelper, keys);
        ArgumentCaptor<DownloadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);

        when(singleDownloadFunction.apply(requestArgumentCaptor.capture()))
            .thenReturn(completedDownload());
        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .listObjectsV2RequestTransformer(l -> l.prefix(prefix))
                                                                              .build());
        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().join();
        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();

        List<DownloadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();

        assertThat(actualRequests.size()).isEqualTo(keys.length);

        List<String> destinations =
            actualRequests.stream().map(u -> u.destination().toString())
                          .collect(Collectors.toList());

        String jimfsSeparator = jimfs.getSeparator();

        List<String> expectedPaths =
            Arrays.asList("def/image.jpg", "def/title.jpg", "abcd").stream()
                  .map(k -> DIRECTORY_NAME + jimfsSeparator + k.replace("/",jimfsSeparator)).collect(Collectors.toList());
        assertThat(destinations).isEqualTo(expectedPaths);
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
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .listObjectsV2RequestTransformer(r -> r.delimiter(delimiter))
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
        assertThatThrownBy(() -> downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder().destination(file)
                                                                                                   .bucket("bucketName").build()).completionFuture().join())
            .hasMessageContaining("is not a directory").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static DefaultFileDownload completedDownload() {
        return new DefaultFileDownload(CompletableFuture.completedFuture(CompletedFileDownload.builder()
                                                                                              .response(GetObjectResponse.builder().build())
                                                                                              .build()),
                                       new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder()
                                                                                                  .transferredBytes(0L)
                                                                                                  .build()),
                                       () -> DownloadFileRequest.builder().getObjectRequest(GetObjectRequest.builder().build())
                                                                .destination(Paths.get("."))
                                                                .build(),
                                       null);
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
                                                                                                  .transferredBytes(0L)
                                                                                                  .build()),
                                       () -> DownloadFileRequest.builder().destination(Paths.get(
                                           ".")).getObjectRequest(GetObjectRequest.builder().build()).build(),
                                       null);
    }
}
