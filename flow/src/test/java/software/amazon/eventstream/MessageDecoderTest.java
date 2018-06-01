package software.amazon.eventstream;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class MessageDecoderTest {
    long SEED = 8912374098123423L;

    @Test
    public void testDecoder() throws Exception {
        TestUtils utils = new TestUtils(SEED);
        Random rand = new Random(SEED);
        List<Message> expected = IntStream.range(0, 100_000)
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

        Assert.assertEquals(expected, actual);
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
        assertThat(actual, Matchers.hasSize(0));

        // Feed rest of message in it's entirety.
        bs = new byte[messageSize - 15];
        buf.get(bs);
        decoder.feed(bs);

        // Should have successfully decoded the one message
        assertThat(actual, Matchers.hasSize(1));
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
        assertThat(actual, Matchers.hasSize(0));

        // Feed rest of message in it's entirety.
        bs = new byte[messageSize - 15];
        buf.get(bs);
        decoder.feed(bs);

        // Should have successfully decoded the one message
        assertThat(actual, Matchers.hasSize(1));
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
        assertThat(actual, Matchers.hasSize(1));
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
        assertThat(decoder.currentBufferSize(), greaterThan(9001));
    }

}
