package software.amazon.awssdk.protocol.tests.util;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Mock implementation of {@link SdkAsyncHttpClient}.
 */
public class MockAsyncHttpClient implements SdkAsyncHttpClient {
    private HttpExecuteResponse nextResponse;
    private long fixedDelay;

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {

        byte[] content = nextResponse.responseBody().map(p -> invokeSafely(() -> IoUtils.toByteArray(p)))
                                     .orElseGet(() -> new byte[0]);

        request.responseHandler().onHeaders(nextResponse.httpResponse());
        request.responseHandler().onStream(new ResponsePublisher(content));

        try {
            Thread.sleep(fixedDelay);
        } catch (InterruptedException e) {
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
    }

    public void stubNextResponse(HttpExecuteResponse nextResponse) {
        this.nextResponse = nextResponse;
    }

    public void withFixedDelay(long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    private static class ResponsePublisher implements SdkHttpContentPublisher {
        private final byte[] content;

        private ResponsePublisher(byte[] content) {
            this.content = content;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of((long) content.length);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                private boolean running = true;

                @Override
                public void request(long n) {
                    if (n <= 0) {
                        running = false;
                        s.onError(new IllegalArgumentException("Demand must be positive"));
                    } else if (running) {
                        running = false;
                        s.onNext(ByteBuffer.wrap(content));
                        s.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    running = false;
                }
            });
        }
    }

}
