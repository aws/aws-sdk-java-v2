package software.amazon.awssdk.http.crt;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Sha256BodySubscriber  implements Subscriber<ByteBuffer> {
    private MessageDigest digest;
    private CompletableFuture<String> future;

    public Sha256BodySubscriber() throws NoSuchAlgorithmException {
        digest =  MessageDigest.getInstance("SHA-256");
        future = new CompletableFuture<>();
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        digest.update(byteBuffer);
    }

    @Override
    public void onError(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        future.complete(encodeHexString(digest.digest()).toUpperCase());
    }

    public CompletableFuture<String> getFuture() {
        return future;
    }
}
