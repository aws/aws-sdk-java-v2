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

package software.amazon.awssdk.core.auth.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Test;
import software.amazon.awssdk.core.AmazonClientException;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsSessionCredentials;
import software.amazon.awssdk.core.auth.profile.internal.Profile;

public class CredentialProfilesTest {

    /**
     * Name of the default profile used in the configuration file.
     */
    private static final String DEFAULT_PROFILE_NAME = "default";

    /**
     * Name of the sample test profile used during testing.
     */
    private static final String PROFILE_NAME_TEST = "test";

    @Test(expected = IllegalArgumentException.class)
    public void loadProfileFromNonExistentFile() {
        ProfilesConfigFile.create(new File("/some/invalid/file/location.txt"));
    }

    /**
     * Tests two profiles having same name. The second profile overrides the first profile. Also
     * checks if the AWS Access Key ID and AWS Secret Access Key are mapped properly under the
     * profile.
     */
    @Test
    public void testTwoProfileWithSameName() throws URISyntaxException {
        ProfilesConfigFile profile = ProfilesConfigFile.create(
                ProfileResourceLoader.profilesWithSameProfileName().asFile());

        AwsCredentials defaultCred = profile.getCredentials(DEFAULT_PROFILE_NAME);
        assertNotNull(defaultCred);
        assertTrue(defaultCred instanceof AwsCredentials);

        AwsCredentials testCred = profile.getCredentials(PROFILE_NAME_TEST);
        assertNotNull(testCred);
        assertTrue(testCred instanceof AwsSessionCredentials);
        AwsSessionCredentials testSessionCred = (AwsSessionCredentials) testCred;
        assertEquals(testSessionCred.accessKeyId(), "testProfile2");
        assertEquals(testSessionCred.secretAccessKey(), "testProfile2");
        assertEquals(testSessionCred.sessionToken(), "testProfile2");

    }

    /**
     * Tests loading profile with a profile name having only spaces. An exception should be thrown
     * in this case.
     */
    @Test
    public void testProfileNameWithJustSpaces() {
        checkExpectedException(ProfileResourceLoader.profileNameWithSpaces(),
                               AmazonClientException.class,
                               "Should throw an exception as there is a profile mentioned with no profile name.");
    }

    /**
     * Tests loading profile with a profile name not mentioned. An exception should be thrown in
     * this case.
     */
    @Test
    public void testProfileWithNoProfileNameGiven() {
        checkExpectedException(ProfileResourceLoader.profilesWithNoProfileName(),
                               AmazonClientException.class,
                               "Should throw an exception as there is a profile mentioned with only spaces.");
    }

    /**
     * Tests loading profile with a profile name not having opening or closing braces. An exception
     * should be thrown in this case.
     */
    @Test
    public void testProfileWithProfileNameNotHavingOpeningOrClosingBraces() {
        checkExpectedException(ProfileResourceLoader.profileNameWithNoClosingBraces(),
                               IllegalArgumentException.class,
                               "Should throw an exception as there is a profile name mentioned with no closing braces.");

        checkExpectedException(ProfileResourceLoader.profileNameWithNoOpeningBraces(),
                               IllegalArgumentException.class,
                               "Should throw an exception as there is a profile name mentioned with no opening braces.");

        checkExpectedException(ProfileResourceLoader.profileNameWithNoBraces(),
                               IllegalArgumentException.class,
                               "Should throw an exception as there is a profile name mentioned with no braces.");
    }

    /**
     * Tests loading profile with AWS Access Key not specified for a profile. An exception should be
     * thrown in this case.
     */
    @Test
    public void testProfileWithAccessKeyNotSpecified() throws URISyntaxException {
        checkDeferredException(ProfileResourceLoader.accessKeyNotSpecified(),
                               AmazonClientException.class, "test2",
                               "Should throw an exception as there is a profile with AWS Access Key ID not specified.");
    }

    @Test
    public void testProfileWithEmptyAccessKey() throws URISyntaxException {
        checkDeferredException(ProfileResourceLoader.profileWithEmptyAccessKey(),
                               AmazonClientException.class, "test",
                               "Should throw an exception as there is a profile with an empty AWS Access Key ID");
    }

    /**
     * Tests loading profile with AWS Secret Access Key not specified for a profile. An exception
     * should be thrown in this case.
     */
    @Test
    public void testProfileWithSecretAccessKeyNotSpecified() throws Exception {
        checkDeferredException(ProfileResourceLoader.profilesWithSecretAccessKeyNotSpecified(),
                               AmazonClientException.class, "profile test2",
                               "Should throw an exception as there is a profile with AWS Secret Access Key not specified.");
    }

    @Test
    public void testProfileWithEmptySecretAccessKey() throws URISyntaxException {
        checkDeferredException(ProfileResourceLoader.profileWithEmptySecretKey(),
                               AmazonClientException.class, "test",
                               "Should throw an exception as there is a profile with an empty AWS Secret Access Key.");
    }

    /**
     * Tests loading profile with a profile having multiple AWS Access Key ID's. An exception should
     * be thrown in this case.
     */
    @Test
    public void testProfileWithMultipleAccessOrSecretKeysUnderSameProfile() {
        checkExpectedException(ProfileResourceLoader.profilesWithTwoAccessKeyUnderSameProfile(),
                               IllegalArgumentException.class,
                               "Should throw an exception as there is a profile with two AWS Access Key ID's.");
    }

    /**
     * Tests loading profile with a file that contains other configuration informations like region,
     * output format etc., The file should be parsed correctly and the profiles must be loaded.
     */
    @Test
    public void testProfileWithOtherConfigurations() throws URISyntaxException {
        ProfilesConfigFile profile = ProfilesConfigFile.create(
                ProfileResourceLoader.profilesContainingOtherConfiguration().asFile());

        assertNotNull(profile.getCredentials(DEFAULT_PROFILE_NAME));

        assertNotNull(profile.getCredentials(PROFILE_NAME_TEST));

        assertEquals(profile.getCredentials(PROFILE_NAME_TEST).accessKeyId(), "test");

        assertEquals(profile.getCredentials(PROFILE_NAME_TEST).secretAccessKey(), "test key");
    }

    /**
     * Test verifying we pick up a change to a file.
     */
    @Test
    public void testReadUpdatedProfile() throws URISyntaxException, IOException {
        ProfilesConfigFile fixture = ProfilesConfigFile.create(
                ProfileResourceLoader.basicProfile().asFile());
        File modifiable = File.createTempFile("UpdatableProfile", ".tst");
        ProfilesConfigFileWriter.dumpToFile(modifiable, true, fixture.getAllProfiles().values()
                                                                     .toArray(new Profile[1]));

        ProfilesConfigFile test = ProfilesConfigFile.create(modifiable);
        AwsCredentials orig = test.getCredentials(DEFAULT_PROFILE_NAME);
        assertEquals("defaultAccessKey", orig.accessKeyId());
        assertEquals("defaultSecretAccessKey", orig.secretAccessKey());
        //Sleep to ensure that the timestamp on the file (when we modify it) is
        //distinguishably later from the original write.
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // Ignored or expected.
        }

        Profile newProfile = new Profile(DEFAULT_PROFILE_NAME,
                                         AwsCredentials.create("newAccessKey", "newSecretKey"));
        ProfilesConfigFileWriter.modifyOneProfile(modifiable, DEFAULT_PROFILE_NAME, newProfile);

        test.refresh();
        AwsCredentials updated = test.getCredentials(DEFAULT_PROFILE_NAME);
        assertEquals("newAccessKey", updated.accessKeyId());
        assertEquals("newSecretKey", updated.secretAccessKey());
    }

    /**
     * Tests loading a profile that assumes a role, but the source profile does not exist.
     */
    @Test
    public void testRoleProfileWithNoSourceName() throws URISyntaxException {
        checkDeferredException(ProfileResourceLoader.roleProfileWithNoSourceName(),
                               AmazonClientException.class, "test",
                               "Should throw an exception as there is a role profile with a missing source role");
    }

    /**
     * Tests loading a profile that assumes a role, but the source profile does not exist.
     */
    @Test
    public void testRoleProfileWithEmptySourceName() throws URISyntaxException {
        checkDeferredException(ProfileResourceLoader.roleProfileWithEmptySourceName(),
                               AmazonClientException.class, "test",
                               "Should throw an exception as there is a role profile with an empty source role");
    }

    /**
     * Tests loading a profile that assumes a role, but the source profile does not exist.
     */
    @Test
    public void testRoleProfileMissingSource() throws URISyntaxException {
        checkDeferredException(ProfileResourceLoader.roleProfileMissingSource(),
                               AmazonClientException.class, "test",
                               "Should throw an exception as there is a role profile without a source specified");
    }

    /**
     * Tests loading a profile that assumes a role, but the source profile does not exist.
     */
    @Test
    public void testRoleProfileWithRoleSource() throws URISyntaxException {
        checkDeferredException(ProfileResourceLoader.roleProfileWithRoleSource(),
                               AmazonClientException.class, "test",
                               "Should throw an exception as a role profile can not use a role profile as its source");
    }

    /**
     * Configures the Profile with the data returned from the file. Throws an error if the
     * configuration is success as the file contains data not in the correct format.
     *
     * @param resource       Resource object to source profiles from.
     * @param failureMessage failure message to be displayed if the file configuration is success
     */
    private void checkExpectedException(ProfileResourceLoader resource,
                                        Class<? extends Exception> expectedExceptionClass,
                                        String failureMessage) {
        try {
            ProfilesConfigFile.create(resource.asFile());
            fail(failureMessage);
        } catch (Exception e) {
            if (!expectedExceptionClass.isInstance(e)) {
                throw new RuntimeException(e);
            }
        }
    }

    private void checkDeferredException(ProfileResourceLoader resource,
                                        Class<? extends Exception> expectedExceptionClass,
                                        String profileName, String failureMessage) throws
                                                                                   URISyntaxException {
        ProfilesConfigFile configFile = ProfilesConfigFile.create(resource.asFile());
        try {
            configFile.getCredentials(profileName);
            fail(failureMessage);
        } catch (Exception e) {
            if (!expectedExceptionClass.isInstance(e)) {
                throw new RuntimeException(e);
            }
        }
    }

}
