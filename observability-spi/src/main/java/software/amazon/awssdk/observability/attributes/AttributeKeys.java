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

package software.amazon.awssdk.observability.attributes;

import java.util.List;

public enum AttributeKeys {
    // General Attributes
    ERROR("error", Boolean.class),
    EXCEPTION_MESSAGE("exception.message", String.class),
    EXCEPTION_STACKTRACE("exception.stacktrace", String.class),
    EXCEPTION_TYPE("exception.type", String.class),

    // RPC Attributes
    RPC_SYSTEM("rpc.system", String.class),
    RPC_METHOD("rpc.method", String.class),
    RPC_SERVICE("rpc.service", String.class),

    // AWS Specific
    AWS_REQUEST_ID("aws.request_id", String.class),
    AWS_EXTENDED_REQUEST_ID("aws.extended_request_id", String.class),
    AWS_ERROR_CODE("aws.error_code", String.class),

    // Thread Info
    THREAD_ID("thread.id", Long.class),
    THREAD_NAME("thread.name", String.class),

    // Code Location
    CODE_FUNCTION("code.function", String.class),
    CODE_NAMESPACE("code.namespace", String.class),

    // HTTP Attributes
    HTTP_STATUS_CODE("http.status_code", Long.class),
    HTTP_REQUEST_CONTENT_LENGTH("http.request_content_length", Long.class),
    HTTP_RESPONSE_CONTENT_LENGTH("http.response_content_length", Long.class),
    HTTP_METHOD("http.method", String.class),

    // Network Attributes
    NET_PROTOCOL_NAME("net.protocol.name", String.class),
    NET_PROTOCOL_VERSION("net.protocol.version", String.class),
    NET_SOCK_FAMILY("net.sock.family", String.class),
    NET_PEER_IP("net.peer.ip", String.class),
    NET_PEER_NAME("net.peer.name", String.class),
    NET_PEER_PORT("net.peer.port", String.class),

    // Example of plural array attribute
    PROCESS_COMMAND_ARGS("process.command_args", List.class);

    private final String key;
    private final Class<?> valueType;

    AttributeKeys(String key, Class<?> valueType) {
        this.key = key;
        this.valueType = valueType;
    }

    public String key() {
        return key;
    }

    public Class<?> valueType() {
        return valueType;
    }

    @Override
    public String toString() {
        return key;
    }
}