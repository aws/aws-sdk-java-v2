/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, oUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.sts;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Resource;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.Statement.Effect;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyResponse;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysResponse;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesResponse;
import software.amazon.awssdk.services.iam.model.PutUserPolicyRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

@ReviewBeforeRelease("This could be useful to cleanup and present as a customer sample")
@Ignore
public class AssumeRoleIntegrationTest extends IntegrationTestBaseWithIAM {

    private static final int SESSION_DURATION = 60 * 60;
    private static final String ROLE_ARN = "arn:aws:iam::599169622985:role/java-test-role";
    private static final String USER_NAME = "user-" + System.currentTimeMillis();
    private static final String ALL_SECURITY_TOKEN_SERVICE_ACTIONS = "sts:*";
    private static final String ASSUME_ROLE = "sts:AssumeRole";

    @AfterClass
    public static void tearDown() {
        deleteUser(USER_NAME);
    }

    private static void deleteAccessKeysForUser(String userName) {
        ListAccessKeysResponse response = iam.listAccessKeys(ListAccessKeysRequest.builder().userName(userName).build());
        for (AccessKeyMetadata akm : response.accessKeyMetadata()) {
            iam.deleteAccessKey(DeleteAccessKeyRequest.builder().userName(userName).accessKeyId(akm.accessKeyId()).build());
        }
    }

    private static void deleteUserPoliciesForUser(String userName) {
        ListUserPoliciesResponse response = iam.listUserPolicies(ListUserPoliciesRequest.builder().userName(userName).build());
        for (String pName : response.policyNames()) {
            iam.deleteUserPolicy(DeleteUserPolicyRequest.builder().userName(userName).policyName(pName).build());
        }
    }

    private static void deleteUser(String userName) {
        try {
            deleteAccessKeysForUser(userName);
        } catch (Exception e) {
            // Ignore.
        }
        try {
            deleteUserPoliciesForUser(userName);
        } catch (Exception e) {
            // Ignore.
        }
        try {
            iam.deleteLoginProfile(DeleteLoginProfileRequest.builder()
                                                            .userName(userName).build());
        } catch (Exception e) {
            // Ignore.
        }
        try {
            iam.deleteUser(DeleteUserRequest.builder().userName(userName).build());
        } catch (Exception e) {
            // Ignore.
        }
    }

    /** Tests that we can call assumeRole successfully. */
    @Test
    public void testAssumeRole() throws InterruptedException {
        Statement statement = new Statement(Effect.Allow)
                .withActions(new Action(ALL_SECURITY_TOKEN_SERVICE_ACTIONS))
                                   .withResources(new Resource("*"));
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                                                               .durationSeconds(SESSION_DURATION)
                                                               .roleArn(ROLE_ARN)
                                                               .roleSessionName("Name")
                                                               .policy(new Policy().withStatements(statement).toJson()).build();

        STSClient sts = stsClient();

        AssumeRoleResponse assumeRoleResult = sts.assumeRole(assumeRoleRequest);
        assertNotNull(assumeRoleResult.assumedRoleUser());
        assertNotNull(assumeRoleResult.assumedRoleUser().arn());
        assertNotNull(assumeRoleResult.assumedRoleUser().assumedRoleId());
        assertNotNull(assumeRoleResult.credentials());
        assertNotNull(assumeRoleResult.packedPolicySize());
    }

    private STSClient stsClient() {
        iam.createUser(CreateUserRequest.builder().userName(USER_NAME).build());

        String policyDoc = new Policy()
                .withStatements(new Statement(Effect.Allow)
                                        .withActions(new Action(ASSUME_ROLE))
                                        .withResources(new Resource("*")))
                .toJson();

        iam.putUserPolicy(PutUserPolicyRequest.builder().policyDocument(policyDoc)
                                              .userName(USER_NAME).policyName("assume-role").build());
        CreateAccessKeyResponse createAccessKeyResult =
                iam.createAccessKey(CreateAccessKeyRequest.builder().userName(USER_NAME).build());
        AwsCredentials credentials = AwsCredentials.create(createAccessKeyResult.accessKey().accessKeyId(),
                                                        createAccessKeyResult.accessKey().secretAccessKey());
        return STSClient.builder().credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
    }
}
