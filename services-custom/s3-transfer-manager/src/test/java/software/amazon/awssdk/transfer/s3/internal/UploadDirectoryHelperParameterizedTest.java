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
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.util.Set;
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
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.Upload;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.UploadDirectoryTransfer;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgress;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Testing {@link UploadDirectoryHelper} with different file systems.
 */
@RunWith(Parameterized.class)
public class UploadDirectoryHelperParameterizedTest {
    private static final Set<Configuration> FILE_SYSTEMS = Sets.newHashSet(Arrays.asList(Configuration.unix(),
                                                                                         Configuration.osX(),
                                                                                         Configuration.windows(),
                                                                                         Configuration.forCurrentPlatform()));
    private Function<UploadRequest, Upload> singleUploadFunction;
    private UploadDirectoryHelper uploadDirectoryHelper;
    private Path directory;

    @Parameterized.Parameter
    public Configuration configuration;

    private FileSystem jimfs;

    @Parameterized.Parameters
    public static Collection<Configuration> fileSystems() {
        return FILE_SYSTEMS;
    }

    @Before
    public void methodSetup() throws IOException {
        singleUploadFunction = mock(Function.class);

        if (!configuration.equals(Configuration.forCurrentPlatform())) {
            jimfs = Jimfs.newFileSystem(configuration);
            uploadDirectoryHelper = new UploadDirectoryHelper(TransferManagerConfiguration.builder().build(), singleUploadFunction, jimfs);
        } else {
            uploadDirectoryHelper = new UploadDirectoryHelper(TransferManagerConfiguration.builder().build(), singleUploadFunction);
        }
        directory = createTestDirectory();
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
    public void uploadDirectory_defaultSetting_shouldRecursivelyUpload() {
        ArgumentCaptor<UploadRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture()))
            .thenReturn(completedUpload());
        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .overrideConfiguration(o -> o.followSymbolicLinks(false))
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));

        assertThat(actualRequests.size()).isEqualTo(3);

        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys).containsOnly("bar.txt", "foo/1.txt", "foo/2.txt");
    }

    @Test
    public void uploadDirectory_recursiveFalse_shouldOnlyUploadTopLevel() {
        ArgumentCaptor<UploadRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .overrideConfiguration(o -> o.recursive(false))
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadRequest> actualRequests = requestArgumentCaptor.getAllValues();

        assertThat(actualRequests.size()).isEqualTo(1);

        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));
        assertThat(actualRequests.get(0).putObjectRequest().key()).isEqualTo("bar.txt");
    }

    @Test
    public void uploadDirectory_recursiveFalseFollowSymlinkTrue_shouldOnlyUploadTopLevel() {
        // skip the test if we are using jimfs because it doesn't work well with symlink
        assumeTrue(configuration.equals(Configuration.forCurrentPlatform()));
        ArgumentCaptor<UploadRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .overrideConfiguration(o -> o.recursive(false).followSymbolicLinks(true))
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadRequest> actualRequests = requestArgumentCaptor.getAllValues();
        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys.size()).isEqualTo(2);
        assertThat(keys).containsOnly("bar.txt", "symlink2");
    }

    @Test
    public void uploadDirectory_FollowSymlinkTrue_shouldIncludeLinkedFiles()  {
        // skip the test if we are using jimfs because it doesn't work well with symlink
        assumeTrue(configuration.equals(Configuration.forCurrentPlatform()));

        ArgumentCaptor<UploadRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .overrideConfiguration(o -> o.followSymbolicLinks(true))
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));

        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                                 .collect(Collectors.toList());

        assertThat(keys.size()).isEqualTo(5);
        assertThat(keys).containsOnly("bar.txt", "foo/1.txt", "foo/2.txt", "symlink/2.txt", "symlink2");
    }

    @Test
    public void uploadDirectory_withPrefix_keysShouldHavePrefix() {
        ArgumentCaptor<UploadRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .prefix("yolo")
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<String> keys =
            requestArgumentCaptor.getAllValues().stream().map(u -> u.putObjectRequest().key())
                                 .collect(Collectors.toList());

        assertThat(keys.size()).isEqualTo(3);
        keys.forEach(r -> assertThat(r).startsWith("yolo/"));
    }

    @Test
    public void uploadDirectory_withDelimiter_shouldHonor() {
        ArgumentCaptor<UploadRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .delimiter(",")
                                                                        .prefix("yolo")
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<String> keys =
            requestArgumentCaptor.getAllValues().stream().map(u -> u.putObjectRequest().key())
                                 .collect(Collectors.toList());

        assertThat(keys.size()).isEqualTo(3);
        assertThat(keys).containsOnly("yolo,foo,2.txt", "yolo,foo,1.txt", "yolo,bar.txt");
    }

    @Test
    public void uploadDirectory_maxLengthOne_shouldOnlyUploadTopLevel() {
        ArgumentCaptor<UploadRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture()))
            .thenReturn(completedUpload());
        UploadDirectoryTransfer uploadDirectory =
            uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                        .sourceDirectory(directory)
                                                                        .bucket("bucket")
                                                                        .overrideConfiguration(o -> o.maxDepth(1))
                                                                        .build());
        uploadDirectory.completionFuture().join();

        List<UploadRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));

        assertThat(actualRequests.size()).isEqualTo(1);

        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys).containsOnly("bar.txt");
    }


    @Test
    public void uploadDirectory_directoryNotExist_shouldCompleteFutureExceptionally() {
        assertThatThrownBy(() -> uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder().sourceDirectory(Paths.get(
                                                                                                 "randomstringneverexistas234ersaf1231"))
                                                                                             .bucket("bucketName").build()).completionFuture().join())
            .hasMessageContaining("does not exist").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void uploadDirectory_notDirectory_shouldCompleteFutureExceptionally() {
        // skip the test if we are using jimfs because it doesn't work well with symlink
        assumeTrue(configuration.equals(Configuration.forCurrentPlatform()));
        assertThatThrownBy(() -> uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder().sourceDirectory(Paths.get(directory.toString(), "symlink"))
                                                                                             .bucket("bucketName").build()).completionFuture().join())
            .hasMessageContaining("is not a directory").hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void uploadDirectory_notDirectoryFollowSymlinkTrue_shouldCompleteSuccessfully() {
        // skip the test if we are using jimfs because it doesn't work well with symlink
        assumeTrue(configuration.equals(Configuration.forCurrentPlatform()));
        ArgumentCaptor<UploadRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadRequest.class);

        when(singleUploadFunction.apply(requestArgumentCaptor.capture())).thenReturn(completedUpload());
        UploadDirectoryTransfer uploadDirectory = uploadDirectoryHelper.uploadDirectory(UploadDirectoryRequest.builder()
                                                                                                              .overrideConfiguration(o -> o.followSymbolicLinks(true))
                                                                                                              .sourceDirectory(Paths.get(directory.toString(), "symlink"))
                                                                                                              .bucket("bucket").build());

        uploadDirectory.completionFuture().join();

        List<UploadRequest> actualRequests = requestArgumentCaptor.getAllValues();
        actualRequests.forEach(r -> assertThat(r.putObjectRequest().bucket()).isEqualTo("bucket"));

        assertThat(actualRequests.size()).isEqualTo(1);

        List<String> keys =
            actualRequests.stream().map(u -> u.putObjectRequest().key())
                          .collect(Collectors.toList());

        assertThat(keys).containsOnly("2.txt");
    }

    private DefaultUpload completedUpload() {
        return new DefaultUpload(CompletableFuture.completedFuture(CompletedUpload.builder()
                                                                                  .response(PutObjectResponse.builder().build())
                                                                                  .build()),
                                 new DefaultTransferProgress(DefaultTransferProgressSnapshot.builder().build()));
    }

    private Path createTestDirectory() throws IOException {

        if (jimfs != null) {
            return createJmfsTestDirectory();
        }

        return createLocalTestDirectoryWithSymLink();
    }

    /**
     * Create a test directory with the following structure
     * - test1
     *     - foo
     *        - 1.txt
     *        - 2.txt
     *     - bar.txt
     *     - symlink -> test2
     *     - symlink2 -> test3/4.txt
     * - test2
     *     - 2.txt
     * - test3
     *     - 4.txt
     */
    private Path createLocalTestDirectoryWithSymLink() throws IOException {
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
     * Create a test directory with the following structure
     * - test1
     *     - foo
     *        - 1.txt
     *        - 2.txt
     *     - bar.txt
     */
    private Path createJmfsTestDirectory() throws IOException {
        String directoryName = "test";
        Path directory = jimfs.getPath(directoryName);

        Files.createDirectory(directory);

        Files.createDirectory(jimfs.getPath(directoryName + "/foo"));

        Files.write(jimfs.getPath(directoryName, "bar.txt"), "bar".getBytes(StandardCharsets.UTF_8));
        Files.write(jimfs.getPath(directoryName, "foo/1.txt"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(jimfs.getPath(directoryName, "foo/2.txt"), "2".getBytes(StandardCharsets.UTF_8));
        return directory;
    }
}
