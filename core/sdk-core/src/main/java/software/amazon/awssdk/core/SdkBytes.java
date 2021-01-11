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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An in-memory representation of data being given to a service or being returned by a service.
 *
 * This can be created via static methods, like {@link SdkBytes#fromByteArray(byte[])}. This can be converted to binary types
 * via instance methods, like {@link SdkBytes#asByteArray()}.
 */
@SdkPublicApi
public final class SdkBytes extends BytesWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    // Needed for serialization
    private SdkBytes() {
        super();
    }

    /**
     * @see #fromByteArray(byte[])
     * @see #fromByteBuffer(ByteBuffer)
     * @see #fromInputStream(InputStream)
     * @see #fromUtf8String(String)
     * @see #fromString(String, Charset)
     */
    private SdkBytes(byte[] bytes) {
        super(bytes);
    }

    /**
     * Create {@link SdkBytes} from a Byte buffer. This will read the remaining contents of the byte buffer.
     */
    public static SdkBytes fromByteBuffer(ByteBuffer byteBuffer) {
        Validate.paramNotNull(byteBuffer, "byteBuffer");
        return new SdkBytes(BinaryUtils.copyBytesFrom(byteBuffer));
    }

    /**
     * Create {@link SdkBytes} from a Byte array. This will copy the contents of the byte array.
     */
    public static SdkBytes fromByteArray(byte[] bytes) {
        Validate.paramNotNull(bytes, "bytes");
        return new SdkBytes(Arrays.copyOf(bytes, bytes.length));
    }

    /**
     * Create {@link SdkBytes} from a Byte array <b>without</b> copying the contents of the byte array. This introduces
     * concurrency risks, allowing: (1) the caller to modify the byte array stored in this {@code SdkBytes} implementation AND
     * (2) any users of {@link #asByteArrayUnsafe()} to modify the byte array passed into this {@code SdkBytes} implementation.
     *
     * <p>As the method name implies, this is unsafe. Use {@link #fromByteArray(byte[])} unless you're sure you know the risks.
     */
    public static SdkBytes fromByteArrayUnsafe(byte[] bytes) {
        Validate.paramNotNull(bytes, "bytes");
        return new SdkBytes(bytes);
    }

    /**
     * Create {@link SdkBytes} from a string, using the provided charset.
     */
    public static SdkBytes fromString(String string, Charset charset) {
        Validate.paramNotNull(string, "string");
        Validate.paramNotNull(charset, "charset");
        return new SdkBytes(string.getBytes(charset));
    }

    /**
     * Create {@link SdkBytes} from a string, using the UTF-8 charset.
     */
    public static SdkBytes fromUtf8String(String string) {
        return fromString(string, StandardCharsets.UTF_8);
    }

    /**
     * Create {@link SdkBytes} from an input stream. This will read all of the remaining contents of the stream, but will not
     * close it.
     */
    public static SdkBytes fromInputStream(InputStream inputStream) {
        Validate.paramNotNull(inputStream, "inputStream");
        return new SdkBytes(invokeSafely(() -> IoUtils.toByteArray(inputStream)));
    }

    @Override
    public String toString() {
        return ToString.builder("SdkBytes")
                       .add("bytes", asByteArrayUnsafe())
                       .build();
    }
}
