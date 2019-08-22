/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.progress;


import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Byte counting progress events
 */
@SdkPublicApi
public enum ByteCountEventType implements ProgressEventType {
    /**
     * Event of the content length to be sent in a request.
     */
    REQUEST_CONTENT_LENGTH_EVENT,
    /**
     * Event of the content length received in a response.
     */
    RESPONSE_CONTENT_LENGTH_EVENT,

    /**
     * Used to indicate the number of bytes to be sent to the services.
     */
    REQUEST_BYTE_TRANSFER_EVENT,

    /**
     * Used to indicate the number of bytes received from services.
     */
    RESPONSE_BYTE_TRANSFER_EVENT
}
