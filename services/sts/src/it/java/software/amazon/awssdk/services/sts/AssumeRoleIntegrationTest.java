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

package software.amazon.awssdk.services.sts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
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
import software.amazon.awssdk.services.iam.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.iam.model.MalformedPolicyDocumentException;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringInputStream;

//TODO This could be useful to cleanup and present as a customer sample
public class AssumeRoleIntegrationTest extends IntegrationTestBaseWithIAM {

    private static final int SESSION_DURATION = 60 * 60;

    private static final String USER_NAME = "assume-role-integration-test-user";
    private static final String USER_ARN_FORMAT = "arn:aws:iam::%s:user/" + USER_NAME;
    private static String USER_ARN;

    private static final String POLICY_NAME = "AssumeRoleIntegrationTestPolicy";
    private static final String POLICY_ARN_FORMAT = "arn:aws:iam::%s:policy/" + POLICY_NAME;

    private static final String ROLE_NAME = "assume-role-integration-test-role";
    private static final String ROLE_ARN_FORMAT = "arn:aws:iam::%s:role/" + ROLE_NAME;
    private static String ROLE_ARN;

    private static final String ASSUME_ROLE = "sts:AssumeRole";

    private static AwsCredentials userCredentials;

    @BeforeClass
    public static void setup() {
        String accountId = sts.getCallerIdentity().account();
        USER_ARN = String.format(USER_ARN_FORMAT, accountId);
        ROLE_ARN = String.format(ROLE_ARN_FORMAT, accountId);

        // Create a user
        try {
            iam.createUser(r -> r.userName(USER_NAME));
        } catch (EntityAlreadyExistsException e) {
            // Test user already exists - awesome.
        }

        // Create a managed policy that allows the user to assume a role
        try {
            iam.createPolicy(r -> r.policyName("AssumeRoleIntegrationTestPolicy")
                                   .policyDocument(new Policy().withStatements(new Statement(Effect.Allow)
                                                                                       .withActions(new Action(ASSUME_ROLE))
                                                                                       .withResources(new Resource("*")))
                                                               .toJson()));
        } catch (EntityAlreadyExistsException e) {
            // Policy already exists - awesome.
        }

        // Attach the policy to the user (if it isn't already attached)
        iam.attachUserPolicy(r -> r.userName(USER_NAME).policyArn(String.format(POLICY_ARN_FORMAT, accountId)));

        // Try to create a role that can be assumed by the user, until the eventual consistency catches up.
        try {
            String rolePolicyDoc = new Policy()
                    .withStatements(new Statement(Effect.Allow)
                                            .withPrincipals(new Principal("AWS", USER_ARN, false))
                                            .withActions(new Action(ASSUME_ROLE)))
                    .toJson();

            Waiter.run(() -> iam.createRole(r -> r.roleName(ROLE_NAME)
                                                  .assumeRolePolicyDocument(rolePolicyDoc)))
                  .ignoringException(MalformedPolicyDocumentException.class)
                  .orFailAfter(Duration.ofMinutes(2));
        } catch (EntityAlreadyExistsException e) {
            // Role already exists - awesome.
        }

        // Delete the oldest credentials for the user. We don't want to hit our limit.
                iam.listAccessKeysPaginator(r -> r.userName(USER_NAME))
                   .accessKeyMetadata().stream()
                   .min(Comparator.comparing(AccessKeyMetadata::createDate))
                   .ifPresent(key -> iam.deleteAccessKey(r -> r.userName(USER_NAME).accessKeyId(key.accessKeyId())));

        // Create new credentials for the user
        CreateAccessKeyResponse createAccessKeyResult = iam.createAccessKey(r -> r.userName(USER_NAME));
        userCredentials = AwsBasicCredentials.create(createAccessKeyResult.accessKey().accessKeyId(),
                                                     createAccessKeyResult.accessKey().secretAccessKey());

        // Try to assume the role to make sure we won't hit issues during testing.
        StsClient userCredentialSts = StsClient.builder()
                                               .credentialsProvider(() -> userCredentials)
                                               .build();
        Waiter.run(() -> userCredentialSts.assumeRole(r -> r.durationSeconds(SESSION_DURATION)
                                                            .roleArn(ROLE_ARN)
                                                            .roleSessionName("Test")))
              .ignoringException(StsException.class)
              .orFailAfter(Duration.ofMinutes(5));
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

    @Test
    public void profileCredentialProviderCanAssumeRolesWithEnvironmentCredentialSource() throws InterruptedException {
        EnvironmentVariableHelper.run(helper -> {
            helper.set("AWS_ACCESS_KEY_ID", userCredentials.accessKeyId());
            helper.set("AWS_SECRET_ACCESS_KEY", userCredentials.secretAccessKey());
            helper.remove("AWS_SESSION_TOKEN");

            String ASSUME_ROLE_PROFILE =
                "[test]\n"
                + "region = us-west-1\n"
                + "credential_source = Environment\n"
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
        });
    }

    @Test
    public void profileCredentialProviderWithEnvironmentCredentialSourceAndSystemProperties() throws InterruptedException {
        System.setProperty("aws.accessKeyId", userCredentials.accessKeyId());
        System.setProperty("aws.secretAccessKey", userCredentials.secretAccessKey());

        try {
            EnvironmentVariableHelper.run(helper -> {
                helper.remove("AWS_ACCESS_KEY_ID");
                helper.remove("AWS_SECRET_ACCESS_KEY");
                helper.remove("AWS_SESSION_TOKEN");

                String ASSUME_ROLE_PROFILE =
                    "[test]\n"
                    + "region = us-west-1\n"
                    + "credential_source = Environment\n"
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
            });
        } finally {
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
        }
    }
}
