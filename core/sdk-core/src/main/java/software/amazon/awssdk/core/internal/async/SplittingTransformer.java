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
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.DelegatingBufferingSubscriber;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Split a {@link AsyncResponseTransformer} into multiple ones, publishing them as a {@link SdkPublisher}. Created using the
 * {@link AsyncResponseTransformer#split(long) split} method. The upstream {@link AsyncResponseTransformer} that is split will
 * receive data from the individual transformers.
 * <p>
 * This publisher also buffers an amount of data before sending it to the upstream transformer, as specified by the
 * maximumBufferSize. ByteBuffers will be published once the buffer has been reached, or when the subscription to this
 * publisher is cancelled.
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
    private final int maximumBufferSize;

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
     * {@link Subscription#request(long) request} method is called on the downstream subscriber's subscription.
     * Corresponds to the number of {@code AsyncResponseTransformer} that will be published to the downstream subscriber.
     */
    private final AtomicLong outstandingDemand = new AtomicLong(0);


    public SplittingTransformer(AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer,
                                long bufferSize, CompletableFuture<ResultT> returnFuture) {
        this.upstreamResponseTransformer = Validate.paramNotNull(upstreamResponseTransformer, "asyncRequestBody");
        this.returnFuture = Validate.paramNotNull(returnFuture, "returnFuture");
        this.maximumBufferSize = NumericUtils.saturatedCast(bufferSize);
    }

    /**
     * @param downstreamSubscriber the {@link Subscriber} to the individual AsyncResponseTransformer
     */
    @Override
    public void subscribe(Subscriber<? super AsyncResponseTransformer<ResponseT, ResponseT>> downstreamSubscriber) {
        this.downstreamSubscriber = downstreamSubscriber;
        downstreamSubscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                if (!isCancelled.get()) {
                    long totalNewDemand = outstandingDemand.addAndGet(n);
                    log.trace(() -> String.format("new outstanding demand: %s", totalNewDemand));
                    downstreamSubscriber.onNext(new IndividualTransformer());
                }
            }

            @Override
            public void cancel() {
                // called by downstream subscriber to notify no more data is needed, we are done
                if (isCancelled.compareAndSet(false, true)) {
                    log.trace(() -> "Cancelling splitting transformer");
                    publisherToUpstream.complete();
                    downstreamSubscriber.onComplete();
                }
            }
        });
    }

    // The AsyncResponseTransformer for each of the individual requests that is sent back to the downstreamSubscriber when
    // requested. A future is created per request that is completed when onComplete is called on the subscriber for that request
    // body publisher.
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
                upstreamFuture.whenComplete((res, e) -> {
                    if (e != null) {
                        returnFuture.completeExceptionally(e);
                    } else {
                        returnFuture.complete(res);
                    }
                });
            }
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
            if (onStreamCalled.compareAndSet(false, true)) {
                log.trace(() -> "calling onStream on the upstream transformer");
                upstreamResponseTransformer.onStream(upstreamSubscriber ->
                    publisherToUpstream.subscribe(new DelegatingBufferingSubscriber(maximumBufferSize, upstreamSubscriber))
                );
            }
            publisher.subscribe(new IndividualPartSubscriber(this.individualFuture, response));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            upstreamResponseTransformer.exceptionOccurred(error);
        }
    }

    // the Subscriber for each of the individual request ByteBuffer publisher
    private class IndividualPartSubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<ResponseT> future;
        private final ResponseT response;
        private Subscription subscription;

        IndividualPartSubscriber(CompletableFuture<ResponseT> future, ResponseT response) {
            this.future = future;
            this.response = response;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            // request everything, data will be buffered by the DelegatingBufferingSubscriber
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            publisherToUpstream.send(byteBuffer);
            // we can request everything, buffering is done in DelegatingBufferingSubscriber
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onError(Throwable t) {
            publisherToUpstream.error(t);
            future.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            future.complete(response);
            long demandLeft = outstandingDemand.decrementAndGet();
            log.trace(() -> String.format("demand left: %s", demandLeft));
            if (demandLeft > 0) {
                downstreamSubscriber.onNext(new IndividualTransformer());
            }
        }
    }
}
