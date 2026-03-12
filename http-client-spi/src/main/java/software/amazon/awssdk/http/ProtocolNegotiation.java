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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The protocol negotiation selection scheme used by the HTTP client to establish connections
 */
@SdkPublicApi
public enum ProtocolNegotiation {

    /**
     * Uses prior knowledge
     */
    ASSUME_PROTOCOL,

    /**
     * Uses Application Level Protocol Negotiation
     */
    ALPN
}
