/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.apache5.internal.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

/**
 * Bridges Apache5 {@link AsyncResponseConsumer} to {@link SdkAsyncHttpResponseHandler}.
 * Completes the provided {@link CompletableFuture} when the response body is fully consumed.
 */
@SdkInternalApi
public final class Apache5AsyncResponseConsumer implements AsyncResponseConsumer<Void> {

    private final SdkAsyncHttpResponseHandler responseHandler;
    private final CompletableFuture<Void> future;
    private final AtomicBoolean errorSignalled = new AtomicBoolean(false);

    // Reactive bridge for body streaming
    private volatile BodyPublisher bodyPublisher;

    public Apache5AsyncResponseConsumer(SdkAsyncHttpResponseHandler responseHandler, CompletableFuture<Void> future) {
        this.responseHandler = responseHandler;
        this.future = future;
    }

    @Override
    public void consumeResponse(HttpResponse response, EntityDetails entityDetails,
                                HttpContext context, FutureCallback<Void> resultCallback)
            throws HttpException, IOException {

        // Build SDK response from Apache5 response
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder()
                                                         .statusCode(response.getCode())
                                                         .statusText(response.getReasonPhrase());
        for (Header header : response.getHeaders()) {
            builder.appendHeader(header.getName(), header.getValue());
        }
        responseHandler.onHeaders(builder.build());

        // Create body publisher and hand it to the SDK handler
        bodyPublisher = new BodyPublisher();
        responseHandler.onStream(bodyPublisher);

        if (entityDetails == null) {
            // No body
            bodyPublisher.complete();
            resultCallback.completed(null);
        }
    }

    @Override
    public void informationResponse(HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        // 1xx informational - ignore
    }

    @Override
    public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
        // Signal we can accept data
        capacityChannel.update(Integer.MAX_VALUE);
    }

    @Override
    public void consume(ByteBuffer src) throws IOException {
        BodyPublisher publisher = bodyPublisher;
        if (publisher != null) {
            // Copy buffer since Apache5 may reuse it
            ByteBuffer copy = ByteBuffer.allocate(src.remaining());
            copy.put(src);
            copy.flip();
            publisher.emit(copy);
        }
    }

    @Override
    public void streamEnd(List<? extends Header> trailers) throws HttpException, IOException {
        BodyPublisher publisher = bodyPublisher;
        if (publisher != null) {
            publisher.complete();
        }
        future.complete(null);
    }

    @Override
    public void failed(Exception cause) {
        if (errorSignalled.compareAndSet(false, true)) {
            BodyPublisher publisher = bodyPublisher;
            if (publisher != null) {
                publisher.error(cause);
            }
            responseHandler.onError(cause);
            future.completeExceptionally(cause);
        }
    }

    @Override
    public void releaseResources() {
        // no-op
    }

    /**
     * A simple reactive {@link Publisher} that buffers emitted items until a subscriber arrives.
     */
    private static final class BodyPublisher implements Publisher<ByteBuffer> {

        private volatile Subscriber<? super ByteBuffer> subscriber;
        private final java.util.Queue<ByteBuffer> buffer = new java.util.concurrent.ConcurrentLinkedQueue<>();
        private volatile boolean completed = false;
        private volatile Throwable error = null;
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            if (!subscribed.compareAndSet(false, true)) {
                s.onError(new IllegalStateException("Only one subscriber allowed"));
                return;
            }
            subscriber = s;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    drain();
                }

                @Override
                public void cancel() {
                    subscriber = null;
                }
            });
            drain();
        }

        void emit(ByteBuffer buf) {
            buffer.add(buf);
            drain();
        }

        void complete() {
            completed = true;
            drain();
        }

        void error(Throwable t) {
            error = t;
            Subscriber<? super ByteBuffer> s = subscriber;
            if (s != null) {
                s.onError(t);
            }
        }

        private void drain() {
            Subscriber<? super ByteBuffer> s = subscriber;
            if (s == null) {
                return;
            }
            ByteBuffer buf;
            while ((buf = buffer.poll()) != null) {
                s.onNext(buf);
            }
            if (completed && buffer.isEmpty()) {
                s.onComplete();
            }
        }
    }
}
