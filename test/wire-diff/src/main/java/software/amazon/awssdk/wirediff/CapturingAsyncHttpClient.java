package software.amazon.awssdk.wirediff;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * The async counterpart of {@link CapturingHttpClient}: records the request an async client hands its
 * transport and replies with a canned response.
 *
 * <p>Same reasoning for capturing here rather than at a socket — this is the last point the SDK controls
 * — and one extra obligation that the sync version does not have: the request body is a publisher, and a
 * transport that never subscribes to it would record an empty body and pass. So this subscribes and
 * drains it before replying, which is also what a real transport does, and a body that never completes
 * shows up as a hung test rather than as a wrong golden.
 *
 * <p>Completes on the calling thread. The async-ness under test is the SDK's, not this stub's, and a
 * deterministic thread keeps the capture order stable.
 */
public final class CapturingAsyncHttpClient implements SdkAsyncHttpClient {

    private final List<CapturingHttpClient.CapturedRequest> captured = Collections.synchronizedList(new ArrayList<>());
    private final int responseStatus;
    private final byte[] responseBody;
    private final String responseContentType;
    private final java.util.Map<String, String> extraHeaders = new java.util.LinkedHashMap<>();

    private volatile long delayMillis;

    /** Delays every response, for the tests that need a call to be slow (timeouts). */
    public CapturingAsyncHttpClient withDelay(long millis) {
        this.delayMillis = millis;
        return this;
    }

    /** Adds a response header, for the tests that need one the canned response does not carry. */
    public CapturingAsyncHttpClient withResponseHeader(String name, String value) {
        extraHeaders.put(name, value);
        return this;
    }

    public CapturingAsyncHttpClient(int responseStatus, byte[] responseBody, String responseContentType) {
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.responseContentType = responseContentType;
    }

    /** A 200 with an XML body, the shape of nearly every S3 non-streaming response. */
    public static CapturingAsyncHttpClient xml(String body) {
        return new CapturingAsyncHttpClient(200, body.getBytes(StandardCharsets.UTF_8), "application/xml");
    }

    public List<CapturingHttpClient.CapturedRequest> captured() {
        return new ArrayList<>(captured);
    }

    public CapturingHttpClient.CapturedRequest only() {
        List<CapturingHttpClient.CapturedRequest> snapshot = captured();
        if (snapshot.size() != 1) {
            throw new IllegalStateException("expected exactly one captured request, got " + snapshot.size());
        }
        return snapshot.get(0);
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        request.requestContentPublisher().subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                body.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable t) {
                request.responseHandler().onError(t);
                done.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                captured.add(new CapturingHttpClient.CapturedRequest(toFullRequest(request.request()), body.toByteArray()));
                if (delayMillis > 0) {
                    CompletableFuture.delayedExecutor(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                                     .execute(() -> reply(request, done));
                } else {
                    reply(request, done);
                }
            }
        });
        return done;
    }

    private void reply(AsyncExecuteRequest request, CompletableFuture<Void> done) {
        request.responseHandler().onHeaders(SdkHttpResponse.builder()
                                                           .statusCode(responseStatus)
                                                           .putHeader("Content-Type", responseContentType)
                                                           .putHeader("Content-Length",
                                                                      String.valueOf(responseBody.length))
                                                           .putHeader("x-amz-request-id", "WIREDIFF000000000")
                                                           .putHeader("x-amz-id-2", "wirediff")
                                                                   .applyMutation(b -> extraHeaders.forEach(b::putHeader))
                                                           .build());
        request.responseHandler().onStream(SdkPublisher.adapt(
            s -> new SingleBufferSubscription(s, ByteBuffer.wrap(responseBody)).start()));
        done.complete(null);
    }

    private static SdkHttpFullRequest toFullRequest(SdkHttpRequest request) {
        // The render in WireFormat reads an SdkHttpFullRequest; the body is recorded separately, so the
        // conversion only has to carry method, URI, and headers -- which is all a SdkHttpRequest has.
        return SdkHttpFullRequest.builder()
                                 .method(request.method())
                                 .uri(request.getUri())
                                 .headers(request.headers())
                                 .build();
    }

    @Override
    public String clientName() {
        return "WireDiffAsyncCapture";
    }

    @Override
    public void close() {
    }

    /** Emits one buffer (or none, for an empty body) and completes, honoring demand. */
    private static final class SingleBufferSubscription implements Subscription {
        private final Subscriber<? super ByteBuffer> subscriber;
        private final ByteBuffer buffer;
        private boolean done;

        private SingleBufferSubscription(Subscriber<? super ByteBuffer> subscriber, ByteBuffer buffer) {
            this.subscriber = subscriber;
            this.buffer = buffer;
        }

        void start() {
            subscriber.onSubscribe(this);
        }

        @Override
        public synchronized void request(long n) {
            if (done || n <= 0) {
                return;
            }
            done = true;
            if (buffer.hasRemaining()) {
                subscriber.onNext(buffer);
            }
            subscriber.onComplete();
        }

        @Override
        public synchronized void cancel() {
            done = true;
        }
    }
}
