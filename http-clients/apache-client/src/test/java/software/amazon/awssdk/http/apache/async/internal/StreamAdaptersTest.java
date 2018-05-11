package software.amazon.awssdk.http.apache.async.internal;

import org.junit.Test;
import org.reactivestreams.Publisher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests involving both {@link InputStreamToPublisherAdapter} and {@link PublisherToInputStreamAdapter}.
 */
public class StreamAdaptersTest {
    private final InputStreamToPublisherAdapter isAdapter = new InputStreamToPublisherAdapter();
    private final PublisherToInputStreamAdapter pubAdapter = new PublisherToInputStreamAdapter();

    @Test
    public void testRoundtrip() throws IOException {
        byte[] originalBytes = new byte[32000];
        new Random().nextBytes(originalBytes);
        ByteArrayInputStream originalIs = new ByteArrayInputStream(originalBytes);

        Publisher<ByteBuffer> publisherFromIs = isAdapter.adapt(originalIs);
        InputStream isFromPublisher = pubAdapter.adapt(publisherFromIs);

        ByteArrayOutputStream readContents = new ByteArrayOutputStream();
        byte[] readBuf = new byte[4096];
        int read;
        while ((read = isFromPublisher.read(readBuf)) != -1) {
            readContents.write(readBuf, 0, read);
        }

        assertArrayEquals(originalBytes, readContents.toByteArray());
    }
}
