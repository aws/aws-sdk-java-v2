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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.utils.Logger;

/**
 * Tests the behavior of traversing local directories with special Unicode characters in their path name. These characters have
 * known to be problematic when using Java's old File API or with Windows (which uses UTF-16 for file-name encoding).
 */
@RunWith(Parameterized.class)
public class S3TransferManagerUploadDirectoryUnicodeIntegrationTest extends S3IntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3TransferManagerUploadDirectoryUnicodeIntegrationTest.class);
    private static final String TEST_BUCKET = temporaryBucketName(S3TransferManagerUploadDirectoryUnicodeIntegrationTest.class);

    private static S3TransferManager tm;
    private static S3Client s3Client;

    private final String directoryPrefix;
    private Path testDirectory;

    @Parameterized.Parameters(name = "Directory Prefix: {0}")
    public static Collection<Object> data() {
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

    public S3TransferManagerUploadDirectoryUnicodeIntegrationTest(String directoryPrefix) {
        this.directoryPrefix = directoryPrefix;
        log.info(() -> "Testing directory prefix: " + directoryPrefix);
        log.info(() -> "UTF-8 bytes: " + Hex.encodeHexString(directoryPrefix.getBytes(StandardCharsets.UTF_8)));
        log.info(() -> "UTF-16 bytes: " + Hex.encodeHexString(directoryPrefix.getBytes(StandardCharsets.UTF_16)));
    }

    @BeforeClass
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(TEST_BUCKET);
        tm = S3TransferManager.builder()
                              .s3AsyncClient(S3CrtAsyncClient.builder()
                                                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                             .region(DEFAULT_REGION)
                                                             .build())
                              .build();
        s3Client = S3Client.builder()
                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(DEFAULT_REGION)
                           .build();
    }

    @Before
    public void testSetup() throws Exception {
        testDirectory = createLocalTestDirectory();
    }

    @After
    public void testTeardown() {
        try {
            FileUtils.cleanUpTestDirectory(testDirectory);
        } catch (Exception exception) {
            log.warn(() -> "Failed to clean up test directory " + testDirectory, exception);
        }
    }

    @AfterClass
    public static void teardown() {
        try {
            deleteBucketAndAllContents(TEST_BUCKET);
        } catch (Exception exception) {
            log.warn(() -> "Failed to delete s3 bucket " + TEST_BUCKET, exception);
        }
        closeQuietly(tm, log.logger());
        closeQuietly(s3Client, log.logger());
        S3IntegrationTestBase.cleanUp();
    }

    @Test
    public void uploadDirectory_traversedCorrectly() {
        DirectoryUpload uploadDirectory = tm.uploadDirectory(u -> u.sourceDirectory(testDirectory)
                                                                   .bucket(TEST_BUCKET)
                                                                   .overrideConfiguration(o -> o.recursive(true)));
        CompletedDirectoryUpload completedDirectoryUpload = uploadDirectory.completionFuture().join();
        assertThat(completedDirectoryUpload.failedTransfers()).isEmpty();

        List<String> keys = s3Client.listObjectsV2Paginator(b -> b.bucket(TEST_BUCKET))
                                    .contents()
                                    .stream()
                                    .map(S3Object::key)
                                    .collect(Collectors.toList());

        assertThat(keys).containsOnly("bar.txt", "foo/1.txt", "foo/2.txt");

        keys.forEach(this::verifyContent);
    }


    private Path createLocalTestDirectory() throws IOException {
        Path dir = Files.createTempDirectory(directoryPrefix);
        Files.createDirectory(Paths.get(dir + "/foo"));
        Files.write(dir.resolve("bar.txt"), randomBytes(1024));
        Files.write(dir.resolve("foo/1.txt"), randomBytes(1024));
        Files.write(dir.resolve("foo/2.txt"), randomBytes(1024));
        return dir;
    }

    private void verifyContent(String key) {
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
