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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class HostnameValidator {

    private static final Pattern HOSTNAME_COMPLIANT_PATTERN = Pattern.compile("[A-Za-z0-9\\-]+");
    private static final int HOSTNAME_MAX_LENGTH = 63;

    private HostnameValidator() {
    }

    public static void validateHostnameCompliant(String hostnameComponent, String paramName, String object) {
        if (StringUtils.isEmpty(hostnameComponent)) {
            throw new IllegalArgumentException(
                String.format("The provided %s is not valid: the required '%s' "
                              + "component is missing.", object, paramName));
        }

        if (hostnameComponent.length() > HOSTNAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                String.format("The provided %s is not valid: the '%s' "
                              + "component exceeds the maximum length of %d characters.", object, paramName,
                              HOSTNAME_MAX_LENGTH));
        }

        Matcher m = HOSTNAME_COMPLIANT_PATTERN.matcher(hostnameComponent);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                String.format("The provided %s is not valid: the '%s' "
                              + "component must only contain alphanumeric characters and dashes.", object, paramName));
        }
    }
}
