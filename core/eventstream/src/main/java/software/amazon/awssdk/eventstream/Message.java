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

package software.amazon.awssdk.eventstream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.eventstream.internal.Checksums;
import software.amazon.awssdk.eventstream.internal.Header;
import software.amazon.awssdk.eventstream.internal.Prelude;

/**
 * An eventstream message.
 */
@SdkProtectedApi
public class Message {
    private static final int TRAILING_CRC_LENGTH = 4;
    public static final int MESSAGE_OVERHEAD = Prelude.LENGTH_WITH_CRC + TRAILING_CRC_LENGTH;

    private final Map<String, HeaderValue> headers;
    private final byte[] payload;

    /**
     * Create a message.
     *
     * @param headers a non-null key-value map of headers, which will be encoded on the wire according to the
     *     iteration order of the given map
     * @param payload a non-null byte array containing zero or more bytes
     */
    public Message(Map<String, HeaderValue> headers, byte[] payload) {
        this(headers, payload, true);
    }

    private Message(Map<String, HeaderValue> headers, byte[] payload, boolean clonePayload) {
        this.headers = requireNonNull(headers, "headers");
        requireNonNull(payload, "payload");
        if (clonePayload) {
            this.payload = payload.clone();
        } else {
            this.payload = payload;
        }
    }

    /**
     * Returns this message's headers. If this message was obtained through {@link #decode(ByteBuffer)}, the iteration
     * order of the headers is guaranteed to match the order in which the headers appeared on the wire.
     *
     * @return an unmodifiable non-null key-value map of this message's headers
     */
    public Map<String, HeaderValue> getHeaders() {
        return headers;
    }

    /**
     * Returns this message's payload.
     *
     * @return a non-null array of zero or more bytes
     */
    public byte[] getPayload() {
        return payload.clone();
    }

    public static Message decode(ByteBuffer buf) {
        return decode(Prelude.decode(buf), buf);
    }

    /**
     * Decodes a message with an already decoded prelude. Useful for not decoding the prelude twice.
     *
     * @param prelude Decoded prelude of message.
     * @param buf Data of message (including prelude which will be skipped over).
     * @return Decoded message
     */
    static Message decode(Prelude prelude, ByteBuffer buf) {
        int totalLength = prelude.getTotalLength();
        validateMessageCrc(buf, totalLength);
        buf.position(buf.position() + Prelude.LENGTH_WITH_CRC);

        long headersLength = prelude.getHeadersLength();
        byte[] headerBytes = new byte[Math.toIntExact(headersLength)];
        buf.get(headerBytes);
        Map<String, HeaderValue> headers = decodeHeaders(ByteBuffer.wrap(headerBytes));

        byte[] payload = new byte[Math.toIntExact(totalLength - MESSAGE_OVERHEAD - headersLength)];
        buf.get(payload);
        buf.getInt(); // skip past the message CRC

        return new Message(headers, payload, false);
    }

    private static void validateMessageCrc(ByteBuffer buf, int totalLength) {
        Checksum crc = new CRC32();

        Checksums.update(crc, (ByteBuffer) buf.duplicate().limit(buf.position() + totalLength - 4));
        long computedMessageCrc = crc.getValue();

        long wireMessageCrc = Integer.toUnsignedLong(buf.getInt(buf.position() + totalLength - 4));

        if (wireMessageCrc != computedMessageCrc) {
            throw new IllegalArgumentException(format("Message checksum failure: expected 0x%x, computed 0x%x",
                wireMessageCrc, computedMessageCrc));
        }
    }

    static Map<String, HeaderValue> decodeHeaders(ByteBuffer buf) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();

        while (buf.hasRemaining()) {
            Header header = Header.decode(buf);
            headers.put(header.getName(), header.getValue());
        }

        return Collections.unmodifiableMap(headers);
    }

    public ByteBuffer toByteBuffer() {
        byte[] encodedHeaders = encodeHeaders(headers.entrySet());
        int totalLength = Prelude.LENGTH_WITH_CRC + encodedHeaders.length + payload.length + 4;
        CompactByteArrayOutputStream out = new CompactByteArrayOutputStream(totalLength);
        encode(out, encodedHeaders);
        return out.toByteBuffer();
    }

    public void encode(OutputStream os) {
        encode(os, encodeHeaders(headers.entrySet()));
    }

    private void encode(OutputStream os, byte[] encodedHeaders) {
        try {
            CheckedOutputStream checkedOutputStream = new CheckedOutputStream(os, new CRC32());
            int totalLength = Prelude.LENGTH_WITH_CRC + encodedHeaders.length + payload.length + 4;
            {
                byte[] preludeBytes = getPrelude(totalLength, encodedHeaders.length);
                Checksum crc = new CRC32();
                crc.update(preludeBytes, 0, preludeBytes.length);

                DataOutputStream dos = new DataOutputStream(checkedOutputStream);
                dos.write(preludeBytes);
                long value = crc.getValue();
                int value1 = (int) value;
                dos.writeInt(value1);
                dos.flush();
            }

            checkedOutputStream.write(encodedHeaders);
            checkedOutputStream.write(payload);
            long messageCrc = checkedOutputStream.getChecksum().getValue();
            checkedOutputStream.write((int) (0xFF & messageCrc >> 24));
            checkedOutputStream.write((int) (0xFF & messageCrc >> 16));
            checkedOutputStream.write((int) (0xFF & messageCrc >> 8));
            checkedOutputStream.write((int) (0xFF & messageCrc));

            checkedOutputStream.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Encode the given {@code headers}, without any leading or trailing metadata such as checksums or lengths.
     *
     * @param headers a sequence of zero or more headers, which will be encoded in iteration order
     * @return a byte array corresponding to the {@code headers} section of a {@code Message}
     */
    public static byte[] encodeHeaders(Iterable<Entry<String, HeaderValue>> headers) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (Entry<String, HeaderValue> entry : headers) {
                Header.encode(entry, dos);
            }
            dos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getPrelude(int totalLength, int headerLength) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(totalLength);
        dos.writeInt(headerLength);

        dos.close();
        return baos.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Message message = (Message) o;

        if (!headers.equals(message.headers)) {
            return false;
        }
        return Arrays.equals(payload, message.payload);
    }

    @Override
    public int hashCode() {
        int result = headers.hashCode();
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();

        for (Entry<String, HeaderValue> entry : headers.entrySet()) {
            ret.append(entry.getKey());
            ret.append(": ");
            ret.append(entry.getValue().toString());
            ret.append('\n');
        }
        ret.append('\n');

        String contentType = headers.getOrDefault(":content-type", HeaderValue.fromString("application/octet-stream"))
            .getString();
        if (contentType.contains("json") || contentType.contains("text")) {
            ret.append(new String(payload, StandardCharsets.UTF_8));
        } else {
            ret.append(Base64.getEncoder().encodeToString(payload));
        }
        ret.append('\n');
        return ret.toString();
    }

    /**
     * A ByteArrayOutputStream optimized for returning a ByteBuffer with low copy/allocation.
     *
     * Normally, to get a ByteBuffer from a ByteArrayOutputStream you might use:
     * ByteBuffer.wrap(ByteArrayOutputStream.toByteArray())
     * However, toByteArray() does a copy of the buffer contents for two reasons:
     * 1. Not to leak the internal buffer; could cause issues if it's used externally while the stream is in use.
     * 2. Because the buffer may have unused space at the end, which needs to be trimmed.
     *
     * These are not an issue for us, so we can avoid the wasted allocation.
     * 1. We will not use the stream after we have exposed the buffer externally.
     * 2. We will pre-size the stream to avoid unused space in the buffer
     *    (We also set the ByteBuffer limit to the used space, so incorrect pre-sizing is not unsafe, just not optimal)
     *
     */
    private static class CompactByteArrayOutputStream extends ByteArrayOutputStream {
        CompactByteArrayOutputStream(int size) {
            super(size);
        }

        ByteBuffer toByteBuffer() {
            return ByteBuffer.wrap(this.buf, 0, this.count);
        }
    }
}
