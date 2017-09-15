package software.amazon.awssdk.http.nio.netty.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class DongiePublisher implements Publisher<ByteBuffer> {
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final ConcurrentLinkedQueue<? super ByteBuffer> data = new ConcurrentLinkedQueue<>();

    private final AtomicReference<SubscriptionImpl> subscription = new AtomicReference<>(null);

    public DongiePublisher() {
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        SubscriptionImpl newSub = new SubscriptionImpl(subscriber);
        subscriber.onSubscribe(newSub);
        final boolean first = subscription.compareAndSet(null, newSub);
        if (first) {
            // do the normal stuff
        } else {
            newSub.cancel();
            newSub.subscriber().onError(new IllegalStateException("The publisher has already been subscribed to!"));
        }
    }

    public Subscriber<? super ByteBuffer> getSubscriber() {
        return subscription.get().subscriber;
    }

    public final void newData(ByteBuffer bb) {
//        exec.submit(() -> {
//            if (subscriber != null) {
//                subscriber.onNext(bb);
//            }
//        });
    }

    private static class SubscriptionImpl implements Subscription {

        private final Subscriber<? super ByteBuffer> subscriber;

        public SubscriptionImpl(Subscriber<? super ByteBuffer> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long l) {

        }

        @Override
        public void cancel() {

        }

        public Subscriber<? super ByteBuffer> subscriber() {
            return subscriber;
        }
    }
}
