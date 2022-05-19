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

package software.amazon.awssdk.services.s3.crt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class CrtExceptionTransformationIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(CrtExceptionTransformationIntegrationTest.class);

    private static final String KEY = "some-key";

    private static final int OBJ_SIZE = 8 * 1024;
    private static RandomTempFile testFile;
    private static S3CrtAsyncClient s3Crt;

    @BeforeAll
    public static void setupFixture() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(BUCKET);
        testFile = new RandomTempFile(BUCKET, OBJ_SIZE);
        s3Crt = S3CrtAsyncClient.builder()
                                .credentialsProvider(AwsTestBase.CREDENTIALS_PROVIDER_CHAIN)
                                .region(S3IntegrationTestBase.DEFAULT_REGION)
                                .build();
    }

    @AfterAll
    public static void tearDownFixture() {
        S3IntegrationTestBase.deleteBucketAndAllContents(BUCKET);
        s3Crt.close();
        testFile.delete();
        CrtResource.waitForNoResources();
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
        assertThatThrownBy(() -> s3Crt.getObject(GetObjectRequest.builder().bucket("nonExistingTestBucket" + UUID.randomUUID()).key(KEY).build(),
                Paths.get(randomBaseDirectory).resolve("testFile")).get())
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
        assertThatThrownBy(() -> s3Crt.getObject(GetObjectRequest.builder().bucket("nonExistingTestBucket" + UUID.randomUUID()).key(KEY).build(),
                Paths.get(randomBaseDirectory).resolve("testFile")).get())
                .hasCauseInstanceOf(NoSuchBucketException.class)
                .hasMessageContaining("software.amazon.awssdk.services.s3.model.NoSuchBucketException: The specified bucket does not exist");
    }
}