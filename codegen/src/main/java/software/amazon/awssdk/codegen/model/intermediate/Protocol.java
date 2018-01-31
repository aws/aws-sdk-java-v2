/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.codegen.protocol.ApiGatewayProtocolMetadataProvider;
import software.amazon.awssdk.codegen.protocol.AwsCborProtocolMetadataProvider;
import software.amazon.awssdk.codegen.protocol.AwsJsonProtocolMetadataProvider;
import software.amazon.awssdk.codegen.protocol.Ec2ProtocolMetadataProvider;
import software.amazon.awssdk.codegen.protocol.IonProtocolMetadataProvider;
import software.amazon.awssdk.codegen.protocol.ProtocolMetadataProvider;
import software.amazon.awssdk.codegen.protocol.QueryProtocolMetadataProvider;
import software.amazon.awssdk.codegen.protocol.RestJsonProtocolMetadataProvider;
import software.amazon.awssdk.codegen.protocol.RestXmlProtocolMetadataProvider;

public enum Protocol {
    EC2("ec2", new Ec2ProtocolMetadataProvider()),
    AWS_JSON("json", new AwsJsonProtocolMetadataProvider()),
    REST_JSON("rest-json", new RestJsonProtocolMetadataProvider()),
    CBOR("cbor", new AwsCborProtocolMetadataProvider()),
    QUERY("query", new QueryProtocolMetadataProvider()),
    REST_XML("rest-xml", new RestXmlProtocolMetadataProvider()),
    API_GATEWAY("api-gateway", new ApiGatewayProtocolMetadataProvider()),
    ION("ion", new IonProtocolMetadataProvider());

    private String protocol;
    private ProtocolMetadataProvider metadataProvider;

    Protocol(String protocol, ProtocolMetadataProvider metadataProvider) {
        this.protocol = protocol;
        this.metadataProvider = metadataProvider;
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

    public ProtocolMetadataProvider getProvider() {
        return metadataProvider;
    }
}
