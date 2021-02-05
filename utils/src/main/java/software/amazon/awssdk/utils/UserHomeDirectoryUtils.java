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

package software.amazon.awssdk.utils;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Load the home directory that should be used for the stored file. This will check the same environment variables as the CLI
 * to identify the location of home, before falling back to java-specific resolution.
 */
@SdkProtectedApi
public final class UserHomeDirectoryUtils {

    private UserHomeDirectoryUtils() {

    }

    public static String userHomeDirectory() {
        // To match the logic of the CLI we have to consult environment variables directly.
        // CHECKSTYLE:OFF
        String home = System.getenv("HOME");

        if (home != null) {
            return home;
        }

        boolean isWindows = JavaSystemSetting.OS_NAME.getStringValue()
                                                     .map(s -> StringUtils.lowerCase(s).startsWith("windows"))
                                                     .orElse(false);

        if (isWindows) {
            String userProfile = System.getenv("USERPROFILE");

            if (userProfile != null) {
                return userProfile;
            }

            String homeDrive = System.getenv("HOMEDRIVE");
            String homePath = System.getenv("HOMEPATH");

            if (homeDrive != null && homePath != null) {
                return homeDrive + homePath;
            }
        }

        return JavaSystemSetting.USER_HOME.getStringValueOrThrow();
        // CHECKSTYLE:ON
    }
}
