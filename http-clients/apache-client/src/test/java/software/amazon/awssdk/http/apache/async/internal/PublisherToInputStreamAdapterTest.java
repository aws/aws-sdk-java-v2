package software.amazon.awssdk.http.apache.async.internal;

import org.junit.Test;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link PublisherToInputStreamAdapter}.
 */
public class PublisherToInputStreamAdapterTest {
    private final PublisherToInputStreamAdapter adapter = new PublisherToInputStreamAdapter();

    @Test
    public void returnsEofWhenComplete() throws IOException {
        InputStream is = adapter.adapt(subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                subscriber.onNext(ByteBuffer.wrap(new byte[8]));
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
            }
        }));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buf[] = new byte[8];
        int read;
        while ((read = is.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }

        assertEquals(8, baos.size());
    }

    @Test(expected = IOException.class)
    public void throwsIoeWhenPublisherSignalsError() throws IOException {
        InputStream is = adapter.adapt(subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                subscriber.onError(new RuntimeException("boo"));
            }

            @Override
            public void cancel() {

            }
        }));

        is.read();
    }
}
