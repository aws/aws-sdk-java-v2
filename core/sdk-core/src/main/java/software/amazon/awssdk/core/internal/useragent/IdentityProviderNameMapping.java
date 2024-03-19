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

package software.amazon.awssdk.core.internal.useragent;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A enum class representing a short form of identity providers to record in the UA string.
 */
public enum IdentityProviderNameMapping {

    SYS("SystemPropertyCredentialsProvider"),
    ENV("EnvironmentVariableCredentialsProvider"),
    STSWEB("StsAssumeRoleWithWebIdentity"),
    STSROLE("StsAssumeRoleCredentialsProvider"),
    STSSAML("StsAssumeRoleWithWebIdentityCredentialsProvider"),
    STSFED("StsGetFederationTokenCredentialsProvider"),
    STSSESS("StsGetSessionTokenCredentialsProvider"),
    SSO("SsoCredentialsProvider"),
    PROF("ProfileCredentialsProvider"),
    CONT("ContainerCredentialsProvider"),
    IMDS("InstanceProfileCredentialsProvider"),
    STAT("StaticCredentialsProvider"),
    PROC("ProcessCredentialsProvider");

    private static final Pattern CLASS_NAME_CHARACTERS = Pattern.compile("[a-zA-Z_$\\d]{0,62}");
    private final String value;

    IdentityProviderNameMapping(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<String> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }

        for (IdentityProviderNameMapping provider : values()) {
            if (provider.value().equals(value)) {
                return Optional.of(provider.name());
            }
        }
        return Optional.ofNullable(sanitizedProviderOrNull(value));
    }

    private static String sanitizedProviderOrNull(String value) {
        if (containsAllowedCharacters(value) && value.toLowerCase(Locale.US).endsWith("provider")) {
            return value;
        }
        return null;
    }

    private static boolean containsAllowedCharacters(String input) {
        return CLASS_NAME_CHARACTERS.matcher(input).matches();
    }
}
