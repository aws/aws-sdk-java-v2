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

package software.amazon.awssdk.messagemanager.sns.model;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The signature version used to sign an SNS message.
 */
@SdkPublicApi
public enum SignatureVersion {
    VERSION_1("1"),
    VERSION_2("2"),
    UNKNOWN(null)
    ;

    private final String value;

    SignatureVersion(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static SignatureVersion fromValue(String value) {
        for (SignatureVersion v : values()) {
            if (Objects.equals(v.value, value)) {
                return v;
            }
        }

        return UNKNOWN;
    }
}
