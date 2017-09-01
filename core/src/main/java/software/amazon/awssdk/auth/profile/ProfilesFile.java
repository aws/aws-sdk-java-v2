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

import static java.util.stream.Collectors.toMap;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import software.amazon.awssdk.auth.profile.internal.ProfilesConfigFileReader;
import software.amazon.awssdk.utils.Validate;

/**
 * Provides programmatic access to the contents of an AWS configuration profile file.
 *
 * AWS configuration profiles allow you to share multiple sets of AWS
 * security credentials between different tools such as the AWS SDK for Java
 * and the AWS CLI.
 * <p>
 * In addition to the required <code>default</code> profile, you can specify as
 * many additional named profiles as you need:
 * <pre>
 * [default]
 * aws_access_key_id=AKIAXXXXXXXXXX
 * aws_secret_access_key=abc01234567890
 *
 * [test]
 * aws_access_key_id=AKIAZZZZZZZZZZ
 * aws_secret_access_key=xyz01234567890
 * </pre>
 * <p>
 * Role assumption is also supported for cross account access. The source profile credentials are
 * used to assume the given role when the <pre>test</pre> profile is used. One requirement to use
 * assume role profiles is that the STS SDK module be on the class path.
 * <pre>
 * [default]
 * aws_access_key_id=AKIAXXXXXXXXXX
 * aws_secret_access_key=abc01234567890
 *
 * [test]
 * role_arn=arn:aws:iam::123456789012:role/role-name
 * source_profile=default
 * # Optionally, provide a session name
 * # role_session_name=mysession
 * # Optionally, provide an external id
 * # external_id=abc01234567890
 * # Optionally, provide a region to use for the role assumption service calls
 * # region=us-east-1
 * </pre>
 *
 *
 * <p>
 * You can use {@link software.amazon.awssdk.auth.ProfileCredentialsProvider} to
 * access your AWS configuration profiles and supply your credentials to code
 * using the AWS SDK for Java.
 *
 * <p>
 * The same profiles are used by the AWS CLI.
 *
 * <p>
 * For more information on setting up AWS configuration profiles, see:
 * http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html
 */
public final class ProfilesFile {
    private final String location;
    private final Map<String, Profile> profiles;

    /**
     * Create a profiles file that will load the profiles at the given path.
     *
     * @param profileLocation The location from which the profile should be loaded.
     */
    public ProfilesFile(Path profileLocation) {
        Validate.paramNotNull(profileLocation, "profileLocation");
        Validate.validState(Files.exists(profileLocation), "Profile file '%s' does not exist.", profileLocation);

        this.location = profileLocation.toAbsolutePath().toString();
        this.profiles = readProfilesFile(invokeSafely(() -> Files.newInputStream(profileLocation)));
    }

    /**
     * Create a profiles file that will load the profiles from the given input stream.
     *
     * @param profileStream The input stream from which the profile should be loaded.
     */
    public ProfilesFile(InputStream profileStream) {
        Validate.paramNotNull(profileStream, "profileStream");

        this.location = "InputStream";
        this.profiles = readProfilesFile(profileStream);
    }

    /**
     * Retrieve the profile from this file with the given name.
     *
     * @param profileName The name of the profile that should be retrieved from this file.
     * @return The profile, if available.
     */
    public Optional<Profile> profile(String profileName) {
        return Optional.ofNullable(profiles.get(profileName));
    }

    /**
     * Read the profiles file into memory from the given input stream.
     */
    private Map<String, Profile> readProfilesFile(InputStream profileStream) {
        Map<String, Profile> result = ProfilesConfigFileReader.parseProfileProperties(profileStream).entrySet().stream()
                                                              .map(this::convertToProfileMapEntry)
                                                              .collect(toMap(Entry::getKey, Entry::getValue));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Convert between the raw format used by {@link ProfilesConfigFileReader} to the {@link Profile} object.
     */
    private Entry<String, Profile> convertToProfileMapEntry(Entry<String, Map<String, String>> stringMapEntry) {
        String profileName = stringMapEntry.getKey();
        Map<String, String> profileProperties = stringMapEntry.getValue();
        return new AbstractMap.SimpleImmutableEntry<>(profileName, convertToProfile(profileName, profileProperties));
    }

    private Profile convertToProfile(String profileName, Map<String, String> profileProperties) {
        return Profile.builder().name(profileName).properties(profileProperties).profilesFile(this).build();
    }

    @Override
    public String toString() {
        return "ProfilesFile(" + location + ")";
    }
}
