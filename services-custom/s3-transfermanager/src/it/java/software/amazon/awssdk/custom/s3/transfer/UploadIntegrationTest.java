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

package software.amazon.awssdk.custom.s3.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.custom.s3.transfer.TransferManagerTestUtils.computeMd5;
import static software.amazon.awssdk.custom.s3.transfer.TransferManagerTestUtils.tryDeleteFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.custom.s3.transfer.util.SizeConstant;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.service.S3BucketUtils;

/**
 * Integration test for TransferManager uploads.
 */
public class UploadIntegrationTest extends S3TransferManagerIntegrationTestBase {
    private static S3TransferManager transferManager;
    private static final String BUCKET = S3BucketUtils.temporaryBucketName(UploadIntegrationTest.class);
    private static final String KEY_8KiB = "8kb_test_file.dat";
    private static final String KEY_16MiB = "16mb_test_file.dat";
    private static Path testFile8KiB;
    private static Path testFile16MiB;
    private static final MessageDigest MD5_DIGEST;
    private static String testFile8KiBDigest;
    private static String testFile16MiBDigest;

    static {
        try {
            MD5_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not instantiate MD5 digest");
        }
    }

    @BeforeClass
    public static void setup() throws Exception {
        S3TransferManagerIntegrationTestBase.setUp();

        transferManager = S3TransferManager.builder()
                                           .s3client(s3Async)
                                           .multipartUploadConfiguration(b -> b.enableMultipartUploads(true)
                                           .multipartUploadThreshold(16 * SizeConstant.MiB - 1))
                                           .build();

        testFile8KiB = new RandomTempFile(8 * SizeConstant.KiB).toPath();
        testFile16MiB = new RandomTempFile(16 * SizeConstant.MiB).toPath();

        testFile8KiBDigest = computeMd5(testFile8KiB, MD5_DIGEST);
        testFile16MiBDigest = computeMd5(testFile16MiB, MD5_DIGEST);

        createBucket(BUCKET);
    }

    @AfterClass
    public static void teardown() {
        deleteBucketAndAllContents(BUCKET);
        tryDeleteFiles(testFile8KiB, testFile16MiB);
    }

    @Test
    public void singlePartUpload_apiRequest() throws IOException {
        transferManager.upload(BUCKET, KEY_8KiB, testFile8KiB).completionFuture().join();
        verify(KEY_8KiB, testFile8KiBDigest);
    }


    @Test
    public void multiPartUpload_apiRequest() throws IOException {
        transferManager.upload(BUCKET, KEY_16MiB, testFile16MiB).completionFuture().join();
        verify(KEY_16MiB, testFile16MiBDigest);
    }

    private static void verify(String key, String expectedMd5) throws IOException {
        Path tempFile = RandomTempFile.randomUncreatedFile().toPath();
        try {
            s3Async.getObject(b -> b.bucket(BUCKET).key(key), AsyncResponseTransformer.toFile(tempFile)).join();
            String downloadedFileMd5 = computeMd5(tempFile, MD5_DIGEST);
            assertThat(downloadedFileMd5).isEqualTo(expectedMd5);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
