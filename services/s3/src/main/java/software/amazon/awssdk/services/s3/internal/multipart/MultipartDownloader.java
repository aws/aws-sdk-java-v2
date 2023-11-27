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
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Helper class to handle multipart get requests to S3 using part number. Will buffer the response body Single-use helper class
 * for a multipart get response.
 * <p>DO NOT REUSE THAT CLASS FOR MULTIPLE REQUESTS!
 * <p>
 * todo(future)
 *  Future cancellation and error handling needs to be done correctly
 * <p>
 * todo(error)
 *  When an error is encountered, stop the process and cancel in-flight requests
 *
 * @param <T> Type that the response handler produces. I.E. the type you are transforming the response into.
 */
@SdkInternalApi
public class MultipartDownloader<T> {

    private static final Logger log = Logger.loggerFor(MultipartDownloader.class);

    /**
     * The {@link S3AsyncClient} delegate that will perform individual Get Object requests
     */
    private final S3AsyncClient s3AsyncClient;

    /**
     * The original {@link GetObjectRequest} that was provided, to be converted into multiple Get Object with various part
     * number.
     */
    private GetObjectRequest getObjectRequest;

    /**
     * The provided {@link AsyncResponseTransformer} of the original Get Object request. Bodies of each individual part are sent
     * to this instance in order.
     */
    private AsyncResponseTransformer<GetObjectResponse, T> responseTransformer;

    /**
     * The future that will be returned by the call to
     * {@link S3AsyncClient#getObject(GetObjectRequest, AsyncResponseTransformer)}.
     */
    private CompletableFuture<T> returnFuture;

    /**
     * The total number of parts of the object being downloaded, as returned in the response of the call to
     * {@link S3AsyncClient#getObject(GetObjectRequest, AsyncResponseTransformer)}.
     */
    private Integer totalParts;

    /**
     * Out of all parts that are being downloaded, this field indicated the furthest one. For example, if part 3, 4 and 5 are
     * being downloaded, and we are waiting for buffer to be available to dowload part 6, this field will have value '5'.
     */
    private final AtomicInteger highestDownloadingPart = new AtomicInteger(0);

    /**
     * The number of completed parts. Parts are completed once the have been consumed but the downstream
     * {@link AsyncResponseTransformer}.
     */
    private final AtomicLong completedParts = new AtomicLong(0);

    /**
     * Indicates if this multipart request is cancelled, ususally due to an individual part failure
     */
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    /**
     * The maximum amount of memory buffer configured.
     */
    private final long maxMemBufferSizeInBytes;

    /**
     * The current amount of byte buffered in memory
     */
    private final AtomicLong totalMemBufferedInBytes = new AtomicLong(0);

    /**
     * Publisher that send buffered parts that are ready to be consumed by to the downstream {@link AsyncResponseTransformer}
     */
    private final SimplePublisher<ByteBuffer> bodyPartsPublisher = new SimplePublisher<>();

    /**
     * Ordered collection of individual buffered body parts. Filled within {@link IndividualBodyPartSubscriber#onNext(ByteBuffer)}
     * when body ByteBuffer is received and emptied in {@link IndividualBodyPartSubscriber#onComplete()}.
     */
    private final PriorityQueue<PartBuffer> bufferedParts = new PriorityQueue<>(Comparator.comparingInt(p -> p.partNumber));

    MultipartDownloader(S3AsyncClient s3AsyncClient, long maxMemBufferSizeInBytes) {
        this.s3AsyncClient = s3AsyncClient;
        this.maxMemBufferSizeInBytes = maxMemBufferSizeInBytes;
    }

    public CompletableFuture<T> getObject(GetObjectRequest getObjectRequest,
                                          AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {

        this.responseTransformer = asyncResponseTransformer;
        this.getObjectRequest = getObjectRequest;
        this.returnFuture = new CompletableFuture<>();

        GetObjectRequest firstPartRequest = getObjectRequest.copy(b -> b.partNumber(1));

        MultiGetAsyncResponseTransformer multiResponseTransformer = new MultiGetAsyncResponseTransformer();
        log.info(() -> String.format("sending GetObject request for part %d", 1));
        this.highestDownloadingPart.incrementAndGet();
        CompletableFuture<T> response = s3AsyncClient.getObject(firstPartRequest, multiResponseTransformer);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, response); // propagate cancellation
        return returnFuture;
    }

    private void sendGetRequestIfBufferIsAvailable() {
        // do not send more request if multipart encountered an error
        if (isCancelled.get()) {
            return;
        }

        if (!bufferIsFull() && !completedLastPart() && highestDownloadingPart.get() < totalParts) {
            highestDownloadingPart.incrementAndGet();
            log.info(() -> String.format("sending GetObject request for part %d", highestDownloadingPart.get()));
            GetObjectRequest partRequest = getObjectRequest.copy(b -> b.partNumber(highestDownloadingPart.get()));
            CompletableFuture<ResponsePublisher<GetObjectResponse>> responsePublisherFuture =
                s3AsyncClient.getObject(partRequest, AsyncResponseTransformer.toPublisher());
            responsePublisherFuture.whenComplete((responsePublisher, e) -> {
                if (e != null) {
                    // todo(future): need to stop in flight requests?
                    returnFuture.completeExceptionally(e);
                    return;
                }
                // body starts streaming
                GetObjectResponse response = responsePublisher.response();
                responsePublisher.subscribe(new IndividualBodyPartSubscriber(highestDownloadingPart.get(), response));
                totalMemBufferedInBytes.addAndGet(response.contentLength());
                sendGetRequestIfBufferIsAvailable();
            });
        }
    }

    /**
     * The AsyncResponseTransformer of the first GetObject request being used
     */
    private final class MultiGetAsyncResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, T> {

        private GetObjectResponse response;

        @Override
        public CompletableFuture<T> prepare() {
            CompletableFuture<T> asyncResponseTransformerFuture = responseTransformer.prepare();
            asyncResponseTransformerFuture.whenComplete((t, e) -> {
                if (e != null) {
                    returnFuture.completeExceptionally(e);
                    return;
                }
                returnFuture.complete(t);
            });
            CompletableFutureUtils.forwardExceptionTo(returnFuture, asyncResponseTransformerFuture);
            return asyncResponseTransformerFuture;
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            this.response = response;
            totalParts = response.partsCount();
            if (!isSinglePartObject()) {
                log.info(() -> String.format("Total parts: %d. Using multipart Get with part number", totalParts));
                sendGetRequestIfBufferIsAvailable();
            }
            // todo: if parts, just use the first response for now.
            //  But we might need to construct a custom response object instead.
            responseTransformer.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            if (isSinglePartObject()) {
                // do not use multipart
                responseTransformer.onStream(publisher);
                return;
            }
            publisher.subscribe(new IndividualBodyPartSubscriber(1, response));
            responseTransformer.onStream(SdkPublisher.adapt(bodyPartsPublisher));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            log.info(() -> "MultiGetAsyncResponseTransformer exceptionOccurred : " + error.toString());
            isCancelled.set(true);
            responseTransformer.exceptionOccurred(error);
        }
    }

    /**
     * Subscriber to each of the individual body parts being received. Manages the buffering of each individual part body into
     * memory, and flushing of the buffer once it is full.
     */
    private final class IndividualBodyPartSubscriber implements Subscriber<ByteBuffer> {
        private final int partNumber;
        private final PartBuffer partsToRequest;
        private final GetObjectResponse response;
        private Subscription subscription;

        private IndividualBodyPartSubscriber(int partNumber, GetObjectResponse response) {
            this.partNumber = partNumber;
            this.response = response;
            this.partsToRequest = new PartBuffer(partNumber, response.contentLength());
            bufferedParts.offer(partsToRequest);
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(this.response.contentLength());
        }

        @Override
        public void onNext(ByteBuffer body) {
            this.partsToRequest.put(body);
            totalMemBufferedInBytes.addAndGet(body.capacity());
        }

        @Override
        public void onError(Throwable t) {
            log.error(() -> String.format("IndividualPartSubscriber[%d] onError: %s", partNumber, t), t);
            this.subscription.cancel();
            isCancelled.set(true);
            responseTransformer.exceptionOccurred(t);
        }

        @Override
        public void onComplete() {
            completedParts.incrementAndGet();
            if (bufferIsFull() || completedLastPart()) {
                // send full buffer to provided AsyncResponseTransformer
                log.info(() -> String.format(
                    "IndividualBodyPartSubscriber[%d] sending full buffer to downstream publisher", partNumber));
                // int total = bufferedParts
                //     .stream()
                //     .map(p -> {
                //         p.buffer.position(0);
                //         return p.buffer.capacity();
                //     }).mapToInt(i -> i)
                //     .sum();
                // ByteBuffer bb = ByteBuffer.allocate(total);
                while (!bufferedParts.isEmpty()) {
                    // ByteBuffer buffered = bufferedParts.poll().buffer;
                    // bb.put(buffered);
                    // PartBuffer part = bufferedParts.poll();
                    // part.sendToPublisher();
                    // bb.position(0);
                    SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
                    responseTransformer.onStream(SdkPublisher.adapt(publisher));
                    bufferedParts.poll().buffer.position(0);
                    publisher.send(bufferedParts.poll().buffer);
                    // bodyPartsPublisher.send(bb);
                }
                totalMemBufferedInBytes.set(0);

                if (!completedLastPart()) {
                    sendGetRequestIfBufferIsAvailable();
                }
            }

            log.info(() -> String.format(
                "Completed GetRequest for part %d "
                + "- %d/%d completed parts "
                + "- %s content range "
                + "- %d/%d current memory buffered",
                partNumber,
                completedParts.get(), totalParts,
                response.contentRange(),
                totalMemBufferedInBytes.get(), maxMemBufferSizeInBytes
            ));

            if (completedLastPart()) {
                // We are done with all the parts, complete the whole request
                log.info(() -> "All parts completed");
                bodyPartsPublisher.complete();
            } else {
                sendGetRequestIfBufferIsAvailable();
            }
        }
    }

    private boolean isSinglePartObject() {
        return totalParts == null || totalParts < 2;
    }

    private boolean bufferIsFull() {
        return totalMemBufferedInBytes.get() >= maxMemBufferSizeInBytes;
    }

    private boolean completedLastPart() {
        return completedParts.get() >= totalParts;
    }

    private class PartBuffer {
        private final int partNumber;
        private final ByteBuffer buffer;

        PartBuffer(int partNumber, Long contentLength) {
            this.partNumber = partNumber;
            this.buffer = ByteBuffer.allocate(contentLength.intValue());
        }

        void sendToPublisher() {
            this.buffer.position(0);
            bodyPartsPublisher.send(this.buffer);
        }

        void put(ByteBuffer body) {
            this.buffer.put(body);
        }
    }

}
