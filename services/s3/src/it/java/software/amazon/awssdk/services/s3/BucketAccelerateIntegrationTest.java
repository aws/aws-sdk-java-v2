/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.AccelerateConfiguration;
import software.amazon.awssdk.services.s3.model.BucketAccelerateStatus;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.DeleteBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.retry.AssertCallable;
import software.amazon.awssdk.testutils.retry.RetryableAssertion;
import software.amazon.awssdk.testutils.retry.RetryableParams;

/**
 * Integration tests for S3 bucket accelerate configuration.
 */
@ReviewBeforeRelease("These tests are a bit flaky. Looks like S3 returns 307 Temporary Redirect occasionally " +
                     "for a newly accelerated bucket. Not sure what the right fix is without following redirects " +
                     "which we don't want to do for other reasons.")
@Ignore
public class BucketAccelerateIntegrationTest extends S3IntegrationTestBase {

    private static final String US_BUCKET_NAME = temporaryBucketName("s3-accelerate-us-east-1");
    private static final String KEY_NAME = "key";

    private static S3Client accelerateClient;

    @BeforeClass
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();

        accelerateClient = S3Client.builder()
                                   .region(Region.US_WEST_2)
                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                   .advancedConfiguration(S3AdvancedConfiguration.builder()
                                                                                 .accelerateModeEnabled(true)
                                                                                 .build())
                                   .build();

        setUpBuckets();
    }

    @AfterClass
    public static void cleanup() {
        deleteBucketAndAllContents(US_BUCKET_NAME);
    }

    private static void setUpBuckets() {
        createBucket(US_BUCKET_NAME);
    }

    @Test
    public void testControlPlaneOperationsUnderAccelerateMode() throws Exception {
        enableAccelerateOnBucket();

        Tagging tags = Tagging.builder()
                              .tagSet(Tag.builder()
                                         .key("foo")
                                         .value("bar")
                                         .build())
                              .build();

        accelerateClient.putBucketTagging(PutBucketTaggingRequest.builder().bucket(US_BUCKET_NAME).tagging(tags).build());
        accelerateClient.putBucketVersioning(PutBucketVersioningRequest.builder()
                                                                       .bucket(US_BUCKET_NAME)
                                                                       .versioningConfiguration(
                                                                               VersioningConfiguration.builder()
                                                                                                      .status("Enabled")
                                                                                                      .build())
                                                                       .build());

        // Retry a couple of times due to eventual consistency
        RetryableAssertion.doRetryableAssert(new AssertCallable() {
            @Override
            public void doAssert() {
                List<Tag> taggingConfiguration = accelerateClient
                        .getBucketTagging(GetBucketTaggingRequest.builder().bucket(US_BUCKET_NAME).build()).tagSet();

                assertEquals("foo", taggingConfiguration.get(0).key());
                assertEquals("bar", taggingConfiguration.get(0).value());
            }
        }, new RetryableParams().withMaxAttempts(30).withDelayInMs(200));

        assertEquals(BucketVersioningStatus.ENABLED,
                     accelerateClient.getBucketVersioning(GetBucketVersioningRequest.builder()
                                                                                    .bucket(US_BUCKET_NAME)
                                                                                    .build())
                                     .status());

        accelerateClient.deleteBucketTagging(DeleteBucketTaggingRequest.builder().bucket(US_BUCKET_NAME).build());
    }

    @Test
    public void testUpdateAccelerateConfiguration() throws InterruptedException {

        String status = s3.getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest.builder()
                                                                                                   .bucket(US_BUCKET_NAME)
                                                                                                   .build())
                          .statusString();

        if (status == null || !status.equals("Enabled")) {
            enableAccelerateOnBucket();
        }

        assertEquals(
                BucketAccelerateStatus.ENABLED,
                s3.getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest.builder()
                                                                                           .bucket(US_BUCKET_NAME)
                                                                                           .build())
                  .status());

        disableAccelerateOnBucket();
        assertEquals(BucketAccelerateStatus.SUSPENDED,
                     s3.getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest.builder()
                                                                                                .bucket(US_BUCKET_NAME)
                                                                                                .build())
                       .status());
    }

    @Test
    public void testAccelerateEndpoint() throws Exception {

        String status = s3.getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest.builder()
                                                                                                   .bucket(US_BUCKET_NAME)
                                                                                                   .build())
                          .statusString();

        if (status == null || !status.equals("Enabled")) {
            enableAccelerateOnBucket();
        }

        // PutObject
        File uploadFile = new RandomTempFile(KEY_NAME, 1000);
        accelerateClient.putObject(PutObjectRequest.builder()
                                                   .bucket(US_BUCKET_NAME)
                                                   .key(KEY_NAME)
                                                   .build(),
                                   RequestBody.fromFile(uploadFile));
    }

    private void enableAccelerateOnBucket() throws InterruptedException {
        s3.putBucketAccelerateConfiguration(
                PutBucketAccelerateConfigurationRequest.builder()
                                                       .bucket(US_BUCKET_NAME)
                                                       .accelerateConfiguration(AccelerateConfiguration.builder()
                                                                                                       .status(BucketAccelerateStatus.ENABLED)
                                                                                                       .build())
                                                       .build());
        // Wait a bit for accelerate to kick in
        Thread.sleep(1000);
    }

    private void disableAccelerateOnBucket() {
        s3.putBucketAccelerateConfiguration(
                PutBucketAccelerateConfigurationRequest.builder()
                                                       .bucket(US_BUCKET_NAME)
                                                       .accelerateConfiguration(AccelerateConfiguration.builder()
                                                                                                       .status(BucketAccelerateStatus.SUSPENDED)
                                                                                                       .build())
                                                       .build());
    }

    @Test
    public void testUnsupportedOperationsUnderAccelerateMode() {
        try {
            accelerateClient.listBuckets(ListBucketsRequest.builder().build());
        } catch (Exception e) {
            throw e;
            //fail("Exception is not expected!");
        }
    }
}
