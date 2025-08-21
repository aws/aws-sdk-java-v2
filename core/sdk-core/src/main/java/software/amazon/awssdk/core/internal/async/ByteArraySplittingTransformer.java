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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * A splitting transformer that creates individual {@link ByteArrayAsyncResponseTransformer} instances for each part of a
 * multipart download. This is necessary to support retries of individual part downloads.
 *
 * <p>
 * This class is created by {@link ByteArrayAsyncResponseTransformer#split} and used internally by the multipart
 * download logic.
 */

@SdkInternalApi
public class ByteArraySplittingTransformer<ResponseT> implements SdkPublisher<AsyncResponseTransformer<ResponseT, ResponseT>> {
    private static final Logger log = Logger.loggerFor(ByteArraySplittingTransformer.class);
    private final AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> upstreamResponseTransformer;
    private final CompletableFuture<ResponseBytes<ResponseT>> resultFuture;
    private Subscriber<? super AsyncResponseTransformer<ResponseT, ResponseT>> downstreamSubscriber;
    private final AtomicInteger nextPartNumber = new AtomicInteger(1);
    private final AtomicReference<ResponseT> responseT = new AtomicReference<>();

    private final SimplePublisher<ByteBuffer> publisherToUpstream = new SimplePublisher<>();
    /**
     * The amount requested by the downstream subscriber that is still left to fulfill. Updated when the
     * {@link Subscription#request(long) request} method is called on the downstream subscriber's subscription. Corresponds to the
     * number of {@code AsyncResponseTransformer} that will be published to the downstream subscriber.
     */
    private final AtomicLong outstandingDemand = new AtomicLong(0);

    /**
     * This flag stops the current thread from publishing transformers while another thread is already publishing.
     */
    private final AtomicBoolean emitting = new AtomicBoolean(false);

    /**
     * Synchronization lock that protects the {@code onStreamCalled} flag and cancellation
     * workflow from concurrent access. Ensures thread-safety of subscription cancellation.
     */
    private final Object lock = new Object();

    /**
     * Set to true once {@code .onStream()} is called on the upstreamResponseTransformer
     */
    private boolean onStreamCalled;

    /**
     * Set to true once {@code .cancel()} is called in the subscription of the downstream subscriber, or if the
     * {@code resultFuture} is cancelled.
     */
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    private final Map<Integer, ByteBuffer> buffers;

    public ByteArraySplittingTransformer(AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>>
                                             upstreamResponseTransformer,
                                         CompletableFuture<ResponseBytes<ResponseT>> resultFuture) {
        this.upstreamResponseTransformer = upstreamResponseTransformer;
        this.resultFuture = resultFuture;
        this.buffers = new ConcurrentHashMap<>();
    }

    @Override
    public void subscribe(Subscriber<? super AsyncResponseTransformer<ResponseT, ResponseT>> subscriber) {
        this.downstreamSubscriber = subscriber;
        subscriber.onSubscribe(new DownstreamSubscription());
    }

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

            demand = outstandingDemand.decrementAndGet();
            downstreamSubscriber.onNext(new IndividualTransformer(nextPartNumber.getAndIncrement()));
        }
        return false;
    }

    /**
     * Handle the {@code .cancel()} signal received from the downstream subscription. Data that is being sent to the upstream
     * transformer need to finish processing before we complete. One typical use case for this is completing the multipart
     * download, the subscriber having reached the final part will signal that it doesn't need more
     * {@link AsyncResponseTransformer}s by calling {@code .cancel()} on the subscription.
     */
    private void handleSubscriptionCancel() {
        synchronized (lock) {
            if (downstreamSubscriber == null) {
                log.trace(() -> "downstreamSubscriber already null, skipping downstreamSubscriber.onComplete()");
                return;
            }
            if (!onStreamCalled) {
                // we never subscribe publisherToUpstream to the upstream, it would not complete
                downstreamSubscriber = null;
                return;
            }

            // if result future is already complete (likely by exception propagation), skip.
            if (resultFuture.isDone()) {
                return;
            }

            try {
                CompletableFuture<ResponseBytes<ResponseT>> upstreamPrepareFuture = upstreamResponseTransformer.prepare();
                CompletableFutureUtils.forwardResultTo(upstreamPrepareFuture, resultFuture);

                upstreamResponseTransformer.onResponse(responseT.get());

                int totalPartCount = nextPartNumber.get() - 1;
                if (buffers.size() != totalPartCount) {
                    resultFuture.completeExceptionally(
                        SdkClientException.create(String.format("Number of parts in buffer [%d] does not match total part count"
                                                                + " [%d], some parts did not complete successfully.",
                                                                buffers.size(), totalPartCount)));
                    return;
                }
                for (int i = 1; i <= totalPartCount; ++i) {
                    publisherToUpstream.send(buffers.get(i)).exceptionally(ex -> {
                        resultFuture.completeExceptionally(SdkClientException.create("unexpected error occurred", ex));
                        return null;
                    });
                }

                publisherToUpstream.complete().exceptionally(ex -> {
                    resultFuture.completeExceptionally(SdkClientException.create("unexpected error occurred", ex));
                    return null;
                });
                upstreamResponseTransformer.onStream(SdkPublisher.adapt(publisherToUpstream));

            } catch (Throwable throwable) {
                resultFuture.completeExceptionally(SdkClientException.create("unexpected error occurred", throwable));
            }
        }
    }

    private final class IndividualTransformer implements AsyncResponseTransformer<ResponseT, ResponseT> {
        private final int partNumber;
        private final ByteArrayAsyncResponseTransformer<ResponseT> delegate = new ByteArrayAsyncResponseTransformer<>();

        private IndividualTransformer(int partNumber) {
            this.partNumber = partNumber;
        }

        @Override
        public CompletableFuture<ResponseT> prepare() {
            CompletableFuture<ResponseBytes<ResponseT>> prepare = delegate.prepare();
            return prepare.thenApply(responseBytes -> {
                buffers.put(partNumber, responseBytes.asByteBuffer());
                return responseBytes.response();
            });
        }

        @Override
        public void onResponse(ResponseT response) {
            responseT.compareAndSet(null, response);
            delegate.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
            synchronized (lock) {
                if (!onStreamCalled) {
                    onStreamCalled = true;
                }
            }
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            delegate.exceptionOccurred(error);
        }
    }
}
