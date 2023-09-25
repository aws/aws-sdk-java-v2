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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
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
 * Helper class to handle multipart get requests to S3. Will buffer the response body
 * Single-use helper class for a multipart get response. DO NOT REUSE THAT CLASS FOR MULTIPLE REQUESTS!
 * <p>
 * todo(order)
 * todo(buffer)
 *  Right now, all the part requests are sent in parallel, and streamed to the provided
 *  {@link AsyncResponseTransformer} as they come, which will most certainly be
 *  out of order.
 * <p>
 * <p>
 * todo(future)
 *  Future cancellation and error handling needs to be done correctly
 *
 * todo(customization)
 *  Need to be able to provide buffer size.
 *
 * todo(error)
 *  When an error is encountered, stop the process and cancel in-flight requests
 * @param <T>
 */
@SdkInternalApi
public class MultipartDownloadHelper<T> {

    private static final Logger log = Logger.loggerFor(MultipartDownloadHelper.class);
    private static final long DEFAULT_MEMORY_BUFFERED = 64 * 1024 * 1024; // todo(buffer) todo(customization)

    /**
     * The {@link S3AsyncClient} delegate that will perform individual Get Object requests
     */
    private final S3AsyncClient s3AsyncClient;

    /**
     * The original {@link GetObjectRequest} that was provided, to be converted into multiple Get Obejct with various part number.
     */
    private GetObjectRequest getObjectRequest;

    /**
     * The firs {@link GetObjectResponse} returned by the S3 server, used to determine if multipart is required or not, as well
     * as the individual part size.
     */
    private GetObjectResponse firstResponse;

    /**
     * The provided {@link AsyncResponseTransformer} of the original Get Object request. Bodies of each individual part are sent
     * to this instance in order.
     */
    private AsyncResponseTransformer<GetObjectResponse, T> responseTransformer;

    private CompletableFuture<T> returnFuture;
    private CompletableFuture<T> asyncBodyFuture;
    private Integer totalParts;
    private int currentPart = 1;
    private long individualPartContentLength;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final Set<Integer> completedParts = Collections.synchronizedSet(new HashSet<>());
    private SimplePublisher<ByteBuffer> bodyPartsPublisher = new SimplePublisher<>();

    private long memBufferSizeInBytes = DEFAULT_MEMORY_BUFFERED; //todo(buffer) todo(customization)
    private AtomicLong totalMemBufferedInBytes = new AtomicLong(0);

    /**
     * Ordered collection of individual buffered body parts. Filled within {@link IndividualBodyPartSubscriber#onNext(ByteBuffer)}
     * when body ByteBuffer is received and emptied in {@link IndividualBodyPartSubscriber#onComplete()}
     */
    private PriorityQueue<PartsToRequest> bufferedParts = new PriorityQueue<>(Comparator.comparingInt(p -> p.partNumber));

    MultipartDownloadHelper(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
        this.returnFuture = new CompletableFuture<>();
    }

    public CompletableFuture<T> getObject(GetObjectRequest getObjectRequest,
                                          AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {
        this.responseTransformer = asyncResponseTransformer;
        this.getObjectRequest = getObjectRequest;

        GetObjectRequest firstPartRequest = getObjectRequest.copy(b -> b.partNumber(1));

        MultiGetAsyncResponseTransformer multiResponseTransformer = new MultiGetAsyncResponseTransformer(asyncResponseTransformer);
        CompletableFuture<T> response = s3AsyncClient.getObject(firstPartRequest, multiResponseTransformer);
        // response.whenComplete((res, err) -> {
        //     if (err != null) {
        //         log.info(() -> String.format("MultiGetAsyncResponseTransformer future error: %s", err.toString()));
        //         returnFuture.completeExceptionally(err);
        //         return;
        //     }
        //     log.info(() -> String.format("MultiGetAsyncResponseTransformer future completed: %s", res.toString()));
        //     // returnFuture.complete(res); // todo(future)
        // });
        CompletableFutureUtils.forwardExceptionTo(returnFuture, response); // propagate cancellation
        return returnFuture;
    }

    // send Get Object Requests for individual parts and buffer the response body until the response buffer is full, or until all
    // individual part requests are sent.
    private void sendUploadRequestsUntilBufferIsFull() {
        log.info(() -> String.format(
            "Buffering request: current part: %d, memBufferSizeInBytes: %d, individualPartContentLength: %d",
            currentPart, memBufferSizeInBytes, individualPartContentLength));
        long totalBufferAvailable = memBufferSizeInBytes;
        while (totalBufferAvailable > 0 && currentPart < totalParts) {
            currentPart++;
            int part = currentPart;
            log.info(() -> String.format("sending request for part %d", part));
            GetObjectRequest partRequest = getObjectRequest.copy(b -> b.partNumber(part));
            CompletableFuture<ResponsePublisher<GetObjectResponse>> responsePublisherFuture =
                s3AsyncClient.getObject(partRequest, AsyncResponseTransformer.toPublisher());
            responsePublisherFuture.whenComplete((responsePublisher, e) -> {
                if (e != null) {
                    // todo(future): need to stop in flight requests?
                    returnFuture.completeExceptionally(e);
                    return;
                }
                // response received and body starts streaming
                log.info(() -> String.format("received content range for part %d: %s",
                                             part, responsePublisher.response().contentRange()));
                responsePublisher.subscribe(new IndividualBodyPartSubscriber(part, responsePublisher.response()));
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
            asyncBodyFuture = this.responseTransformer.prepare();
            asyncBodyFuture.whenComplete((t, e) -> {
                log.info(() -> "!!!!!!!!!!!! MultiGetAsyncResponseTransformer future completed!!!!");
                if (e != null) {
                    log.error(() -> "error", e);
                    returnFuture.completeExceptionally(e);
                    return;
                }
                returnFuture.complete(t);
            });
            CompletableFutureUtils.forwardExceptionTo(returnFuture, asyncBodyFuture);
            return asyncBodyFuture;
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            totalParts = response.partsCount();
            individualPartContentLength = response.contentLength();
            log.info(() -> String.format("onResponse MultiGetAsyncResponseTransformer: %s", response)); // todo(log)
            log.info(() -> String.format("onResponse totalParts=%d", response.partsCount()));
            log.info(() -> String.format("onResponse contentLength=%d", response.contentLength()));
            firstResponse = response;
            if (totalParts > 1) {
                completedParts.add(1);
                sendUploadRequestsUntilBufferIsFull();
            }
            // todo: if parts, just use the first response for now.
            //  But we might need to construct a custom response object instead.
            this.responseTransformer.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            log.info(() -> "MultiGetAsyncResponseTransformer onStream"); // todo(log)
            if (totalParts == null || totalParts <= 1) {
                // do not use part
                this.responseTransformer.onStream(publisher);
                return;
            }
            publisher.subscribe(new IndividualBodyPartSubscriber(1, firstResponse));
            this.responseTransformer.onStream(SdkPublisher.adapt(bodyPartsPublisher));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            log.info(() -> "MultiGetAsyncResponseTransformer exceptionOccurred : " + error.toString());
            this.responseTransformer.exceptionOccurred(error);
        }
    }

    /**
     * Subscriber to each of the individual body parts being received.
     * Manages the buffering of each individual part body into memory, and flushing of the buffer once it is full.
     */
    private class IndividualBodyPartSubscriber implements Subscriber<ByteBuffer> {
        private final int partNumber;
        private final GetObjectResponse response;
        private final PartsToRequest partsToRequest;

        public IndividualBodyPartSubscriber(int partNumber, GetObjectResponse response) {
            this.partNumber = partNumber;
            this.response = response;
            this.partsToRequest = new PartsToRequest(partNumber);
            bufferedParts.offer(partsToRequest);
        }

        @Override
        public void onSubscribe(Subscription s) {
            log.info(() -> String.format("IndividualPartSubscriber[%d] onSubscribe", partNumber));
            s.request(individualPartContentLength); // todo(buffer) todo(order)
        }

        @Override
        public void onNext(ByteBuffer body) {
            // log.info(() -> String.format("IndividualPartSubscriber[%d] onNext: limit=%d", partNumber, body.limit()));
            // bodyPartsPublisher.send(body.duplicate());
            totalMemBufferedInBytes.addAndGet(body.capacity());
            partsToRequest.body.add(body.duplicate());
        }

        @Override
        public void onError(Throwable t) {
            log.info(() -> String.format("IndividualPartSubscriber[%d] onError: %s", partNumber, t));
            responseTransformer.exceptionOccurred(t);
        }

        @Override
        public void onComplete() {
            // todo(synchronized) with onNext? There might be other IndividualBodyPartSubscriber instance running
            log.info(() -> String.format("IndividualBodyPartSubscriber[%d] onComplete - totalBuffered: %d",
                                     partNumber, totalMemBufferedInBytes.get()));
            completedParts.add(partNumber);
            if (totalMemBufferedInBytes.get() >= memBufferSizeInBytes || completedParts.size() == totalParts) {
                // Buffer is full:
                //     we are done downloading this subset of parts, so send them IN ORDER to the downstream async request body
                while (!bufferedParts.isEmpty()) {
                    PartsToRequest part = bufferedParts.poll();
                    log.info(() -> String.format("IndividualBodyPartSubscriber[%d] onComplete - sending body of part %d",
                                                 partNumber, part.partNumber));
                    for (ByteBuffer bb : part.body) {
                        bodyPartsPublisher.send(bb);
                    }
                }
                totalMemBufferedInBytes.set(0);
                sendUploadRequestsUntilBufferIsFull();
            }

            log.info(() -> String.format("completed part %d - %d/%d completed parts",
                                         partNumber, completedParts.size(), totalParts));

            if (completedParts.size() == totalParts) {
                if (completed.compareAndSet(false, true)) {
                    // We are done with all the parts, complete the whole request
                    log.info(() -> "All parts completed");
                    // todo(order)
                    // todo(buffer)
                    bodyPartsPublisher.complete().whenComplete((v, e) -> {
                        // debug purpose
                        if (e != null) {
                            log.error(() -> "error while processing message in SimplePublisher", e);
                            return;
                        }
                        log.info(() -> "Done processing messages in SimplePublisher");
                    });
                }
            }
        }
    }

    private class PartsToRequest {
        int partNumber;
        List<ByteBuffer> body;

        PartsToRequest(int partNumber) {
            this.partNumber = partNumber;
            this.body = new ArrayList<>();
        }
    }
}
