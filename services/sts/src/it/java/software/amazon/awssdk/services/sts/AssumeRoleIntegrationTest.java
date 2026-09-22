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

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Principal;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.Statement.Effect;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.iam.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.iam.model.MalformedPolicyDocumentException;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringInputStream;

//TODO This could be useful to cleanup and present as a customer sample
public class AssumeRoleIntegrationTest extends IntegrationTestBaseWithIAM {

    private static final int SESSION_DURATION = 60 * 60;

    private static final String ROLE_NAME = "assume-role-integration-test-role-" + RandomStringUtils.randomAlphanumeric(10);
    private static final String ROLE_ARN_FORMAT = "arn:aws:iam::%s:role/" + ROLE_NAME;
    private static String ROLE_ARN;
    private static String accountId;

    private static final String ASSUME_ROLE = "sts:AssumeRole";

    /** Matches an assumed-role session ARN so the underlying role ARN can be used in a trust policy. */
    private static final Pattern ASSUMED_ROLE_ARN = Pattern.compile("arn:aws:sts::(\\d+):assumed-role/([^/]+)/.*");

    /**
     * The credentials the test itself runs with. These are used as the source credentials for the assume-role chain, so
     * the test does not need to create an IAM user or a long-term access key.
     */
    private static AwsCredentials sourceCredentials;

    @BeforeClass
    public static void setup() {
        accountId = sts.getCallerIdentity().account();
        ROLE_ARN = String.format(ROLE_ARN_FORMAT, accountId);

        sourceCredentials = CREDENTIALS_PROVIDER_CHAIN.resolveCredentials();

        // Try to create a role that can be assumed by the identity running this test, until the eventual consistency
        // catches up.
        try {
            String rolePolicyDoc = new Policy()
                    .withStatements(new Statement(Effect.Allow)
                                            .withPrincipals(new Principal("AWS", callerRoleArn(), false))
                                            .withActions(new Action(ASSUME_ROLE)))
                    .toJson();

            Waiter.run(() -> iam.createRole(r -> r.roleName(ROLE_NAME)
                                                  .assumeRolePolicyDocument(rolePolicyDoc)))
                  .ignoringException(MalformedPolicyDocumentException.class)
                  .orFailAfter(Duration.ofMinutes(4));
        } catch (EntityAlreadyExistsException e) {
            // Role already exists - awesome.
        }

        // Try to assume the role to make sure we won't hit issues during testing.
        Waiter.run(() -> sts.assumeRole(r -> r.durationSeconds(SESSION_DURATION)
                                              .roleArn(ROLE_ARN)
                                              .roleSessionName("Test")))
              .ignoringException(StsException.class)
              .orFailAfter(Duration.ofMinutes(8));
    }

    @AfterClass
    public static void cleanup() {
        iam.deleteRole(req -> req.roleName(ROLE_NAME));
    }

    /**
     * Returns the ARN to name in the role's trust policy. A trust policy cannot reference an assumed-role session ARN, so
     * sessions are mapped back to the ARN of the role that was assumed.
     */
    private static String callerRoleArn() {
        String callerArn = sts.getCallerIdentity().arn();
        Matcher matcher = ASSUMED_ROLE_ARN.matcher(callerArn);
        return matcher.matches() ? String.format("arn:aws:iam::%s:role/%s", matcher.group(1), matcher.group(2))
                                 : callerArn;
    }

    /** The session token of the credentials running this test, or null if they are long-term credentials. */
    private static String sourceSessionToken() {
        return sourceCredentials instanceof AwsSessionCredentials
               ? ((AwsSessionCredentials) sourceCredentials).sessionToken()
               : null;
    }

    @Test
    public void profileCredentialsProviderCanAssumeRoles() throws InterruptedException {
        String sessionToken = sourceSessionToken();
        String ASSUME_ROLE_PROFILE =
            "[source]\n"
            + "aws_access_key_id = " + sourceCredentials.accessKeyId() + "\n"
            + "aws_secret_access_key = " + sourceCredentials.secretAccessKey() + "\n"
            + (sessionToken == null ? "" : "aws_session_token = " + sessionToken + "\n")
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
            new ProfileCredentialsUtils(profiles, profile.get(), profiles::profile).credentialsProvider().get();


        // Try to assume the role until the eventual consistency catches up.
        AwsCredentials awsCredentials = Waiter.run(awsCredentialsProvider::resolveCredentials)
                                              .ignoringException(StsException.class)
                                              .orFail();

        assertThat(awsCredentials.accessKeyId()).isNotBlank();
        assertThat(awsCredentials.secretAccessKey()).isNotBlank();
        assertThat(awsCredentials.accountId()).isPresent();
        ((SdkAutoCloseable) awsCredentialsProvider).close();
    }

    @Test
    public void profileCredentialProviderCanAssumeRolesWithEnvironmentCredentialSource() throws InterruptedException {
        EnvironmentVariableHelper.run(helper -> {
            helper.set("AWS_ACCESS_KEY_ID", sourceCredentials.accessKeyId());
            helper.set("AWS_SECRET_ACCESS_KEY", sourceCredentials.secretAccessKey());
            String sessionToken = sourceSessionToken();
            if (sessionToken == null) {
                helper.remove("AWS_SESSION_TOKEN");
            } else {
                helper.set("AWS_SESSION_TOKEN", sessionToken);
            }

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
                new ProfileCredentialsUtils(profiles, profile.get(), profiles::profile).credentialsProvider().get();


            // Try to assume the role until the eventual consistency catches up.
            AwsCredentials awsCredentials = Waiter.run(awsCredentialsProvider::resolveCredentials)
                                                  .ignoringException(StsException.class)
                                                  .orFail();

            assertThat(awsCredentials.accessKeyId()).isNotBlank();
            assertThat(awsCredentials.secretAccessKey()).isNotBlank();
            assertThat(awsCredentials.accountId()).isPresent();
            ((SdkAutoCloseable) awsCredentialsProvider).close();
        });
    }

    @Test
    public void profileCredentialProviderWithEnvironmentCredentialSourceAndSystemProperties() throws InterruptedException {
        System.setProperty("aws.accessKeyId", sourceCredentials.accessKeyId());
        System.setProperty("aws.secretAccessKey", sourceCredentials.secretAccessKey());
        String sessionToken = sourceSessionToken();
        if (sessionToken != null) {
            System.setProperty("aws.sessionToken", sessionToken);
        }

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
                    new ProfileCredentialsUtils(profiles, profile.get(), profiles::profile).credentialsProvider().get();


                // Try to assume the role until the eventual consistency catches up.
                AwsCredentials awsCredentials = Waiter.run(awsCredentialsProvider::resolveCredentials)
                                                      .ignoringException(StsException.class)
                                                      .orFail();

                assertThat(awsCredentials.accessKeyId()).isNotBlank();
                assertThat(awsCredentials.secretAccessKey()).isNotBlank();
                assertThat(awsCredentials.accountId()).isPresent();
                ((SdkAutoCloseable) awsCredentialsProvider).close();
            });
        } finally {
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
            System.clearProperty("aws.sessionToken");
        }
    }
}
