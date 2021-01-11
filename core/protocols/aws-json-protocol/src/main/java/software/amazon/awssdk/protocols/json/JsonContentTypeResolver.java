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

package software.amazon.awssdk.protocols.json;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Interface to compute the content type to send in requests for JSON based protocols.
 */
@SdkProtectedApi
public interface JsonContentTypeResolver {

    /**
     * Computes content type to send in requests.
     *
     * @param protocolMetadata Metadata about the protocol.
     * @return Correct content type to send in request based on metadata about the client.
     */
    String resolveContentType(AwsJsonProtocolMetadata protocolMetadata);
}
