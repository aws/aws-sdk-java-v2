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

package software.amazon.awssdk.auth.profile.internal;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Scanner;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A collection of utility methods for reading a profiles config file.
 */
@SdkInternalApi
public final class ProfilesConfigFileReader {
    private static final String DEPRECATED_PROFILE_PREFIX = "profile ";

    private ProfilesConfigFileReader() {}

    /**
     * Parses the input and returns a map from profile name to key-value pairs. This is the raw representation of what was in the
     * profile file, so profile names may be prefixed with "profile".
     */
    public static Map<String, Map<String, String>> parseProfileProperties(InputStream profileStream) {
        Map<String, Map<String, String>> profiles = new LinkedHashMap<>();

        int lineNumber = 0;
        String currentProfileName = null;

        try (Scanner scanner = new Scanner(profileStream)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                ++lineNumber;

                // Skip empty or comment lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                Optional<String> newProfileName = parseProfileName(line, lineNumber);

                if (newProfileName.isPresent()) {
                    String profileName = newProfileName.get();
                    Validate.validState(!profiles.containsKey(profileName),
                                        "Duplicate profile definitions for '%s' on line %s.", profileName, lineNumber);
                    profiles.put(profileName, new LinkedHashMap<>());

                    currentProfileName = profileName;
                } else {
                    Validate.validState(currentProfileName != null,
                                        "Expected a profile name on line %s.", lineNumber);

                    Entry<String, String> property = parsePropertyLine(line, lineNumber);
                    Map<String, String> properties = profiles.get(currentProfileName);

                    Validate.validState(!properties.containsKey(property.getKey()),
                                        "Duplicate property values for '%s' on line %s.", property.getValue(), lineNumber);

                    properties.put(property.getKey(), property.getValue());
                }
            }
        }

        return profiles;
    }

    /**
     * Returns the profile name if this line indicates the beginning of a new profile section. Otherwise, returns Optional.empty.
     */
    private static Optional<String> parseProfileName(String trimmedLine, int lineNumber) {
        if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
            String profileNameWithoutBrackets = trimmedLine.substring(1, trimmedLine.length() - 1).trim();
            String profileNameWithoutProfilePrefix = removeProfilePrefix(profileNameWithoutBrackets);

            Validate.validState(!profileNameWithoutProfilePrefix.isEmpty(),
                                "Invalid profile name: Profile name empty on line %s.", lineNumber);

            return Optional.of(profileNameWithoutProfilePrefix);
        }
        return Optional.empty();
    }

    private static String removeProfilePrefix(String profileName) {
        return profileName.startsWith(DEPRECATED_PROFILE_PREFIX)
               ? profileName.substring(DEPRECATED_PROFILE_PREFIX.length()).trim()
               : profileName;
    }

    /**
     * Returns the property if this line is a key-value pair. Otherwise, throws an exception.
     */
    private static Entry<String, String> parsePropertyLine(String propertyLine, int lineNumber) {
        String[] pair = propertyLine.split("=", 2);
        Validate.validState(pair.length == 2, "Invalid property format: no '=' character found on line %s.", lineNumber);

        String propertyKey = pair[0].trim();
        String propertyValue = pair[1].trim();

        return new AbstractMap.SimpleImmutableEntry<>(propertyKey, propertyValue);
    }
}
