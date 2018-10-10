/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.eventstream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

import static java.lang.String.format;

/**
 * An eventstream message.
 */
public class Message {
    private static final int TRAILING_CRC_LENGTH = 4;
    static final int MESSAGE_OVERHEAD = Prelude.LENGTH_WITH_CRC + TRAILING_CRC_LENGTH;

    private final Map<String, HeaderValue> headers;
    private final byte[] payload;

    public Message(Map<String, HeaderValue> headers, byte[] payload) {
        this.headers = headers;
        this.payload = payload.clone();
    }

    public Map<String, HeaderValue> getHeaders() {
        return headers;
    }

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

        return new Message(headers, payload);
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
        Map<String, HeaderValue> headers = new HashMap<>();

        while (buf.hasRemaining()) {
            Header header = Header.decode(buf);
            headers.put(header.getName(), header.getValue());
        }

        return Collections.unmodifiableMap(headers);
    }

    public ByteBuffer toByteBuffer() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encode(baos);
            baos.close();
            return ByteBuffer.wrap(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void encode(OutputStream os) {
        try {
            CheckedOutputStream checkedOutputStream = new CheckedOutputStream(os, new CRC32());
            encodeOrThrow(checkedOutputStream);
            long messageCrc = checkedOutputStream.getChecksum().getValue();
            os.write((int) (0xFF & messageCrc >> 24));
            os.write((int) (0xFF & messageCrc >> 16));
            os.write((int) (0xFF & messageCrc >> 8));
            os.write((int) (0xFF & messageCrc));

            os.flush();
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

    private void encodeOrThrow(OutputStream os) throws IOException {
        ByteArrayOutputStream headersAndPayload = new ByteArrayOutputStream();
        headersAndPayload.write(encodeHeaders(headers.entrySet()));
        headersAndPayload.write(payload);

        int totalLength = Prelude.LENGTH_WITH_CRC + headersAndPayload.size() + 4;

        {
            byte[] preludeBytes = getPrelude(totalLength);
            Checksum crc = new CRC32();
            crc.update(preludeBytes, 0, preludeBytes.length);

            DataOutputStream dos = new DataOutputStream(os);
            dos.write(preludeBytes);
            long value = crc.getValue();
            int value1 = (int) value;
            dos.writeInt(value1);
            dos.flush();
        }

        headersAndPayload.writeTo(os);
    }

    private byte[] getPrelude(int totalLength) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
        DataOutputStream dos = new DataOutputStream(baos);

        int headerLength = totalLength - Message.MESSAGE_OVERHEAD - payload.length;
        dos.writeInt(totalLength);
        dos.writeInt(headerLength);

        dos.close();
        return baos.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (!headers.equals(message.headers)) return false;
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
}
