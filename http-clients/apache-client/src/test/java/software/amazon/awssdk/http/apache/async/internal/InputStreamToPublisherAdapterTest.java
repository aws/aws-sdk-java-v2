package software.amazon.awssdk.http.apache.async.internal;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link InputStreamToPublisherAdapter}
 */
public class InputStreamToPublisherAdapterTest {
    private final InputStreamToPublisherAdapter adapter = new InputStreamToPublisherAdapter();

    // This tests ensures that Subscription.cancel() short circuits the
    // fulfillment of demand so that we stop as soon as possible after cancel()
    // is called instead of waiting to until all outstanding requests are
    // fulfilled
    @Test(timeout = 5000L)
    public void eventuallyStopsOnNextSignalsAfterCancelled() throws InterruptedException {
        Publisher<ByteBuffer> publisher = adapter.adapt(new InputStream() {
            @Override
            public int read() {
                return 0xFE;
            }
        });

        TestSubscriber subscriber = new TestSubscriber() {
            @Override
            public void onSubscribe(Subscription subscription) {
                super.onSubscribe(subscription);
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                super.onNext(byteBuffer);
                if (getOnNextEvents() > 500) {
                    getSubscription().cancel();
                }
            }
        };

        publisher.subscribe(subscriber);

        while (!subscriber.isComplete()) {
            Thread.sleep(1000);
        }
    }

    @Test(timeout = 5000L)
    public void SignalsCompleteAfterReadingEof() throws InterruptedException {
        InputStream is = new ByteArrayInputStream(new byte[1]);
        Publisher<ByteBuffer> publisher = adapter.adapt(is);

        TestSubscriber subscriber = new TestSubscriber();
        publisher.subscribe(subscriber);

        subscriber.getSubscription().request(1);

        while (subscriber.getOnNextEvents() < 1) {
            Thread.sleep(1000);
        }

        assertFalse(subscriber.isComplete());

        subscriber.getSubscription().request(1);

        while (!subscriber.isComplete()) {
            Thread.sleep(1000);
        }
    }

    @Test(timeout = 2000L)
    public void SignalsErrorAfterReadingException() throws InterruptedException {
        Publisher<ByteBuffer> publisher = adapter.adapt(new ErrorThrowingInputStream());
        TestSubscriber subscriber = new TestSubscriber();
        publisher.subscribe(subscriber);
        subscriber.getSubscription().request(Long.MAX_VALUE);

        while (!subscriber.isErrored()) {
            Thread.sleep(1000);
        }

        assertNotNull(subscriber.getErrorThrown());
    }


    private static class ErrorThrowingInputStream extends InputStream {
        private int bytesRead = 0;

        @Override
        public int read() throws IOException {
            if (++bytesRead > 1) {
                throw new IOException("Hi");
            }
            return 0xFE;
        }
    }

    private static class TestSubscriber implements Subscriber<ByteBuffer> {
        private Subscription subscription;
        private volatile boolean complete = false;

        private volatile boolean errored = false;
        private volatile Throwable errorThrown;
        private volatile int onNextEvents = 0;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            ++onNextEvents;
        }

        @Override
        public void onError(Throwable throwable) {
            errorThrown = throwable;
            errored = true;
        }

        @Override
        public void onComplete() {
            complete = true;
        }

        public boolean isComplete() {
            return complete;
        }

        public boolean isErrored() {
            return errored;
        }

        public int getOnNextEvents() {
            return onNextEvents;
        }

        public Throwable getErrorThrown() {
            return errorThrown;
        }

        public Subscription getSubscription() {
            return subscription;
        }
    }
}
