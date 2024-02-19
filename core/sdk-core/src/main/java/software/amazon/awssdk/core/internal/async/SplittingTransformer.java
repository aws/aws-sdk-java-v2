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

    private SplittingTransformer(AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer,
                                 Long maximumBufferInBytes,
                                 CompletableFuture<ResultT> returnFuture) {
        this.upstreamResponseTransformer = Validate.paramNotNull(upstreamResponseTransformer, "asyncRequestBody");
        this.returnFuture = Validate.paramNotNull(returnFuture, "returnFuture");
        this.maximumBufferInBytes = Validate.notNull(maximumBufferInBytes, "maximumBufferInBytes");
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
                handleCancelState();
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

    private synchronized void handleCancelState() {
        if (downstreamSubscriber == null) {
            return;
        }
        publisherToUpstream.complete();
        downstreamSubscriber = null;
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
            individualFuture.whenComplete((r, e) -> {
                if (isCancelled.get()) {
                    handleCancelState();
                }
            });
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
                upstreamResponseTransformer.onStream(upstreamSubscriber -> publisherToUpstream.subscribe(
                    // DelegatingBufferingSubscriber.builder()
                    //                              .maximumBufferInBytes(maximumBufferInBytes)
                    //                              .delegate(upstreamSubscriber)
                    //                              .build())
                    upstreamSubscriber
                ));
            }
            publisher.subscribe(new IndividualPartSubscriber<>(this.individualFuture, response, publisherToUpstream));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            individualFuture.completeExceptionally(error);
            upstreamResponseTransformer.exceptionOccurred(error);
        }
    }

    /**
     * the Subscriber for each of the individual request's ByteBuffer publisher
     */
    static class IndividualPartSubscriber<T> implements Subscriber<ByteBuffer> {

        private final CompletableFuture<T> future;
        private final T response;
        private final SimplePublisher<ByteBuffer> publisherToUpstream;
        private Subscription subscription;

        IndividualPartSubscriber(CompletableFuture<T> future, T response,
                                 SimplePublisher<ByteBuffer> bodyPartPublisher) {
            this.future = future;
            this.response = response;
            this.publisherToUpstream = bodyPartPublisher;
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
                subscription.request(1);
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
         * {@link AsyncResponseTransformer#split(SplittingTransformerConfiguration)} )} method was called.
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
        public Builder<ResponseT, ResultT> returnFuture(CompletableFuture<ResultT> returnFuture) {
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
