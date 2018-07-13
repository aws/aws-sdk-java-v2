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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
abstract class BytesWrapper {
    private final byte[] bytes;

    BytesWrapper(byte[] bytes) {
        this.bytes = Validate.paramNotNull(bytes, "bytes");
    }

    final byte[] wrappedBytes() {
        return bytes;
    }

    /**
     * @return The output as a read-only byte buffer.
     */
    public final ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(bytes).asReadOnlyBuffer();
    }

    /**
     * @return A copy of the output as a byte array.
     * @see #asByteBuffer() to prevent creating an additional array copy.
     */
    public final byte[] asByteArray() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Retrieve the output as a string.
     *
     * @param charset The charset of the string.
     * @return The output as a string.
     * @throws UncheckedIOException with a {@link CharacterCodingException} as the cause if the bytes cannot be encoded using the
     * provided charset
     */
    public final String asString(Charset charset) throws UncheckedIOException {
        return StringUtils.fromBytes(bytes, charset);
    }

    /**
     * @return The output as a utf-8 encoded string.
     * @throws UncheckedIOException with a {@link CharacterCodingException} as the cause if the bytes cannot be encoded as UTF-8.
     */
    public final String asUtf8String() throws UncheckedIOException {
        return asString(UTF_8);
    }

    /**
     * @return The output as an input stream. This stream will not need to be closed.
     */
    public final InputStream asInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BytesWrapper sdkBytes = (BytesWrapper) o;

        return Arrays.equals(bytes, sdkBytes.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
