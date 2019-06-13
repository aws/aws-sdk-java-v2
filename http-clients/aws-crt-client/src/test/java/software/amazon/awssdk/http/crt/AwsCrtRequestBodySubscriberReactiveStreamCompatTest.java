package software.amazon.awssdk.http.crt;

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.http.crt.internal.AwsCrtRequestBodySubscriber;

public class AwsCrtRequestBodySubscriberReactiveStreamCompatTest extends SubscriberWhiteboxVerification<ByteBuffer> {
    private static final int DEFAULT_STREAM_WINDOW_SIZE = 16 * 1024 * 1024; // 16 MB Total Buffer size

    public AwsCrtRequestBodySubscriberReactiveStreamCompatTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber(WhiteboxSubscriberProbe<ByteBuffer> probe) {
        AwsCrtRequestBodySubscriber actualSubscriber = new AwsCrtRequestBodySubscriber(DEFAULT_STREAM_WINDOW_SIZE);

        // Pass Through calls to AwsCrtRequestBodySubscriber, but also register calls to the whitebox probe
        Subscriber<ByteBuffer> passthroughSubscriber = new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                actualSubscriber.onSubscribe(s);
                probe.registerOnSubscribe(new SubscriberPuppet() {

                    @Override
                    public void triggerRequest(long elements) {
                        s.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        s.cancel();
                    }
                });
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                actualSubscriber.onNext(byteBuffer);
                probe.registerOnNext(byteBuffer);
            }

            @Override
            public void onError(Throwable t) {
                actualSubscriber.onError(t);
                probe.registerOnError(t);
            }

            @Override
            public void onComplete() {
                actualSubscriber.onComplete();
                probe.registerOnComplete();
            }
        };

        return passthroughSubscriber;
    }

    @Override
    public ByteBuffer createElement(int element) {
        return ByteBuffer.wrap(Integer.toString(element).getBytes());
    }
}
