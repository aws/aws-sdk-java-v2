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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
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
                                   .serviceConfiguration(S3Configuration.builder()
                                                                        .accelerateModeEnabled(true)
                                                                        .build())
                                   .overrideConfiguration(o -> o.addExecutionInterceptor(new AccelerateValidatingInterceptor()))
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
    public void testUpdateAccelerateConfiguration() throws InterruptedException {

        String status = s3.getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest.builder()
                                                                                                   .bucket(US_BUCKET_NAME)
                                                                                                   .build())
                          .statusAsString();

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
                          .statusAsString();

        if (status == null || !status.equals("Enabled")) {
            enableAccelerateOnBucket();
        }

        // PutObject
        File uploadFile = new RandomTempFile(KEY_NAME, 1000);
        try {
            accelerateClient.putObject(PutObjectRequest.builder()
                                                       .bucket(US_BUCKET_NAME)
                                                       .key(KEY_NAME)
                                                       .build(),
                                       RequestBody.fromFile(uploadFile));
        } catch (Exception e) {
            // We really only need to verify the request is using the accelerate endpoint
        }
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

    private static final class AccelerateValidatingInterceptor implements ExecutionInterceptor {

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        if (!(context.request() instanceof ListBucketsRequest)) {
            assertEquals(context.httpRequest().host(), US_BUCKET_NAME + ".s3-accelerate.amazonaws.com");
        }
        }
    }
}
