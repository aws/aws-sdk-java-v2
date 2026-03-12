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

package software.amazon.awssdk.protocols.json.internal;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.utils.MapUtils;

/**
 * Represents know static facts about each protocol.
 */
@SdkInternalApi
public interface ProtocolFact {

    /**
     * Defaults used by all protocols that do not have overrides.
     */
    ProtocolFact DEFAULT = new ProtocolFact() {};

    /**
     * Overrides for AWS JSON.
     */
    ProtocolFact AWS_JSON = new ProtocolFact() {

        /**
         * AWS JSON always generates body.
         */
        @Override
        public boolean generatesBody(OperationInfo info) {
            return true;
        }
    };

    /**
     * Overrides for Smithy RPCv2.
     */
    ProtocolFact SMITHY_RPC_V2_CBOR = new ProtocolFact() {
        private final Map<String, String> extraHeaders = Collections.unmodifiableMap(MapUtils.of("smithy-protocol",
                                                                                                 "rpc-v2-cbor",
                                                                                                 "Accept",
                                                                                                 "application/cbor"));
        /**
         * Smithy RPCv2 only skips body generation for operation without input defined. These operations mark themselves using
         * the {@link BaseAwsJsonProtocolFactory#GENERATES_BODY} metadata attribute. Otherwise, the protocol default is to
         * generate a body for the operation.
         */
        @Override
        public boolean generatesBody(OperationInfo info) {
            return true;
        }

        /**
         * Smithy RPCv2 always sends a header with key "smithy-protocol" and value "rpc-v2-cbor".
         */
        @Override
        public Map<String, String> extraHeaders() {
            return extraHeaders;
        }
    };

    /**
     * Returns true if the operation generates a body, false otherwise. By default, this depends on whether the operation input
     * has members bound to the payload.
     */
    default boolean generatesBody(OperationInfo info) {
        return info.hasPayloadMembers();
    }

    /**
     * Returns a configured set of headers to be added to each request of the protocol.
     */
    default Map<String, String> extraHeaders() {
        return Collections.emptyMap();
    }

    /**
     * Returns the object representing a collection of facts for each protocol.
     */
    static ProtocolFact from(AwsJsonProtocol protocol) {
        if (protocol == null) {
            return DEFAULT;
        }
        switch (protocol) {
            case SMITHY_RPC_V2_CBOR:
                return SMITHY_RPC_V2_CBOR;
            case AWS_JSON:
                return AWS_JSON;
            default:
                return DEFAULT;
        }
    }
}
