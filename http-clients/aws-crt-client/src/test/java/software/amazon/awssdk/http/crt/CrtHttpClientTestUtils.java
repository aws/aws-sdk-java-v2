package software.amazon.awssdk.http.crt;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyMap;

public class CrtHttpClientTestUtils {

    static Subscriber<ByteBuffer> createDummySubscriber() {
        return new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        };
    }

    static SdkAsyncHttpResponseHandler createTestResponseHandler(AtomicReference<SdkHttpResponse> response,
                                                                 CompletableFuture<Boolean> streamReceived,
                                                                 AtomicReference<Throwable> error,
                                                                 Subscriber<ByteBuffer> subscriber) {
        return new SdkAsyncHttpResponseHandler() {
            @Override
            public void onHeaders(SdkHttpResponse headers) {
                response.compareAndSet(null, headers);
            }
            @Override
            public void onStream(Publisher<ByteBuffer> stream) {
                stream.subscribe(subscriber);
                streamReceived.complete(true);
            }

            @Override
            public void onError(Throwable t) {
                error.compareAndSet(null, t);
            }
        };
    }

    public static SdkHttpFullRequest createRequest(URI endpoint) {
        return createRequest(endpoint, "/", null, SdkHttpMethod.GET, emptyMap());
    }

    static SdkHttpFullRequest createRequest(URI endpoint,
                                             String resourcePath,
                                             byte[] body,
                                             SdkHttpMethod method,
                                             Map<String, String> params) {

        String contentLength = (body == null) ? null : String.valueOf(body.length);
        return SdkHttpFullRequest.builder()
                .uri(endpoint)
                .method(method)
                .encodedPath(resourcePath)
                .applyMutation(b -> params.forEach(b::putRawQueryParameter))
                .applyMutation(b -> {
                    b.putHeader("Host", endpoint.getHost());
                    if (contentLength != null) {
                        b.putHeader("Content-Length", contentLength);
                    }
                }).build();
    }
}
