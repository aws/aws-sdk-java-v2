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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 *
 */
@SdkInternalApi
public class SplittingTransformer<ResponseT, ResultT>
    implements SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> {
    private static final Logger log = Logger.loggerFor(SplittingTransformer.class);

    // transformer on which `.split()` was called
    private final AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer;

    private final ByteBufferStoringSubscriber bufferingSubscriber;

    // link between individual byte buffers of each AsyncResponseTransformer and ByteBufferStoringSubscriber
    private SimplePublisher<ByteBuffer> publisherToBuffering;

    // todo we probably dont need all of these?
    private final AtomicBoolean preparedCalled = new AtomicBoolean(false);
    private final AtomicBoolean onResponseCalled = new AtomicBoolean(false);
    private final AtomicBoolean onStreamCalled = new AtomicBoolean(false);
    private final AtomicBoolean futureComplete = new AtomicBoolean(false);

    // the future to complete when upstream is finished
    private final CompletableFuture<ResultT> returnFuture;

    // This future should get completed by the user-provided upstream AsyncResponseTransformer in its subscriber onComplete
    // method to the publisher of the onStream method
    private CompletableFuture<ResultT> upstreamFuture;

    private final long maximumBufferSize;
    private AtomicInteger currentlyBuffered = new AtomicInteger(0);

    // the subscriber for the upstream publisher
    private Subscriber<? super ByteBuffer> upstreamSubscriber;

    private SimplePublisher<ByteBuffer> tmp_publisherToUpstream = new SimplePublisher<>();

    public SplittingTransformer(AsyncResponseTransformer<ResponseT, ResultT> upstreamResponseTransformer,
                                long bufferSize, CompletableFuture<ResultT> returnFuture) {
        this.upstreamResponseTransformer = Validate.paramNotNull(upstreamResponseTransformer, "asyncRequestBody");
        this.returnFuture = Validate.paramNotNull(returnFuture, "returnFuture");
        this.maximumBufferSize = bufferSize;
        this.bufferingSubscriber = new ByteBufferStoringSubscriber(bufferSize);
        // this.publisherToBuffering = new SimplePublisher<>();
    }

    /**
     * @param downStreamSubscriber the {@link Subscriber} to the individual AsyncResponseTransformer
     */
    @Override
    public void subscribe(Subscriber<? super AsyncResponseTransformer<ResponseT, ResponseT>> downStreamSubscriber) {
        // publisherToBuffering.subscribe(bufferingSubscriber);
        downStreamSubscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                downStreamSubscriber.onNext(new IndividualTransformer(n));
            }

            @Override
            // called by downstream subscriber to notify no more data is needed, we are done
            public void cancel() {
                System.out.printf("[%s] cancel: cleaning up%n", "SplittingTransformer");
                tmp_publisherToUpstream.complete();
                // bufferingSubscriber.onComplete();
                // publisherToBuffering.complete();
                // upstreamSubscriber.onComplete();
                downStreamSubscriber.onComplete();
            }
        });
    }

    // Transformer for each of the individual requests.
    // A future is created per request that is completed when
    private class IndividualTransformer implements AsyncResponseTransformer<ResponseT, ResponseT> {
        private final long requested;
        private ResponseT response;
        private CompletableFuture<ResponseT> individualFuture;

        public IndividualTransformer(long requested) {
            this.requested = requested;
        }

        @Override
        public CompletableFuture<ResponseT> prepare() {
            this.individualFuture = new CompletableFuture<>();
            if (preparedCalled.compareAndSet(false, true)) {
                upstreamFuture = upstreamResponseTransformer.prepare();
                upstreamFuture.whenComplete((res, e) -> {
                    System.out.println("[IndividualTransformer] upstream future complete");
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
            System.out.println("[IndividualTransformer] onResponse");
            System.out.println("response: " + response.toString());
            // call onResponse on upstream only once
            if (onResponseCalled.compareAndSet(false, true)) {
                upstreamResponseTransformer.onResponse(response);
            }
            this.response = response;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            // call onStream on upstream only once
            if (onStreamCalled.compareAndSet(false, true)) {
                // upstreamResponseTransformer.onStream(new UpstreamPublisher());
                upstreamResponseTransformer.onStream(SdkPublisher.adapt(tmp_publisherToUpstream));
            }

            // send the bytes to publisherToBuffering
            System.out.println("[IndividualTransformer] onStream");
            publisher.subscribe(new IndividualPartSubscriber(requested, this.individualFuture, response));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            upstreamResponseTransformer.exceptionOccurred(error);
        }
    }

    // the Subscriber for each of the individual request ByteBuffer publisher
    private class IndividualPartSubscriber implements Subscriber<ByteBuffer> {
        private final long requested;
        private final CompletableFuture<ResponseT> future;
        private final ResponseT response;
        private Subscription subscription;

        public IndividualPartSubscriber(long requested, CompletableFuture<ResponseT> future, ResponseT response) {
            this.requested = requested;
            this.future = future;
            this.response = response;
        }

        @Override
        public void onSubscribe(Subscription s) {
            System.out.println("[IndividualPartSubscriber] onSubscribe");
            this.subscription = s;
            s.request(Long.MAX_VALUE); // todo
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            // System.out.println("[IndividualBufferingSubscriber] onNext");
            // todo check if we have enough memory buffered left
            currentlyBuffered.addAndGet(byteBuffer.capacity());

            // send data to bufferingSubscriber via publisherToBuffering
            // publisherToBuffering.send(byteBuffer);

            tmp_publisherToUpstream.send(byteBuffer);
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onError(Throwable t) {
            System.out.println("[IndividualPartSubscriber] onError");
            bufferingSubscriber.onError(t);
        }

        @Override
        public void onComplete() {
            System.out.println("[IndividualPartSubscriber] onComplete");
            future.complete(response);
        }
    }

    // byte buffer publisher for the upstream AsyncResponseTransformer
    private class UpstreamPublisher implements SdkPublisher<ByteBuffer> {
        private AtomicBoolean completed = new AtomicBoolean(false);

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            upstreamSubscriber = subscriber;
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long requested) {
                    System.out.printf("[UpstreamPublisher] requested %d%n", requested);
                    if (completed.get()) {
                        System.out.println("[UpstreamPublisher] already completed");
                        return;
                    }
                    int bufferedAmount = currentlyBuffered.get();
                    if (bufferedAmount <= 0) {
                        // I don't have any data to send you, I can't fulfill this request right now
                        System.out.println("[UpstreamPublisher] nothing buffered yet");
                        return;
                    }
                    System.out.printf("[UpstreamPublisher] currently buffered: %d%n", bufferedAmount);
                    int amountToSend = Math.min(NumericUtils.saturatedCast(requested), bufferedAmount);
                    System.out.printf("[UpstreamPublisher] sending: %d%n", amountToSend);
                    ByteBuffer out = ByteBuffer.allocate(amountToSend);
                    bufferingSubscriber.transferTo(out);
                    currentlyBuffered.addAndGet(-amountToSend);
                    subscriber.onNext(out);
                }

                @Override
                public void cancel() {
                    System.out.println("[UpstreamPublisher] cancel");
                    completed.compareAndSet(false, true);
                    upstreamSubscriber.onComplete();
                }
            });
        }
    }

}