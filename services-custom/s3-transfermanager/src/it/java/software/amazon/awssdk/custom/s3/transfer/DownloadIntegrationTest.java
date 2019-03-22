/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.custom.s3.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.custom.s3.transfer.util.SizeConstant;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.service.S3BucketUtils;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Integration test for TransferManager downloads.
 */
public class DownloadIntegrationTest extends S3TransferManagerIntegrationTestBase {
    private static final String BUCKET = S3BucketUtils.temporaryBucketName(DownloadIntegrationTest.class);
    private static final String KEY_8KiB = "8kb_test_file.dat";
    private static final String KEY_16MiB = "16mb_test_file.dat";
    private static final Path TMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"));
    private static final MessageDigest MD5_DIGEST;

    static {
        try {
            MD5_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not instantiate MD5 digest");
        }
    }

    private static S3TransferManager transferManager;

    private static Path testFile8KiB;
    private static Path testFile16MiB;

    private static String testFile8KiBDigest;
    private static String testFile16MiBDigest;

    @BeforeClass
    public static void setup() throws Exception {
        S3TransferManagerIntegrationTestBase.setUp();

        transferManager = S3TransferManager.builder()
                .s3client(s3Async)
                .multipartDownloadConfiguration(MultipartDownloadConfiguration.defaultConfig()
                        .toBuilder()
                        .multipartDownloadThreshold(16 * SizeConstant.MiB - 1)
                        .build())
                .build();

        testFile8KiB = new RandomTempFile(8 * SizeConstant.KiB).toPath();
        testFile16MiB = new RandomTempFile(16 * SizeConstant.MiB).toPath();

        testFile8KiBDigest = computeMd5(testFile8KiB);
        testFile16MiBDigest = computeMd5(testFile16MiB);

        createBucket(BUCKET);
        putFile(KEY_8KiB, testFile8KiB);
        putFile(KEY_16MiB, testFile16MiB);
    }

    @AfterClass
    public static void teardown() {
        deleteBucketAndAllContents(BUCKET);
        tryDeleteFiles(testFile8KiB, testFile16MiB);
    }

    @Test
    public void singlePartDownload() throws IOException {
        downloadTest(KEY_8KiB, testFile8KiBDigest);
    }

    @Test
    public void multipartDownload() throws IOException {
        downloadTest(KEY_16MiB, testFile16MiBDigest);
    }

    private static void putFile(String key, Path file) {
        s3.putObject(r -> r.bucket(BUCKET).key(key), file);
    }

    private static void downloadTest(String key, String expectedMd5) throws IOException {
        Path tempFile = createTempPath();
        try {
            transferManager.download(BUCKET, key, tempFile).completionFuture().join();
            String downloadedFileMd5 = computeMd5(tempFile);
            assertThat(downloadedFileMd5).isEqualTo(expectedMd5);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static Path createTempPath() {
        return TMP_DIR.resolve(DownloadIntegrationTest.class.getSimpleName() + "-" + System.currentTimeMillis());
    }

    private static String computeMd5(Path file) {
        try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
            MD5_DIGEST.reset();
            byte[] buff = new byte[4096];
            int read;
            while ((read = is.read(buff)) != -1) {
                MD5_DIGEST.update(buff, 0, read);
            }
            return BinaryUtils.toBase64(MD5_DIGEST.digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void tryDeleteFiles(Path... files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                System.err.println("Could not delete file " + file);
            }
        }
    }
}
