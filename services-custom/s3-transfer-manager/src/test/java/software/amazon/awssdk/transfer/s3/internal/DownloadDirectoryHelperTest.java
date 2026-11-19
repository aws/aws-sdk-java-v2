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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.transfer.s3.util.S3ApiCallMockUtils.stubSuccessfulListObjects;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.EncodingType;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileDownload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FailedFileDownload;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.async.SimplePublisher;

public class DownloadDirectoryHelperTest {
    private static final String DIRECTORY_NAME = "test";
    private FileSystem fs;
    private Path directory;
    private DownloadDirectoryHelper.FileDownloader singleDownloadFunction;
    private DownloadDirectoryHelper downloadDirectoryHelper;
    private ListObjectsHelper listObjectsHelper;

    @BeforeEach
    public void methodSetup() {
        fs = Jimfs.newFileSystem();
        directory = fs.getPath("test");
        listObjectsHelper = mock(ListObjectsHelper.class);
        singleDownloadFunction = mock(DownloadDirectoryHelper.FileDownloader.class);
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

        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(fileDownload, fileDownload2));

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<DownloadFileRequest> argumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        verify(singleDownloadFunction, times(2)).download(argumentCaptor.capture(), any(Path.class), any(Runnable.class));

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertThat(argumentCaptor.getAllValues()).element(0).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key1"));
        assertThat(argumentCaptor.getAllValues()).element(1).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key2"));
    }

    @Test
    void downloadDirectory_containsFolderObjects_shouldSkip() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, S3Object.builder().key("key1").size(10L).build(),
                                  S3Object.builder().key("key2").size(0L).build(),
                                  S3Object.builder().key("folder/").size(0L).build());

        FileDownload fileDownload = newSuccessfulDownload();
        FileDownload fileDownload2 = newSuccessfulDownload();

        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(fileDownload, fileDownload2));

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<DownloadFileRequest> argumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        verify(singleDownloadFunction, times(2)).download(argumentCaptor.capture(), any(Path.class), any(Runnable.class));

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        List<DownloadFileRequest> allValues = argumentCaptor.getAllValues();
        assertThat(allValues).size().isEqualTo(2);
        assertThat(allValues).element(0).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key1"));
        assertThat(allValues).element(1).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/blah",
                            "../blah/object.dat",
                            "blah/../../object.dat",
                            "blah/../object/../../blah/another/object.dat",
                            "../{directory-name}-2/object.dat"})
    void invalidKey_shouldReportFailedDownloadAndContinue(String testingString) throws Exception {
        Path destinationDirectory = Files.createTempDirectory("test");
        String lastElement = destinationDirectory.getName(destinationDirectory.getNameCount() - 1).toString();
        String key = testingString.replace("{directory-name}", lastElement);
        stubSuccessfulListObjects(listObjectsHelper, key);
        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(destinationDirectory)
                                                                              .bucket("bucket")
                                                                              .build());

        CompletedDirectoryDownload completed = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completed.failedTransfers()).hasSize(1);
        assertRefused(completed.failedTransfers().get(0), destinationDirectory, key);
        verify(singleDownloadFunction, never()).download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class));
    }

    @Test
    void downloadDirectory_destinationDoesNotExistAndOnlyKeyRefused_shouldReportFailureAndNotCreateDestination()
        throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "../escape.txt");

        CompletedDirectoryDownload completed =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build())
                                   .completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completed.failedTransfers()).hasSize(1);
        assertRefused(completed.failedTransfers().get(0), directory, "../escape.txt");
        assertThat(Files.exists(directory)).isFalse();
        verify(singleDownloadFunction, never()).download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class));
    }

    @Test
    void downloadDirectory_twoRefusedKeys_shouldReportDifferentPlaceholders() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "../one.txt", "/two.txt");

        CompletedDirectoryDownload completed =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build())
                                   .completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completed.failedTransfers()).hasSize(2);
        List<Path> placeholders = completed.failedTransfers().stream()
                                           .map(f -> f.request().destination())
                                           .collect(Collectors.toList());
        assertThat(placeholders.get(0)).isNotEqualTo(placeholders.get(1));
        assertThat(placeholders.get(0).getParent()).isNotEqualTo(placeholders.get(1).getParent());
    }

    @Test
    void downloadDirectory_manyRefusedKeysInOneListingPage_shouldReportEachWithoutStackOverflow() throws Exception {
        // S3 returns at most 1,000 keys per page. A real ListObjectsHelper is used because the listing publisher delivers the
        // items of one page from a recursive drain, which a Flowable-based stub would not exercise.
        int numKeys = 1_000;
        List<S3Object> contents = IntStream.range(0, numKeys)
                                           .mapToObj(i -> S3Object.builder().key("../escape-" + i + ".txt").size(1L).build())
                                           .collect(Collectors.toList());
        ListObjectsV2Response page = ListObjectsV2Response.builder().contents(contents).isTruncated(false).build();
        ListObjectsHelper realListObjectsHelper = new ListObjectsHelper(r -> CompletableFuture.completedFuture(page));
        DownloadDirectoryHelper helper = new DownloadDirectoryHelper(TransferManagerConfiguration.builder().build(),
                                                                     realListObjectsHelper,
                                                                     singleDownloadFunction);

        CompletedDirectoryDownload completed =
            helper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                             .destination(directory)
                                                             .bucket("bucket")
                                                             .build())
                  .completionFuture().get(30, TimeUnit.SECONDS);

        assertThat(completed.failedTransfers()).hasSize(numKeys);
        assertThat(completed.failedTransfers()).allSatisfy(f -> assertThat(f.exception())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Cannot download key"));
        verify(singleDownloadFunction, never()).download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class));
        assertThat(Files.exists(directory)).isFalse();
    }

    @Test
    void downloadDirectory_refusedKeyAmongBenignKeys_shouldDownloadOthersAndReportOneFailure() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "a.txt", "b.txt", "c/../../escape.txt", "d.txt");
        ArgumentCaptor<DownloadFileRequest> requestCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        when(singleDownloadFunction.download(requestCaptor.capture(), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(completedDownload()));
        DownloadDirectoryHelper helper =
            new DownloadDirectoryHelper(TransferManagerConfiguration.builder().transferDirectoryMaxConcurrency(1).build(),
                                        listObjectsHelper,
                                        singleDownloadFunction);

        CompletedDirectoryDownload completed =
            helper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                             .destination(directory)
                                                             .bucket("bucket")
                                                             .build())
                  .completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(requestCaptor.getAllValues()).extracting(r -> r.getObjectRequest().key())
                                                .containsExactly("a.txt", "b.txt", "d.txt");
        assertThat(completed.failedTransfers()).hasSize(1);
        assertRefused(completed.failedTransfers().get(0), directory, "c/../../escape.txt");
    }

    @Test
    void downloadDirectory_keyInvalidOnWindowsFileSystem_shouldReportFailedDownloadAndContinue() throws Exception {
        String invalidKey = "a?b.txt";
        try (FileSystem windows = Jimfs.newFileSystem(Configuration.windows())) {
            Path windowsDirectory = windows.getPath("test");
            assertThatThrownBy(() -> windowsDirectory.resolve(invalidKey)).isInstanceOf(InvalidPathException.class);

            stubSuccessfulListObjects(listObjectsHelper, "ok.txt", invalidKey);
            ArgumentCaptor<DownloadFileRequest> requestCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
            when(singleDownloadFunction.download(requestCaptor.capture(), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(completedDownload()));

            CompletedDirectoryDownload completed =
                downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                                  .destination(windowsDirectory)
                                                                                  .bucket("bucket")
                                                                                  .build())
                                       .completionFuture().get(5, TimeUnit.SECONDS);

            assertThat(requestCaptor.getAllValues()).extracting(r -> r.getObjectRequest().key()).containsExactly("ok.txt");
            assertThat(completed.failedTransfers()).hasSize(1);
            FailedFileDownload failed = completed.failedTransfers().get(0);
            assertRefused(failed, windowsDirectory, invalidKey);
            assertThat(failed.exception()).hasMessageContaining("not a valid path on this file system")
                                          .hasCauseInstanceOf(InvalidPathException.class);
        }
    }

    @Test
    void downloadDirectory_keysWithControlCharacters_debugLogsAreOneEscapedLineEach() throws Exception {
        try (FileSystem unixFs = Jimfs.newFileSystem(Configuration.unix())) {
            Path destination = unixFs.getPath("/dest");
            String sentKey = "ok.txt\r\nFORGED level=ERROR";
            String refusedKey = "../escape\r\nFORGED level=ERROR";
            stubSuccessfulListObjects(listObjectsHelper, sentKey, refusedKey);
            ArgumentCaptor<DownloadFileRequest> requestCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
            when(singleDownloadFunction.download(requestCaptor.capture(), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(completedDownload()));

            CompletedDirectoryDownload completed;
            List<String> messages;
            try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
                completed = downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                                              .destination(destination)
                                                                                              .bucket("bucket")
                                                                                              .build())
                                                   .completionFuture().get(5, TimeUnit.SECONDS);
                messages = logCaptor.loggedEvents()
                                    .stream()
                                    .filter(e -> S3TransferManager.class.getName().equals(e.getLoggerName()))
                                    .map(LogEvent::getMessage)
                                    .map(m -> m.getFormattedMessage())
                                    .collect(Collectors.toList());
            }

            assertThat(requestCaptor.getAllValues()).extracting(r -> r.getObjectRequest().key()).containsExactly(sentKey);
            assertThat(completed.failedTransfers()).hasSize(1);
            assertThat(completed.failedTransfers().get(0).request().getObjectRequest().key()).isEqualTo(refusedKey);

            assertThat(messages).allSatisfy(m -> assertThat(m).doesNotContain("\r", "\n"));
            assertThat(messages).anySatisfy(m -> assertThat(m).startsWith("Sending download request")
                                                              .contains("ok.txt\\r\\nFORGED level=ERROR"));
            assertThat(messages).anySatisfy(m -> assertThat(m).startsWith("Skipping download of key")
                                                              .contains("../escape\\r\\nFORGED level=ERROR"));
        }
    }

    private static void assertRefused(FailedFileDownload failed, Path destinationDirectory, String key) {
        assertThat(failed.exception()).isInstanceOf(SdkClientException.class)
                                      .hasMessageContaining("Cannot download key " + key);
        assertThat(failed.request().getObjectRequest().bucket()).isEqualTo("bucket");
        assertThat(failed.request().getObjectRequest().key()).isEqualTo(key);

        Path placeholder = failed.request().destination();
        assertThat(placeholder.getParent().getParent()).isEqualTo(destinationDirectory);
        assertThat(placeholder).isNotEqualTo(destinationDirectory);
        assertThat(placeholder.getParent()).isNotEqualTo(destinationDirectory);
        assertThat(placeholder.getParent().getFileName().toString()).startsWith(".s3tm-refused-");
        assertThat(Files.exists(placeholder.getParent())).isFalse();
        assertThat(Files.exists(placeholder)).isFalse();
    }

    @Test
    void downloadDirectory_cancel_shouldCancelAllFutures() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        CompletableFuture<CompletedFileDownload> future = new CompletableFuture<>();
        FileDownload fileDownload = newDownload(future);

        CompletableFuture<CompletedFileDownload> future2 = new CompletableFuture<>();
        FileDownload fileDownload2 = newDownload(future2);

        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch bothStarted = new CountDownLatch(2);
        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(invocation -> {
            bothStarted.countDown();
            return callCount.incrementAndGet() == 1 ? fileDownload : fileDownload2;
        });

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .build());

        assertThat(bothStarted.await(5, TimeUnit.SECONDS)).isTrue();

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

        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(fileDownload, fileDownload2));

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

        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(fileDownload, fileDownload2));

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .filter(s3Object -> "key2".equals(s3Object.key()))
                                                                              .build());

        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        ArgumentCaptor<DownloadFileRequest> argumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);
        verify(singleDownloadFunction, times(1)).download(argumentCaptor.capture(), any(Path.class), any(Runnable.class));

        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertThat(argumentCaptor.getAllValues()).element(0).satisfies(d -> assertThat(d.getObjectRequest().key()).isEqualTo(
            "key2"));
    }

    @Test
    void downloadDirectory_withDownloadRequestTransformer_shouldTransform() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        FileDownload fileDownload = newSuccessfulDownload();
        FileDownload fileDownload2 = newSuccessfulDownload();

        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(fileDownload, fileDownload2));
        Path newDestination = directory.getFileSystem().getPath("/new/path");
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
        verify(singleDownloadFunction, times(2)).download(argumentCaptor.capture(), any(Path.class), any(Runnable.class));

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

        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(fileDownload, fileDownload2));
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

        when(singleDownloadFunction.download(requestArgumentCaptor.capture(), any(Path.class), any(Runnable.class)))
            .thenAnswer(writingWritePath(completedDownload()));
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

        when(singleDownloadFunction.download(requestArgumentCaptor.capture(), any(Path.class), any(Runnable.class)))
            .thenAnswer(writingWritePath(completedDownload()));
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


        String jimfsSeparator = jimfs.getSeparator();

        List<String> expectedPaths =
            Arrays.asList("image.jpg", "title.jpg", "ghi/xyz.txt").stream()
                  .map(k -> DIRECTORY_NAME + jimfsSeparator + k.replace("/",jimfsSeparator)).collect(Collectors.toList());
        assertDestinations(actualRequests, expectedPaths);
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void downloadDirectory_containsObjectWithPrefixInIt_shouldInclude(FileSystem jimfs) {
        String prefix = "abc";
        directory = jimfs.getPath("test");
        String[] keys = {"abc/def/image.jpg", "abc/def/title.jpg", "abcd"};
        stubSuccessfulListObjects(listObjectsHelper, keys);
        ArgumentCaptor<DownloadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);

        when(singleDownloadFunction.download(requestArgumentCaptor.capture(), any(Path.class), any(Runnable.class)))
            .thenAnswer(writingWritePath(completedDownload()));
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


        String jimfsSeparator = jimfs.getSeparator();

        List<String> expectedPaths =
            Arrays.asList("def/image.jpg", "def/title.jpg", "abcd").stream()
                  .map(k -> DIRECTORY_NAME + jimfsSeparator + k.replace("/",jimfsSeparator)).collect(Collectors.toList());
        assertDestinations(actualRequests, expectedPaths);
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void downloadDirectory_withDelimiter_shouldHonor(FileSystem jimfs) {
        directory = jimfs.getPath("test");
        String delimiter = "|";
        String[] keys = {"1.png", "2020|1.png", "2021|1.png", "2022|1.png", "2023|1|1.png"};
        stubSuccessfulListObjects(listObjectsHelper, keys);
        ArgumentCaptor<DownloadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DownloadFileRequest.class);

        when(singleDownloadFunction.download(requestArgumentCaptor.capture(), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(completedDownload()));
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

    @Test
    void downloadDirectory_withDownloadRequestTransformer_transformerThrows_failsDownload() {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        FileDownload fileDownload = newSuccessfulDownload();
        FileDownload fileDownload2 = newSuccessfulDownload();

        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(fileDownload, fileDownload2));


        RuntimeException exception = new RuntimeException("boom");
        Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer = b -> {
            throw exception;
        };

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .downloadFileRequestTransformer(downloadFileRequestTransformer)
                                                                              .build());

        assertThatThrownBy(downloadDirectory.completionFuture()::join).getCause().hasCause(exception);
    }

    @Test
    void downloadDirectory_withListObjectsRequestTransformer_transformerThrows_failsDownload() {
        stubSuccessfulListObjects(listObjectsHelper, "key1", "key2");

        FileDownload fileDownload = newSuccessfulDownload();
        FileDownload fileDownload2 = newSuccessfulDownload();

        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(fileDownload, fileDownload2));


        RuntimeException exception = new RuntimeException("boom");
        Consumer<ListObjectsV2Request.Builder> downloadFileRequestTransformer = b -> {
            throw exception;
        };

        DirectoryDownload downloadDirectory =
            downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder()
                                                                              .destination(directory)
                                                                              .bucket("bucket")
                                                                              .listObjectsV2RequestTransformer(downloadFileRequestTransformer)
                                                                              .build());

        assertThatThrownBy(downloadDirectory.completionFuture()::join).hasCause(exception);
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
        List<String> expectedPaths =
            Arrays.stream(keys).map(k -> DIRECTORY_NAME + jimfsSeparator + k.replace(delimiter, jimfsSeparator)).collect(Collectors.toList());
        assertDestinations(actualRequests, expectedPaths);
    }

    private FileDownload newSuccessfulDownload() {
        GetObjectResponse getObjectResponse = GetObjectResponse.builder().eTag(UUID.randomUUID().toString()).build();
        CompletedFileDownload completedFileDownload = CompletedFileDownload.builder().response(getObjectResponse).build();
        CompletableFuture<CompletedFileDownload> successfulFuture = new CompletableFuture<>();
        FileDownload fileDownload = newDownload(successfulFuture);
        successfulFuture.complete(completedFileDownload);
        return fileDownload;
    }

    private FileDownload newFailedDownload(RuntimeException exception) {
        CompletableFuture<CompletedFileDownload> failedFuture = new CompletableFuture<>();
        FileDownload fileDownload2 = newDownload(failedFuture);
        failedFuture.completeExceptionally(exception);
        return fileDownload2;
    }

    @Test
    void downloadDirectory_cancelledFuture_shouldNotCreateDirectories() throws Exception {
        SimplePublisher<S3Object> publisher = new SimplePublisher<>();
        when(listObjectsHelper.listS3ObjectsRecursively(any(ListObjectsV2Request.class)))
            .thenReturn(SdkPublisher.adapt(publisher));

        DownloadDirectoryHelper helper = new DownloadDirectoryHelper(
            TransferManagerConfiguration.builder().build(),
            listObjectsHelper,
            singleDownloadFunction);

        DirectoryDownload directoryDownload = helper.downloadDirectory(
            DownloadDirectoryRequest.builder()
                                    .destination(directory)
                                    .bucket("bucket")
                                    .build());

        directoryDownload.completionFuture().cancel(true);

        publisher.send(S3Object.builder().key("subdir/file.txt").size(100L).build());
        publisher.complete();

        Thread.sleep(200);

        Path subdir = directory.resolve("subdir");
        assertThat(Files.exists(subdir)).isFalse();
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

    @Test
    void downloadDirectory_keysResolvingToSameFile_shouldLeaveOneCompleteObject() throws Exception {
        String longBody = "C2:x//y.txt";
        String shortBody = "C2:x/y.txt";
        stubSuccessfulListObjects(listObjectsHelper, "x//y.txt", "x/y.txt");
        Map<String, FileWritingDownload> downloads = stubFileWritingDownloads();

        DirectoryDownload directoryDownload = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build());

        awaitStarted(downloads, "x//y.txt", "x/y.txt");
        Path destination = directory.resolve("x/y.txt");
        assertThat(downloads.values()).allSatisfy(d -> {
            assertThat(d.request.destination()).isEqualTo(destination);
            assertTempFileFor(d.writePath, destination);
        });
        assertThat(downloads.get("x//y.txt").writePath).isNotEqualTo(downloads.get("x/y.txt").writePath);
        downloads.get("x//y.txt").succeed(longBody);
        downloads.get("x/y.txt").succeed(shortBody);

        CompletedDirectoryDownload completed = directoryDownload.completionFuture().get(5, TimeUnit.SECONDS);
        assertThat(completed.failedTransfers()).isEmpty();
        assertThat(readString(directory.resolve("x/y.txt"))).isEqualTo(shortBody);
        assertNoTempFiles(directory);
    }

    @Test
    void downloadDirectory_keyFailsAfterCollidingKeySucceeded_shouldKeepSucceededObject() throws Exception {
        String body = "C2:x//y.txt";
        stubSuccessfulListObjects(listObjectsHelper, "x//y.txt", "x/y.txt");
        Map<String, FileWritingDownload> downloads = stubFileWritingDownloads();

        DirectoryDownload directoryDownload = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build());

        awaitStarted(downloads, "x//y.txt", "x/y.txt");
        downloads.get("x//y.txt").succeed(body);
        downloads.get("x/y.txt").fail(SdkClientException.create("connection reset"));

        CompletedDirectoryDownload completed = directoryDownload.completionFuture().get(5, TimeUnit.SECONDS);
        assertThat(completed.failedTransfers()).hasSize(1);
        FailedFileDownload failed = completed.failedTransfers().iterator().next();
        assertThat(failed.request().getObjectRequest().key()).isEqualTo("x/y.txt");
        assertThat(failed.request().destination()).isEqualTo(directory.resolve("x/y.txt"));
        assertThat(failed.exception()).isInstanceOf(SdkClientException.class).hasMessageContaining("connection reset");
        assertThat(readString(directory.resolve("x/y.txt"))).isEqualTo(body);
        assertNoTempFiles(directory);
    }

    @Test
    void downloadDirectory_downloadFails_shouldKeepExistingFile() throws Exception {
        Files.createDirectories(directory);
        Files.write(directory.resolve("a.txt"), "existing".getBytes(StandardCharsets.UTF_8));
        stubSuccessfulListObjects(listObjectsHelper, "a.txt");
        Map<String, FileWritingDownload> downloads = stubFileWritingDownloads();

        DirectoryDownload directoryDownload = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build());

        awaitStarted(downloads, "a.txt");
        downloads.get("a.txt").fail(SdkClientException.create("failed"));

        CompletedDirectoryDownload completed = directoryDownload.completionFuture().get(5, TimeUnit.SECONDS);
        assertThat(completed.failedTransfers()).hasSize(1);
        assertThat(readString(directory.resolve("a.txt"))).isEqualTo("existing");
        assertNoTempFiles(directory);
    }

    @Test
    void downloadDirectory_downloadSucceeds_shouldReplaceExistingFile() throws Exception {
        Files.createDirectories(directory);
        Files.write(directory.resolve("a.txt"), "a longer existing body".getBytes(StandardCharsets.UTF_8));
        stubSuccessfulListObjects(listObjectsHelper, "a.txt");
        Map<String, FileWritingDownload> downloads = stubFileWritingDownloads();

        DirectoryDownload directoryDownload = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build());

        awaitStarted(downloads, "a.txt");
        downloads.get("a.txt").succeed("new");

        assertThat(directoryDownload.completionFuture().get(5, TimeUnit.SECONDS).failedTransfers()).isEmpty();
        assertThat(readString(directory.resolve("a.txt"))).isEqualTo("new");
        assertNoTempFiles(directory);
    }

    @Test
    void downloadDirectory_cancelledDuringDownload_shouldRemoveTempFileAndNotCreateDestination() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "a.txt");
        Map<String, FileWritingDownload> downloads = stubFileWritingDownloads();

        DirectoryDownload directoryDownload = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build());

        awaitStarted(downloads, "a.txt");
        directoryDownload.completionFuture().cancel(true);

        assertThatThrownBy(() -> downloads.get("a.txt").future.get(1, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);
        assertThat(Files.exists(directory.resolve("a.txt"))).isFalse();
        assertNoTempFiles(directory);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "\u00e9", "\u6f22", "\ud83d\ude00"})
    void downloadDirectory_nameTooLongForSuffix_shouldUseSuffixOnlyWithin255Bytes(String character) throws Exception {
        int bytesPerCharacter = character.getBytes(StandardCharsets.UTF_8).length;
        String longName = IntStream.range(0, 255 / bytesPerCharacter).mapToObj(i -> character).collect(Collectors.joining());

        Path writePath = writePathFor(longName);

        String tempName = writePath.getFileName().toString();
        assertThat(tempName).matches("s3tmp\\.[0-9a-f]{16}");
        assertThat(writePath.getParent()).isEqualTo(directory);
    }

    @Test
    void downloadDirectory_nameThatFitsWithSuffix_shouldKeepName() throws Exception {
        String name = IntStream.range(0, 232).mapToObj(i -> "a").collect(Collectors.joining());

        Path writePath = writePathFor(name);

        String tempName = writePath.getFileName().toString();
        assertThat(tempName).startsWith(name + ".s3tmp.");
        assertThat(tempName.getBytes(StandardCharsets.UTF_8)).hasSize(255);
    }

    @Test
    void downloadDirectory_nameOneByteTooLongForSuffix_shouldUseSuffixOnly() throws Exception {
        String name = IntStream.range(0, 233).mapToObj(i -> "a").collect(Collectors.joining());

        Path writePath = writePathFor(name);

        assertThat(writePath.getFileName().toString()).startsWith("s3tmp.");
    }

    private Path writePathFor(String key) throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, key);
        ArgumentCaptor<Path> writePathCaptor = ArgumentCaptor.forClass(Path.class);
        when(singleDownloadFunction.download(any(DownloadFileRequest.class), writePathCaptor.capture(), any(Runnable.class)))
            .thenAnswer(writingWritePath(completedDownload()));
        downloadDirectoryHelper.downloadDirectory(DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build())
                               .completionFuture().get(5, TimeUnit.SECONDS);
        return writePathCaptor.getValue();
    }

    @Test
    void downloadDirectory_destinationHasNoFileName_shouldReportThatKeyAndDownloadOthers() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "good.txt", "bad.txt");
        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(writingWritePath(completedDownload()));
        Path root = directory.toAbsolutePath().getRoot();

        CompletedDirectoryDownload completed = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder()
                                    .destination(directory)
                                    .bucket("bucket")
                                    .downloadFileRequestTransformer(f -> {
                                        if ("bad.txt".equals(f.build().getObjectRequest().key())) {
                                            f.destination(root);
                                        }
                                    })
                                    .build()).completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completed.failedTransfers()).hasSize(1);
        FailedFileDownload failed = completed.failedTransfers().iterator().next();
        assertThat(failed.request().getObjectRequest().key()).isEqualTo("bad.txt");
        assertThat(failed.exception()).isInstanceOf(SdkClientException.class).hasMessageContaining("not a path to a file");
        verify(singleDownloadFunction, times(1)).download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class));
    }

    /**
     * Answers like a real download: the write path exists once the download is started, and a download that has already
     * succeeded has run its before-completion step. Returns the downloads in order, then keeps returning the last one.
     */
    private Answer<FileDownload> writingWritePath(FileDownload... downloads) {
        AtomicInteger calls = new AtomicInteger();
        return invocation -> {
            Path writePath = invocation.getArgument(1);
            if (writePath.getParent() != null) {
                Files.createDirectories(writePath.getParent());
            }
            if (!Files.exists(writePath)) {
                Files.createFile(writePath);
            }
            FileDownload download = downloads[Math.min(calls.getAndIncrement(), downloads.length - 1)];
            CompletableFuture<CompletedFileDownload> completion = download.completionFuture();
            if (completion.isDone() && !completion.isCompletedExceptionally()) {
                try {
                    Runnable beforeCompletion = invocation.getArgument(2);
                    beforeCompletion.run();
                } catch (RuntimeException e) {
                    return newFailedDownload(e);
                }
            }
            return download;
        };
    }

    @Test
    void downloadDirectory_downloadSucceedsWithoutWritingFile_shouldReportKeyAndKeepExistingFile() throws Exception {
        Files.createDirectories(directory);
        Files.write(directory.resolve("a.txt"), "existing".getBytes(StandardCharsets.UTF_8));
        stubSuccessfulListObjects(listObjectsHelper, "a.txt");
        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class)))
            .thenAnswer(invocation -> {
                try {
                    ((Runnable) invocation.getArgument(2)).run();
                } catch (RuntimeException e) {
                    return newFailedDownload(e);
                }
                return completedDownload();
            });

        CompletedDirectoryDownload completed = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build()).completionFuture()
                                                                      .get(5, TimeUnit.SECONDS);

        assertThat(completed.failedTransfers()).hasSize(1);
        assertThat(completed.failedTransfers().iterator().next().exception())
            .isInstanceOf(SdkClientException.class).hasMessageContaining("completed without writing");
        assertThat(readString(directory.resolve("a.txt"))).isEqualTo("existing");
    }

    @Test
    void downloadDirectory_destinationIsSymbolicLink_shouldReplaceLinkAndLeaveItsTargetUnchanged() throws Exception {
        Files.createDirectories(directory);
        Path linkTarget = fs.getPath("elsewhere.txt");
        Files.write(linkTarget, "link target".getBytes(StandardCharsets.UTF_8));
        Files.createSymbolicLink(directory.resolve("a.txt"), linkTarget.toAbsolutePath());
        stubSuccessfulListObjects(listObjectsHelper, "a.txt");
        Map<String, FileWritingDownload> downloads = stubFileWritingDownloads();

        DirectoryDownload directoryDownload = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build());

        awaitStarted(downloads, "a.txt");
        downloads.get("a.txt").succeed("new");

        assertThat(directoryDownload.completionFuture().get(5, TimeUnit.SECONDS).failedTransfers()).isEmpty();
        assertThat(Files.isSymbolicLink(directory.resolve("a.txt"))).isFalse();
        assertThat(readString(directory.resolve("a.txt"))).isEqualTo("new");
        assertThat(readString(linkTarget)).isEqualTo("link target");
    }

    @Test
    void downloadDirectory_keyResolvesToDestinationDirectory_shouldReportKeyAndWriteNothing() throws Exception {
        Files.createDirectories(directory);
        stubSuccessfulListObjects(listObjectsHelper, "p/", "p/a.txt");
        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class)))
            .thenAnswer(writingWritePath(completedDownload()));

        CompletedDirectoryDownload completed = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder()
                                    .destination(directory)
                                    .bucket("bucket")
                                    .listObjectsV2RequestTransformer(l -> l.prefix("p/"))
                                    .build()).completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completed.failedTransfers()).hasSize(1);
        FailedFileDownload failed = completed.failedTransfers().iterator().next();
        assertThat(failed.request().getObjectRequest().key()).isEqualTo("p/");
        assertThat(failed.exception()).isInstanceOf(SdkClientException.class)
                                      .hasMessageContaining("resolves to the destination directory itself");
        verify(singleDownloadFunction, times(1)).download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class));
        assertThat(Files.isDirectory(directory)).isTrue();
        assertThat(readString(directory.resolve("a.txt"))).isEmpty();
        assertNoTempFiles(directory.toAbsolutePath().getParent());
    }

    @Test
    void downloadDirectory_cancelledBeforeMove_shouldDeleteTempFileAndNotCreateDestination() throws Exception {
        stubSuccessfulListObjects(listObjectsHelper, "a.txt");
        CompletableFuture<CompletableFuture<CompletedDirectoryDownload>> directoryFuture = new CompletableFuture<>();
        AtomicReference<Path> writtenTempFile = new AtomicReference<>();
        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class)))
            .thenAnswer(invocation -> {
                Path writePath = invocation.getArgument(1);
                Files.createDirectories(writePath.getParent());
                Files.write(writePath, "body".getBytes(StandardCharsets.UTF_8));
                writtenTempFile.set(writePath);
                // The directory download is cancelled after the object is written and before it is moved into place.
                directoryFuture.get(5, TimeUnit.SECONDS).cancel(true);
                try {
                    ((Runnable) invocation.getArgument(2)).run();
                } catch (RuntimeException e) {
                    return newFailedDownload(e);
                }
                return completedDownload();
            });

        DirectoryDownload directoryDownload = downloadDirectoryHelper.downloadDirectory(
            DownloadDirectoryRequest.builder().destination(directory).bucket("bucket").build());
        directoryFuture.complete(directoryDownload.completionFuture());

        assertThatThrownBy(() -> directoryDownload.completionFuture().get(5, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while ((writtenTempFile.get() == null || Files.exists(writtenTempFile.get())) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(writtenTempFile.get()).isNotNull();
        assertThat(Files.exists(writtenTempFile.get())).isFalse();
        assertThat(Files.exists(directory.resolve("a.txt"))).isFalse();
    }

    private Map<String, FileWritingDownload> stubFileWritingDownloads() {
        Map<String, FileWritingDownload> downloads = new ConcurrentHashMap<>();
        when(singleDownloadFunction.download(any(DownloadFileRequest.class), any(Path.class), any(Runnable.class))).thenAnswer(invocation -> {
            DownloadFileRequest request = invocation.getArgument(0);
            FileWritingDownload download = new FileWritingDownload(request, invocation.getArgument(1), invocation.getArgument(2));
            downloads.put(request.getObjectRequest().key(), download);
            return newDownload(download.future);
        });
        return downloads;
    }

    private static void awaitStarted(Map<String, FileWritingDownload> downloads, String... keys) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!downloads.keySet().containsAll(Arrays.asList(keys))) {
            assertThat(System.nanoTime()).as("downloads started: %s", downloads.keySet()).isLessThan(deadline);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
        }
    }

    private static String readString(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void assertNoTempFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
            assertThat(paths.filter(p -> p.getFileName().toString().contains("s3tmp."))).isEmpty();
        }
    }

    private static void assertDestinations(List<DownloadFileRequest> requests, List<String> expectedPaths) {
        assertThat(requests.stream().map(r -> r.destination().toString()).collect(Collectors.toList()))
            .isEqualTo(expectedPaths);
    }

    private static void assertTempFileFor(Path actual, Path destination) {
        assertThat(actual.getParent()).isEqualTo(destination.getParent());
        assertThat(actual.getFileName().toString()).matches(Pattern.quote(destination.getFileName().toString())
                                                            + "\\.s3tmp\\.[0-9a-f]{16}");
    }

    /**
     * Writes to the write path the way the SDK's file writers do: the file is created and truncated when the download starts,
     * the body is written from offset 0 without truncating when it arrives, and a partial file is left in place when the
     * download fails.
     */
    private static final class FileWritingDownload {
        private final DownloadFileRequest request;
        private final Path writePath;
        private final Runnable beforeCompletion;
        private final CompletableFuture<CompletedFileDownload> future = new CompletableFuture<>();

        FileWritingDownload(DownloadFileRequest request, Path writePath, Runnable beforeCompletion) throws IOException {
            this.request = request;
            this.writePath = writePath;
            this.beforeCompletion = beforeCompletion;
            Files.write(writePath, new byte[0]);
        }

        void succeed(String body) throws IOException {
            try (FileChannel channel = FileChannel.open(writePath, StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)), 0);
            }
            try {
                beforeCompletion.run();
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(CompletedFileDownload.builder().response(GetObjectResponse.builder().build()).build());
        }

        void fail(SdkClientException exception) throws IOException {
            try (FileChannel channel = FileChannel.open(writePath, StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap("partial".getBytes(StandardCharsets.UTF_8)), 0);
            }
            future.completeExceptionally(exception);
        }
    }
}
