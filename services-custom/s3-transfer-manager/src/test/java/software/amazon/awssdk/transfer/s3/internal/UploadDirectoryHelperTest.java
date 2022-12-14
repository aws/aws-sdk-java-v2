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
import static org.mockito.Mockito.when;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.transfer.s3.config.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.internal.model.DefaultFileUpload;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

public class UploadDirectoryHelperTest {
    private FileSystem jimfs;
    private Path directory;

    /**
     * Local directory is needed to test symlinks because jimfs doesn't work well with symlinks
     */
    private static Path localDirectory;
    private Function<UploadFileRequest, FileUpload> singleUploadFunction;
    private UploadDirectoryHelper uploadDirectoryHelper;

    public static Collection<FileSystem> fileSystems() {
        return Arrays.asList(Jimfs.newFileSystem(Configuration.unix()),
                             Jimfs.newFileSystem(Configuration.osX()),
                             Jimfs.newFileSystem(Configuration.windows()));
    }

    @BeforeAll
    public static void setUp() throws IOException {
        localDirectory = createLocalTestDirectory();
    }

    @AfterAll
    public static void tearDown() throws IOException {
        FileUtils.cleanUpTestDirectory(localDirectory);
    }

    @BeforeEach
    public void methodSetup() throws IOException {
        jimfs = Jimfs.newFileSystem();
        directory = jimfs.getPath("test");
        Files.createDirectory(directory);
        Files.createFile(jimfs.getPath("test/1"));
        Files.createFile(jimfs.getPath("test/2"));

        singleUploadFunction = mock(Function.class);

        uploadDirectoryHelper = new UploadDirectoryHelper(TransferManagerConfiguration.builder().build(), singleUploadFunction);
    }

    @AfterEach
    public void methodCleanup() throws IOException {
        jimfs.close();
    }

    @Test
    void uploadDirectory_cancel_shouldCancelAllFutures() {
        CompletableFuture<CompletedFileUpload> future = new CompletableFuture<>();
        FileUpload fileUpload = newUpload(future);

        CompletableFuture<CompletedFileUpload> future2 = new CompletableFuture<>();
        FileUpload fileUpload2 = newUpload(future2);

        when(singleUploadFunction.apply(any(UploadFileRequest.class))).thenReturn(fileUpload, fileUpload2);

        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .source(directory)
                                                                        .bucket("bucket")
                                                                        .build());

        uploadDirectory.completionFuture().cancel(true);

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);

        assertThatThrownBy(() -> future2.get(1, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);
    }

    @Test
    void uploadDirectory_allUploadsSucceed_failedUploadsShouldBeEmpty() throws Exception {
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().eTag("1234").build();
        CompletedFileUpload completedFileUpload = CompletedFileUpload.builder().response(putObjectResponse).build();
        CompletableFuture<CompletedFileUpload> successfulFuture = new CompletableFuture<>();

        FileUpload fileUpload = newUpload(successfulFuture);
        successfulFuture.complete(completedFileUpload);

        PutObjectResponse putObjectResponse2 = PutObjectResponse.builder().eTag("5678").build();
        CompletedFileUpload completedFileUpload2 = CompletedFileUpload.builder().response(putObjectResponse2).build();
        CompletableFuture<CompletedFileUpload> successfulFuture2 = new CompletableFuture<>();
        FileUpload fileUpload2 = newUpload(successfulFuture2);
        successfulFuture2.complete(completedFileUpload2);

        when(singleUploadFunction.apply(any(UploadFileRequest.class))).thenReturn(fileUpload, fileUpload2);

        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .source(directory)
                                                                        .bucket("bucket")
                                                                        .build());

        CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completedDirectoryUpload.failedTransfers()).isEmpty();
    }

    @Test
    void uploadDirectory_partialSuccess_shouldProvideFailedUploads() throws Exception {
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
                                                                        .source(directory)
                                                                        .bucket("bucket")
                                                                        .build());

        CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().get(5, TimeUnit.SECONDS);

        assertThat(completedDirectoryUpload.failedTransfers()).hasSize(1);
        assertThat(completedDirectoryUpload.failedTransfers().iterator().next().exception()).isEqualTo(exception);
        assertThat(completedDirectoryUpload.failedTransfers().iterator().next().request().source().toString())
            .isEqualTo("test" + directory.getFileSystem().getSeparator() + "2");
    }

    @Test
    void uploadDirectory_withRequestTransformer_usesRequestTransformer() throws Exception {
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().eTag("1234").build();
        CompletedFileUpload completedFileUpload = CompletedFileUpload.builder().response(putObjectResponse).build();
        CompletableFuture<CompletedFileUpload> successfulFuture = new CompletableFuture<>();

        FileUpload upload = newUpload(successfulFuture);
        successfulFuture.complete(completedFileUpload);

        PutObjectResponse putObjectResponse2 = PutObjectResponse.builder().eTag("5678").build();
        CompletedFileUpload completedFileUpload2 = CompletedFileUpload.builder().response(putObjectResponse2).build();
        CompletableFuture<CompletedFileUpload> successfulFuture2 = new CompletableFuture<>();
        FileUpload upload2 = newUpload(successfulFuture2);
        successfulFuture2.complete(completedFileUpload2);

        ArgumentCaptor<UploadFileRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(uploadRequestCaptor.capture())).thenReturn(upload, upload2);

        Path newSource = Paths.get("/new/path");
        PutObjectRequest newPutObjectRequest = PutObjectRequest.builder().build();
        TransferRequestOverrideConfiguration newOverrideConfig = TransferRequestOverrideConfiguration.builder()
                                                                                                     .build();
        List<TransferListener> listeners = Arrays.asList(LoggingTransferListener.create());

        Consumer<UploadFileRequest.Builder> uploadFileRequestTransformer = r -> r.source(newSource)
                                                                                 .putObjectRequest(newPutObjectRequest)
                                                                                 .transferListeners(listeners);

        uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                    .source(directory)
                                                                    .bucket("bucket")
                                                                    .uploadFileRequestTransformer(uploadFileRequestTransformer)
                                                                    .build())
                             .completionFuture()
                             .get(5, TimeUnit.SECONDS);

        List<UploadFileRequest> uploadRequests = uploadRequestCaptor.getAllValues();
        assertThat(uploadRequests).hasSize(2);
        assertThat(uploadRequests).element(0).satisfies(r -> {
            assertThat(r.source()).isEqualTo(newSource);
            assertThat(r.putObjectRequest()).isEqualTo(newPutObjectRequest);
            assertThat(r.transferListeners()).isEqualTo(listeners);
        });
        assertThat(uploadRequests).element(1).satisfies(r -> {
            assertThat(r.source()).isEqualTo(newSource);
            assertThat(r.putObjectRequest()).isEqualTo(newPutObjectRequest);
            assertThat(r.transferListeners()).isEqualTo(listeners);
        });
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void uploadDirectory_defaultSetting_shouldRecursivelyUpload(FileSystem fileSystem) {
        directory = createJimFsTestDirectory(fileSystem);
        ArgumentCaptor<UploadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture()))
            .thenReturn(completedUpload());
        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .source(directory)
                                                                        .bucket("bucket")
                                                                        .followSymbolicLinks(false)
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));

        assertThat(actualRequests.size()).isEqualTo(3);

        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys).containsOnly("bar.txt", "foo/1.txt", "foo/2.txt");
    }

    @Test
    void uploadDirectory_depth1FollowSymlinkTrue_shouldOnlyUploadTopLevel() {
        ArgumentCaptor<UploadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .source(localDirectory)
                                                                        .bucket("bucket")
                                                                        .maxDepth(1)
                                                                        .followSymbolicLinks(true)
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();
        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys.size()).isEqualTo(2);
        assertThat(keys).containsOnly("bar.txt", "symlink2");
    }

    @Test
    void uploadDirectory_FollowSymlinkTrue_shouldIncludeLinkedFiles() {
        ArgumentCaptor<UploadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .source(localDirectory)
                                                                        .bucket("bucket")
                                                                        .followSymbolicLinks(true)
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));

        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys.size()).isEqualTo(5);
        assertThat(keys).containsOnly("bar.txt", "foo/1.txt", "foo/2.txt", "symlink/2.txt", "symlink2");
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void uploadDirectory_withPrefix_keysShouldHavePrefix(FileSystem fileSystem) {
        directory = createJimFsTestDirectory(fileSystem);
        ArgumentCaptor<UploadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .source(directory)
                                                                        .bucket("bucket")
                                                                        .s3Prefix("yolo")
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<String> keys =
            requestArgumentCaptor.getAllValues().stream().map(u -> u.putObjectRequest().key())
                                 .collect(Collectors.toList());

        assertThat(keys.size()).isEqualTo(3);
        keys.forEach(r -> assertThat(r).startsWith("yolo/"));
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void uploadDirectory_withDelimiter_shouldHonor(FileSystem fileSystem) {
        directory = createJimFsTestDirectory(fileSystem);
        ArgumentCaptor<UploadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .source(directory)
                                                                        .bucket("bucket")
                                                                        .s3Delimiter(",")
                                                                        .s3Prefix("yolo")
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<String> keys =
            requestArgumentCaptor.getAllValues().stream().map(u -> u.putObjectRequest().key())
                                 .collect(Collectors.toList());

        assertThat(keys.size()).isEqualTo(3);
        assertThat(keys).containsOnly("yolo,foo,2.txt", "yolo,foo,1.txt", "yolo,bar.txt");
    }

    @ParameterizedTest
    @MethodSource("fileSystems")
    void uploadDirectory_maxLengthOne_shouldOnlyUploadTopLevel(FileSystem fileSystem) {
        directory = createJimFsTestDirectory(fileSystem);
        ArgumentCaptor<UploadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture()))
            .thenReturn(completedUpload());
        DirectoryUpload uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .source(directory)
                                                                        .bucket("bucket")
                                                                        .maxDepth(1)
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));

        assertThat(actualRequests.size()).isEqualTo(1);

        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys).containsOnly("bar.txt");
    }


    @ParameterizedTest
    @MethodSource("fileSystems")
    void uploadDirectory_directoryNotExist_shouldCompleteFutureExceptionally(FileSystem fileSystem) {
        directory = createJimFsTestDirectory(fileSystem);
        assertThatThrownBy(() -> uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder().source(Paths.get(
                                                                                                 "randomstringneverexistas234ersaf1231"))
                                                                                             .bucket("bucketName").build()).completionFuture().join())
            .hasMessageContaining("does not exist").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadDirectory_notDirectory_shouldCompleteFutureExceptionally() {
        assertThatThrownBy(() -> uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                                             .source(Paths.get(localDirectory.toString(), "symlink"))
                                                                                             .bucket("bucketName").build()).completionFuture().join())
            .hasMessageContaining("is not a directory").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadDirectory_notDirectoryFollowSymlinkTrue_shouldCompleteSuccessfully() {
        ArgumentCaptor<UploadFileRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadFileRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        DirectoryUpload uploadDirectory = uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                                                      .followSymbolicLinks(true)
                                                                                                      .source(Paths.get(localDirectory.toString(), "symlink"))
                                                                                                      .bucket("bucket").build());

        uploadDirectory.completionFuture().join();

        List<UploadFileRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));

        assertThat(actualRequests.size()).isEqualTo(1);

        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys).containsOnly("2.txt");
    }

    private DefaultFileUpload completedUpload() {
        return new DefaultFileUpload(CompletableFuture.completedFuture(CompletedFileUpload.builder()
                                                                                          .response(PutObjectResponse.builder().build())
                                                                                          .build()),
                                     new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder()
                                                                                                .transferredBytes(0L)
                                                                                                .build()),
                                     new S3MetaRequestPauseObservable(),
                                     UploadFileRequest.builder()
                                                      .source(Paths.get(".")).putObjectRequest(b -> b.bucket("bucket").key("key"))
                                                      .build(), S3ClientType.CRT_BASED);
    }

    private FileUpload newUpload(CompletableFuture<CompletedFileUpload> future) {
        return new DefaultFileUpload(future,
                                     new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder()
                                                                                                .transferredBytes(0L)
                                                                                                .build()),
                                     new S3MetaRequestPauseObservable(),
                                     UploadFileRequest.builder()
                                                      .putObjectRequest(p -> p.key("key").bucket("bucket")).source(Paths.get(
                                                          "test.txt"))
                                                      .build(), S3ClientType.CRT_BASED);
    }

    private Path createJimFsTestDirectory(FileSystem fileSystem) {

        try {
            return createJmfsTestDirectory(fileSystem);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

    }

    private static Path createLocalTestDirectory() {

        try {
            return createLocalTestDirectoryWithSymLink();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Create a test directory with the following structure - test1 - foo - 1.txt - 2.txt - bar.txt - symlink -> test2 - symlink2
     * -> test3/4.txt - test2 - 2.txt - test3 - 4.txt
     */
    private static Path createLocalTestDirectoryWithSymLink() throws IOException {
        Path directory = Files.createTempDirectory("test1");
        Path anotherDirectory = Files.createTempDirectory("test2");
        Path thirdDirectory = Files.createTempDirectory("test3");

        String directoryName = directory.toString();
        String anotherDirectoryName = anotherDirectory.toString();

        Files.createDirectory(Paths.get(directory + "/foo"));

        Files.write(Paths.get(directoryName, "bar.txt"), "bar".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "foo/1.txt"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "foo/2.txt"), "2".getBytes(StandardCharsets.UTF_8));

        Files.write(Paths.get(anotherDirectoryName, "2.txt"), "2".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(thirdDirectory.toString(), "3.txt"), "3".getBytes(StandardCharsets.UTF_8));

        Files.createSymbolicLink(Paths.get(directoryName, "symlink"), anotherDirectory);
        Files.createSymbolicLink(Paths.get(directoryName, "symlink2"), Paths.get(thirdDirectory.toString(), "3.txt"));
        return directory;
    }

    /**
     * Create a test directory with the following structure - test1 - foo - 1.txt - 2.txt - bar.txt
     */
    private Path createJmfsTestDirectory(FileSystem jimfs) throws IOException {
        String directoryName = "test";
        Path directory = jimfs.getPath(directoryName);

        Files.createDirectory(directory);

        Files.createDirectory(jimfs.getPath(directoryName + "/foo"));

        Files.write(jimfs.getPath(directoryName, "bar.txt"), "bar".getBytes(StandardCharsets.UTF_8));
        Files.write(jimfs.getPath(directoryName, "foo", "1.txt"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(jimfs.getPath(directoryName, "foo", "2.txt"), "2".getBytes(StandardCharsets.UTF_8));
        return directory;
    }
}
