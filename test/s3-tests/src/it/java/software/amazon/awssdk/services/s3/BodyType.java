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

package software.amazon.awssdk.services.s3;

public enum BodyType {
    INPUTSTREAM_RESETABLE,
    INPUTSTREAM_NOT_RESETABLE,

    STRING,

    FILE,

    CONTENT_PROVIDER_WITH_LENGTH,
    CONTENT_PROVIDER_NO_LENGTH,

    BYTES,
    BYTE_BUFFER,
    REMAINING_BYTE_BUFFER,

    BYTES_UNSAFE,
    BYTE_BUFFER_UNSAFE,
    REMAINING_BYTE_BUFFER_UNSAFE,

    BUFFERS,
    BUFFERS_REMAINING,
    BUFFERS_UNSAFE,
    BUFFERS_REMAINING_UNSAFE,

    BLOCKING_INPUT_STREAM,
    BLOCKING_OUTPUT_STREAM
}
