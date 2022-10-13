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

package software.amazon.awssdk.imds;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Enum Class for the Endpoint Mode.
 */
@SdkPublicApi
public enum EndpointMode {

    IPV4("http://169.254.169.254"),
    IPV6("http://[fd00:ec2::254]");

    public final String serviceEndpoint;

    EndpointMode(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public String getServiceEndpoint() {
        return this.serviceEndpoint;
    }

    /**
     * Returns the appropriate EndpointMode Value after parsing the parameter.
     * @param s EndpointMode in String Format.
     * @return EndpointMode enumValue (IPV4 or IPV6).
     * @throws IllegalArgumentException Unrecognized value for endpoint mode.
     */
    public static EndpointMode fromValue(String s) {
        if (s == null) {
            return null;
        }

        for (EndpointMode value : values()) {
            if (value.name().equalsIgnoreCase(s)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unrecognized value for endpoint mode: " + s);
    }
}
