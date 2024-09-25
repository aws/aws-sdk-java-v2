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

package software.amazon.awssdk.awscore.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public enum AwsServiceProtocol {
    EC2("ec2"),
    AWS_JSON("json"),
    REST_JSON("rest-json"),
    CBOR("cbor"),
    SMITHY_RPC_V2_CBOR("smithy-rpc-v2-cbor"),
    QUERY("query"),
    REST_XML("rest-xml");

    private String protocol;

    AwsServiceProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static AwsServiceProtocol fromValue(String strProtocol) {
        if (strProtocol == null) {
            return null;
        }

        for (AwsServiceProtocol protocol : values()) {
            if (protocol.protocol.equals(strProtocol)) {
                return protocol;
            }
        }

        throw new IllegalArgumentException("Unknown enum value for Protocol : " + strProtocol);
    }

    @Override
    public String toString() {
        return protocol;
    }
}
