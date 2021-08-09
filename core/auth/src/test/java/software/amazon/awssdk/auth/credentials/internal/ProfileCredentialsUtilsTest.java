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

package software.amazon.awssdk.auth.credentials.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProcessCredentialsProviderTest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.StringInputStream;

public class ProfileCredentialsUtilsTest {
    private static String scriptLocation;

    @BeforeClass
    public static void setup()  {
        scriptLocation = ProcessCredentialsProviderTest.copyProcessCredentialsScript();
    }

    @AfterClass
    public static void teardown() {
        if (scriptLocation != null && !new File(scriptLocation).delete()) {
            throw new IllegalStateException("Failed to delete file: " + scriptLocation);
        }
    }

    @Test
    public void roleProfileCanInheritFromAnotherFile() {
        String sourceProperties =
            "aws_access_key_id=defaultAccessKey\n" +
            "aws_secret_access_key=defaultSecretAccessKey";

        String childProperties =
            "source_profile=source\n" +
            "role_arn=arn:aws:iam::123456789012:role/testRole";

        String configSource = "[profile source]\n" + sourceProperties;
        String credentialsSource = "[source]\n" + sourceProperties;
        String configChild = "[profile child]\n" + childProperties;
        String credentialsChild = "[child]\n" + childProperties;

        ProfileFile sourceProfile = aggregateFileProfiles(configSource, credentialsChild);
        ProfileFile configProfile = aggregateFileProfiles(configChild, credentialsSource);

        Consumer<ProfileFile> profileValidator = profileFile ->
            Assertions.assertThatThrownBy(new ProfileCredentialsUtils(profileFile.profiles().get("child"),
                                                                      profileFile::profile)::credentialsProvider)
                      .hasMessageContaining("the 'sts' service module must be on the class path");

        Assertions.assertThat(sourceProfile).satisfies(profileValidator);
        Assertions.assertThat(configProfile).satisfies(profileValidator);
    }

    @Test
    public void roleProfileWithMissingSourceThrowsException() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "source_profile=source\n" +
                                             "role_arn=arn:aws:iam::123456789012:role/testRole");
        Assertions.assertThatThrownBy(new ProfileCredentialsUtils(profileFile.profile("test")
                                                                             .get(), profileFile::profile)::credentialsProvider)
                  .hasMessageContaining("source profile has no credentials configured.");
    }

    @Test
    public void roleProfileWithSourceThatHasNoCredentialsThrowsExceptionWhenLoadingCredentials() {
        ProfileFile profiles = configFile("[profile source]\n" +
                                          "[profile test]\n" +
                                          "source_profile=source\n" +
                                          "role_arn=arn:aws:iam::123456789012:role/testRole");

        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            ProfileCredentialsUtils profileCredentialsUtils = new ProfileCredentialsUtils(profile, profiles::profile);
            Assertions.assertThatThrownBy(profileCredentialsUtils::credentialsProvider)
                      .hasMessageContaining("source profile has no credentials configured");
        });
    }

    @Test
    public void profileFileWithRegionLoadsCorrectly() {
        assertThat(allTypesProfile().profile("profile-with-region")).hasValueSatisfying(profile -> {
            assertThat(profile.property(ProfileProperty.REGION)).hasValue("us-east-1");
        });
    }

    @Test
    public void profileFileWithStaticCredentialsLoadsCorrectly() {
        ProfileFile profileFile = allTypesProfile();
        assertThat(profileFile.profile("default")).hasValueSatisfying(profile -> {
            assertThat(profile.name()).isEqualTo("default");
            assertThat(profile.property(ProfileProperty.AWS_ACCESS_KEY_ID)).hasValue("defaultAccessKey");
            assertThat(profile.toString()).contains("default");
            assertThat(profile.property(ProfileProperty.REGION)).isNotPresent();
            assertThat(new ProfileCredentialsUtils(profile, profileFile::profile).credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider.resolveCredentials()).satisfies(credentials -> {
                    assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
                    assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
                });
            });
        });
    }

    @Test
    public void profileFileWithSessionCredentialsLoadsCorrectly() {
        ProfileFile profileFile = allTypesProfile();
        assertThat(profileFile.profile("profile-with-session-token")).hasValueSatisfying(profile -> {
            assertThat(profile.property(ProfileProperty.REGION)).isNotPresent();
            assertThat(new ProfileCredentialsUtils(profile, profileFile::profile).credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider.resolveCredentials()).satisfies(credentials -> {
                    assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
                    assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
                    assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
                    Assertions.assertThat(((AwsSessionCredentials) credentials).sessionToken()).isEqualTo("awsSessionToken");
                });
            });
        });
    }

    @Test
    public void profileFileWithProcessCredentialsLoadsCorrectly() {
        ProfileFile profileFile = allTypesProfile();
        assertThat(profileFile.profile("profile-credential-process")).hasValueSatisfying(profile -> {
            assertThat(profile.property(ProfileProperty.REGION)).isNotPresent();
            assertThat(new ProfileCredentialsUtils(profile, profileFile::profile).credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider.resolveCredentials()).satisfies(credentials -> {
                    assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
                    assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
                    assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
                });
            });
        });
    }

    @Test
    public void profileFileWithAssumeRoleThrowsExceptionWhenRetrievingCredentialsProvider() {
        ProfileFile profileFile = allTypesProfile();
        assertThat(profileFile.profile("profile-with-assume-role")).hasValueSatisfying(profile -> {
            assertThat(profile.property(ProfileProperty.REGION)).isNotPresent();

            ProfileCredentialsUtils profileCredentialsUtils = new ProfileCredentialsUtils(profile, profileFile::profile);
            Assertions.assertThatThrownBy(profileCredentialsUtils::credentialsProvider).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    public void profileFileWithCredentialSourceThrowsExceptionWhenRetrievingCredentialsProvider() {
        ProfileFile profileFile = allTypesProfile();
        List<String> profiles = Arrays.asList(
            "profile-with-container-credential-source",
            "profile-with-instance-credential-source",
            "profile-with-environment-credential-source"
        );
        profiles.forEach(profileName -> {
            assertThat(profileFile.profile(profileName)).hasValueSatisfying(profile -> {
                assertThat(profile.property(ProfileProperty.REGION)).isNotPresent();

                ProfileCredentialsUtils profileCredentialsUtils = new ProfileCredentialsUtils(profile, profileFile::profile);
                Assertions.assertThatThrownBy(profileCredentialsUtils::credentialsProvider).isInstanceOf(IllegalStateException.class);
            });
        });
    }

    @Test
    public void profileFileWithCircularDependencyThrowsExceptionWhenResolvingCredentials() {
        ProfileFile configFile = configFile("[profile source]\n" +
                                       "aws_access_key_id=defaultAccessKey\n" +
                                       "aws_secret_access_key=defaultSecretAccessKey\n" +
                                       "\n" +
                                       "[profile test]\n" +
                                       "source_profile=test3\n" +
                                       "role_arn=arn:aws:iam::123456789012:role/testRole\n" +
                                       "\n" +
                                       "[profile test2]\n" +
                                       "source_profile=test\n" +
                                       "role_arn=arn:aws:iam::123456789012:role/testRole2\n" +
                                       "\n" +
                                       "[profile test3]\n" +
                                       "source_profile=test2\n" +
                                       "role_arn=arn:aws:iam::123456789012:role/testRole3");
        Assertions.assertThatThrownBy(() -> new ProfileCredentialsUtils(configFile.profile("test").get(), configFile::profile)
            .credentialsProvider())
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("Invalid profile file: Circular relationship detected with profiles");
    }

    @Test
    public void profileWithBothCredentialSourceAndSourceProfileThrowsException() {
        ProfileFile configFile = configFile("[profile test]\n" +
                                            "source_profile=source\n" +
                                            "credential_source=Environment\n" +
                                            "role_arn=arn:aws:iam::123456789012:role/testRole3\n" +
                                            "\n" +
                                            "[profile source]\n" +
                                            "aws_access_key_id=defaultAccessKey\n" +
                                            "aws_secret_access_key=defaultSecretAccessKey");
        Assertions.assertThatThrownBy(() -> new ProfileCredentialsUtils(configFile.profile("test").get(), configFile::profile)
            .credentialsProvider())
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("Invalid profile file: profile has both source_profile and credential_source.");
    }

    @Test
    public void profileWithInvalidCredentialSourceThrowsException() {
        ProfileFile configFile = configFile("[profile test]\n" +
                                            "credential_source=foobar\n" +
                                            "role_arn=arn:aws:iam::123456789012:role/testRole3");
        Assertions.assertThatThrownBy(() -> new ProfileCredentialsUtils(configFile.profile("test").get(), configFile::profile)
            .credentialsProvider())
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("foobar is not a valid credential_source");
    }

    private ProfileFile credentialFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CREDENTIALS)
                          .build();
    }

    private ProfileFile aggregateFileProfiles(String configFile, String credentialFile) {
        return ProfileFile.aggregator()
                          .addFile(credentialFile(credentialFile))
                          .addFile(configFile(configFile))
                          .build();
    }

    private ProfileFile allTypesProfile() {
        return configFile("[default]\n" +
                          "aws_access_key_id = defaultAccessKey\n" +
                          "aws_secret_access_key = defaultSecretAccessKey\n" +
                          "\n" +
                          "[profile profile-with-session-token]\n" +
                          "aws_access_key_id = defaultAccessKey\n" +
                          "aws_secret_access_key = defaultSecretAccessKey\n" +
                          "aws_session_token = awsSessionToken\n" +
                          "\n" +
                          "[profile profile-with-region]\n" +
                          "region = us-east-1\n" +
                          "\n" +
                          "[profile profile-with-assume-role]\n" +
                          "source_profile=default\n" +
                          "role_arn=arn:aws:iam::123456789012:role/testRole\n" +
                          "\n" +
                          "[profile profile-credential-process]\n" +
                          "credential_process=" + scriptLocation +" defaultAccessKey defaultSecretAccessKey\n" +
                          "\n" +
                          "[profile profile-with-container-credential-source]\n" +
                          "credential_source=ecscontainer\n" +
                          "role_arn=arn:aws:iam::123456789012:role/testRole\n" +
                          "\n" +
                          "[profile profile-with-instance-credential-source]\n" +
                          "credential_source=ec2instancemetadata\n" +
                          "role_arn=arn:aws:iam::123456789012:role/testRole\n" +
                          "\n" +
                          "[profile profile-with-environment-credential-source]\n" +
                          "credential_source=environment\n" +
                          "role_arn=arn:aws:iam::123456789012:role/testRole\n");
    }

    private ProfileFile configFile(String configFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(configFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

}
