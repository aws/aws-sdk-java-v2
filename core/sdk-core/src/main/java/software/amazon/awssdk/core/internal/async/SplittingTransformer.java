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
 *
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
     * Future to track the status of the upstreamResponseTransformer. Will be completed when the future returned by calling
     * {@code prepare()} on the upstreamResponseTransformer itself completes.
     */
    private final CompletableFuture<ResultT> returnFuture;

    /**
     * The buffer size used to buffer the content received from the downstream subscriber
     */
    private final int maximumBufferSize;

    /**
     * This publisher is used to send the bytes received from the downstream subscriber to a
     * {@link DelegatingBufferingSubscriber} that will buffer a number of bytes specified by the {@code maximumBufferSize}.
     */
    private final SimplePublisher<ByteBuffer> publisherToUpstream = new SimplePublisher<>();

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
        downstreamSubscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                downstreamSubscriber.onNext(new IndividualTransformer(n));
            }

            @Override
            public void cancel() {
                // called by downstream subscriber to notify no more data is needed, we are done
                // System.out.printf("[%s] cancel: cleaning up%n", "SplittingTransformer");
                publisherToUpstream.complete();
                downstreamSubscriber.onComplete();
            }
        });
    }

    // The AsyncResponseTransformer for each of the individual requests that is sent back to the downstreamSubscriber when
    // requested. A future is created per request that is completed when onComplete is called on the subscriber for that request
    // body publisher.
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
                CompletableFuture<ResultT> upstreamFuture = upstreamResponseTransformer.prepare();
                CompletableFutureUtils.forwardExceptionTo(returnFuture, upstreamFuture);
                upstreamFuture.whenComplete((res, e) -> {
                    // System.out.println("[IndividualTransformer] upstream future complete");
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
            // System.out.println("[IndividualTransformer] onResponse: " + response.toString());
            if (onResponseCalled.compareAndSet(false, true)) {
                upstreamResponseTransformer.onResponse(response);
            }
            this.response = response;
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            if (onStreamCalled.compareAndSet(false, true)) {
                upstreamResponseTransformer.onStream(
                    subscriber -> publisherToUpstream.subscribe(new DelegatingBufferingSubscriber(maximumBufferSize, subscriber)));
            }

            // send the bytes to publisherToBuffering
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
            this.subscription = s;
            // request everything, data will be buffered by the DelegatingBufferingSubscriber
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            // currentlyBuffered.addAndGet(byteBuffer.capacity());

            publisherToUpstream.send(byteBuffer);
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onError(Throwable t) {
            // System.out.println("[IndividualPartSubscriber] onError");
            publisherToUpstream.error(t);
            future.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            // System.out.println("[IndividualPartSubscriber] onComplete");
            future.complete(response);
        }
    }

}