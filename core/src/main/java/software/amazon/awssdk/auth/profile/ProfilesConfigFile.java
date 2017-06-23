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

package software.amazon.awssdk.auth.profile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.auth.profile.internal.AllProfiles;
import software.amazon.awssdk.auth.profile.internal.BasicProfile;
import software.amazon.awssdk.auth.profile.internal.BasicProfileConfigLoader;
import software.amazon.awssdk.auth.profile.internal.Profile;
import software.amazon.awssdk.auth.profile.internal.ProfileAssumeRoleCredentialsProvider;
import software.amazon.awssdk.auth.profile.internal.ProfileStaticCredentialsProvider;
import software.amazon.awssdk.auth.profile.internal.securitytoken.ProfileCredentialsService;
import software.amazon.awssdk.auth.profile.internal.securitytoken.StsProfileCredentialsServiceLoader;
import software.amazon.awssdk.profile.path.AwsProfileFileLocationProvider;
import software.amazon.awssdk.util.ValidationUtils;

/**
 * Loads the local AWS credential profiles from the standard location (~/.aws/credentials), which
 * can be easily overridden through the <code>AWS_CREDENTIAL_PROFILES_FILE</code> environment
 * variable or by specifying an alternate credentials file location through this class' constructor.
 * <p> The AWS credentials file format allows you to specify multiple profiles, each with their own
 * set of AWS security credentials:
 * <pre>
 * [default]
 * aws_access_key_id=testAccessKey
 * aws_secret_access_key=testSecretKey
 * aws_session_token=testSessionToken
 *
 * [test-user]
 * aws_access_key_id=testAccessKey
 * aws_secret_access_key=testSecretKey
 * aws_session_token=testSessionToken
 * </pre>
 *
 * <p> These credential profiles allow you to share multiple sets of AWS security credentails
 * between different tools such as the AWS SDK for Java and the AWS CLI.
 *
 * <p> For more information on setting up AWS credential profiles, see:
 * http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html
 *
 * @see ProfileCredentialsProvider
 */
public class ProfilesConfigFile {
    private final File profileFile;
    private final ProfileCredentialsService profileCredentialsService;

    /**
     * Cache credential providers as credentials from profiles are requested. Doesn't really make a
     * difference for basic credentials but for assume role it's more efficient as each assume role
     * provider has it's own async refresh logic.
     */
    private final ConcurrentHashMap<String, AwsCredentialsProvider> credentialProviderCache = new ConcurrentHashMap<>();
    private volatile AllProfiles allProfiles;
    private volatile long profileFileLastModified;

    /**
     * Loads the AWS credential profiles file from the default location (~/.aws/credentials) or from
     * an alternate location if <code>AWS_CREDENTIAL_PROFILES_FILE</code> is set.
     */
    public ProfilesConfigFile() throws SdkClientException {
        this(getCredentialProfilesFile());
    }

    /**
     * Loads the AWS credential profiles from the file. The path of the file is specified as a
     * parameter to the constructor.
     */
    public ProfilesConfigFile(String filePath) {
        this(new File(validateFilePath(filePath)));
    }

    /**
     * Loads the AWS credential profiles from the file. The path of the file is specified as a
     * parameter to the constructor.
     */
    public ProfilesConfigFile(String filePath, ProfileCredentialsService credentialsService) throws
                                                                                             SdkClientException {
        this(new File(validateFilePath(filePath)), credentialsService);
    }

    /**
     * Loads the AWS credential profiles from the file. The reference to the file is specified as a
     * parameter to the constructor.
     */
    public ProfilesConfigFile(File file) throws SdkClientException {
        this(file, StsProfileCredentialsServiceLoader.getInstance());
    }

    /**
     * Loads the AWS credential profiles from the file. The reference to the file is specified as a
     * parameter to the constructor.
     */
    public ProfilesConfigFile(File file, ProfileCredentialsService credentialsService) throws
                                                                                       SdkClientException {
        profileFile = ValidationUtils.assertNotNull(file, "profile file");
        profileCredentialsService = credentialsService;
        profileFileLastModified = file.lastModified();
        allProfiles = loadProfiles(profileFile);
    }

    private static String validateFilePath(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Unable to load AWS profiles: specified file path is null.");
        }
        return filePath;
    }

    private static File getCredentialProfilesFile() {
        return AwsProfileFileLocationProvider.DEFAULT_CREDENTIALS_LOCATION_PROVIDER.getLocation();
    }

    private static AllProfiles loadProfiles(File file) {
        return BasicProfileConfigLoader.INSTANCE.loadProfiles(file);
    }

    /**
     * Returns the AWS credentials for the specified profile.
     */
    public AwsCredentials getCredentials(String profileName) {
        final AwsCredentialsProvider provider = credentialProviderCache.get(profileName);
        if (provider != null) {
            return provider.getCredentials();
        } else {
            BasicProfile profile = allProfiles.getProfile(profileName);
            if (profile == null) {
                throw new IllegalArgumentException("No AWS profile named '" + profileName + "'");
            }
            final AwsCredentialsProvider newProvider = fromProfile(profile);
            credentialProviderCache.put(profileName, newProvider);
            return newProvider.getCredentials();
        }
    }

    /**
     * Reread data from disk.
     */
    public void refresh() {
        if (profileFile.lastModified() > profileFileLastModified) {
            profileFileLastModified = profileFile.lastModified();
            allProfiles = loadProfiles(profileFile);
        }
        credentialProviderCache.clear();
    }

    public Map<String, BasicProfile> getAllBasicProfiles() {
        return allProfiles.getProfiles();
    }

    @Deprecated
    public Map<String, Profile> getAllProfiles() {
        Map<String, Profile> legacyProfiles = new HashMap<String, Profile>();
        for (Map.Entry<String, BasicProfile> entry : getAllBasicProfiles().entrySet()) {
            final String profileName = entry.getKey();
            legacyProfiles.put(profileName,
                               new Profile(profileName, entry.getValue().getProperties(),
                                           new StaticCredentialsProvider(getCredentials(profileName))));
        }
        return legacyProfiles;
    }

    private AwsCredentialsProvider fromProfile(BasicProfile profile) {
        if (profile.isRoleBasedProfile()) {
            return new ProfileAssumeRoleCredentialsProvider(profileCredentialsService, allProfiles,
                                                            profile);
        } else {
            return new ProfileStaticCredentialsProvider(profile);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + profileFile + ")";
    }
}
