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

package software.amazon.awssdk.services.marketplacecommerceanalytics;

import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.iam.IAMClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DetachRolePolicyRequest;
import software.amazon.awssdk.services.marketplacecommerceanalytics.model.DataSetType;
import software.amazon.awssdk.services.marketplacecommerceanalytics.model.GenerateDataSetRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.sns.SNSClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {

    private static final String POLICY_NAME = "MarketplaceCommerceAnalyticsPolicy";

    private static final String ROLE_NAME = "MarketplaceCommerceAnalyticsRole";

    private static final String ASSUME_ROLE_POLICY_LOCATION = "assume-role-policy.json";

    private static final String POLICY_DOCUMENT_LOCATION = "policy-document.json";

    private static final String TOPIC_NAME = "marketplace-commerce-analytics-topic";

    private static final String BUCKET_NAME = temporaryBucketName("java-sdk-integ-mp-commerce");

    private MarketplaceCommerceAnalyticsClient client;
    private S3Client s3;
    private SNSClient sns;
    private IAMClient iam;

    private String topicArn;
    private String roleArn;
    private String policyArn;

    @Before
    public void setup() throws Exception {
        setupClients();
        setupResources();
    }

    private void setupClients() {
        s3 = S3Client.builder()
                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                     .region(Region.US_EAST_1)
                     .build();

        client = MarketplaceCommerceAnalyticsClient.builder()
                                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                   .region(Region.US_EAST_1)
                                                   .build();

        sns = SNSClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.US_EAST_1).build();

        iam = IAMClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.AWS_GLOBAL).build();
    }

    private void setupResources() throws IOException, Exception {
        s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        topicArn = sns.createTopic(CreateTopicRequest.builder().name(TOPIC_NAME).build()).topicArn();
        policyArn = createPolicy();
        roleArn = createRole();
        iam.attachRolePolicy(AttachRolePolicyRequest.builder().roleName(ROLE_NAME).policyArn(policyArn).build());
    }

    private String createPolicy() throws IOException {
        CreatePolicyRequest createPolicyRequest = CreatePolicyRequest.builder().policyName(POLICY_NAME)
                                                                     .policyDocument(getPolicyDocument()).build();
        return iam.createPolicy(createPolicyRequest).policy().arn();
    }

    private String createRole() throws Exception {
        CreateRoleRequest createRoleRequest = CreateRoleRequest.builder().roleName(ROLE_NAME)
                                                               .assumeRolePolicyDocument(getAssumeRolePolicy()).build();
        return iam.createRole(createRoleRequest).role().arn();
    }

    @After
    public void tearDown() {
        try {
            iam.detachRolePolicy(DetachRolePolicyRequest.builder().roleName(ROLE_NAME).policyArn(policyArn).build());
            iam.deleteRole(DeleteRoleRequest.builder().roleName(ROLE_NAME).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            iam.deletePolicy(DeletePolicyRequest.builder().policyArn(policyArn).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            sns.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            s3.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(expected = SdkServiceException.class)
    public void test() {
        client.generateDataSet(GenerateDataSetRequest.builder()
                                                     .dataSetPublicationDate(Instant.now())
                                                     .roleNameArn(roleArn).destinationS3BucketName(BUCKET_NAME)
                                                     .snsTopicArn(topicArn)
                                                     .destinationS3BucketName(BUCKET_NAME).destinationS3Prefix("some-prefix")
                                                     .dataSetType(DataSetType.CUSTOMER_SUBSCRIBER_HOURLY_MONTHLY_SUBSCRIPTIONS)
                                                     .build());
    }

    private String getAssumeRolePolicy() throws Exception {
        return getResourceAsString(getClass(), ASSUME_ROLE_POLICY_LOCATION);
    }

    private String getPolicyDocument() throws IOException {
        return getResourceAsString(getClass(), POLICY_DOCUMENT_LOCATION);
    }

}