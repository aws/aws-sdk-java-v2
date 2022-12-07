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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.utils.Logger;

public class S3TransferManagerUploadDirectoryIntegrationTest extends S3IntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3TransferManagerUploadDirectoryIntegrationTest.class);
    private static final String TEST_BUCKET = temporaryBucketName(S3TransferManagerUploadDirectoryIntegrationTest.class);

    private static Path directory;
    private static String randomString;

    @BeforeAll
    public static void setUp() throws Exception {
        createBucket(TEST_BUCKET);
        randomString = RandomStringUtils.random(100);
        directory = createLocalTestDirectory();
    }

    @AfterAll
    public static void teardown() {
        try {
            FileUtils.cleanUpTestDirectory(directory);
        } catch (Exception exception) {
            log.warn(() -> "Failed to clean up test directory " + directory, exception);
        }

        try {
            deleteBucketAndAllContents(TEST_BUCKET);
        } catch (Exception exception) {
            log.warn(() -> "Failed to delete s3 bucket " + TEST_BUCKET, exception);
        }
    }

    @Test
    void uploadDirectory_filesSentCorrectly() {
        String prefix = "yolo";
        DirectoryUpload uploadDirectory = tm.uploadDirectory(u -> u.source(directory)
                                                                   .bucket(TEST_BUCKET)
                                                                   .s3Prefix(prefix));
        CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().join();
        assertThat(completedDirectoryUpload.failedTransfers()).isEmpty();

        List<String> keys =
            s3.listObjectsV2Paginator(b -> b.bucket(TEST_BUCKET).prefix(prefix)).contents().stream().map(S3Object::key)
                    .collect(Collectors.toList());

        assertThat(keys).containsOnly(prefix + "/bar.txt", prefix + "/foo/1.txt", prefix + "/foo/2.txt");

        keys.forEach(k -> verifyContent(k, k.substring(prefix.length() + 1) + randomString));
    }

    @Test
    void uploadDirectory_nonExistsBucket_shouldAddFailedRequest() {
        String prefix = "yolo";
        DirectoryUpload uploadDirectory = tm.uploadDirectory(u -> u.source(directory)
                                                                   .bucket("nonExistingTestBucket" + UUID.randomUUID())
                                                                   .s3Prefix(prefix));
        CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().join();
        assertThat(completedDirectoryUpload.failedTransfers()).hasSize(3).allSatisfy(f ->
            assertThat(f.exception()).isInstanceOf(NoSuchBucketException.class));
    }

    @Test
    void uploadDirectory_withDelimiter_filesSentCorrectly() {
        String prefix = "hello";
        String delimiter = "0";
        DirectoryUpload uploadDirectory = tm.uploadDirectory(u -> u.source(directory)
                                                                          .bucket(TEST_BUCKET)
                                                                          .s3Delimiter(delimiter)
                                                                          .s3Prefix(prefix));
        CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().join();
        assertThat(completedDirectoryUpload.failedTransfers()).isEmpty();

        List<String> keys =
            s3.listObjectsV2Paginator(b -> b.bucket(TEST_BUCKET).prefix(prefix)).contents().stream().map(S3Object::key)
                    .collect(Collectors.toList());

        assertThat(keys).containsOnly(prefix + "0bar.txt", prefix + "0foo01.txt", prefix + "0foo02.txt");
        keys.forEach(k -> {
            String path = k.replace(delimiter, "/");
            verifyContent(k, path.substring(prefix.length() + 1) + randomString);
        });
    }

    @Test
    void uploadDirectory_withRequestTransformer_usesRequestTransformer() throws Exception {
        String prefix = "requestTransformerTest";
        Path newSourceForEachUpload = Paths.get(directory.toString(), "bar.txt");

        CompletedDirectoryUpload result =
            tm.uploadDirectory(r -> r.source(directory)
                                     .bucket(TEST_BUCKET)
                                     .s3Prefix(prefix)
                                     .uploadFileRequestTransformer(f -> f.source(newSourceForEachUpload)))
              .completionFuture()
              .get(10, TimeUnit.SECONDS);
        assertThat(result.failedTransfers()).isEmpty();

        s3.listObjectsV2Paginator(b -> b.bucket(TEST_BUCKET).prefix(prefix)).contents().forEach(object -> {
            verifyContent(object.key(), "bar.txt" + randomString);
        });
    }

    public static Collection<String> prefix() {
        return Arrays.asList(
            /* ASCII, 1-byte UTF-8 */
            "E",
            /* ASCII, 2-byte UTF-8 */
            "É",
            /* Non-ASCII, 2-byte UTF-8 */
            "Ũ",
            /* Non-ASCII, 3-byte UTF-8 */
            "स",
            /* Non-ASCII, 4-byte UTF-8 */
            "\uD808\uDC8C"
        );
    }

    /**
     * Tests the behavior of traversing local directories with special Unicode characters in their path name. These characters have
     * known to be problematic when using Java's old File API or with Windows (which uses UTF-16 for file-name encoding).
     */
    @ParameterizedTest
    @MethodSource("prefix")
    void uploadDirectory_fileNameWithUnicode_traversedCorrectly(String directoryPrefix) throws IOException {
        assumeTrue(Charset.defaultCharset().equals(StandardCharsets.UTF_8), "Ignoring the test if the test directory can't be "
                                                                            + "created");
        Path testDirectory = null;
        try {
            log.info(() -> "Testing directory prefix: " + directoryPrefix);
            log.info(() -> "UTF-8 bytes: " + Hex.encodeHexString(directoryPrefix.getBytes(StandardCharsets.UTF_8)));
            log.info(() -> "UTF-16 bytes: " + Hex.encodeHexString(directoryPrefix.getBytes(StandardCharsets.UTF_16)));
            testDirectory = createLocalTestDirectory(directoryPrefix);

            Path finalTestDirectory = testDirectory;
            DirectoryUpload uploadDirectory = tm.uploadDirectory(u -> u.source(finalTestDirectory)
                                                                       .bucket(TEST_BUCKET));
            CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().join();
            assertThat(completedDirectoryUpload.failedTransfers()).isEmpty();

            List<String> keys = s3.listObjectsV2Paginator(b -> b.bucket(TEST_BUCKET))
                                  .contents()
                                  .stream()
                                  .map(S3Object::key)
                                  .collect(Collectors.toList());

            assertThat(keys).containsOnly("bar.txt", "foo/1.txt", "foo/2.txt");

            keys.forEach(k -> verifyContent(finalTestDirectory, k));

        } finally {
            FileUtils.cleanUpTestDirectory(testDirectory);
        }

    }

    private static Path createLocalTestDirectory() throws IOException {
        Path directory = Files.createTempDirectory("test");

        String directoryName = directory.toString();

        Files.createDirectory(Paths.get(directory + "/foo"));
        Files.write(Paths.get(directoryName, "bar.txt"), ("bar.txt" + randomString).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "foo/1.txt"), ("foo/1.txt" + randomString).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "foo/2.txt"), ("foo/2.txt" + randomString).getBytes(StandardCharsets.UTF_8));

        return directory;
    }

    private Path createLocalTestDirectory(String directoryPrefix) throws IOException {
        Path dir = Files.createTempDirectory(directoryPrefix);
        Files.createDirectory(Paths.get(dir + "/foo"));
        Files.write(dir.resolve("bar.txt"), randomBytes(1024));
        Files.write(dir.resolve("foo/1.txt"), randomBytes(1024));
        Files.write(dir.resolve("foo/2.txt"), randomBytes(1024));
        return dir;
    }

    private static void verifyContent(String key, String expectedContent) {
        String actualContent = s3.getObject(r -> r.bucket(TEST_BUCKET).key(key),
                                      ResponseTransformer.toBytes()).asUtf8String();

        assertThat(actualContent).isEqualTo(expectedContent);
    }

    private void verifyContent(Path testDirectory, String key) {
        try {
            byte[] expectedContent = Files.readAllBytes(testDirectory.resolve(key));
            byte[] actualContent = s3.getObject(r -> r.bucket(TEST_BUCKET).key(key), ResponseTransformer.toBytes()).asByteArray();
            assertThat(actualContent).isEqualTo(expectedContent);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] randomBytes(long size) {
        byte[] bytes = new byte[Math.toIntExact(size)];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}
