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
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.auth;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.auth.profile.ProfileResourceLoader;
import software.amazon.awssdk.auth.profile.ProfilesConfigFile;

public class ProfileCredentialsProviderTest {
    private static File profileLocation = null;

    @BeforeClass
    public static void setUp() {
        profileLocation = ProfileResourceLoader.profilesContainingOtherConfiguration().asFile();
    }

    private ProfileCredentialsProvider newProvider() {
        return ProfileCredentialsProvider.builder()
                                         .profilesConfigFile(new ProfilesConfigFile(profileLocation.getAbsolutePath()))
                                         .build();
    }

    @Test
    public void testDefault() {
        ProfileCredentialsProvider provider = newProvider();
        AwsCredentials credentials = provider.getCredentials();

        // Yep, this is correct - they're backwards in
        // ProfilesContainingOtherConfigurations.tst
        Assert.assertEquals("defaultSecretAccessKey", credentials.accessKeyId());
        Assert.assertEquals("defaultAccessKey", credentials.secretAccessKey());
    }

    @Test
    public void testNoProfileFile() {
        ProfileCredentialsProvider nullProvider =
                ProfileCredentialsProvider.builder().defaultProfilesConfigFileLocator(() -> null).build();
        Assert.assertNull(nullProvider.loadCredentials());
    }

    @Test
    public void testEnvironmentVariable() throws Exception {
        Map<String, String> env = getMutableSystemEnvironment();

        try {
            env.put(AwsSystemSetting.AWS_DEFAULT_PROFILE.environmentVariable(), "test");

            ProfileCredentialsProvider provider = newProvider();

            AwsCredentials credentials = provider.getCredentials();
            Assert.assertEquals("test", credentials.accessKeyId());
            Assert.assertEquals("test key", credentials.secretAccessKey());
        } finally {
            env.remove(AwsSystemSetting.AWS_DEFAULT_PROFILE.environmentVariable());
        }
    }

    @Test
    public void testSystemProperty() {
        try {
            System.setProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property(), "test");

            ProfileCredentialsProvider provider = newProvider();

            AwsCredentials credentials = provider.getCredentials();
            Assert.assertEquals("test", credentials.accessKeyId());
            Assert.assertEquals("test key", credentials.secretAccessKey());
        } finally {
            System.clearProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property());
        }
    }

    @Test
    public void testBoth() throws Exception {
        Map<String, String> env = getMutableSystemEnvironment();

        try {
            // If both are set, property should take precedence.
            env.put(AwsSystemSetting.AWS_DEFAULT_PROFILE.environmentVariable(), "bogus");
            System.setProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property(), "test");

            ProfileCredentialsProvider provider = newProvider();

            AwsCredentials credentials = provider.getCredentials();
            Assert.assertEquals("test", credentials.accessKeyId());
            Assert.assertEquals("test key", credentials.secretAccessKey());
        } finally {
            System.clearProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property());
            env.remove(AwsSystemSetting.AWS_DEFAULT_PROFILE.environmentVariable());
        }
    }

    @Test
    public void testExplicit() throws Exception {
        Map<String, String> env = getMutableSystemEnvironment();

        try {
            env.put(AwsSystemSetting.AWS_DEFAULT_PROFILE.environmentVariable(), "test");
            System.setProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property(), "test");

            // If an explicit override is provided, that beats anything else.
            ProfileCredentialsProvider provider =
                    ProfileCredentialsProvider.builder()
                                              .profilesConfigFile(new ProfilesConfigFile(profileLocation.getAbsolutePath()))
                                              .profileName("bogus")
                                              .build();

            try {
                provider.getCredentials();
                Assert.fail("Expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                // Ignored or expected.
            }

        } finally {
            System.clearProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property());

            env.remove(AwsSystemSetting.AWS_DEFAULT_PROFILE.environmentVariable());
        }
    }

    @Test
    public void testAssumeRole() throws Exception {
        ProfilesConfigFile profilesFile = new ProfilesConfigFile(
                ProfileResourceLoader.profileWithRole().asFile(), targetRoleInfo -> {
                    AwsCredentials credentials = targetRoleInfo.getLongLivedCredentialsProvider()
                                                               .getCredentials();
                    Assert.assertEquals("sourceProfile AWSAccessKeyId", "defaultAccessKey",
                                        credentials.accessKeyId());
                    Assert.assertEquals("sourceProfile AWSSecretKey", "defaultSecretAccessKey",
                                        credentials.secretAccessKey());
                    Assert.assertEquals("role_arn", "arn:aws:iam::123456789012:role/testRole",
                                        targetRoleInfo.getRoleArn());
                    Assert.assertNull("external_id", targetRoleInfo.getExternalId());
                    Assert.assertTrue("role_session_name",
                                      targetRoleInfo.getRoleSessionName().startsWith("aws-sdk-java-"));
                    return new StaticCredentialsProvider(
                            new AwsCredentials("sessionAccessKey", "sessionSecretKey"));
                });

        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.builder()
                                                                                          .profilesConfigFile(profilesFile)
                                                                                          .profileName("test")
                                                                                          .build();
        AwsCredentials credentials = profileCredentialsProvider.getCredentials();

        Assert.assertEquals("sessionAccessKey", credentials.accessKeyId());
        Assert.assertEquals("sessionSecretKey", credentials.secretAccessKey());
    }

    @Test
    public void testAssumeRoleWithNameAndExternalId() throws Exception {
        ProfilesConfigFile profilesFile = new ProfilesConfigFile(
                ProfileResourceLoader.profileWithRole2().asFile(), targetRoleInfo -> {
                    AwsCredentials credentials = targetRoleInfo.getLongLivedCredentialsProvider()
                                                               .getCredentials();
                    Assert.assertEquals("sourceProfile AWSAccessKeyId", "defaultAccessKey",
                                        credentials.accessKeyId());
                    Assert.assertEquals("sourceProfile AWSSecretKey", "defaultSecretAccessKey",
                                        credentials.secretAccessKey());
                    Assert.assertEquals("role_arn", "arn:aws:iam::123456789012:role/testRole",
                                        targetRoleInfo.getRoleArn());
                    Assert.assertEquals("external_id", "testExternalId",
                                        targetRoleInfo.getExternalId());
                    Assert.assertEquals("role_session_name", "testSessionName",
                                        targetRoleInfo.getRoleSessionName());
                    return new StaticCredentialsProvider(
                            new AwsCredentials("sessionAccessKey", "sessionSecretKey"));
                });

        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.builder()
                                                                                          .profilesConfigFile(profilesFile)
                                                                                          .profileName("test")
                                                                                          .build();
        AwsCredentials credentials = profileCredentialsProvider.getCredentials();

        Assert.assertEquals("sessionAccessKey", credentials.accessKeyId());
        Assert.assertEquals("sessionSecretKey", credentials.secretAccessKey());
    }

    @Test
    public void testAssumeRoleWithSourceAfterRole() throws Exception {
        ProfilesConfigFile profilesFile = new ProfilesConfigFile(
                ProfileResourceLoader.profileWithSourceAfterRole().asFile(), targetRoleInfo -> {
                    AwsCredentials credentials = targetRoleInfo
                            .getLongLivedCredentialsProvider().getCredentials();
                    Assert.assertEquals("sourceProfile AWSAccessKeyId", "defaultAccessKey",
                                        credentials.accessKeyId());
                    Assert.assertEquals("sourceProfile AWSSecretKey", "defaultSecretAccessKey",
                                        credentials.secretAccessKey());
                    Assert.assertEquals("role_arn", "arn:aws:iam::123456789012:role/testRole",
                                        targetRoleInfo.getRoleArn());
                    Assert.assertNull("external_id", targetRoleInfo.getExternalId());
                    Assert.assertTrue("role_session_name", targetRoleInfo.getRoleSessionName()
                                                                         .startsWith("aws-sdk-java-"));
                    return new StaticCredentialsProvider(
                            new AwsCredentials("sessionAccessKey", "sessionSecretKey"));
                });

        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.builder()
                                                                                          .profilesConfigFile(profilesFile)
                                                                                          .profileName("test")
                                                                                          .build();
        AwsCredentials credentials = profileCredentialsProvider.getCredentials();

        Assert.assertEquals("sessionAccessKey", credentials.accessKeyId());
        Assert.assertEquals("sessionSecretKey", credentials.secretAccessKey());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getMutableSystemEnvironment() throws Exception {
        Map<String, String> immutableEnv = System.getenv();

        Class<?> unMap = Class.forName("java.util.Collections$UnmodifiableMap");
        Field m = unMap.getDeclaredField("m");
        m.setAccessible(true);

        return (Map<String, String>) m.get(immutableEnv);
    }
}
