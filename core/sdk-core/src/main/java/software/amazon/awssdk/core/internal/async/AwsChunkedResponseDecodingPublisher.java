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

package software.amazon.awssdk.core.internal.async;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.io.AwsChunkedResponseParser;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.DelegatingSubscription;

/**
 * Async aws-chunked response decoder. Strips framing and exposes trailers via {@link #trailersFuture()}.
 *
 * <p>Emits at most one buffer per upstream buffer, and requests more when a buffer held only framing. Single
 * subscriber only.
 */
@SdkInternalApi
public final class AwsChunkedResponseDecodingPublisher implements SdkPublisher<ByteBuffer> {

    private final Publisher<ByteBuffer> upstream;
    private final long expectedDecodedLength;
    private final Set<String> declaredTrailers;
    private final CompletableFuture<Map<String, String>> trailersFuture = new CompletableFuture<>();
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    // Skips the declared-trailer check; for tests
    AwsChunkedResponseDecodingPublisher(Publisher<ByteBuffer> upstream, long expectedDecodedLength) {
        this(upstream, expectedDecodedLength, null);
    }

    /**
     * @param declaredTrailers see {@link AwsChunkedResponseParser}
     */
    public AwsChunkedResponseDecodingPublisher(Publisher<ByteBuffer> upstream,
                                               long expectedDecodedLength,
                                               Set<String> declaredTrailers) {
        this.upstream = Validate.paramNotNull(upstream, "upstream");
        this.expectedDecodedLength = expectedDecodedLength;
        this.declaredTrailers = declaredTrailers;
    }

    /**
     * Completes with the trailers before downstream {@code onComplete}; fails on error or cancel.
     */
    public CompletableFuture<Map<String, String>> trailersFuture() {
        return trailersFuture;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Validate.paramNotNull(subscriber, "subscriber");
        if (!subscribed.compareAndSet(false, true)) {
            subscriber.onSubscribe(NoOpSubscription.INSTANCE);
            subscriber.onError(new IllegalStateException(
                AwsChunkedResponseDecodingPublisher.class.getSimpleName() + " supports only one subscriber"));
            return;
        }
        AwsChunkedResponseParser parser = new AwsChunkedResponseParser(expectedDecodedLength, declaredTrailers);
        upstream.subscribe(new DecodingSubscriber(subscriber, parser, trailersFuture));
    }

    private static final class DecodingSubscriber implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> downstream;
        private final AwsChunkedResponseParser parser;
        private final CompletableFuture<Map<String, String>> trailersFuture;

        // Decoded slices of the current upstream buffer, reused across onNext calls
        private final List<ByteBuffer> decodedSlices = new ArrayList<>(2);

        private Subscription subscription;

        // Drops signals after termination, e.g. upstream signalling after cancel
        private boolean terminated = false;

        DecodingSubscriber(Subscriber<? super ByteBuffer> downstream,
                           AwsChunkedResponseParser parser,
                           CompletableFuture<Map<String, String>> trailersFuture) {
            this.downstream = downstream;
            this.parser = parser;
            this.trailersFuture = trailersFuture;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            downstream.onSubscribe(new CancellationAwareSubscription(s, trailersFuture));
        }

        @Override
        public void onNext(ByteBuffer buf) {
            if (terminated) {
                return;
            }

            ByteBuffer decoded;
            try {
                decoded = decode(buf);
            } catch (RuntimeException e) {
                fail(e);
                return;
            }

            if (decoded == null) {
                // Framing only: request a replacement to honor downstream demand
                subscription.request(1);
                return;
            }

            downstream.onNext(decoded);
        }

        @Override
        public void onError(Throwable t) {
            if (terminated) {
                return;
            }
            terminated = true;
            trailersFuture.completeExceptionally(t);
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (terminated) {
                return;
            }

            Map<String, String> trailers;
            try {
                parser.endOfInput();
                trailers = parser.trailers();
            } catch (RuntimeException e) {
                terminated = true;
                trailersFuture.completeExceptionally(e);
                downstream.onError(e);
                return;
            }

            terminated = true;
            // Before onComplete, so downstream can read trailers
            trailersFuture.complete(trailers);
            downstream.onComplete();
        }

        private void fail(Throwable t) {
            terminated = true;
            subscription.cancel();
            trailersFuture.completeExceptionally(t);
            downstream.onError(t);
        }

        /**
         * Returns the buffer's decoded data as one buffer, or {@code null} if it held only framing.
         */
        private ByteBuffer decode(ByteBuffer buf) {
            decodedSlices.clear();
            while (buf.hasRemaining()) {
                long dataRemaining = parser.dataRemaining();
                if (dataRemaining > 0) {
                    int take = (int) Math.min(buf.remaining(), dataRemaining);
                    ByteBuffer slice = buf.slice();
                    slice.limit(take);
                    buf.position(buf.position() + take);
                    parser.consumeData(take);
                    decodedSlices.add(slice);
                } else {
                    parser.consumeFramingByte(buf.get() & 0xFF);
                }
            }
            return combine(decodedSlices);
        }

        private static ByteBuffer combine(List<ByteBuffer> slices) {
            if (slices.isEmpty()) {
                return null;
            }
            if (slices.size() == 1) {
                return slices.get(0);
            }
            int total = 0;
            for (ByteBuffer slice : slices) {
                total += slice.remaining();
            }
            ByteBuffer combined = ByteBuffer.allocate(total);
            for (ByteBuffer slice : slices) {
                combined.put(slice);
            }
            combined.flip();
            return combined;
        }
    }

    /**
     * Fails the trailers future on downstream cancel, since upstream then sends no terminal signal.
     */
    private static final class CancellationAwareSubscription extends DelegatingSubscription {
        private final CompletableFuture<Map<String, String>> trailersFuture;

        private CancellationAwareSubscription(Subscription delegate,
                                              CompletableFuture<Map<String, String>> trailersFuture) {
            super(delegate);
            this.trailersFuture = trailersFuture;
        }

        @Override
        public void cancel() {
            trailersFuture.completeExceptionally(
                new CancellationException("Subscription was cancelled before the response body was fully decoded"));
            super.cancel();
        }
    }

    private static final class NoOpSubscription implements Subscription {
        private static final NoOpSubscription INSTANCE = new NoOpSubscription();

        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    }
}
