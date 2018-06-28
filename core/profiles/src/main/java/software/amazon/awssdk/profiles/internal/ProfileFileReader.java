/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.profiles.internal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Converts an {@link InputStream} to a configuration or credentials file into a map of profiles and their properties.
 *
 * @see #parseFile(InputStream, ProfileFile.Type)
 */
@SdkInternalApi
public final class ProfileFileReader {
    private static final Logger log = Logger.loggerFor(ProfileFileReader.class);

    private static final Pattern EMPTY_LINE = Pattern.compile("^[\t ]*$");

    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_\\-]*$");

    private ProfileFileReader() {}

    /**
     * Parses the input and returns a mutable map from profile name to a map of properties. This will not close the provided
     * stream.
     */
    public static Map<String, Map<String, String>> parseFile(InputStream profileStream, ProfileFile.Type fileType) {
        ParserState state = new ParserState(fileType);

        BufferedReader profileReader = new BufferedReader(new InputStreamReader(profileStream, StandardCharsets.UTF_8));
        profileReader.lines().forEach(line -> parseLine(state, line));

        return state.profiles;
    }

    /**
     * Parse a line and update the parser state.
     */
    private static void parseLine(ParserState state, String line) {
        ++state.currentLineNumber;

        if (isEmptyLine(line) || isCommentLine(line)) {
            return; // Skip line
        }

        if (isProfileDefinitionLine(line)) {
            readProfileDefinitionLine(state, line);
        } else if (isPropertyContinuationLine(line)) {
            readPropertyContinuationLine(state, line);
        } else {
            readPropertyDefinitionLine(state, line);
        }
    }

    /**
     * Read a profile line and update the parser state with the results. This marks future properties as being in this profile.
     *
     * Configuration Files: [ Whitespace? profile Whitespace Identifier Whitespace? ] Whitespace? CommentLine?
     * Credentials Files: [ Whitespace? Identifier Whitespace? ] Whitespace? CommentLine?
     */
    private static void readProfileDefinitionLine(ParserState state, String line) {
        // Profile definitions do not require a space between the closing bracket and the comment delimiter
        String lineWithoutComments = removeTrailingComments(line, "#", ";");
        String lineWithoutWhitespace = StringUtils.trim(lineWithoutComments);

        Validate.isTrue(lineWithoutWhitespace.endsWith("]"),
                        "Profile definition must end with ']' on line " + state.currentLineNumber);

        Optional<String> profileName = parseProfileDefinition(state, lineWithoutWhitespace);

        // If we couldn't get the profile name, ignore this entire profile.
        if (!profileName.isPresent()) {
            state.ignoringCurrentProfile = true;
            return;
        }

        state.currentProfileBeingRead = profileName.get();
        state.currentPropertyBeingRead = null;
        state.ignoringCurrentProfile = false;
        state.ignoringCurrentProperty = false;

        // If we've seen this profile before, don't override the existing properties. We'll be merging them.
        state.profiles.computeIfAbsent(profileName.get(), i -> new LinkedHashMap<>());
    }

    /**
     * Read a property definition line and update the parser state with the results. This adds the property to the current profile
     * and marks future property continuations as being part of this property.
     *
     * Identifier Whitespace? = Whitespace? Value? Whitespace? (Whitespace CommentLine)?
     */
    private static void readPropertyDefinitionLine(ParserState state, String line) {
        // If we're in an invalid profile, ignore its properties
        if (state.ignoringCurrentProfile) {
            return;
        }

        Validate.isTrue(state.currentProfileBeingRead != null,
                        "Expected a profile definition on line " + state.currentLineNumber);

        // Property definition comments must have whitespace before them, or they will be considered part of the value
        String lineWithoutComments = removeTrailingComments(line, " #", " ;", "\t#", "\t;");
        String lineWithoutWhitespace = StringUtils.trim(lineWithoutComments);

        Optional<Pair<String, String>> propertyDefinition = parsePropertyDefinition(state, lineWithoutWhitespace);

        // If we couldn't get the property key and value, ignore this entire property.
        if (!propertyDefinition.isPresent()) {
            state.ignoringCurrentProperty = true;
            return;
        }

        Pair<String, String> property = propertyDefinition.get();

        if (state.profiles.get(state.currentProfileBeingRead).containsKey(property.left())) {
            log.warn(() -> "Warning: Duplicate property '" + property.left() + "' detected on line " + state.currentLineNumber +
                           ". The later one in the file will be used.");
        }

        state.currentPropertyBeingRead = property.left();
        state.ignoringCurrentProperty = false;
        state.validatingContinuationsAsSubProperties = property.right().equals("");

        state.profiles.get(state.currentProfileBeingRead).put(property.left(), property.right());
    }

    /**
     * Read a property continuation line and update the parser state with the results. This adds the value in the continuation
     * to the current property, prefixed with a newline.
     *
     * Non-Blank Parent Property: Whitespace Value Whitespace?
     * Blank Parent Property (Sub-Property): Whitespace Identifier Whitespace? = Whitespace? Value Whitespace?
     */
    private static void readPropertyContinuationLine(ParserState state, String line) {
        // If we're in an invalid profile or property, ignore its continuations
        if (state.ignoringCurrentProfile || state.ignoringCurrentProperty) {
            return;
        }

        Validate.isTrue(state.currentProfileBeingRead != null && state.currentPropertyBeingRead != null,
                        "Expected a profile or property definition on line " + state.currentLineNumber);

        // Comments are not removed on property continuation lines. They're considered part of the value.
        line = StringUtils.trim(line);

        Map<String, String> profileProperties = state.profiles.get(state.currentProfileBeingRead);

        String currentPropertyValue = profileProperties.get(state.currentPropertyBeingRead);
        String newPropertyValue = currentPropertyValue + "\n" + line;

        // If this is a sub-property, make sure it can be parsed correctly by the CLI.
        if (state.validatingContinuationsAsSubProperties) {
            parsePropertyDefinition(state, line);
        }

        profileProperties.put(state.currentPropertyBeingRead, newPropertyValue);
    }

    /**
     * Given a profile line, load the profile name based on the file type. If the profile name is invalid for the file type,
     * this will return empty.
     */
    private static Optional<String> parseProfileDefinition(ParserState state, String lineWithoutWhitespace) {
        String lineWithoutBrackets = lineWithoutWhitespace.substring(1, lineWithoutWhitespace.length() - 1);
        String rawProfileName = StringUtils.trim(lineWithoutBrackets);
        boolean hasProfilePrefix = rawProfileName.startsWith("profile ") || rawProfileName.startsWith("profile\t");

        String standardizedProfileName;
        if (state.fileType == ProfileFile.Type.CONFIGURATION) {
            if (hasProfilePrefix) {
                standardizedProfileName = StringUtils.trim(rawProfileName.substring("profile".length()));
            } else if (rawProfileName.equals("default")) {
                standardizedProfileName = "default";
            } else {
                log.warn(() -> "Ignoring profile '" + rawProfileName + "' on line " + state.currentLineNumber + " because it " +
                               "did not start with 'profile ' and it was not 'default'.");
                return Optional.empty();
            }
        } else if (state.fileType == ProfileFile.Type.CREDENTIALS) {
            standardizedProfileName = rawProfileName;
        } else {
            throw new IllegalStateException("Unknown profile file type: " + state.fileType);
        }

        String profileName = StringUtils.trim(standardizedProfileName);

        // If the profile name includes invalid characters, it should be ignored.
        if (!isValidIdentifier(profileName)) {
            log.warn(() -> "Ignoring profile '" + standardizedProfileName + "' on line " + state.currentLineNumber + " because " +
                           "it was not alphanumeric with dashes or underscores.");
            return Optional.empty();
        }

        // [profile default] must take priority over [default] in configuration files.
        boolean isDefaultProfile = profileName.equals("default");
        boolean seenProfileBefore = state.profiles.containsKey(profileName);

        if (state.fileType == ProfileFile.Type.CONFIGURATION && isDefaultProfile && seenProfileBefore) {
            if (!hasProfilePrefix && state.seenDefaultProfileWithProfilePrefix) {
                log.warn(() -> "Ignoring profile '[default]' on line " + state.currentLineNumber + ", because " +
                               "'[profile default]' was already seen in the same file.");
                return Optional.empty();
            } else if (hasProfilePrefix && !state.seenDefaultProfileWithProfilePrefix) {
                log.warn(() -> "Ignoring earlier-seen '[default]', because '[profile default]' was found on line " +
                               state.currentLineNumber);
                state.profiles.remove("default");
            }
        }

        if (isDefaultProfile && hasProfilePrefix) {
            state.seenDefaultProfileWithProfilePrefix = true;
        }

        return Optional.of(profileName);
    }

    /**
     * Given a property line, load the property key and value. If the property line is invalid and should be ignored, this will
     * return empty.
     */
    private static Optional<Pair<String, String>> parsePropertyDefinition(ParserState state, String line) {
        int firstEqualsLocation = line.indexOf('=');
        Validate.isTrue(firstEqualsLocation != -1, "Expected an '=' sign defining a property on line " + state.currentLineNumber);

        String propertyKey = StringUtils.trim(line.substring(0, firstEqualsLocation));
        String propertyValue = StringUtils.trim(line.substring(firstEqualsLocation + 1));

        Validate.isTrue(!propertyKey.isEmpty(), "Property did not have a name on line " + state.currentLineNumber);

        // If the profile name includes invalid characters, it should be ignored.
        if (!isValidIdentifier(propertyKey)) {
            log.warn(() -> "Ignoring property '" + propertyKey + "' on line " + state.currentLineNumber + " because " +
                           "its name was not alphanumeric with dashes or underscores.");
            return Optional.empty();
        }

        return Optional.of(Pair.of(propertyKey, propertyValue));
    }

    /**
     * Remove trailing comments from the provided line, using the provided patterns to find where the comment starts.
     *
     * Profile definitions don't require spaces before comments.
     * Property definitions require spaces before comments.
     * Property continuations don't allow trailing comments. They're considered part of the value.
     */
    private static String removeTrailingComments(String line, String... commentPatterns) {
        return line.substring(0, findEarliestMatch(line, commentPatterns));
    }

    /**
     * Search the provided string for the requested patterns, returning the index of the match that is closest to the front of the
     * string. If no match is found, this returns the length of the string (one past the final index in the string).
     */
    private static int findEarliestMatch(String line, String... searchPatterns) {
        return Stream.of(searchPatterns)
                     .mapToInt(line::indexOf)
                     .filter(location -> location >= 0)
                     .min()
                     .orElse(line.length());
    }

    private static boolean isEmptyLine(String line) {
        return EMPTY_LINE.matcher(line).matches();
    }

    private static boolean isCommentLine(String line) {
        return line.startsWith("#") || line.startsWith(";");
    }

    private static boolean isProfileDefinitionLine(String line) {
        return line.startsWith("[");
    }

    private static boolean isPropertyContinuationLine(String line) {
        return line.startsWith(" ") || line.startsWith("\t");
    }

    private static boolean isValidIdentifier(String value) {
        return VALID_IDENTIFIER.matcher(value).matches();
    }

    /**
     * When {@link #parseFile(InputStream, ProfileFile.Type)} is invoked, this is used to track the state of the parser as it
     * reads the input stream.
     */
    private static final class ParserState {
        /**
         * The type of file being parsed.
         */
        private final ProfileFile.Type fileType;

        /**
         * The line currently being parsed. Useful for error messages.
         */
        private int currentLineNumber = 0;

        /**
         * Which profile is currently being read. Updated after each [profile] has been successfully read.
         *
         * Properties read will be added to this profile.
         */
        private String currentProfileBeingRead = null;

        /**
         * Which property is currently being read. Updated after each foo = bar has been successfully read.
         *
         * Property continuations read will be appended to this property.
         */
        private String currentPropertyBeingRead = null;

        /**
         * Whether we are ignoring the current profile. Updated after a [profile] has been identified as being invalid, but we
         * do not want to fail parsing the whole file.
         *
         * All lines other than property definitions read while this is true are dropped.
         */
        private boolean ignoringCurrentProfile = false;

        /**
         * Whether we are ignoring the current property. Updated after a foo = bar has been identified as being invalid, but we
         * do not want to fail parsing the whole file.
         *
         * All property continuations read while this are true are dropped.
         */
        private boolean ignoringCurrentProperty = false;

        /**
         * Whether we are validating the current property value continuations are formatted like sub-properties. This will ensure
         * that when the same file is used with the CLI, it won't cause failures.
         */
        private boolean validatingContinuationsAsSubProperties = false;

        /**
         * Whether within the current file, we've seen the [profile default] profile definition. This is only used for
         * configuration files. If this is true we'll ignore [default] profile definitions, because [profile default] takes
         * precedence.
         */
        private boolean seenDefaultProfileWithProfilePrefix = false;

        /**
         * The profiles read so far by the parser.
         */
        private Map<String, Map<String, String>> profiles = new LinkedHashMap<>();

        private ParserState(ProfileFile.Type fileType) {
            this.fileType = fileType;
        }
    }
}
