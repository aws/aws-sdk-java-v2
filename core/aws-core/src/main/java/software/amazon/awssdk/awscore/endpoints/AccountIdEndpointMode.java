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

package software.amazon.awssdk.awscore.endpoints;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Enum Class for AccountId Endpoint Mode.
 */
@SdkPublicApi
public enum AccountIdEndpointMode {

    /**
     * Default value that indicates account ID values will be used in endpoint rules if available.
     */
    PREFERRED,

    /**
     * When mode is disabled, any resolved account ID will not be used in endpoint construction and rules that
     * reference them will be bypassed.
     */
    DISABLED,

    /**
     * Required mode would be used in scenarios where endpoint resolution should return an error if no account ID is
     * available.
     */
    REQUIRED;

    /**
     * Returns the appropriate AccountIdEndpointMode value after parsing the parameter.
     * @param s AccountIdEndpointMode in String Format.
     * @return AccountIdEndpointMode enumValue
     * @throws IllegalArgumentException Unrecognized value for endpoint mode.
     */
    public static AccountIdEndpointMode fromValue(String s) {
        if (s == null) {
            return null;
        }

        for (AccountIdEndpointMode value : values()) {
            if (value.name().equalsIgnoreCase(s)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unrecognized value for account id endpoint mode: " + s);
    }
}
