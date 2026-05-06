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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

/**
 * Bridges {@link SdkHttpContentPublisher} (reactive) to Apache5 {@link AsyncRequestProducer}.
 *
 * <p>Design: Uses a concurrent queue to buffer data from the reactive publisher.
 * Apache5 calls {@link #produce(DataStreamChannel)} when ready to accept data.
 * The reactive subscriber pushes data into the queue and signals Apache5 via
 * {@link DataStreamChannel#requestOutput()} to call {@code produce()} again.
 *
 * <p>Backpressure: We request one item at a time from the publisher. After draining
 * a buffer in {@code produce()}, we request the next item.
 */
@SdkInternalApi
public final class Apache5AsyncRequestProducer implements AsyncRequestProducer {

    private final SdkHttpRequest sdkRequest;
    private final SdkHttpContentPublisher publisher;

    private final ConcurrentLinkedQueue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private final AtomicBoolean streamCompleted = new AtomicBoolean(false);
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final AtomicReference<DataStreamChannel> channelRef = new AtomicReference<>();
    private volatile Subscription subscription;

    public Apache5AsyncRequestProducer(SdkHttpRequest sdkRequest, SdkHttpContentPublisher publisher) {
        this.sdkRequest = sdkRequest;
        this.publisher = publisher;
    }

    /** No-op: body is collected lazily via produce(). */
    public void collectBody() {
        // Body is streamed via produce() - no upfront collection needed
    }

    /** No-op: body is streamed via produce(). */
    public void setDelegate(org.apache.hc.core5.http.nio.AsyncEntityProducer entityProducer) {
        // Not used in streaming mode
    }

    /** Returns a completed future - streaming mode doesn't need upfront collection. */
    public java.util.concurrent.CompletableFuture<org.apache.hc.core5.http.nio.AsyncEntityProducer> collectBodyAsync() {
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }

    @Override
    public void sendRequest(RequestChannel channel, HttpContext context) throws HttpException, IOException {
        BasicHttpRequest request = new BasicHttpRequest(sdkRequest.method().name(), sdkRequest.getUri());

        // Copy headers - skip Host (Apache5 sets from URI) and Content-Length
        // (Apache5 sets it from EntityDetails.getContentLength()).
        for (Map.Entry<String, List<String>> entry : sdkRequest.headers().entrySet()) {
            String name = entry.getKey();
            if ("Host".equalsIgnoreCase(name) || "Content-Length".equalsIgnoreCase(name)) {
                continue;
            }
            for (String value : entry.getValue()) {
                request.addHeader(name, value);
            }
        }

        EntityDetails entityDetails = publisher != null ? new PublisherEntityDetails() : null;
        channel.sendRequest(request, entityDetails, context);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public int available() {
        if (error.get() != null) {
            return 0;
        }
        ByteBuffer head = queue.peek();
        if (head != null) {
            return head.remaining();
        }
        // Signal data may be coming (not yet subscribed or publisher not done)
        return streamCompleted.get() ? 0 : 1;
    }

    /**
     * Called by Apache5 IO thread when ready to accept body data.
     * Subscribes to the publisher on first call, then drains the queue.
     */
    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        channelRef.set(channel);

        Throwable err = error.get();
        if (err != null) {
            throw new IOException("Request body publisher failed", err);
        }

        // Subscribe on first produce() call
        if (subscribed.compareAndSet(false, true)) {
            if (publisher == null) {
                channel.endStream();
                return;
            }
            publisher.subscribe(new BodySubscriber());
            // Return - data will arrive via onNext and trigger requestOutput()
            return;
        }

        // Drain queue into channel
        drainQueue(channel);
    }

    private void drainQueue(DataStreamChannel channel) throws IOException {
        ByteBuffer buf;
        while ((buf = queue.peek()) != null) {
            if (!buf.hasRemaining()) {
                queue.poll();
                // Request next item from publisher
                Subscription sub = subscription;
                if (sub != null) {
                    sub.request(1);
                }
                continue;
            }
            int written = channel.write(buf);
            if (written == 0) {
                // Channel buffer full - stop and wait for next produce() call
                return;
            }
            if (!buf.hasRemaining()) {
                queue.poll();
                // Request next item from publisher
                Subscription sub = subscription;
                if (sub != null) {
                    sub.request(1);
                }
            }
        }

        if (streamCompleted.get() && queue.isEmpty()) {
            channel.endStream();
        }
    }

    @Override
    public void failed(Exception cause) {
        Subscription sub = subscription;
        if (sub != null) {
            sub.cancel();
        }
    }

    @Override
    public void releaseResources() {
        // no-op
    }

    private final class BodySubscriber implements Subscriber<ByteBuffer> {

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            s.request(1); // Request first chunk
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            // Copy buffer since publisher may reuse it
            ByteBuffer copy = ByteBuffer.allocate(byteBuffer.remaining());
            copy.put(byteBuffer);
            copy.flip();
            queue.add(copy);

            // Signal Apache5 IO thread to call produce() again
            DataStreamChannel ch = channelRef.get();
            if (ch != null) {
                ch.requestOutput();
            }
            // Don't request next item here - produce() will request after draining
        }

        @Override
        public void onError(Throwable t) {
            error.set(t);
            DataStreamChannel ch = channelRef.get();
            if (ch != null) {
                ch.requestOutput();
            }
        }

        @Override
        public void onComplete() {
            streamCompleted.set(true);
            DataStreamChannel ch = channelRef.get();
            if (ch != null) {
                ch.requestOutput();
            }
        }
    }

    private final class PublisherEntityDetails implements EntityDetails {

        @Override
        public long getContentLength() {
            // Use Content-Length from the SDK request headers (which has the signed value,
            // e.g., chunked-encoded length for aws-chunked encoding).
            // Fall back to publisher.contentLength() if not present.
            return sdkRequest.firstMatchingHeader("Content-Length")
                             .map(Long::parseLong)
                             .orElseGet(() -> publisher.contentLength().orElse(-1L));
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public String getContentEncoding() {
            return null;
        }

        @Override
        public boolean isChunked() {
            // Use chunked only when Content-Length is not known
            return !sdkRequest.firstMatchingHeader("Content-Length").isPresent()
                   && !publisher.contentLength().isPresent();
        }

        @Override
        public Set<String> getTrailerNames() {
            return java.util.Collections.emptySet();
        }
    }
}
