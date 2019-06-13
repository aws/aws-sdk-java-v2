package software.amazon.awssdk.http.crt;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

public class SdkTestHttpContentPublisher implements SdkHttpContentPublisher {
    private final byte[] body;
    private final AtomicReference<Subscriber<? super ByteBuffer>> subscriber = new AtomicReference<>(null);
    private final AtomicBoolean complete = new AtomicBoolean(false);

    public SdkTestHttpContentPublisher(byte[] body) {
        this.body = body;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        boolean wasFirstSubscriber = subscriber.compareAndSet(null, s);

        SdkTestHttpContentPublisher publisher = this;

        if (wasFirstSubscriber) {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    publisher.request(n);
                }

                @Override
                public void cancel() {
                    // Do nothing
                }
            });
        } else {
            s.onError(new RuntimeException("Only allow one subscriber"));
        }
    }

    protected void request(long n) {
        // Send the whole body if they request >0 ByteBuffers
        if (n >  0 && !complete.get()) {
            complete.set(true);
            subscriber.get().onNext(ByteBuffer.wrap(body));
            subscriber.get().onComplete();
        }
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of((long)body.length);
    }
}
