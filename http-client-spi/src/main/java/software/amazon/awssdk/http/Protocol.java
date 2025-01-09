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

package software.amazon.awssdk.http;

public enum Protocol {

    /**
     * Uses HTTP/1.1 with prior knowledge.
     */
    HTTP1_1,

    /**
     * Uses HTTP/2 with prior knowledge.
     */
    HTTP2,

    /**
     * Uses ALPN (Application-Layer Protocol Negotiation) to determine the protocol.
     * Prefers HTTP/2 over HTTP/1.1, but the server's protocol preference takes precedence.
     * <p>
     * Note: For Java 8, ALPN is only supported in versions 1.8.0_251 and newer.
     */
    ALPN_AUTO,

    /**
     * Uses ALPN (Application-Layer Protocol Negotiation) to enforce HTTP/2 only.
     * Does not allow fallback to HTTP/1.1; the connection will fail if HTTP/2 is not supported by the server.
     * <p>
     * Note: For Java 8, ALPN is only supported in versions 1.8.0_251 and newer.
     */
    ALPN_H2
}