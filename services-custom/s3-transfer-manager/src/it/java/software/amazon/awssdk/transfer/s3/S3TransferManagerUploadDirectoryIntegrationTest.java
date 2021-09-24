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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.testutils.FileUtils;

public class S3TransferManagerUploadDirectoryIntegrationTest extends S3IntegrationTestBase {
    private static final String TEST_BUCKET = temporaryBucketName(S3TransferManagerUploadIntegrationTest.class);

    private static S3TransferManager tm;
    private static Path directory;
    private static S3Client s3Client;

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        createBucket(TEST_BUCKET);

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
        tm.close();
        s3Client.close();
        deleteBucketAndAllContents(TEST_BUCKET);
        FileUtils.cleanUpTestDirectory(directory);
        S3IntegrationTestBase.cleanUp();
    }

    @Test
    public void uploadDirectory_filesSentCorrectly() {
        String prefix = "yolo";
        UploadDirectory uploadDirectory = tm.uploadDirectory(u -> u.sourceDirectory(directory)
                                                                   .bucket(TEST_BUCKET)
                                                                   .prefix(prefix)
                                                                   .overrideConfiguration(o -> o.recursive(true)));
        uploadDirectory.completionFuture().join();

        List<String> keys =
            s3Client.listObjectsV2Paginator(b -> b.bucket(TEST_BUCKET).prefix(prefix)).contents().stream().map(S3Object::key)
                    .collect(Collectors.toList());

        assertThat(keys).containsOnly(prefix + "/bar.txt", prefix + "/foo/1.txt", prefix + "/foo/2.txt");
    }

    private static Path createLocalTestDirectory() throws IOException {
        Path directory = Files.createTempDirectory("test");

        String directoryName = directory.toString();

        Files.createDirectory(Paths.get(directory + "/foo"));

        Files.write(Paths.get(directoryName, "bar.txt"), "bar".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "foo/1.txt"), "1".getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(directoryName, "foo/2.txt"), "2".getBytes(StandardCharsets.UTF_8));

        return directory;
    }
}
