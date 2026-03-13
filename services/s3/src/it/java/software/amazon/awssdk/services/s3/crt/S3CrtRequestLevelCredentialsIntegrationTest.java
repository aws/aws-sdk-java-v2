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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Integration tests verifying that request-level credential overrides work correctly
 * with the S3 CRT client.
 */
public class S3CrtRequestLevelCredentialsIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3CrtRequestLevelCredentialsIntegrationTest.class);
    private static final String KEY = "request-level-creds-test-key";
    private static File testFile;

    private static final StaticCredentialsProvider INVALID_CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("invalidAccessKey", "invalidSecretKey"));

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(BUCKET);
        testFile = new RandomTempFile(1024);
        S3IntegrationTestBase.s3.putObject(PutObjectRequest.builder()
                                                           .bucket(BUCKET)
                                                           .key(KEY)
                                                           .build(), testFile.toPath());
    }

    @AfterAll
    public static void cleanup() {
        S3IntegrationTestBase.deleteBucketAndAllContents(BUCKET);
    }

    @Test
    void getObject_withValidRequestLevelCredentials_overridingInvalidClientCredentials_shouldSucceed() {
        // Client is built with INVALID credentials, but the request overrides with VALID ones.
        // If request-level override works, the request should succeed.
        try (S3AsyncClient crtClient = S3CrtAsyncClient.builder()
                                                       .region(DEFAULT_REGION)
                                                       .credentialsProvider(INVALID_CREDENTIALS)
                                                       .build()) {

            byte[] result = crtClient.getObject(
                b -> b.bucket(BUCKET).key(KEY)
                      .overrideConfiguration(o -> o.credentialsProvider(AwsTestBase.CREDENTIALS_PROVIDER_CHAIN)),
                AsyncResponseTransformer.toBytes()).join().asByteArray();

            assertThat(result).hasSize(1024);
        }
    }

    @Test
    void putObject_withValidRequestLevelCredentials_overridingInvalidClientCredentials_shouldSucceed() {
        String overrideKey = KEY + "-put-override";
        try (S3AsyncClient crtClient = S3CrtAsyncClient.builder()
                            
                                                       .region(DEFAULT_REGION)
                                                       .credentialsProvider(INVALID_CREDENTIALS)
                                                       .build()) {

            crtClient.putObject(
                b -> b.bucket(BUCKET).key(overrideKey)
                      .overrideConfiguration(o -> o.credentialsProvider(AwsTestBase.CREDENTIALS_PROVIDER_CHAIN)),
                AsyncRequestBody.fromString("hello")).join();

            byte[] content = S3IntegrationTestBase.s3.getObjectAsBytes(
                b -> b.bucket(BUCKET).key(overrideKey)).asByteArray();
            assertThat(new String(content)).isEqualTo("hello");
        }
    }

    @Test
    void getObject_withInvalidRequestLevelCredentials_overridingValidClientCredentials_shouldFail() {
        // Client is built with VALID credentials, but the request overrides with INVALID ones.
        // The request should fail with a signing/auth error, proving the override is actually used.
        try (S3AsyncClient crtClient = S3CrtAsyncClient.builder()
                                                       .region(DEFAULT_REGION)
                                                       .credentialsProvider(AwsTestBase.CREDENTIALS_PROVIDER_CHAIN)
                                                       .build()) {

            assertThatThrownBy(() -> crtClient.getObject(
                b -> b.bucket(BUCKET).key(KEY)
                      .overrideConfiguration(o -> o.credentialsProvider(INVALID_CREDENTIALS)),
                AsyncResponseTransformer.toBytes()).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(S3Exception.class);
        }
    }

    @Test
    void getObject_withValidClientCredentials_noOverride_shouldSucceed() {
        // Baseline: client-level credentials work when no override is provided.
        try (S3AsyncClient crtClient = S3CrtAsyncClient.builder()
                                                       .region(DEFAULT_REGION)
                                                       .credentialsProvider(AwsTestBase.CREDENTIALS_PROVIDER_CHAIN)
                                                       .build()) {

            byte[] result = crtClient.getObject(
                b -> b.bucket(BUCKET).key(KEY),
                AsyncResponseTransformer.toBytes()).join().asByteArray();

            assertThat(result).hasSize(1024);
        }
    }
}
