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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MessageTest {
    @Test
    public void emptyVector() {
        Message message = new Message(emptyMap(), new byte[]{});
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.encode(baos);
        byte[] bytes = baos.toByteArray();

        byte[] expected = new byte[]{
            0, 0, 0, 16,       // total_length
            0, 0, 0, 0,        // headers_length
            5, -62, 72, -21,   // prelude_crc
            125, -104, -56, -1 // message_crc
        };

        assertArrayEquals(expected, bytes);

        assertEquals(message, Message.decode(ByteBuffer.wrap(expected)));
    }

    @Test
    public void appdataVector() {
        byte[] bytes = new byte[]{
            0, 0, 0, 0x1d,                              // total_length
            0, 0, 0, 0,                                 // headers_length
            (byte) 0xfd, 0x52, (byte) 0x8c, 0x5a,       // prelude_crc
            0x7b, 0x27, 0x66, 0x6f, 0x6f, 0x27, 0x3a,   // payload
            0x27, 0x62, 0x61, 0x72, 0x27, 0x7d,
            // 0xc3 65 39 36
            (byte) 0xc3, 0x65, 0x39, 0x36               // message_crc
        };

        Message message = new Message(emptyMap(), "{'foo':'bar'}".getBytes(UTF_8));

        assertEquals(message, Message.decode(ByteBuffer.wrap(bytes)));
    }

    @Test
    public void roundTripTests() {
        roundTrip(new Message(emptyMap(), new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));

        Map<String, HeaderValue> headers = new HashMap<>();
        headers.put(":content-type", HeaderValue.fromString("application/json"));
        headers.put("content-encoding", HeaderValue.fromString("gzip"));
        headers.put("request-id", HeaderValue.fromByteArray(new byte[]{ 1, 2, 3, 4, 5 }));
        headers.put("more-stuff", HeaderValue.fromInteger(27));
        roundTrip(new Message(headers, new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
    }

    @Test
    public void generativeTest() throws Exception {
        long SEED = 8912374098123423L;
        TestUtils utils = new TestUtils(SEED);
        Random rand = new Random(SEED);
        for (int i = 0; i < 10_000; i++) {
            byte[] padding = new byte[rand.nextInt(128 * 1024)];
            rand.nextBytes(padding);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(padding);
            Message message = utils.randomMessage();
            message.encode(baos);

            int arraylen = 256 * 1024;
            int arrayoffset = rand.nextInt(4 * 1024);
            ByteBuffer buf = ((ByteBuffer) ByteBuffer.allocate(arraylen).position(arrayoffset)).slice();
            buf.put(baos.toByteArray());
            buf.flip();

            buf.get(padding);
            Message decoded = Message.decode(buf);

            assertEquals(message, decoded);
        }
    }

    static void roundTrip(Message expected) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.encode(baos);
        Message actual = Message.decode(ByteBuffer.wrap(baos.toByteArray()));
        assertEquals(expected, actual);
    }
}
