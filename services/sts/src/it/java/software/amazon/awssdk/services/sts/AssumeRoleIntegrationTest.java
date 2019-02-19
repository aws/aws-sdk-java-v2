/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.sts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Resource;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.Statement.Effect;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyResponse;
import software.amazon.awssdk.services.iam.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysResponse;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesResponse;
import software.amazon.awssdk.services.iam.model.MalformedPolicyDocumentException;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringInputStream;

//TODO This could be useful to cleanup and present as a customer sample
public class AssumeRoleIntegrationTest extends IntegrationTestBaseWithIAM {

    private static final int SESSION_DURATION = 60 * 60;

    private static final String USER_NAME = "user-" + System.currentTimeMillis();
    private static final String USER_ARN_FORMAT = "arn:aws:iam::%s:user/%s";
    private static String USER_ARN;

    private static final String ROLE_NAME = "java-test-role-" + System.currentTimeMillis();
    private static final String ROLE_ARN_FORMAT = "arn:aws:iam::%s:role/%s";
    private static String ROLE_ARN;

    private static final String ASSUME_ROLE = "sts:AssumeRole";

    private static AwsCredentials userCredentials;

    @BeforeClass
    public static void setup() {
        String accountId = sts.getCallerIdentity().account();
        USER_ARN = String.format(USER_ARN_FORMAT, accountId, USER_NAME);
        ROLE_ARN = String.format(ROLE_ARN_FORMAT, accountId, ROLE_NAME);

        // Create a user
        iam.createUser(r -> r.userName(USER_NAME));

        // Create credentials for that user
        CreateAccessKeyResponse createAccessKeyResult = iam.createAccessKey(r -> r.userName(USER_NAME));
        userCredentials = AwsBasicCredentials.create(createAccessKeyResult.accessKey().accessKeyId(),
                                                     createAccessKeyResult.accessKey().secretAccessKey());

        // Allow the user to assume roles
        String policyDoc = new Policy()
                .withStatements(new Statement(Effect.Allow)
                                        .withActions(new Action(ASSUME_ROLE))
                                        .withResources(new Resource("*")))
                .toJson();

        iam.putUserPolicy(r -> r.policyDocument(policyDoc)
                                .userName(USER_NAME)
                                .policyName("assume-role"));

        // Create a role that can be assumed by the user
        String rolePolicyDoc = new Policy()
                .withStatements(new Statement(Effect.Allow)
                                        .withPrincipals(new Principal("AWS", USER_ARN, false))
                                        .withActions(new Action(ASSUME_ROLE)))
                .toJson();

        // Try to create the role until the eventual consistency catches up.
        Waiter.run(() -> iam.createRole(r -> r.roleName(ROLE_NAME)
                                              .assumeRolePolicyDocument(rolePolicyDoc)))
              .ignoringException(MalformedPolicyDocumentException.class)
              .orFailAfter(Duration.ofMinutes(2));

        StsClient userCredentialSts = StsClient.builder()
                                               .credentialsProvider(() -> userCredentials)
                                               .build();

        // Try to assume the role until the eventual consistency catches up.
        Waiter.run(() -> userCredentialSts.assumeRole(r -> r.durationSeconds(SESSION_DURATION)
                                                            .roleArn(ROLE_ARN)
                                                            .roleSessionName("Test")))
              .ignoringException(StsException.class)
              .orFailAfter(Duration.ofMinutes(2));
    }

    @AfterClass
    public static void tearDown() {
        deleteUser(USER_NAME);
        deleteRole(ROLE_NAME);
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

    private static void deleteRole(String roleName) {
        try {
            iam.deleteRole(DeleteRoleRequest.builder().roleName(roleName).build());
        } catch (Exception e) {
            // Ignore.
        }
    }

    /** Tests that we can call assumeRole successfully. */
    @Test
    public void testAssumeRole() throws InterruptedException {
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                                                               .durationSeconds(SESSION_DURATION)
                                                               .roleArn(ROLE_ARN)
                                                               .roleSessionName("Name")
                                                               .build();

        StsClient sts = StsClient.builder().credentialsProvider(StaticCredentialsProvider.create(userCredentials)).build();
        AssumeRoleResponse assumeRoleResult = sts.assumeRole(assumeRoleRequest);
        assertNotNull(assumeRoleResult.assumedRoleUser());
        assertNotNull(assumeRoleResult.assumedRoleUser().arn());
        assertNotNull(assumeRoleResult.assumedRoleUser().assumedRoleId());
        assertNotNull(assumeRoleResult.credentials());
    }

    @Test
    public void profileCredentialsProviderCanAssumeRoles() throws InterruptedException {
        String ASSUME_ROLE_PROFILE =
            "[source]\n"
            + "aws_access_key_id = " + userCredentials.accessKeyId() + "\n"
            + "aws_secret_access_key = " + userCredentials.secretAccessKey() + "\n"
            + "\n"
            + "[test]\n"
            + "region = us-west-1\n"
            + "source_profile = source\n"
            + "role_arn = " + ROLE_ARN;

        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(ASSUME_ROLE_PROFILE))
                                          .type(ProfileFile.Type.CREDENTIALS)
                                          .build();
        Optional<Profile> profile = profiles.profile("test");
        AwsCredentialsProvider awsCredentialsProvider =
            new ProfileCredentialsUtils(profile.get(), profiles::profile).credentialsProvider().get();


        // Try to assume the role until the eventual consistency catches up.
        AwsCredentials awsCredentials = Waiter.run(awsCredentialsProvider::resolveCredentials)
                                              .ignoringException(StsException.class)
                                              .orFail();

        assertThat(awsCredentials.accessKeyId()).isNotBlank();
        assertThat(awsCredentials.secretAccessKey()).isNotBlank();
        ((SdkAutoCloseable) awsCredentialsProvider).close();
    }
}
