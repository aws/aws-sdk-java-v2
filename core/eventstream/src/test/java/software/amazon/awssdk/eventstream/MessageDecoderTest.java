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

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class MessageDecoderTest {
    long SEED = 8912374098123423L;

    @Test
    public void testDecoder() throws Exception {
        TestUtils utils = new TestUtils(SEED);
        Random rand = new Random(SEED);
        List<Message> expected = IntStream.range(0, 10_000)
                                          .mapToObj(x -> utils.randomMessage())
                                          .collect(Collectors.toList());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.forEach(x -> x.encode(baos));
        ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());

        List<Message> actual = new ArrayList<>();
        MessageDecoder decoder = new MessageDecoder(actual::add);
        while (buf.remaining() > 0) {
            int bufSize = Math.min(1 + rand.nextInt(1024), buf.remaining());
            byte[] bs = new byte[bufSize];
            buf.get(bs);
            decoder.feed(bs);
        }

        assertEquals(expected, actual);
    }

    @Test
    public void testDecoder_WithOffset() throws Exception {
        TestUtils utils = new TestUtils(SEED);
        Random rand = new Random(SEED);
        List<Message> expected = IntStream.range(0, 10_000)
                                          .mapToObj(x -> utils.randomMessage())
                                          .collect(Collectors.toList());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.forEach(x -> x.encode(baos));
        byte[] data = baos.toByteArray();
        int toRead = data.length;
        int read = 0;

        List<Message> actual = new ArrayList<>();
        MessageDecoder decoder = new MessageDecoder(actual::add);
        while (toRead > 0) {
            int length = rand.nextInt(100);
            if (read + length > data.length) {
                length = data.length - read;
            }
            decoder.feed(data, read, length);
            read += length;
            toRead -= length;
        }

        assertEquals(expected, actual);
    }

    @Test
    public void preludeFedFirst_DecodesCorrectly() {
        TestUtils utils = new TestUtils(SEED);
        Message message = utils.randomMessage();
        int messageSize = message.toByteBuffer().remaining();
        List<Message> expected = Collections.singletonList(message);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.forEach(x -> x.encode(baos));
        ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());

        List<Message> actual = new ArrayList<>();
        MessageDecoder decoder = new MessageDecoder(actual::add, 8192);

        // Feed just the prelude in it's entirety
        byte[] bs = new byte[15];
        buf.get(bs);
        decoder.feed(bs);

        // No messages should be decoded yet
        assertEquals(emptyList(), actual);

        // Feed rest of message in it's entirety.
        bs = new byte[messageSize - 15];
        buf.get(bs);
        decoder.feed(bs);

        // Should have successfully decoded the one message
        assertEquals(1, actual.size());
    }

    @Test
    public void preludeFedInParts_DecodesCorrectly() {
        TestUtils utils = new TestUtils(SEED);
        Message message = utils.randomMessage();
        int messageSize = message.toByteBuffer().remaining();
        List<Message> expected = Collections.singletonList(message);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.forEach(x -> x.encode(baos));
        ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());

        List<Message> actual = new ArrayList<>();
        MessageDecoder decoder = new MessageDecoder(actual::add, 8192);

        // Feed the prelude in parts
        byte[] bs = new byte[7];
        buf.get(bs);
        decoder.feed(bs);

        // Feed rest of prelude
        bs = new byte[8];
        buf.get(bs);
        decoder.feed(bs);

        // No messages should be decoded yet
        assertEquals(emptyList(), actual);

        // Feed rest of message in it's entirety.
        bs = new byte[messageSize - 15];
        buf.get(bs);
        decoder.feed(bs);

        // Should have successfully decoded the one message
        assertEquals(1, actual.size());
    }

    @Test
    public void bufferNeedsToGrow() {
        TestUtils utils = new TestUtils(SEED);
        Message message = utils.randomMessage(8192 * 2);
        int messageSize = message.toByteBuffer().remaining();
        List<Message> expected = Collections.singletonList(message);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.forEach(x -> x.encode(baos));
        ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());

        List<Message> actual = new ArrayList<>();
        MessageDecoder decoder = new MessageDecoder(actual::add, 8192);

        // Feed all at once
        byte[] bs = new byte[messageSize];
        buf.get(bs);
        decoder.feed(bs);

        // Should have successfully decoded the one message
        assertEquals(1, actual.size());
    }

    @Test
    public void multipleMessagesDoesNotGrowBuffer() {
        TestUtils utils = new TestUtils(SEED);
        Message message = utils.randomMessage(4096);
        List<Message> expected = IntStream.range(0, 100)
                                          .mapToObj(x -> message)
                                          .collect(Collectors.toList());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.forEach(x -> x.encode(baos));
        ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());

        List<Message> actual = new ArrayList<>();
        MessageDecoder decoder = new MessageDecoder(actual::add, 8192);

        // Feed all at once
        byte[] bs = new byte[buf.capacity()];
        buf.get(bs);
        decoder.feed(bs);

        assertEquals(expected, actual);
        assertEquals(8192, decoder.currentBufferSize());
    }

    @Test
    public void multipleLargeMessages_GrowsBufferAsNeeded() {
        TestUtils utils = new TestUtils(SEED);
        Message message = utils.randomMessage(9001);
        List<Message> expected = IntStream.range(0, 100)
                                          .mapToObj(x -> message)
                                          .collect(Collectors.toList());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.forEach(x -> x.encode(baos));
        ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());

        List<Message> actual = new ArrayList<>();
        MessageDecoder decoder = new MessageDecoder(actual::add, 8192);

        // Feed all at once
        byte[] bs = new byte[buf.capacity()];
        buf.get(bs);
        decoder.feed(bs);

        assertEquals(expected, actual);
        assertTrue(decoder.currentBufferSize() > 9001);
    }
}
