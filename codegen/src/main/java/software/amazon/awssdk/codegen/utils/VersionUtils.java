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

package software.amazon.awssdk.codegen.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class VersionUtils {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(.*)");

    private VersionUtils() {
    }

    /**
     * Converts a full version string to a major.minor.x format.
     *
     * @param version The full version string to convert (e.g., "2.32.1")
     * @return The version string in major.minor.x format (e.g., "2.32.x"),
     * or the original string if it doesn't match the expected version pattern
     */
    public static String convertToMajorMinorX(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);

        if (matcher.matches()) {
            String major = matcher.group(1);
            String minor = matcher.group(2);
            String suffix = matcher.group(4);

            return major + "." + minor + ".x" + suffix;
        }

        return version;
    }

}
