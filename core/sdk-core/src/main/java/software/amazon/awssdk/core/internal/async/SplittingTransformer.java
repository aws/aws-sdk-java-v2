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
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.DelegatingBufferingSubscriber;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Split a {@link AsyncResponseTransformer} into multiple ones, publishing them as a {@link SdkPublisher}. Created using the
 * {@link AsyncResponseTransformer#split(SplittingTransformerConfiguration) split} method. The upstream
 * {@link AsyncResponseTransformer} that is split will receive data from the individual transformers.
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
     * The AsyncResponseTransformer on which the {@link AsyncResponseTransformer#split(SplittingTransformerConfiguration) split}
     * method was called.
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
     * Set to true once {@code .cancel()} is called in the subscription of the downstream subscriber, or if the
     * {@code resultFuture} is cancelled.
     */
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    /**
     * Future to track the status of the upstreamResponseTransformer. Will be completed when the future returned by calling
     * {@code prepare()} on the upstreamResponseTransformer itself completes.
     */
    private final CompletableFuture<ResultT> resultFuture;

    /**
     * The buffer size used to buffer the content received from the downstream subscriber
     */
    private final long maximumBufferInBytes;

    /**
     * This publisher is used to send the bytes received from the downstream subscriber's transformers to a
     * {@link DelegatingBufferingSubscriber} that will buffer a number of bytes up to {@code maximumBufferSize}.
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
     * This flag stops the current thread from publishing transformers while another thread is already publishing.
     */
    private final AtomicBoolean emitting = new AtomicBoolean(false);

    private final Object cancelLock = new Object();

    private SplittingTransformer(AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer,
                                 Long maximumBufferSizeInBytes,
                                 CompletableFuture<ResultT> resultFuture) {
        this.upstreamResponseTransformer = Validate.paramNotNull(
            upstreamResponseTransformer, "upstreamResponseTransformer");
        this.resultFuture = Validate.paramNotNull(
            resultFuture, "resultFuture");
        Validate.notNull(maximumBufferSizeInBytes, "maximumBufferSizeInBytes");
        this.maximumBufferInBytes = Validate.isPositive(
            maximumBufferSizeInBytes, "maximumBufferSizeInBytes");

        this.resultFuture.whenComplete((r, e) -> {
            if (e == null) {
                return;
            }
            if (isCancelled.compareAndSet(false, true)) {
                handleFutureCancel(e);
            }
        });
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
            log.trace(() -> String.format("received cancel signal. Current cancel state is 'isCancelled=%s'", isCancelled.get()));
            if (isCancelled.compareAndSet(false, true)) {
                handleSubscriptionCancel();
            }
        }
    }

    private void emit() {
        do {
            if (!emitting.compareAndSet(false, true)) {
                return;
            }
            try {
                if (doEmit()) {
                    return;
                }
            } finally {
                emitting.compareAndSet(true, false);
            }
        } while (outstandingDemand.get() > 0);
    }

    private boolean doEmit() {
        long demand = outstandingDemand.get();

        while (demand > 0) {
            if (isCancelled.get()) {
                return true;
            }
            if (outstandingDemand.get() > 0) {
                demand = outstandingDemand.decrementAndGet();
                downstreamSubscriber.onNext(new IndividualTransformer());
            }
        }
        return false;
    }

    /**
     * Handle the {@code .cancel()} signal received from the downstream subscription. Data that is being sent to the upstream
     * transformer need to finish processing before we complete. One typical use case for this is completing the multipart
     * download, the subscriber having reached the final part will signal that it doesn't need more parts by calling
     * {@code .cancel()} on the subscription.
     */
    private void handleSubscriptionCancel() {
        synchronized (cancelLock) {
            if (downstreamSubscriber == null) {
                log.trace(() -> "downstreamSubscriber already null, skipping downstreamSubscriber.onComplete()");
                return;
            }
            if (!onStreamCalled.get()) {
                // we never subscribe publisherToUpstream to the upstream, it would not complete
                downstreamSubscriber = null;
                return;
            }
            publisherToUpstream.complete().whenComplete((v, t) -> {
                if (downstreamSubscriber == null) {
                    return;
                }
                if (t != null) {
                    downstreamSubscriber.onError(t);
                } else {
                    log.trace(() -> "calling downstreamSubscriber.onComplete()");
                    downstreamSubscriber.onComplete();
                }
                downstreamSubscriber = null;
            });
        }
    }

    /**
     * Handle when the {@link SplittingTransformer#resultFuture} is cancelled or completed exceptionally from the outside. Data
     * need to stop being sent to the upstream transformer immediately. One typical use case for this is transfer manager needing
     * to pause download by calling {@code .cancel(true)} on the future.
     *
     * @param e The exception the future was complete exceptionally with.
     */
    private void handleFutureCancel(Throwable e) {
        synchronized (cancelLock) {
            publisherToUpstream.error(e);
            if (downstreamSubscriber != null) {
                downstreamSubscriber.onError(e);
                downstreamSubscriber = null;
            }
        }
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
                if (isCancelled.get()) {
                    return individualFuture;
                }
                log.trace(() -> "calling prepare on the upstream transformer");
                CompletableFuture<ResultT> upstreamFuture = upstreamResponseTransformer.prepare();
                if (!resultFuture.isDone()) {
                    CompletableFutureUtils.forwardResultTo(upstreamFuture, resultFuture);
                }
            }
            resultFuture.whenComplete((r, e) -> {
                if (e == null) {
                    return;
                }
                individualFuture.completeExceptionally(e);
            });
            individualFuture.whenComplete((r, e) -> {
                if (isCancelled.get()) {
                    handleSubscriptionCancel();
                }
            });
            return this.individualFuture;
        }

        @Override
        public void onResponse(ResponseT response) {
            if (onResponseCalled.compareAndSet(false, true)) {
                log.trace(() -> "calling onResponse on the upstream transformer");
                upstreamResponseTransformer.onResponse(response);
            }
            this.response = response;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            if (downstreamSubscriber == null) {
                return;
            }
            synchronized (cancelLock) {
                if (onStreamCalled.compareAndSet(false, true)) {
                    log.trace(() -> "calling onStream on the upstream transformer");
                    upstreamResponseTransformer.onStream(upstreamSubscriber -> publisherToUpstream.subscribe(
                        DelegatingBufferingSubscriber.builder()
                                                     .maximumBufferInBytes(maximumBufferInBytes)
                                                     .delegate(upstreamSubscriber)
                                                     .build())
                    );
                }
            }
            publisher.subscribe(new IndividualPartSubscriber<>(this.individualFuture, response));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            publisherToUpstream.error(error);
            log.trace(() -> "calling exceptionOccurred on the upstream transformer");
            upstreamResponseTransformer.exceptionOccurred(error);
        }
    }

    /**
     * the Subscriber for each of the individual request's ByteBuffer publisher
     */
    class IndividualPartSubscriber<T> implements Subscriber<ByteBuffer> {

        private final CompletableFuture<T> future;
        private final T response;
        private Subscription subscription;

        IndividualPartSubscriber(CompletableFuture<T> future, T response) {
            this.future = future;
            this.response = response;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.subscription != null) {
                s.cancel();
                return;
            }
            this.subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            if (byteBuffer == null) {
                throw new NullPointerException("onNext must not be called with null byteBuffer");
            }
            publisherToUpstream.send(byteBuffer).whenComplete((r, t) -> {
                if (t != null) {
                    handleError(t);
                    return;
                }
                if (!isCancelled.get()) {
                    subscription.request(1);
                }
            });
        }

        @Override
        public void onError(Throwable t) {
            handleError(t);
        }

        @Override
        public void onComplete() {
            future.complete(response);
        }

        private void handleError(Throwable t) {
            publisherToUpstream.error(t);
            future.completeExceptionally(t);
        }
    }

    public static <ResponseT, ResultT> Builder<ResponseT, ResultT> builder() {
        return new Builder<>();
    }

    public static final class Builder<ResponseT, ResultT> {

        private Long maximumBufferSize;
        private CompletableFuture<ResultT> returnFuture;
        private AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer;

        private Builder() {
        }

        /**
         * The {@link AsyncResponseTransformer} that will receive the data from each of the individually published
         * {@link IndividualTransformer}, usually intended to be the one on which the
         * {@link AsyncResponseTransformer#split(SplittingTransformerConfiguration)})} method was called.
         *
         * @param upstreamResponseTransformer the {@code AsyncResponseTransformer} that was split.
         * @return an instance of this builder
         */
        public Builder<ResponseT, ResultT> upstreamResponseTransformer(
            AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer) {
            this.upstreamResponseTransformer = upstreamResponseTransformer;
            return this;
        }

        /**
         * The amount of data in byte this publisher will buffer into memory before sending it to the upstream transformer. The
         * data will be sent if chunk of {@code maximumBufferSize} to the upstream transformer unless the subscription is
         * cancelled while less amount is buffered, in which case a chunk with a size less than {@code maximumBufferSize} will be
         * sent.
         *
         * @param maximumBufferSize the amount of data buffered and the size of the chunk of data
         * @return an instance of this builder
         */
        public Builder<ResponseT, ResultT> maximumBufferSizeInBytes(Long maximumBufferSize) {
            this.maximumBufferSize = maximumBufferSize;
            return this;
        }

        /**
         * The future that will be completed when the future which is returned by the call to
         * {@link AsyncResponseTransformer#prepare()} completes.
         *
         * @param returnFuture the future to complete.
         * @return an instance of this builder
         */
        public Builder<ResponseT, ResultT> resultFuture(CompletableFuture<ResultT> returnFuture) {
            this.returnFuture = returnFuture;
            return this;
        }

        public SplittingTransformer<ResponseT, ResultT> build() {
            return new SplittingTransformer<>(this.upstreamResponseTransformer,
                                              this.maximumBufferSize,
                                              this.returnFuture);
        }
    }
}
