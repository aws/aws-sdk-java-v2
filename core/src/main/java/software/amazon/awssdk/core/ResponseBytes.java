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

package software.amazon.awssdk.core;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;

/**
 * An in-memory representation of the service's response from a streaming operation. This usually obtained by calling the "bytes"
 * form of a streaming operation, like S3's {@code getObjectBytes}. Can also be retrieved by passing
 * {@link ResponseTransformer#toBytes()} or {@link AsyncResponseTransformer#toBytes()} to a streaming output operation.
 */
@SdkPublicApi
public final class ResponseBytes<ResponseT> {
    private final ResponseT response;
    private final byte[] responseBytes;

    public ResponseBytes(ResponseT response, byte[] responseBytes) {
        this.response = response;
        this.responseBytes = responseBytes;
    }

    /**
     * @return the unmarshalled response object from the service.
     */
    public ResponseT response() {
        return response;
    }

    /**
     * @return The output from the streaming operation as a read-only byte buffer.
     */
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(responseBytes).asReadOnlyBuffer();
    }

    /**
     * @return A copy of the output from the streaming operation as a byte array.
     * @see #asByteBuffer() to prevent creating an additional array copy.
     */
    public byte[] asByteArray() {
        return Arrays.copyOf(responseBytes, responseBytes.length);
    }

    /**
     * Retrieve the output of the streaming operation as a string.
     *
     * @param charset The charset of the string.
     * @return The output from the streaming operation as a string.
     */
    public String asString(Charset charset) {
        return new String(responseBytes, charset);
    }

    /**
     * @return The output from the streaming operation as a utf-8 encoded string.
     */
    public String asUtf8String() {
        return asString(StandardCharsets.UTF_8);
    }
}
