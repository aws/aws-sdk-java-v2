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

package software.amazon.awssdk.codegen.internal;

import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.utils.MapUtils;

/**
 * Enum that maps protocol to metadata attribute constants for a given operation.
 */
public enum ProtocolMetadataDefault {

    SMITHY_RPC_V2_CBOR(Protocol.SMITHY_RPC_V2_CBOR) {
        public ProtocolMetadataConstants protocolMetadata(ShapeMarshaller shapeMarshaller) {
            ProtocolMetadataConstants attributes = new DefaultProtocolMetadataConstants();

            // Smithy RPCv2 requires the header "smithy-protocol" with value "rpc-v2-cbor"
            // See https://smithy.io/2.0/additional-specs/protocols/smithy-rpc-v2.html#requests.
            attributes.put(BaseAwsJsonProtocolFactory.class,
                           BaseAwsJsonProtocolFactory.HTTP_EXTRA_HEADERS,
                           MapUtils.of("smithy-protocol", "rpc-v2-cbor"));

            // If the shape is synthetic that means that no-input was defined in the model. For this
            // case the protocol requires to send an empty body with no content-type. See
            // https://smithy.io/2.0/additional-specs/protocols/smithy-rpc-v2.html#requests.
            // To accomplish this we use a no-op JSON generator. Otherwise, we serialize the input
            // even when no members are defined.
            attributes.put(BaseAwsJsonProtocolFactory.class,
                           BaseAwsJsonProtocolFactory.USE_NO_OP_GENERATOR,
                           shapeMarshaller.getIsSynthetic());
            return attributes;
        }
    },
    DEFAULT(null);

    private final Protocol protocol;

    ProtocolMetadataDefault(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * Returns a function that maps from a {@link ShapeMarshaller} to a set of protocol metadata constants that we codegen.
     */
    public ProtocolMetadataConstants protocolMetadata(ShapeMarshaller shapeMarshaller) {
        return new DefaultProtocolMetadataConstants();
    }

    public static ProtocolMetadataDefault from(Protocol protocol) {
        for (ProtocolMetadataDefault value : values()) {
            if (value.protocol == protocol) {
                return value;
            }
        }
        return DEFAULT;
    }
}
