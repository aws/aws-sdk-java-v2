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

public final class ProfilesFile {
    private final String location;
    private final Map<String, Profile> profiles;

    public ProfilesFile(Path profileLocation) {
        Validate.paramNotNull(profileLocation, "profileLocation");
        Validate.validState(Files.exists(profileLocation), "Profile file '%s' does not exist.", profileLocation);

        this.location = profileLocation.toAbsolutePath().toString();
        this.profiles = readProfilesFile(invokeSafely(() -> Files.newInputStream(profileLocation)));
    }

    public ProfilesFile(InputStream profileStream) {
        Validate.paramNotNull(profileStream, "profileStream");

        this.location = "InputStream";
        this.profiles = readProfilesFile(profileStream);
    }

    public Optional<Profile> profile(String profileName) {
        return Optional.ofNullable(profiles.get(profileName));
    }

    private Map<String, Profile> readProfilesFile(InputStream profileStream) {
        Map<String, Profile> result = ProfilesConfigFileReader.parseProfileProperties(profileStream).entrySet().stream()
                                                              .map(this::convertToProfileMapEntry)
                                                              .collect(toMap(Entry::getKey, Entry::getValue));
        return Collections.unmodifiableMap(result);
    }

    @Override
    public String toString() {
        return "ProfilesFile(" + location + ")";
    }

    private Entry<String, Profile> convertToProfileMapEntry(Entry<String, Map<String, String>> stringMapEntry) {
        String profileName = stringMapEntry.getKey();
        Map<String, String> profileProperties = stringMapEntry.getValue();
        return new AbstractMap.SimpleImmutableEntry<>(profileName, convertToProfile(profileName, profileProperties));
    }

    private Profile convertToProfile(String profileName, Map<String, String> profileProperties) {
        return Profile.builder().name(profileName).properties(profileProperties).profilesFile(this).build();
    }
}
