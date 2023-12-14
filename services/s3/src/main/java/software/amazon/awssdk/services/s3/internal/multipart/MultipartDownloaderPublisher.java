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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Logger;

/**
 * Publishes {@link OrderedByteBuffer} potentially not in the natural order they should be in
 */
// todo
//  single part larger than configured buffer size
public class MultipartDownloaderPublisher<T> implements SdkPublisher<OrderedByteBuffer> {
    private static final Logger log = Logger.loggerFor(MultipartDownloaderPublisher.class);

    private final S3AsyncClient s3AsyncClient;
    private final AtomicInteger partNumber;

    private Integer totalParts;
    private AtomicBoolean totalPartsIsKnown = new AtomicBoolean(false);
    private final AtomicInteger partsRequested = new AtomicInteger(1);
    private final GetObjectRequest request;

    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final AtomicBoolean isCompleted = new AtomicBoolean(false);

    private AtomicReference<GetObjectResponse> responseRef = new AtomicReference<>();

    private Subscription subscription = new DownloaderSubscription();

    /**
     * The subscriber that will receive the OrderedByteBuffer instance, potentially out of their natural ordering
     */
    private Subscriber<? super OrderedByteBuffer> subscriber;

    private AtomicBoolean onStreamCalled = new AtomicBoolean(false);

    /**
     * Original AsyncResponseTransformer
     */
    private AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer;

    /**
     * The downstream Publisher that will send the bytes in order
     */
    private SdkPublisher<ByteBuffer> downstreamPublisher;

    public MultipartDownloaderPublisher(S3AsyncClient client,
                                        GetObjectRequest request,
                                        AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {
        this.s3AsyncClient = client;
        this.request = request;
        this.asyncResponseTransformer = asyncResponseTransformer;
        this.partNumber = new AtomicInteger(0);
    }

    public void start() {
        subscription.request(1);
    }

    @Override
    public void subscribe(Subscriber<? super OrderedByteBuffer> s) {
        this.subscriber = s;
        subscriber.onSubscribe(subscription);
    }

    private boolean isCompleted() {
        return totalParts != null && isCompleted.get();
    }

    private final class DownloaderSubscription implements Subscription {
        @Override
        public void request(long n) {
            // todo(downloader) do we need to deal with amount requested?
            if (isCancelled.get()) {
                log.info(() -> "DownloaderSubscription: New part requested but multipart is cancelled");
                return;
            }

            if (isCompleted()) {
                log.info(() -> String.format("DownloaderSubscription: New part '%d' requested but multipart is completed ",
                                             partNumber.get() + 1));
                return; // do nothing if requesting more parts
            }

            final int requestedPart = partNumber.incrementAndGet();
            log.info(() -> String.format("DownloaderSubscription: requesting new part: %d", partNumber.get()));

            partsRequested.incrementAndGet();
            GetObjectRequest getObjectRequest = request.copy(b -> b.partNumber(requestedPart));
            CompletableFuture<ResponsePublisher<GetObjectResponse>> response = s3AsyncClient.getObject(
                getObjectRequest, AsyncResponseTransformer.toPublisher());
            response.whenComplete((p, t) -> {
                log.info(() -> String.format("received response for part %d", requestedPart));
                if (t != null) {
                    log.error(() -> String.format("error received for part %s", requestedPart), t);
                    isCancelled.set(true);
                    subscriber.onError(t);
                    return;
                }
                if (requestedPart == 1) {
                    responseRef.set(p.response());
                    // todo(response) deal with response object in a better way
                    log.info(() -> "DownloaderSubscription calling asyncResponseTransformer onResponse");
                    asyncResponseTransformer.onResponse(p.response());
                }
                if (totalParts == null) {
                    log.info(() -> "!!!!!!!!!!! total parts = " + p.response().partsCount());
                    totalParts = p.response().partsCount();
                }
                p.subscribe(new IndividualPartSubscriber(requestedPart, p.response()));
            });
        }

        @Override
        public void cancel() {
            isCancelled.set(true);
        }
    }

    private final class IndividualPartSubscriber implements Subscriber<ByteBuffer> {
        private final int partNumber;
        private final OrderedByteBuffer orderedByteBuffer;
        private final GetObjectResponse response;
        private Subscription individualPartSubscription;

        private IndividualPartSubscriber(int partNumber, GetObjectResponse response) {
            this.partNumber = partNumber;
            this.response = response;
            this.orderedByteBuffer = new OrderedByteBuffer(partNumber, response.contentLength());
        }

        @Override
        public void onSubscribe(Subscription individualPartSubscription) {
            this.individualPartSubscription = individualPartSubscription;
            individualPartSubscription.request(this.response.contentLength());
        }

        @Override
        public void onNext(ByteBuffer body) {
            if (onStreamCalled.compareAndSet(false, true)) {
                log.info(() -> String.format("IndividualPartSubscriber[%d] calling onStream", partNumber));
                asyncResponseTransformer.onStream(downstreamPublisher);
            }
            this.orderedByteBuffer.put(body);
        }

        @Override
        public void onError(Throwable t) {
            log.error(() -> String.format("IndividualPartSubscriber[%d] onError: %s", partNumber, t), t);
            this.individualPartSubscription.cancel();
            isCancelled.set(true);
            subscriber.onError(t);
        }

        @Override
        public void onComplete() {
            log.info(() -> String.format("IndividualPartSubscriber[%d] onComplete: finished part", partNumber));
            subscriber.onNext(orderedByteBuffer);
            if (partsRequested.get() >= totalParts) {
                isCompleted.compareAndSet(false, true);
                log.info(() -> String.format("!!!!!!!!!!!!!! IndividualPartSubscriber[%d] onComplete: finished LAST!!! part",
                                             partNumber));
                subscriber.onComplete();
            }
        }
    }

    public Subscription subscription() {
        return subscription;
    }

    public GetObjectResponse response() {
        return responseRef.get();
    }

    public void downstreamPublisher(SdkPublisher<ByteBuffer> publisher) {
        this.downstreamPublisher = publisher;
    }
}
