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

package software.amazon.awssdk.profiles;

import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Logger;

/**
 * A collection of static methods for loading the location for configuration and credentials files.
 */
@SdkPublicApi
public final class ProfileFileLocation {

    private static final Logger LOG = Logger.loggerFor(ProfileFileLocation.class);

    private static final Pattern HOME_DIRECTORY_PATTERN =
        Pattern.compile("^~(/|" + Pattern.quote(FileSystems.getDefault().getSeparator()) + ").*$");

    private ProfileFileLocation() {
    }


    /**
     * Resolve the path for the configuration file, regardless of whether it exists or not.
     *
     * @see #configurationFileLocation()
     */
    public static Path configurationFilePath() {
        return resolveProfileFilePath(
            ProfileFileSystemSetting.AWS_CONFIG_FILE.getStringValue()
                                                    .orElseGet(() -> Paths.get(userHomeDirectory(),
                                                                               ".aws", "config").toString()));
    }

    /**
     * Resolve the location for the credentials file, regardless of whether it exists or not.
     *
     * @see #credentialsFileLocation()
     */
    public static Path credentialsFilePath() {
        return resolveProfileFilePath(
            ProfileFileSystemSetting.AWS_SHARED_CREDENTIALS_FILE.getStringValue()
                                                                .orElseGet(() -> Paths.get(userHomeDirectory(),
                                                                                           ".aws", "credentials").toString()));
    }

    /**
     * Load the location for the configuration file, usually ~/.aws/config unless it's overridden using an environment variable
     * or system property.
     */
    public static Optional<Path> configurationFileLocation() {
        return resolveIfExists(configurationFilePath());
    }

    /**
     * Load the location for the credentials file, usually ~/.aws/credentials unless it's overridden using an environment variable
     * or system property.
     */
    public static Optional<Path> credentialsFileLocation() {
        return resolveIfExists(credentialsFilePath());
    }

    private static Path resolveProfileFilePath(String path) {
        // Resolve ~ using the CLI's logic, not whatever Java decides to do with it.
        if (HOME_DIRECTORY_PATTERN.matcher(path).matches()) {
            path = userHomeDirectory() + path.substring(1);
        }

        return Paths.get(path);
    }

    private static Optional<Path> resolveIfExists(Path path) {
        return Optional.ofNullable(path).filter(ProfileFileLocation::isReadableRegularFile);
    }

    private static boolean isReadableRegularFile(Path path) {
        try {
            return Files.isRegularFile(path) && Files.isReadable(path);
        } catch (SecurityException e) {
            // Treats SecurityExceptions from JVM as non-existent file.
            LOG.debug(() -> String.format("Security restrictions prevented access to profile file: %s", e.getMessage()), e);
            return false;
        }
    }
}
