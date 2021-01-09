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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * A base class for {@link SdkBytes} and {@link ResponseBytes} that enables retrieving an underlying byte array as multiple
 * different types, like a byte buffer (via {@link #asByteBuffer()}, or a string (via {@link #asUtf8String()}.
 */
@SdkPublicApi
public abstract class BytesWrapper {
    private final byte[] bytes;

    // Needed for serialization
    @SdkInternalApi
    BytesWrapper() {
        this(new byte[0]);
    }

    @SdkInternalApi
    BytesWrapper(byte[] bytes) {
        this.bytes = Validate.paramNotNull(bytes, "bytes");
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
     * @return The output as a byte array. This <b>does not</b> create a copy of the underlying byte array. This introduces
     * concurrency risks, allowing: (1) the caller to modify the byte array stored in this object implementation AND
     * (2) the original creator of this object, if they created it using the unsafe method.
     *
     * <p>Consider using {@link #asByteBuffer()}, which is a safer method to avoid an additional array copy because it does not
     * provide a way to modify the underlying buffer. As the method name implies, this is unsafe. If you're not sure, don't use
     * this. The only guarantees given to the user of this method is that the SDK itself won't modify the underlying byte
     * array.</p>
     *
     * @see #asByteBuffer() to prevent creating an additional array copy safely.
     */
    public final byte[] asByteArrayUnsafe() {
        return bytes;
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

    /**
     * @return The output as a {@link ContentStreamProvider}.
     */
    public final ContentStreamProvider asContentStreamProvider() {
        return this::asInputStream;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BytesWrapper sdkBytes = (BytesWrapper) o;

        return Arrays.equals(bytes, sdkBytes.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
