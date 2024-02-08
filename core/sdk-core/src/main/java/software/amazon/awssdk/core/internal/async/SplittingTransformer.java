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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.DelegatingBufferingSubscriber;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Split a {@link AsyncResponseTransformer} into multiple ones, publishing them as a {@link SdkPublisher}. Created using the
 * {@link AsyncResponseTransformer#split(long) split} method. The upstream {@link AsyncResponseTransformer} that is split will
 * receive data from the individual transformers.
 * <p>
 * This publisher also buffers an amount of data before sending it to the upstream transformer, as specified by the
 * maximumBufferSize. ByteBuffers will be published once the buffer has been reached, or when the subscription to this publisher
 * is cancelled.
 * <p>
 * Cancelling the subscription to this publisher signals that no more data needs to be sent to the upstream transformer. This
 * publisher will then send all data currently buffered to the upstream transformer and complete the downstream subscriber.
 */
@SdkInternalApi
public class SplittingTransformer<ResponseT, ResultT> implements SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> {

    private static final Logger log = Logger.loggerFor(SplittingTransformer.class);

    /**
     * The AsyncResponseTransformer on which the {@link AsyncResponseTransformer#split(long) split} method was called.
     */
    private final AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer;

    /**
     * Set to true once {@code .prepare()} is called on the upstreamResponseTransformer
     */
    private final AtomicBoolean preparedCalled = new AtomicBoolean(false);

    /**
     * Set to true once {@code .onResponse()} is called on the upstreamResponseTransformer
     */
    private final AtomicBoolean onResponseCalled = new AtomicBoolean(false);

    /**
     * Set to true once {@code .onStream()} is called on the upstreamResponseTransformer
     */
    private final AtomicBoolean onStreamCalled = new AtomicBoolean(false);

    /**
     * Set to true once {@code .concel()} is called in the subscription of the downstream subscriber
     */
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    /**
     * Future to track the status of the upstreamResponseTransformer. Will be completed when the future returned by calling
     * {@code prepare()} on the upstreamResponseTransformer itself completes.
     */
    private final CompletableFuture<ResultT> returnFuture;

    /**
     * The buffer size used to buffer the content received from the downstream subscriber
     */
    private final long maximumBufferSize;

    /**
     * This publisher is used to send the bytes received from the downstream subscriber's transformers to a
     * {@link DelegatingBufferingSubscriber} that will buffer a number of bytes specified by the {@code maximumBufferSize}.
     */
    private final SimplePublisher<ByteBuffer> publisherToUpstream = new SimplePublisher<>();

    /**
     * The downstream subscriber that is subscribed to this publisher.
     */
    private Subscriber<? super AsyncResponseTransformer<ResponseT, ResponseT>> downstreamSubscriber;

    /**
     * The amount requested by the downstream subscriber that is still left to fulfill. Updated. when the
     * {@link Subscription#request(long) request} method is called on the downstream subscriber's subscription. Corresponds to the
     * number of {@code AsyncResponseTransformer} that will be published to the downstream subscriber.
     */
    private final AtomicLong outstandingDemand = new AtomicLong(0);

    /**
     * This flag stops the current thread from publishing transformers while another thread is already publishing
     */
    private final AtomicBoolean emitting = new AtomicBoolean(false);

    // 'boundedness' info
    private final long maxElements;
    private final boolean isBounded;
    private final AtomicInteger totalEmitted = new AtomicInteger(0);
    private final AtomicBoolean terminated = new AtomicBoolean(false);

    /**
     * Creates a publisher bounded by the {@code maxElements} provided. As per reactive stream specification, this publisher will
     * be considered 'unbounded' if {@code maxElements == LONG.MAX_VALUE}.
     */
    private SplittingTransformer(AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer,
                                 Long bufferSize,
                                 CompletableFuture<ResultT> returnFuture,
                                 Long maxElements) {
        this.upstreamResponseTransformer = Validate.paramNotNull(upstreamResponseTransformer, "asyncRequestBody");
        this.returnFuture = Validate.paramNotNull(returnFuture, "returnFuture");
        this.maximumBufferSize = Validate.notNull(bufferSize, "bufferSize");
        this.maxElements = Validate.getOrDefault(maxElements, () -> Long.MAX_VALUE);
        this.isBounded = maxElements != null && maxElements != Long.MAX_VALUE;
    }

    /**
     * @param downstreamSubscriber the {@link Subscriber} to the individual AsyncResponseTransformer
     */
    @Override
    public void subscribe(Subscriber<? super AsyncResponseTransformer<ResponseT, ResponseT>> downstreamSubscriber) {
        if (downstreamSubscriber == null) {
            throw new NullPointerException("downstreamSubscriber must not be null");
        }
        this.downstreamSubscriber = downstreamSubscriber;
        downstreamSubscriber.onSubscribe(new DownstreamSubscription());
        if (maxElements < 0) {
            downstreamSubscriber.onError(new IllegalArgumentException("Maximum element to request must be positive"));
        }
    }

    /**
     * The subscription implementation for the subscriber to this SplittingTransformer.
     */
    private final class DownstreamSubscription implements Subscription {

        @Override
        public void request(long n) {
            if (n <= 0) {
                downstreamSubscriber.onError(new IllegalArgumentException("Amount requested must be positive"));
                return;
            }
            if (isBounded && totalEmitted.get() >= maxElements) {
                terminate();
                return;
            }
            if (isCancelled.get()) {
                return;
            }
            long newDemand = outstandingDemand.updateAndGet(current -> {
                if (Long.MAX_VALUE - current < n) {
                    return Long.MAX_VALUE;
                }
                return current + n;
            });
            log.trace(() -> String.format("new outstanding demand: %s", newDemand));
            emit();
        }

        @Override
        public void cancel() {
            if (isCancelled.compareAndSet(false, true)) {
                log.trace(() -> "Cancelling splitting transformer");
                publisherToUpstream.complete();
                downstreamSubscriber = null;
            }
        }
    }

    private void terminate() {
        if (terminated.compareAndSet(false, true)) {
            publisherToUpstream.complete();
            downstreamSubscriber.onComplete();
            downstreamSubscriber = null;
        }
    }

    private void emit() {
        do {
            if (!emitting.compareAndSet(false, true)) {
                return;
            }
            try {
                if (isBounded && totalEmitted.get() >= maxElements) {
                    terminate();
                    return;
                }
                if (isCancelled.get() || terminated.get()) {
                    return;
                }
                if (outstandingDemand.get() > 0) {
                    outstandingDemand.decrementAndGet();
                    downstreamSubscriber.onNext(new IndividualTransformer());
                    totalEmitted.incrementAndGet();
                }
            } finally {
                emitting.compareAndSet(true, false);
            }
        } while (outstandingDemand.get() > 0);
    }

    /**
     * The AsyncResponseTransformer for each of the individual requests that is sent back to the downstreamSubscriber when
     * requested. A future is created per request that is completed when onComplete is called on the subscriber for that request
     * body publisher.
     */
    private class IndividualTransformer implements AsyncResponseTransformer<ResponseT, ResponseT> {

        private ResponseT response;
        private CompletableFuture<ResponseT> individualFuture;

        @Override
        public CompletableFuture<ResponseT> prepare() {
            this.individualFuture = new CompletableFuture<>();
            if (preparedCalled.compareAndSet(false, true)) {
                log.trace(() -> "calling prepare on the upstream transformer");
                CompletableFuture<ResultT> upstreamFuture = upstreamResponseTransformer.prepare();
                CompletableFutureUtils.forwardExceptionTo(returnFuture, upstreamFuture);
                CompletableFutureUtils.forwardResultTo(upstreamFuture, returnFuture);
            }
            return this.individualFuture;
        }

        @Override
        public void onResponse(ResponseT response) {
            if (onResponseCalled.compareAndSet(false, true)) {
                log.trace(() -> "calling onResponse on the upstream transformer");
                // todo: should we send back the first response to the upstreamResponseTransformer as-is?
                upstreamResponseTransformer.onResponse(response);
            }
            this.response = response;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            if (onStreamCalled.compareAndSet(false, true)) {
                log.trace(() -> "calling onStream on the upstream transformer");
                upstreamResponseTransformer.onStream(
                    upstreamSubscriber ->
                        publisherToUpstream.subscribe(new DelegatingBufferingSubscriber(maximumBufferSize, upstreamSubscriber))
                );
            }
            publisher.subscribe(new IndividualPartSubscriber<>(this.individualFuture, response, publisherToUpstream));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            upstreamResponseTransformer.exceptionOccurred(error);
        }
    }

    /**
     * the Subscriber for each of the individual request's ByteBuffer publisher
     */
    static class IndividualPartSubscriber<T> implements Subscriber<ByteBuffer> {

        private final CompletableFuture<T> future;
        private final T response;
        private final SimplePublisher<ByteBuffer> bodyPartPublisher;
        private Subscription subscription;

        IndividualPartSubscriber(CompletableFuture<T> future, T response,
                                 SimplePublisher<ByteBuffer> bodyPartPublisher) {
            this.future = future;
            this.response = response;
            this.bodyPartPublisher = bodyPartPublisher;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription != null) {
                s.cancel();
                return;
            }
            this.subscription = s;
            // request everything, data will be buffered by the DelegatingBufferingSubscriber
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            if (byteBuffer == null) {
                throw new NullPointerException("onNext must not be called with null byteBuffer");
            }
            bodyPartPublisher.send(byteBuffer);
            // we can request everything, buffering is done in DelegatingBufferingSubscriber
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onError(Throwable t) {
            bodyPartPublisher.error(t);
            future.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            future.complete(response);
        }
    }

    public static <ResponseT, ResultT> Builder<ResponseT, ResultT> builder() {
        return new Builder<>();
    }

    public static final class Builder<ResponseT, ResultT> {

        private Builder() {
        }

        private AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer;
        private long bufferSize;
        private CompletableFuture<ResultT> returnFuture;
        private Long maxElements;

        public Builder<ResponseT, ResultT> upstreamResponseTransformer(
            AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer) {
            this.upstreamResponseTransformer = upstreamResponseTransformer;
            return this;
        }

        public Builder<ResponseT, ResultT> bufferSize(long bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder<ResponseT, ResultT> returnFuture(CompletableFuture<ResultT> returnFuture) {
            this.returnFuture = returnFuture;
            return this;
        }

        public Builder<ResponseT, ResultT> maxElements(Long maxElements) {
            this.maxElements = maxElements;
            return this;
        }

        public SplittingTransformer<ResponseT, ResultT> build() {
            return new SplittingTransformer<>(this.upstreamResponseTransformer,
                                              this.bufferSize,
                                              this.returnFuture,
                                              this.maxElements);
        }
    }
}
