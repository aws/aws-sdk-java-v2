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

package software.amazon.awssdk.services.cloudfront.internal.auth;

import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public enum PemObjectType {
    PRIVATE_KEY_PKCS1("-----BEGIN RSA PRIVATE KEY-----"),
    PRIVATE_KEY_PKCS8("-----BEGIN PRIVATE KEY-----"),
    PUBLIC_KEY_X509("-----BEGIN PUBLIC KEY-----"),
    CERTIFICATE_X509("-----BEGIN CERTIFICATE-----")
    ;
    private final String beginMarker;

    PemObjectType(String beginMarker) {
        this.beginMarker = beginMarker;
    }

    public String getBeginMarker() {
        return beginMarker;
    }

    public static PemObjectType fromBeginMarker(String beginMarker) {
        return Arrays.stream(values()).filter(e -> e.getBeginMarker().equals(beginMarker)).findFirst().orElse(null);
    }
}
