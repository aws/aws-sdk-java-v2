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
 * Enum class for request checksum calculation setting.
 */
@SdkPublicApi
public enum RequestChecksumCalculation {

    /**
     * Checksum will be calculated for request if supported. Default setting.
     */
    WHEN_SUPPORTED,

    /**
     * Checksum will only be calculated for request if required.
     */
    WHEN_REQUIRED,
    ;

    /**
     * Returns the appropriate RequestChecksumCalculation value after parsing the parameter.
     * @param s RequestChecksumCalculation in String format.
     * @return RequestChecksumCalculation enum value.
     * @throws IllegalArgumentException Unrecognized value for request checksum calculation.
     */
    public static RequestChecksumCalculation fromValue(String s) {
        if (s == null) {
            return null;
        }

        for (RequestChecksumCalculation value : values()) {
            if (value.name().equalsIgnoreCase(s)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unrecognized value for request checksum calculation: " + s + "\n Valid values are: "
                                           + "when_supported and when_required");
    }
}
