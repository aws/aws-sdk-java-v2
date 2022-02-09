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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;
import static software.amazon.awssdk.utils.IoUtils.closeQuietly;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.utils.Logger;

public class S3TransferManagerDownloadDirectoryIntegrationTest extends S3IntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3TransferManagerDownloadDirectoryIntegrationTest.class);
    private static final String TEST_BUCKET = temporaryBucketName(S3TransferManagerUploadIntegrationTest.class);
    private static final String TEST_BUCKET_CUSTOM_DELIMITER = temporaryBucketName("S3TransferManagerUploadIntegrationTest"
                                                                                   + "-delimiter");
    private static final String CUSTOM_DELIMITER = "-";

    private static S3TransferManager tm;
    private static Path sourceDirectory;
    private Path destinationDirectory;

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(TEST_BUCKET);
        createBucket(TEST_BUCKET_CUSTOM_DELIMITER);
        sourceDirectory = createLocalTestDirectory();

        tm = S3TransferManager.builder()
                              .s3ClientConfiguration(b -> b.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                           .region(DEFAULT_REGION)
                                                           .maxConcurrency(100))
                              .build();

        tm.uploadDirectory(u -> u.sourceDirectory(sourceDirectory).bucket(TEST_BUCKET)).completionFuture().join();

        tm.uploadDirectory(u -> u.sourceDirectory(sourceDirectory)
                                 .delimiter(CUSTOM_DELIMITER)
                                 .bucket(TEST_BUCKET_CUSTOM_DELIMITER))
          .completionFuture().join();
    }

    @Before
    public void setUpPerTest() throws IOException {
        destinationDirectory = Files.createTempDirectory("destination");
    }

    @After
    public void cleanup() {
        FileUtils.cleanUpTestDirectory(destinationDirectory);
    }

    @AfterClass
    public static void teardown() {
        try {
            FileUtils.cleanUpTestDirectory(sourceDirectory);
        } catch (Exception exception) {
            log.warn(() -> "Failed to clean up test directory " + sourceDirectory, exception);
        }

        try {
            deleteBucketAndAllContents(TEST_BUCKET);
        } catch (Exception exception) {
            log.warn(() -> "Failed to delete s3 bucket " + TEST_BUCKET, exception);
        }

        try {
            deleteBucketAndAllContents(TEST_BUCKET_CUSTOM_DELIMITER);
        } catch (Exception exception) {
            log.warn(() -> "Failed to delete s3 bucket " + TEST_BUCKET_CUSTOM_DELIMITER, exception);
        }

        closeQuietly(tm, log.logger());
        S3IntegrationTestBase.cleanUp();
    }

    /**
     * The destination directory structure should match with the directory uploaded
     * <pre>
     *   {@code
     *      - destination
     *           - 2021
     *              - 1.txt
     *              - 2.txt
     *           - 2022
     *               - 1.txt
     *           - important.txt
     *   }
     * </pre>
     */
    @Test
    public void downloadDirectory() {
        DirectoryDownload downloadDirectory = tm.downloadDirectory(u -> u.destinationDirectory(destinationDirectory)
                                                                         .bucket(TEST_BUCKET));
        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().join();
        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertTwoDirectoriesHaveSameStructure(sourceDirectory, destinationDirectory);
    }

    /**
     * The destination directory structure should be the following with prefix "notes"
     * <pre>
     *   {@code
     *      - source
     *          - README.md
     *          - CHANGELOG.md
     *          - notes
     *              - 2021
     *                  - 1.txt
     *                  - 2.txt
     *              - 2022
     *                  - 1.txt
     *              - important.txt
     *   }
     * </pre>
     */
    @Test
    public void downloadDirectory_withPrefix() {
        String prefix = "notes";
        DirectoryDownload downloadDirectory = tm.downloadDirectory(u -> u.destinationDirectory(destinationDirectory)
                                                                         .prefix(prefix)
                                                                         .bucket(TEST_BUCKET));
        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().join();
        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();

        assertTwoDirectoriesHaveSameStructure(sourceDirectory.resolve(prefix), destinationDirectory);
    }

    /**
     * The destination directory structure should be the following with prefix "notes"
     * <pre>
     *   {@code
     *      - destination
     *           - 1.txt
     *           - 2.txt
     *   }
     * </pre>
     */
    @Test
    public void downloadDirectory_withPrefixAndDelimiter() {
        String prefix = "notes-2021";
        DirectoryDownload downloadDirectory = tm.downloadDirectory(u -> u.destinationDirectory(destinationDirectory)
                                                                         .delimiter(CUSTOM_DELIMITER)
                                                                         .prefix(prefix)
                                                                         .bucket(TEST_BUCKET_CUSTOM_DELIMITER));
        CompletedDirectoryDownload completedDirectoryDownload = downloadDirectory.completionFuture().join();
        assertThat(completedDirectoryDownload.failedTransfers()).isEmpty();
        assertTwoDirectoriesHaveSameStructure(sourceDirectory.resolve("notes").resolve("2021"), destinationDirectory);
    }

    private static void assertTwoDirectoriesHaveSameStructure(Path path, Path otherPath) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs)
                    throws IOException {
                    FileVisitResult result = super.visitFile(file, attrs);

                    Path relativePath = path.relativize(file);
                    Path otherFile = otherPath.resolve(relativePath);
                    log.debug(() -> String.format("Comparing %s with %s", file, otherFile));
                    assertThat(file).hasSameBinaryContentAs(otherFile);
                    return result;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to compare %s with %s", path, otherPath), e);
        }
    }

    /**
     * Create a test directory with the following structure
     * <pre>
     *   {@code
     *      - source
     *          - README.md
     *          - CHANGELOG.md
     *          - notes
     *              - 2021
     *                  - 1.txt
     *                  - 2.txt
     *              - 2022
     *                  - 1.txt
     *              - important.txt
     *   }
     * </pre>
     */
    private static Path createLocalTestDirectory() throws IOException {
        Path directory = Files.createTempDirectory("source");

        String directoryName = directory.toString();

        Files.createDirectory(Paths.get(directoryName, "notes"));
        Files.createDirectory(Paths.get(directoryName, "notes", "2021"));
        Files.createDirectory(Paths.get(directoryName, "notes", "2022"));
        Files.write(Paths.get(directoryName, "README.md"), RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "CHANGELOG.md"), RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "notes", "2021", "1.txt"),
                    RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "notes", "2021", "2.txt"),
                    RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "notes", "2022", "1.txt"),
                    RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "notes", "important.txt"),
                    RandomStringUtils.random(100).getBytes(StandardCharsets.UTF_8));
        return directory;
    }
}
