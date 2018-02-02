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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Resource;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentRequest;
import software.amazon.awssdk.services.s3.model.Payer;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

public final class OperationsWithNonStandardResponsesIntegrationTest extends S3IntegrationTestBase {

    /**
     * The name of the bucket created, used, and deleted by these tests.
     */
    private static String bucketName = temporaryBucketName("single-string-integ-test");

    @BeforeClass
    public static void setupSuite() {
        createBucket(bucketName);
    }

    @AfterClass
    public static void cleanup() {
        s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }


    @Test
    public void getBucketLocationReturnsAResult() {
        GetBucketLocationRequest request = GetBucketLocationRequest.builder().bucket(bucketName).build();
        assertThat(s3.getBucketLocation(request).locationConstraint()).isEqualTo(BucketLocationConstraint.US_WEST_2);
    }


    @Test
    public void getBucketPolicyReturnsAResult() {
        String policy = createPolicy();
        s3.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucketName).policy(policy).build());

        GetBucketPolicyRequest request = GetBucketPolicyRequest.builder().bucket(bucketName).build();
        assertThat(s3.getBucketPolicy(request).policy()).contains("arn:aws:s3:::" + bucketName);
    }

    @Test
    public void getRequestPayerReturnsAResult() {
        GetBucketRequestPaymentRequest request = GetBucketRequestPaymentRequest.builder().bucket(bucketName).build();
        assertThat(s3.getBucketRequestPayment(request).payer()).isEqualTo(Payer.BUCKET_OWNER);
    }

    private String createPolicy() {
        return new Policy().withStatements(
            new Statement(Statement.Effect.Deny)
                .withPrincipals(new Principal("*"))
                .withResources(new Resource("arn:aws:s3:::" + bucketName + "/*"))
                .withActions(new Action("s3:GetObject")))
                           .toJson();
    }
}
