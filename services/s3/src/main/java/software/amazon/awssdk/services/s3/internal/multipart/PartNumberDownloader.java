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

// todo(bug): some bytes are missing, not all bytes are not sent to the downstream AsyncResponseTransformer
//  once the future is completed.

/**
 * Helper class to handle multipart get requests to S3 using part number. Will buffer the response body Single-use helper class
 * for a multipart get
 * response.
 * <p>DO NOT REUSE THAT CLASS FOR MULTIPLE REQUESTS!
 * <p>
 * todo(future)
 *  Future cancellation and error handling needs to be done correctly
 * <p>
 * todo(customization)
 *  Need to be able to provide buffer size.
 * <p>
 * todo(error)
 *  When an error is encountered, stop the process and cancel in-flight requests
 *
 * @param <T>
 */
@SdkInternalApi
public class PartNumberDownloader<T> {

    private static final Logger log = Logger.loggerFor(PartNumberDownloader.class);
    private static final long DEFAULT_MEMORY_BUFFERED = 64 * 1024 * 1024; // todo(buffer) todo(customization)

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

    private CompletableFuture<T> returnFuture;

    private Integer totalParts;

    private int currentPart;

    private long individualPartContentLength;

    /**
     * The number of completed parts. Parts are completed once the have been consumed but the downstream
     * {@link AsyncResponseTransformer}.
     */
    private final AtomicLong completedParts = new AtomicLong(0);

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
    private PriorityQueue<PartBuffer> bufferedParts = new PriorityQueue<>(Comparator.comparingInt(p -> p.partNumber));

    private long debug_totalBytesTransferredToDownstreamPublisher = 0; // todo(debug) remove
    private long debug_totalBytesReceived = 0; // todo(debug) remove

    PartNumberDownloader(S3AsyncClient s3AsyncClient, long maxMemBufferSizeInBytes) {
        this.s3AsyncClient = s3AsyncClient;
        this.maxMemBufferSizeInBytes = maxMemBufferSizeInBytes;
    }

    public CompletableFuture<T> getObject(GetObjectRequest getObjectRequest,
                                          AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {

        this.responseTransformer = asyncResponseTransformer;
        this.getObjectRequest = getObjectRequest;
        this.returnFuture = new CompletableFuture<>();

        GetObjectRequest firstPartRequest = getObjectRequest.copy(b -> b.partNumber(1));

        MultiGetAsyncResponseTransformer multiResponseTransformer =
            new MultiGetAsyncResponseTransformer(asyncResponseTransformer);
        log.info(() -> String.format("sending GetObject request for part %d", 1));
        this.currentPart = 1;
        CompletableFuture<T> response = s3AsyncClient.getObject(firstPartRequest, multiResponseTransformer);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, response); // propagate cancellation
        return returnFuture;
    }

    private void sendGetRequestsUntilBufferIsFull() {
        long totalBufferAvailable = maxMemBufferSizeInBytes;
        while (totalBufferAvailable > 0 && currentPart < totalParts) {
            currentPart++;
            int partNumber = currentPart;
            log.info(() -> String.format("sending GetObject request for part %d", partNumber));
            GetObjectRequest partRequest = getObjectRequest.copy(b -> b.partNumber(partNumber));
            CompletableFuture<ResponsePublisher<GetObjectResponse>> responsePublisherFuture =
                s3AsyncClient.getObject(partRequest, AsyncResponseTransformer.toPublisher());
            responsePublisherFuture.whenComplete((responsePublisher, e) -> {
                if (e != null) {
                    // todo(future): need to stop in flight requests?
                    returnFuture.completeExceptionally(e);
                    return;
                }
                responsePublisher.subscribe(new IndividualBodyPartSubscriber(partNumber));
            });
            totalBufferAvailable -= individualPartContentLength;
        }
    }

    private class MultiGetAsyncResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, T> {

        private AsyncResponseTransformer<GetObjectResponse, T> responseTransformer;

        public MultiGetAsyncResponseTransformer(AsyncResponseTransformer<GetObjectResponse, T> responseTransformer) {
            this.responseTransformer = responseTransformer;
        }

        @Override
        public CompletableFuture<T> prepare() {
            CompletableFuture<T> asyncResponseTransformerFuture = this.responseTransformer.prepare();
            asyncResponseTransformerFuture.whenComplete((t, e) -> {
                log.info(() -> "[MultiGetAsyncResponseTransformer.prepare] future completed");
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
            totalParts = response.partsCount();
            individualPartContentLength = response.contentLength();
            if (totalParts > 1) {
                completedParts.incrementAndGet();
                sendGetRequestsUntilBufferIsFull();
            }
            // todo: if parts, just use the first response for now.
            //  But we might need to construct a custom response object instead.
            this.responseTransformer.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            if (totalParts == null || totalParts <= 1) {
                // do not use multipart
                this.responseTransformer.onStream(publisher);
                return;
            }
            publisher.subscribe(new IndividualBodyPartSubscriber(1));
            this.responseTransformer.onStream(SdkPublisher.adapt(bodyPartsPublisher));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            log.info(() -> "MultiGetAsyncResponseTransformer exceptionOccurred : " + error.toString());
            this.responseTransformer.exceptionOccurred(error);
        }
    }

    /**
     * Subscriber to each of the individual body parts being received. Manages the buffering of each individual part body into
     * memory, and flushing of the buffer once it is full.
     */
    private class IndividualBodyPartSubscriber implements Subscriber<ByteBuffer> {
        private final int partNumber;
        private final PartBuffer partsToRequest;

        public IndividualBodyPartSubscriber(int partNumber) {
            this.partNumber = partNumber;
            this.partsToRequest = new PartBuffer(partNumber);
            bufferedParts.offer(partsToRequest);
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(maxMemBufferSizeInBytes - totalMemBufferedInBytes.get());
        }

        @Override
        public void onNext(ByteBuffer body) {
            partsToRequest.put(body);
            totalMemBufferedInBytes.addAndGet(body.limit());
            debug_totalBytesReceived += body.limit();
        }

        @Override
        public void onError(Throwable t) {
            log.info(() -> String.format("IndividualPartSubscriber[%d] onError: %s", partNumber, t));
            responseTransformer.exceptionOccurred(t);
        }

        @Override
        public void onComplete() {
            completedParts.incrementAndGet();
            if (bufferIsFull() || completedLastPart()) {
                // send full buffer to downstream publisher
                while (!bufferedParts.isEmpty()) {
                    PartBuffer part = bufferedParts.poll();
                    part.sendToPublisher();
                }
                totalMemBufferedInBytes.set(0);

                if (!completedLastPart()) {
                    sendGetRequestsUntilBufferIsFull();
                }
            }

            log.info(() -> String.format(
                "Completed GetRequest for part %d "
                + "- %d/%d completed parts "
                + "- %d/%d total byte transferred "
                + "- %d/%d current memory buffered",
                partNumber,
                completedParts.get(), totalParts,
                debug_totalBytesTransferredToDownstreamPublisher, individualPartContentLength * totalParts,
                totalMemBufferedInBytes.get(), maxMemBufferSizeInBytes
            ));

            if (completedLastPart()) {
                // We are done with all the parts, complete the whole request
                log.info(() -> "All parts completed");
                bodyPartsPublisher.complete();
            }
        }

        private boolean bufferIsFull() {
            return totalMemBufferedInBytes.get() >= maxMemBufferSizeInBytes;
        }

        private boolean completedLastPart() {
            return completedParts.get() == totalParts;
        }
    }

    private class PartBuffer {
        int partNumber;
        final ByteBuffer buffer;

        PartBuffer(int partNumber) {
            this.partNumber = partNumber;
            this.buffer = ByteBuffer.allocate((int) individualPartContentLength);
        }

        void resetPosition() {
            this.buffer.position(0);
        }

        void sendToPublisher() {
            resetPosition();
            bodyPartsPublisher.send(this.buffer);
            debug_totalBytesTransferredToDownstreamPublisher += this.buffer.capacity();
        }

        void put(ByteBuffer body) {
            this.buffer.put(body);
        }
    }
}
