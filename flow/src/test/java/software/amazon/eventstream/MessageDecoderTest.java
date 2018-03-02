package software.amazon.eventstream;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
}
