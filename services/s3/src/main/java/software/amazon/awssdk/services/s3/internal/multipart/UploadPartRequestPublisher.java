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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.services.s3.internal.crt.RequestConversionUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Generates {@link UploadPartRequest} and the associated {@link AsyncRequestBody} pairs.
 * Upon subscription request, it will start to prepare requests asynchronously by loading content from the provided
 * {@link AsyncRequestBody}. The maximum memory consumed is {@code numOfPartsBuffered * partSize} MB.
 */
@SdkInternalApi
public final class UploadPartRequestPublisher implements SdkPublisher<Pair<UploadPartRequest, AsyncRequestBody>> {
    private static final Logger log = Logger.loggerFor(UploadPartRequestPublisher.class);
    private static final int DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024;
    private final String uploadId;
    private final long optimalPartSize;
    private final PutObjectRequest putObjectRequest;
    private final AsyncRequestBody asyncRequestBody;
    private final ByteBufferStoringSubscriber byteBufferStoringSubscriber;
    private final int numOfPartsBuffered;
    private final Executor executor;
    private final AtomicInteger numOfRemainingParts;
    private final AtomicInteger partNumberIndex = new AtomicInteger(1);
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private final BlockingDeque<Pair<UploadPartRequest, AsyncRequestBody>> requests = new LinkedBlockingDeque<>();
    private volatile boolean done = false;
    private volatile boolean upstreamDone = false;
    private final AtomicLong outstandingDemand = new AtomicLong();
    private final AtomicBoolean isPreparingRequests = new AtomicBoolean(false);
    private final AtomicBoolean isFlushingBuffer = new AtomicBoolean(false);
    private final long lastPartSize;

    private final Object lock = new Object();

    public UploadPartRequestPublisher(Builder builder) {
        this.uploadId = builder.uploadId;
        this.optimalPartSize = builder.partSize;
        this.putObjectRequest = builder.putObjectRequest;
        this.asyncRequestBody = builder.asyncRequestBody;
        this.byteBufferStoringSubscriber = new ByteBufferStoringSubscriber(builder.partSize);
        this.numOfPartsBuffered = builder.numOfPartsBuffered;
        this.executor = builder.executor;
        int totalParts = builder.numOfParts;
        numOfRemainingParts = new AtomicInteger(totalParts);
        lastPartSize = builder.contentLength - optimalPartSize * (totalParts - 1);
    }

    public static Builder builder() {
        return new UploadPartRequestPublisher.Builder();
    }

    @Override
    public void subscribe(Subscriber<? super Pair<UploadPartRequest, AsyncRequestBody>> subscriber) {
        subscriber.onSubscribe(new RequestSubscription(subscriber));
    }

    private class RequestSubscription implements Subscription {
        private final Subscriber<? super Pair<UploadPartRequest, AsyncRequestBody>> subscriber;

        RequestSubscription(Subscriber<? super Pair<UploadPartRequest, AsyncRequestBody>> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            log.trace(() -> "received request, current demand: " + outstandingDemand.get());
            if (n <= 0) {
                signalSubscriberError(n);
                return;
            }

            if (done) {
                return;
            }

            outstandingDemand.updateAndGet(current -> {
                if (Long.MAX_VALUE - current < n) {
                    return Long.MAX_VALUE;
                }

                return current + n;
            });


            if (subscribed.compareAndSet(false, true)) {
                asyncRequestBody.subscribe(byteBufferStoringSubscriber);
            }

            if (requests.isEmpty()) {
                prepareRequests();
            }
            flushBuffer();
        }

        private void signalSubscriberError(long n) {
            subscriber.onSubscribe(new NoopSubscription(subscriber));
            IllegalArgumentException failure = new IllegalArgumentException("A downstream publisher requested an invalid "
                                                                            + "amount of data: " + n);
            subscriber.onError(failure);
        }

        @Override
        public void cancel() {
            // TODO: cancel upstream
        }

        public void flushBuffer() {
            if (isFlushingBuffer.compareAndSet(false, true)) {
                try {
                    log.trace(() -> "Flushing buffer, current outstanding demand: " + outstandingDemand.get());
                    long demand = outstandingDemand.get();

                    while (demand > 0) {
                        if (isComplete()) {
                            signalOnComplete();
                            return;
                        }

                        if (requests.isEmpty()) {
                            prepareRequests();
                        }

                        Pair<UploadPartRequest, AsyncRequestBody> request = takeFirstRequest();
                        signalOnNext(request);
                        demand = outstandingDemand.decrementAndGet();
                    }
                    if (isComplete()) {
                        signalOnComplete();
                    }
                } catch (Throwable throwable) {
                    signalOnError(throwable);
                } finally {
                    isFlushingBuffer.set(false);
                }
            }
        }

        private boolean isComplete() {
            return numOfRemainingParts.get() == 0 && requests.isEmpty();
        }

        public void prepareRequests() {
            CompletableFuture.runAsync(this::doPrepareRequests, executor).exceptionally(throwable -> {
                signalOnError(new RuntimeException("Failed to prepare requests ", throwable));
                return null;
            });
        }

        public void signalOnComplete() {
            synchronized (lock) {
                if (!done) {
                    log.trace(() -> "signalling onComplete");
                    done = true;
                    subscriber.onComplete();
                }
            }
        }

        public void signalOnError(Throwable throwable) {
            synchronized (lock) {
                if (!done) {
                    done = true;
                    subscriber.onError(new RuntimeException("Failed to prepare requests ", throwable));
                }
            }
        }

        public void signalOnNext(Pair<UploadPartRequest, AsyncRequestBody> request) {
            log.trace(() -> "Signalling onNext, current outstanding demand: " + outstandingDemand.get());
            synchronized (lock) {
                subscriber.onNext(request);
            }
        }

        /**
         * Load the content in memory. The maximum memory size utilized is partSize * numOfPartsBuffered
         */
        public void doPrepareRequests() {
            if (isPreparingRequests.compareAndSet(false, true)) {
                try {
                    if (upstreamDone) {
                        return;
                    }

                    int remainingParts = numOfRemainingParts.get();
                    int numOfParts = Math.min(remainingParts, numOfPartsBuffered);
                    int finalRemainingParts = remainingParts;
                    log.info(() -> {
                        int index = partNumberIndex.get();
                        return String.format("Starting to prepare requests for parts %d-%d. Remaining"
                                             + " parts: %d",
                                             index,
                                             index + numOfParts - 1, finalRemainingParts);
                    });

                    generateRequests(remainingParts, numOfParts);
                } finally {
                    isPreparingRequests.set(false);
                }
            }
        }

        private void generateRequests(int remainingParts, int numOfParts) {
            for (int i = 0; i < numOfParts; i++) {
                log.trace(() -> String.format("Processing current part %d", numOfRemainingParts.get()));

                long currentPartSize = remainingParts == 1 ? lastPartSize : optimalPartSize;

                int numOfBuffersForCurrentPart = (int) Math.ceil(currentPartSize / (double) DEFAULT_BUFFER_SIZE);

                SimplePublisher<ByteBuffer> simplePublisher = new SimplePublisher<>();

                sendByteBuffersToSimplePublisher(numOfBuffersForCurrentPart, currentPartSize, simplePublisher);

                int currentPartNum = partNumberIndex.getAndIncrement();
                UploadPartRequest uploadPartRequest =
                    RequestConversionUtils.toUploadPartRequest(putObjectRequest,
                                                               currentPartNum,
                                                               uploadId);

                remainingParts = numOfRemainingParts.decrementAndGet();
                requests.add(Pair.of(uploadPartRequest, new AsyncRequestBody() {
                    @Override
                    public Optional<Long> contentLength() {
                        return Optional.of(currentPartSize);
                    }

                    @Override
                    public void subscribe(Subscriber<? super ByteBuffer> s) {
                        simplePublisher.subscribe(s);
                    }
                }));

                if (remainingParts == 0) {
                    log.debug(() -> "AsyncRequestBody has been drained. All requests have been generated");
                    upstreamDone = true;
                }
            }
        }

        private void sendByteBuffersToSimplePublisher(int numOfBuffersForCurrentPart,
                                                      long remainingBytes,
                                                      SimplePublisher<ByteBuffer> simplePublisher) {
            for (int bufferIndex = 0; bufferIndex < numOfBuffersForCurrentPart; bufferIndex++) {
                int bufferSize = adjustBufferSize(numOfBuffersForCurrentPart, remainingBytes, bufferIndex);

                ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
                byteBufferStoringSubscriber.blockingTransferTo(byteBuffer);

                ByteBuffer onePart = byteBuffer.asReadOnlyBuffer();
                onePart.rewind();

                remainingBytes -= bufferSize;
                simplePublisher.send(onePart).whenComplete((r, t) -> {
                    if (t != null) {
                        log.debug(() -> "Failed to send the ByteBuffer to the downstream subscriber" + onePart, t);
                        subscriber.onError(SdkClientException.create("Failed to send the ByteBuffer to the downstream "
                                                                     + "subscriber ",
                                                                     t));
                    } else {
                        log.trace(() -> "Delivered the ByteBuffer to the downstream subscriber " + onePart);
                    }
                });
            }
        }

        private int adjustBufferSize(int numOfBuffersForCurrentPart, long remainingBytes, int bufferIndex) {
            // If this is the last buffer for this part
            if (bufferIndex == numOfBuffersForCurrentPart - 1) {
                return NumericUtils.saturatedCast(remainingBytes);
            }

            return DEFAULT_BUFFER_SIZE;
        }

        public Pair<UploadPartRequest, AsyncRequestBody> takeFirstRequest() {
            try {
                return requests.takeFirst();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for next event", e);
            }
        }
    }

    public static final class Builder {
        private String uploadId;
        private long partSize;

        private long contentLength;
        private PutObjectRequest putObjectRequest;
        private AsyncRequestBody asyncRequestBody;
        private int numOfParts;
        private int numOfPartsBuffered;
        private Executor executor;

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder partSize(long partSize) {
            this.partSize = partSize;
            return this;
        }

        public Builder contentLength(long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Builder numOfParts(int numOfParts) {
            this.numOfParts = numOfParts;
            return this;
        }

        public Builder putObjectRequest(PutObjectRequest putObjectRequest) {
            this.putObjectRequest = putObjectRequest;
            return this;
        }

        public Builder asyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = asyncRequestBody;
            return this;
        }

        public Builder numOfPartsBuffered(int numOfPartsBuffered) {
            this.numOfPartsBuffered = numOfPartsBuffered;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public UploadPartRequestPublisher build() {
            return new UploadPartRequestPublisher(this);
        }
    }

}