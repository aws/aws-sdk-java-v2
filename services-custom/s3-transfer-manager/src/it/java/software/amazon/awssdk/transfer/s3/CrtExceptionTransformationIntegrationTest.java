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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.internal.S3CrtAsyncClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

public class CrtExceptionTransformationIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(CrtExceptionTransformationIntegrationTest.class);

    private static final String KEY = "some-key";

    private static final int OBJ_SIZE = 8 * 1024;
    private static RandomTempFile testFile;
    private static S3TransferManager transferManager;
    private static S3CrtAsyncClient s3Crt;

    @BeforeClass
    public static void setupFixture() {
        createBucket(BUCKET);
        s3Crt = S3CrtAsyncClient.builder()
                                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                .region(S3IntegrationTestBase.DEFAULT_REGION)
                                .build();
        transferManager =
            S3TransferManager.builder()
                             .s3ClientConfiguration(b -> b.credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                          .region(S3IntegrationTestBase.DEFAULT_REGION)
                                                          .targetThroughputInGbps(20.0)
                                                          .minimumPartSizeInBytes(1000L))
                             .build();
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
        s3Crt.close();
        transferManager.close();
    }

    @Before
    public void methodSetup() throws IOException {
        testFile = new RandomTempFile(BUCKET, OBJ_SIZE);
    }

    @Test
    public void getObjectNoSuchKey() throws IOException {
        String randomBaseDirectory = Files.createTempDirectory(getClass().getSimpleName()).toString();
        assertThatThrownBy(() -> s3Crt.getObject(GetObjectRequest.builder().bucket(BUCKET).key("randomKey").build(),
                Paths.get(randomBaseDirectory).resolve("testFile")).get())
                .hasCauseInstanceOf(NoSuchKeyException.class)
        .hasMessageContaining("software.amazon.awssdk.services.s3.model.NoSuchKeyException: The specified key does not exist");
    }

    @Test
    public void getObjectNoSuchBucket() throws IOException {
        String randomBaseDirectory = Files.createTempDirectory(getClass().getSimpleName()).toString();
        assertThatThrownBy(() -> s3Crt.getObject(GetObjectRequest.builder().bucket("nonExistingTestBucket").key(KEY).build(),
                Paths.get(randomBaseDirectory).resolve("testFile")).get())
                .hasCauseInstanceOf(NoSuchBucketException.class)
                .hasMessageContaining("software.amazon.awssdk.services.s3.model.NoSuchBucketException: The specified bucket does not exist");
    }

    @Test
    public void transferManagerDownloadObjectWithNoSuchKey() throws IOException {
        String randomBaseDirectory = Files.createTempDirectory(getClass().getSimpleName()).toString();
        assertThatThrownBy(() -> transferManager.download(DownloadRequest.builder()
                .getObjectRequest(GetObjectRequest.builder().bucket(BUCKET).key("randomKey").build())
                .destination(Paths.get(randomBaseDirectory).resolve("testFile"))
                .build()).completionFuture().join())
                .hasCauseInstanceOf(NoSuchKeyException.class)
                .hasMessageContaining("software.amazon.awssdk.services.s3.model.NoSuchKeyException: The specified key does not exist");
    }

    @Test
    public void transferManagerDownloadObjectWithNoSuchBucket() throws IOException {
        String randomBaseDirectory = Files.createTempDirectory(getClass().getSimpleName()).toString();
        assertThatThrownBy(() -> transferManager.download(DownloadRequest.builder()
                .getObjectRequest(GetObjectRequest.builder().bucket("nonExistingTestBucket").key(KEY).build())
                .destination(Paths.get(randomBaseDirectory).resolve("testFile"))
                .build()).completionFuture().join())
                .hasCauseInstanceOf(NoSuchBucketException.class)
                .hasMessageContaining("software.amazon.awssdk.services.s3.model.NoSuchBucketException: The specified bucket does not exist");
    }

    @Test
    public void putObjectNoSuchKey() throws IOException {
        String randomBaseDirectory = Files.createTempDirectory(getClass().getSimpleName()).toString();
        assertThatThrownBy(() -> s3Crt.getObject(GetObjectRequest.builder().bucket(BUCKET).key("someRandomKey").build(),
                Paths.get(randomBaseDirectory).resolve("testFile")).get())
                .hasCauseInstanceOf(NoSuchKeyException.class)
                .hasMessageContaining("software.amazon.awssdk.services.s3.model.NoSuchKeyException: The specified key does not exist");
    }

    @Test
    public void putObjectNoSuchBucket() throws IOException {

        String randomBaseDirectory = Files.createTempDirectory(getClass().getSimpleName()).toString();
        assertThatThrownBy(() -> s3Crt.getObject(GetObjectRequest.builder().bucket("nonExistingTestBucket").key(KEY).build(),
                Paths.get(randomBaseDirectory).resolve("testFile")).get())
                .hasCauseInstanceOf(NoSuchBucketException.class)
                .hasMessageContaining("software.amazon.awssdk.services.s3.model.NoSuchBucketException: The specified bucket does not exist");
    }

    @Test
    public void transferManagerUploadObjectWithNoSuchObject() throws IOException{
        assertThatThrownBy(() -> transferManager.upload(UploadRequest.builder()
                .putObjectRequest(PutObjectRequest.builder().bucket("nonExistingTestBucket").key("someKey").build())
                .source(testFile.toPath())
                .build()).completionFuture().join())
                .hasCauseInstanceOf(NoSuchBucketException.class)
                .hasMessageContaining("software.amazon.awssdk.services.s3.model.NoSuchBucketException: The specified bucket does not exist");
    }
}