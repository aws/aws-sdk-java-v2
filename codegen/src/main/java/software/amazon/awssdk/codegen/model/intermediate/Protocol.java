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

package software.amazon.awssdk.codegen.model.intermediate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Protocol {
    EC2("ec2"),
    AWS_JSON("json"),
    REST_JSON("rest-json"),
    CBOR("cbor"),
    QUERY("query"),
    REST_XML("rest-xml"),
    ION("ion");

    private String protocol;

    Protocol(String protocol) {
        this.protocol = protocol;
    }

    @JsonCreator
    public static Protocol fromValue(String strProtocol) {
        if (strProtocol == null) {
            return null;
        }

        for (Protocol protocol : Protocol.values()) {
            if (protocol.protocol.equals(strProtocol)) {
                return protocol;
            }
        }

        throw new IllegalArgumentException("Unknown enum value for Protocol : " + strProtocol);
    }

    @JsonValue
    public String getValue() {
        return protocol;
    }
}
