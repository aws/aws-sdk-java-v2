package software.amazon.awssdk.http.crt;

import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

public class EmptyPublisher implements SdkHttpContentPublisher {
    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(new EmptySubscription(subscriber));
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of(0L);
    }

    private static class EmptySubscription implements Subscription {
        private final Subscriber subscriber;
        private volatile boolean done;

        EmptySubscription(Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long l) {
            if (!done) {
                done = true;
                if (l <= 0) {
                    this.subscriber.onError(new IllegalArgumentException("Demand must be positive"));
                } else {
                    this.subscriber.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            done = true;
        }
    }
}
