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

package software.amazon.awssdk.utils;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.internal.Base16Lower;

/**
 * Utilities for encoding and decoding binary data to and from different forms.
 */
@SdkProtectedApi
public final class BinaryUtils {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private BinaryUtils() {
    }

    /**
     * Converts byte data to a Hex-encoded string in lower case.
     *
     * @param data
     *            data to hex encode.
     *
     * @return hex-encoded string.
     */
    public static String toHex(byte[] data) {
        return Base16Lower.encodeAsString(data);
    }

    /**
     * Converts a Hex-encoded data string to the original byte data.
     *
     * @param hexData
     *            hex-encoded data to decode.
     * @return decoded data from the hex string.
     */
    public static byte[] fromHex(String hexData) {
        return Base16Lower.decode(hexData);
    }

    /**
     * Converts byte data to a Base64-encoded string.
     * @param data
     *
     *            data to Base64 encode.
     * @return encoded Base64 string.
     */
    public static String toBase64(byte[] data) {
        return data == null ? null : new String(toBase64Bytes(data), StandardCharsets.UTF_8);
    }

    /**
     * Converts byte data to a Base64-encoded string.
     * @param data
     *
     *            data to Base64 encode.
     * @return encoded Base64 string.
     */
    public static byte[] toBase64Bytes(byte[] data) {
        return data == null ? null : Base64.getEncoder().encode(data);
    }

    /**
     * Converts a Base64-encoded string to the original byte data.
     *
     * @param b64Data
     *            a Base64-encoded string to decode.
     *
     * @return bytes decoded from a Base64 string.
     */
    public static byte[] fromBase64(String b64Data) {
        return b64Data == null ? null : Base64.getDecoder().decode(b64Data);
    }

    /**
     * Converts a Base64-encoded string to the original byte data.
     *
     * @param b64Data
     *            a Base64-encoded string to decode.
     *
     * @return bytes decoded from a Base64 string.
     */
    public static byte[] fromBase64Bytes(byte[] b64Data) {
        return b64Data == null ? null : Base64.getDecoder().decode(b64Data);
    }

    /**
     * Wraps a ByteBuffer in an InputStream. If the input {@code byteBuffer}
     * is null, returns an empty stream.
     *
     * @param byteBuffer The ByteBuffer to wrap.
     *
     * @return An InputStream wrapping the ByteBuffer content.
     */
    public static ByteArrayInputStream toStream(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return new ByteArrayInputStream(copyBytesFrom(byteBuffer));
    }

    /**
     * Returns a copy of all the bytes from the given <code>ByteBuffer</code>,
     * from the beginning to the buffer's limit; or null if the input is null.
     * <p>
     * The internal states of the given byte buffer will be restored when this
     * method completes execution.
     * <p>
     * When handling <code>ByteBuffer</code> from user's input, it's typical to
     * call the {@link #copyBytesFrom(ByteBuffer)} instead of
     * {@link #copyAllBytesFrom(ByteBuffer)} so as to account for the position
     * of the input <code>ByteBuffer</code>. The opposite is typically true,
     * however, when handling <code>ByteBuffer</code> from withint the
     * unmarshallers of the low-level clients.
     */
    public static byte[] copyAllBytesFrom(ByteBuffer bb) {
        if (bb == null) {
            return null;
        }

        if (bb.hasArray()) {
            return Arrays.copyOfRange(
                    bb.array(),
                    bb.arrayOffset(),
                    bb.arrayOffset() + bb.limit());
        }

        ByteBuffer copy = bb.asReadOnlyBuffer();
        copy.rewind();

        byte[] dst = new byte[copy.remaining()];
        copy.get(dst);
        return dst;
    }

    public static byte[] copyRemainingBytesFrom(ByteBuffer bb) {
        if (bb == null) {
            return null;
        }

        if (!bb.hasRemaining()) {
            return EMPTY_BYTE_ARRAY;
        }

        if (bb.hasArray()) {
            int endIdx = bb.arrayOffset() + bb.limit();
            int startIdx = endIdx - bb.remaining();
            return Arrays.copyOfRange(bb.array(), startIdx, endIdx);
        }

        ByteBuffer copy = bb.asReadOnlyBuffer();

        byte[] dst = new byte[copy.remaining()];
        copy.get(dst);

        return dst;
    }

    /**
     * Returns a copy of the bytes from the given <code>ByteBuffer</code>,
     * ranging from the the buffer's current position to the buffer's limit; or
     * null if the input is null.
     * <p>
     * The internal states of the given byte buffer will be restored when this
     * method completes execution.
     * <p>
     * When handling <code>ByteBuffer</code> from user's input, it's typical to
     * call the {@link #copyBytesFrom(ByteBuffer)} instead of
     * {@link #copyAllBytesFrom(ByteBuffer)} so as to account for the position
     * of the input <code>ByteBuffer</code>. The opposite is typically true,
     * however, when handling <code>ByteBuffer</code> from withint the
     * unmarshallers of the low-level clients.
     */
    public static byte[] copyBytesFrom(ByteBuffer bb) {
        if (bb == null) {
            return null;
        }

        if (bb.hasArray()) {
            return Arrays.copyOfRange(
                    bb.array(),
                    bb.arrayOffset() + bb.position(),
                    bb.arrayOffset() + bb.limit());
        }

        byte[] dst = new byte[bb.remaining()];
        bb.asReadOnlyBuffer().get(dst);
        return dst;
    }

}
