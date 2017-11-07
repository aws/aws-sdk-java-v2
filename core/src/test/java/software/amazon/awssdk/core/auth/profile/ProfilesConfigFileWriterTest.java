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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import software.amazon.awssdk.core.AmazonClientException;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsSessionCredentials;
import software.amazon.awssdk.core.auth.profile.internal.Profile;
import software.amazon.awssdk.core.util.ImmutableMapParameter;

public class ProfilesConfigFileWriterTest {

    private static final AwsCredentials basicCredA = AwsCredentials.create("a", "a");
    private static final AwsCredentials basicCredB = AwsCredentials.create("b", "b");
    private static final AwsCredentials sessionCredC = AwsSessionCredentials.create("c", "c", "c");
    private static final AwsCredentials sessionCredD = AwsSessionCredentials.create("d", "d", "d");

    /**
     * Loads the given credentials file and checks that it contains the same
     * set of profiles as expected.
     */
    private static void checkCredentialsFile(File file, Profile... expectedProfiles) {
        ProfilesConfigFile parsedFile = ProfilesConfigFile.create(file);
        Map<String, Profile> loadedProfiles = parsedFile.getAllProfiles();

        assertTrue(expectedProfiles.length == loadedProfiles.size());

        for (Profile expectedProfile : expectedProfiles) {
            Profile loadedProfile = loadedProfiles.get(expectedProfile.getProfileName());
            assertEqualProfiles(expectedProfile, loadedProfile);
        }
    }

    private static void assertEqualProfiles(Profile expected, Profile actual) {
        assertEquals(expected.getProfileName(), actual.getProfileName());
        assertEqualCredentials(expected.getCredentials(), actual.getCredentials());
    }

    private static void assertEqualCredentials(AwsCredentials expected, AwsCredentials actual) {
        assertEquals(expected.accessKeyId(),
                     actual.accessKeyId());
        assertEquals(expected.accessKeyId(),
                     actual.accessKeyId());

        if (expected instanceof AwsSessionCredentials) {
            assertTrue(actual instanceof AwsSessionCredentials);

            AwsSessionCredentials expectedSession = (AwsSessionCredentials) expected;
            AwsSessionCredentials actualSession = (AwsSessionCredentials) actual;
            assertEquals(expectedSession.sessionToken(),
                         actualSession.sessionToken());
        } else {
            assertFalse(actual instanceof AwsSessionCredentials);
        }
    }

    private static File copyToTempFile(File file) throws IOException {
        File tmpFile = File.createTempFile("credentials.", null);
        tmpFile.delete();
        FileUtils.copyFile(file, tmpFile);
        return tmpFile;
    }

    @Test
    public void testDumpToFile() throws IOException {
        File tmpFile = File.createTempFile("credentials.", null);

        Profile[] abcd = {
                new Profile("a", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.dumpToFile(tmpFile, true, abcd);
        checkCredentialsFile(tmpFile, abcd);

        // Rewrite the file with overwrite=true
        Profile[] a = {new Profile("a", basicCredA)};
        ProfilesConfigFileWriter.dumpToFile(tmpFile, true, a);
        checkCredentialsFile(tmpFile, a);

        // Rewrite the file with overwrite=false is not allowed
        try {
            ProfilesConfigFileWriter.dumpToFile(tmpFile, false,
                                                new Profile("a", basicCredA));
            fail("Should have thrown exception since the destination file already exists.");
        } catch (AmazonClientException expected) {
            // Ignored or expected.
        }

    }

    @Test
    public void testModifyProfile() throws IOException {
        File tmpFile = File.createTempFile("credentials.", null);

        Profile[] abcd = {
                new Profile("a", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.dumpToFile(tmpFile, true, abcd);

        // a <==> c, b <==> d
        Profile[] modified = {
                new Profile("a", sessionCredC),
                new Profile("b", sessionCredD),
                new Profile("c", basicCredA),
                new Profile("d", basicCredB)
        };
        ProfilesConfigFileWriter.modifyOrInsertProfiles(tmpFile, modified);
        checkCredentialsFile(tmpFile, modified);
    }

    @Test
    public void testInsertProfile() throws IOException {
        File tmpFile = File.createTempFile("credentials.", null);

        Profile[] abcd = {
                new Profile("a", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.dumpToFile(tmpFile, true, abcd);

        // Insert [e] profile
        Profile e = new Profile("e", basicCredA);
        ProfilesConfigFileWriter.modifyOrInsertProfiles(tmpFile, e);
        checkCredentialsFile(tmpFile, abcd[0], abcd[1], abcd[2], abcd[3], e);
    }

    @Test
    public void testModifyAndInsertProfile() throws IOException {
        File tmpFile = File.createTempFile("credentials.", null);

        Profile[] abcd = {
                new Profile("a", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.dumpToFile(tmpFile, true, abcd);

        // a <==> c, b <==> d, +e
        Profile[] modified = {
                new Profile("a", sessionCredC),
                new Profile("b", sessionCredD),
                new Profile("c", basicCredA),
                new Profile("d", basicCredB),
                new Profile("e", basicCredA)
        };
        ProfilesConfigFileWriter.modifyOrInsertProfiles(tmpFile, modified);
        checkCredentialsFile(tmpFile, modified);
    }

    /**
     * Tests that comments and unsupported properties are preserved after
     * profile modification.
     */
    @Test
    public void testModifyAndInsertProfile_WithComments() throws IOException, URISyntaxException {
        File credWithComments = ProfileResourceLoader.profilesWithComments().asFile();
        File tmpFile = copyToTempFile(credWithComments);

        String originalContent = FileUtils.readFileToString(tmpFile);

        Profile[] expected = {
                new Profile("a", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };

        // a <==> b, c <==> d, also renaming them to uppercase letters
        Profile[] modified = {
                new Profile("A", basicCredB),
                new Profile("B", basicCredA),
                new Profile("C", sessionCredD),
                new Profile("D", sessionCredC)
        };
        ProfilesConfigFileWriter.modifyProfiles(tmpFile, ImmutableMapParameter
                .of("a", modified[0],
                    "b", modified[1],
                    "c", modified[2],
                    "d", modified[3]));
        checkCredentialsFile(tmpFile, modified);

        // Sanity check that the content is altered
        String modifiedContent = FileUtils.readFileToString(tmpFile);
        assertFalse(originalContent.equals(modifiedContent));

        // Restore the properties
        ProfilesConfigFileWriter.modifyProfiles(tmpFile, ImmutableMapParameter
                .of("A", expected[0],
                    "B", expected[1],
                    "C", expected[2],
                    "D", expected[3]));
        checkCredentialsFile(tmpFile, expected);

        // Check that the content is now the same as the original
        String restoredContent = FileUtils.readFileToString(tmpFile);
        assertEquals(originalContent, restoredContent);
    }

    @Test
    public void testRenameProfile() throws IOException {
        File tmpFile = File.createTempFile("credentials.", null);

        Profile[] abcd = {
                new Profile("a", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.dumpToFile(tmpFile, true, abcd);

        // Rename a to A
        Profile[] modified = {
                new Profile("A", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.modifyOneProfile(tmpFile, "a", new Profile("A", basicCredA));
        checkCredentialsFile(tmpFile, modified);
    }

    @Test
    public void testDeleteProfile() throws IOException {
        File tmpFile = File.createTempFile("credentials.", null);

        Profile[] abcd = {
                new Profile("a", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.dumpToFile(tmpFile, true, abcd);

        // Delete a and c
        Profile[] modified = {
                new Profile("b", basicCredB),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.deleteProfiles(tmpFile, "a", "c");
        checkCredentialsFile(tmpFile, modified);
    }

    /**
     * Tests that the original credentials file is properly restored if the
     * in-place modification fails with error.
     */
    @Test
    public void testInPlaceModificationErrorHandling() throws IOException {
        File tmpFile = File.createTempFile("credentials.", null);

        Profile[] abcd = {
                new Profile("a", basicCredA),
                new Profile("b", basicCredB),
                new Profile("c", sessionCredC),
                new Profile("d", sessionCredD)
        };
        ProfilesConfigFileWriter.dumpToFile(tmpFile, true, abcd);
        String originalContent = FileUtils.readFileToString(tmpFile);

        // Insert [e] profile, which throws RuntimeException when the getProperties method is called.
        Profile e = new ProfileWithException("e", basicCredA);
        try {
            ProfilesConfigFileWriter.modifyOrInsertProfiles(tmpFile, e);
            fail("An exception is expected.");
        } catch (AmazonClientException expected) {
            // Ignored or expected.
        }

        // Check that the original file is restored
        assertTrue(tmpFile.exists());
        String restoredContent = FileUtils.readFileToString(tmpFile);
        assertEquals(originalContent, restoredContent);
    }

    private static class ProfileWithException extends Profile {

        public ProfileWithException(String profileName,
                                    AwsCredentials awsCredentials) {
            super(profileName, awsCredentials);
        }

        @Override
        public Map<String, String> getProperties() {
            throw new RuntimeException("Some exception...");
        }
    }
}
