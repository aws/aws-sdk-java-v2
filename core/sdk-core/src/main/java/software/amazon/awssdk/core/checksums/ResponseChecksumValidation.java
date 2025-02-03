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

package software.amazon.awssdk.core.checksums;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Enum class for response checksum validation setting.
 */
@SdkPublicApi
public enum ResponseChecksumValidation {

    /**
     * Checksum will be validated for response if supported. Default setting.
     */
    WHEN_SUPPORTED,

    /**
     * Checksum will only be validated for response if required.
     */
    WHEN_REQUIRED,
    ;

    /**
     * Returns the appropriate ResponseChecksumValidation value after parsing the parameter.
     * @param s ResponseChecksumValidation in String format.
     * @return ResponseChecksumValidation enum value.
     * @throws IllegalArgumentException Unrecognized value for response checksum validation.
     */
    public static ResponseChecksumValidation fromValue(String s) {
        if (s == null) {
            return null;
        }

        for (ResponseChecksumValidation value : values()) {
            if (value.name().equalsIgnoreCase(s)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unrecognized value for response checksum validation: " + s + "\n Valid values are: "
                                           + "when_supported and when_required");
    }
}
