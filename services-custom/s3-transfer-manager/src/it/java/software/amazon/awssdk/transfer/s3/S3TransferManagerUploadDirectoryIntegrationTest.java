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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.FileUtils;
import software.amazon.awssdk.utils.Logger;

public class S3TransferManagerUploadDirectoryIntegrationTest extends S3IntegrationTestBase {
    private static final Logger log = Logger.loggerFor(S3TransferManagerUploadDirectoryIntegrationTest.class);
    private static final String TEST_BUCKET = temporaryBucketName(S3TransferManagerUploadIntegrationTest.class);

    private static S3TransferManager tm;
    private static Path directory;
    private static S3Client s3Client;
    private static String randomString;

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(TEST_BUCKET);
        randomString = RandomStringUtils.random(100);
        directory = createLocalTestDirectory();

        tm = S3TransferManager.builder()
                              .s3ClientConfiguration(b -> b.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                           .region(DEFAULT_REGION)
                                                           .maxConcurrency(100))
                              .build();

        s3Client = S3Client.builder()
                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(DEFAULT_REGION)
                           .build();
    }

    @AfterClass
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

        closeQuietly(tm, log.logger());
        closeQuietly(s3Client, log.logger());
        S3IntegrationTestBase.cleanUp();
    }

    @Test
    public void uploadDirectory_filesSentCorrectly() {
        String prefix = "yolo";
        UploadDirectoryTransfer uploadDirectory = tm.uploadDirectory(u -> u.sourceDirectory(directory)
                                                                           .bucket(TEST_BUCKET)
                                                                           .prefix(prefix)
                                                                           .overrideConfiguration(o -> o.recursive(true)));
        CompletedUploadDirectory completedUploadDirectory = uploadDirectory.completionFuture().join();
        assertThat(completedUploadDirectory.failedUploads()).isEmpty();

        List<String> keys =
            s3Client.listObjectsV2Paginator(b -> b.bucket(TEST_BUCKET).prefix(prefix)).contents().stream().map(S3Object::key)
                    .collect(Collectors.toList());

        assertThat(keys).containsOnly(prefix + "/bar.txt", prefix + "/foo/1.txt", prefix + "/foo/2.txt");

        keys.forEach(k -> verifyContent(k, k.substring(prefix.length() + 1) + randomString));
    }

    @Test
    public void uploadDirectory_withDelimiter_filesSentCorrectly() {
        String prefix = "hello";
        String delimiter = "0";
        UploadDirectoryTransfer uploadDirectory = tm.uploadDirectory(u -> u.sourceDirectory(directory)
                                                                           .bucket(TEST_BUCKET)
                                                                           .delimiter(delimiter)
                                                                           .prefix(prefix)
                                                                           .overrideConfiguration(o -> o.recursive(true)));
        CompletedUploadDirectory completedUploadDirectory = uploadDirectory.completionFuture().join();
        assertThat(completedUploadDirectory.failedUploads()).isEmpty();

        List<String> keys =
            s3Client.listObjectsV2Paginator(b -> b.bucket(TEST_BUCKET).prefix(prefix)).contents().stream().map(S3Object::key)
                    .collect(Collectors.toList());

        assertThat(keys).containsOnly(prefix + "0bar.txt", prefix + "0foo01.txt", prefix + "0foo02.txt");
        keys.forEach(k -> {
            String path = k.replace(delimiter, "/");
            verifyContent(k, path.substring(prefix.length() + 1) + randomString);
        });
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

    private static void verifyContent(String key, String expectedContent) {
        String actualContent = s3.getObject(r -> r.bucket(TEST_BUCKET).key(key),
                                      ResponseTransformer.toBytes()).asUtf8String();

        assertThat(actualContent).isEqualTo(expectedContent);
    }
}
