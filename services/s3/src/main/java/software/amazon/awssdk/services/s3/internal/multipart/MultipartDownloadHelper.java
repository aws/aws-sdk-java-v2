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
 * Single-use helper class for a multipart get response. DO NOT REUSE THAT CLASS FOR MULTIPLE REQUESTS!
 * <p>
 * todo(order)
 * todo(buffer)
 * Right now, all the part requests are sent in parallel, and streamed to the provided
 * {@link AsyncResponseTransformer} as they come, which will most certainly be
 * out of order.
 * <p>
 * <p>
 * todo(future)
 * Future cancellation and error handling needs to be done correctly
 *
 * @param <T>
 */
@SdkInternalApi
public class MultipartDownloadHelper<T> {

    private static final Logger log = Logger.loggerFor(MultipartDownloadHelper.class);
    private static final long DEFAULT_MEMORY_BUFFERED = 64 * 1024 * 1024;

    private final S3AsyncClient s3AsyncClient;

    private CompletableFuture<T> returnFuture;

    private GetObjectRequest getObjectRequest;
    private GetObjectResponse firstResponse;
    private AsyncResponseTransformer<GetObjectResponse, T> responseTransformer;
    private Integer totalParts;
    private int currentPart = 1;
    private long individualPartContentLength;
    private AtomicBoolean completed = new AtomicBoolean(false);
    private final Set<Integer> completedParts = Collections.synchronizedSet(new HashSet<>());
    private SimplePublisher<ByteBuffer> bodyPartsPublisher = new SimplePublisher<>();

    private long memBufferSizeInBytes = DEFAULT_MEMORY_BUFFERED; //todo(buffer)
    private AtomicLong totalMemBufferedInBytes = new AtomicLong(0);
    private PriorityQueue<PartsToRequest> bufferedParts = new PriorityQueue<>(Comparator.comparingInt(p -> p.partNUmber));

    MultipartDownloadHelper(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
        this.returnFuture = new CompletableFuture<>();
    }

    public CompletableFuture<T> getObject(GetObjectRequest getObjectRequest,
                                          AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {
        this.responseTransformer = asyncResponseTransformer;
        this.getObjectRequest = getObjectRequest;

        GetObjectRequest firstPartRequest = getObjectRequest.copy(b -> b.partNumber(1));

        MultiGetAsyncResponseTransformer multiResponseTransformer = new MultiGetAsyncResponseTransformer();
        CompletableFuture<T> response = s3AsyncClient.getObject(firstPartRequest, multiResponseTransformer);
        response.whenComplete((res, err) -> {
            if (err != null) {
                log.info(() -> String.format("MultiGetAsyncResponseTransformer future error: %s", err.toString()));
                returnFuture.completeExceptionally(err);
                return;
            }
            log.info(() -> String.format("MultiGetAsyncResponseTransformer future completed: %s", res.toString()));
            returnFuture.complete(res); // todo(future)
        });
        CompletableFutureUtils.forwardExceptionTo(returnFuture, response); // propagate cancellation
        return returnFuture;
    }

    // private void doMultipartDownload() {
    //     for (int i = 2; i < totalParts + 1; i++) {
    //         int part = i;
    //         GetObjectRequest partRequest = getObjectRequest.copy(b -> b.partNumber(part));
    //         CompletableFuture<ResponsePublisher<GetObjectResponse>> responsePublisherFuture =
    //             s3AsyncClient.getObject(partRequest, AsyncResponseTransformer.toPublisher());
    //         responsePublisherFuture.whenComplete((responsePublisher, e) -> {
    //             if (e != null) {
    //                 // todo(future): need to stop in flight requests?
    //                 returnFuture.completeExceptionally(e);
    //                 return;
    //             }
    //             // response received and body starts streaming
    //             log.info(() -> String.format("received content range for part %d: %s",
    //                                          part, responsePublisher.response().contentRange()));
    //             responsePublisher.subscribe(new IndividualBodyPartSubscriber(part, responsePublisher.response()));
    //         });
    //     }
    // }

    // send upload and buffer the response body until the response buffer is full, then completes the future once all parts are
    private void sendUploadRequestsUntilBufferIsFull() {
        log.info(() -> String.format(
            "Buffering request: current part: %d, memBufferSizeInBytes: %d, individualPartContentLength: %d",
            currentPart, memBufferSizeInBytes, individualPartContentLength));
        long totalBufferAvailable = memBufferSizeInBytes;
        while (totalBufferAvailable > 0) {
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

        private CompletableFuture<T> future;

        @Override
        public CompletableFuture<T> prepare() {
            this.future = responseTransformer.prepare();
            return this.future;
        }

        @Override
        public void onResponse(GetObjectResponse response) {
            totalParts = response.partsCount();
            log.info(() -> String.format("onResponse MultiGetAsyncResponseTransformer: %s", response));
            log.info(() -> String.format("totalParts=%d", response.partsCount()));
            firstResponse = response;
            if (totalParts > 1) {
                completedParts.add(1);
                sendUploadRequestsUntilBufferIsFull();
            }
            // todo: if parts, just use the first response for now.
            //  But we might need to construct a custom response object instead.
            responseTransformer.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            log.info(() -> "MultiGetAsyncResponseTransformer onStream");
            if (totalParts == null || totalParts <= 1) {
                // do not use part
                responseTransformer.onStream(publisher);
                return;
            }
            publisher.subscribe(new IndividualBodyPartSubscriber(1, firstResponse));
            responseTransformer.onStream(SdkPublisher.adapt(bodyPartsPublisher));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            log.info(() -> "MultiGetAsyncResponseTransformer exceptionOccurred : " + error.toString());
            responseTransformer.exceptionOccurred(error);
        }
    }

    // subscriber to each of the individual body parts being received
    private class IndividualBodyPartSubscriber implements Subscriber<ByteBuffer> {
        private int partNumber;
        private GetObjectResponse response;
        private PartsToRequest partsToRequest;

        public IndividualBodyPartSubscriber(int partNumber, GetObjectResponse response) {
            this.partNumber = partNumber;
            this.response = response;
            this.partsToRequest = new PartsToRequest(partNumber);
            bufferedParts.offer(partsToRequest);
        }

        @Override
        public void onSubscribe(Subscription s) {
            log.info(() -> String.format("IndividualPartSubscriber[%d] onSubscribe", partNumber));
            s.request(memBufferSizeInBytes); // todo(buffer) todo(order)
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
            if (totalMemBufferedInBytes.get() >= memBufferSizeInBytes) {
                // Buffer is full:
                //     we are done downloading this subset of parts, so send them in order to the downstream async request body
                while (!bufferedParts.isEmpty()) {
                    PartsToRequest part = bufferedParts.poll();
                    part.body.forEach(bodyPartsPublisher::send);
                }
                totalMemBufferedInBytes.set(0);
                sendUploadRequestsUntilBufferIsFull();
            }

            completedParts.add(partNumber);
            log.info(() -> String.format("completed part %d - %d/%d completed parts", partNumber, completedParts.size(),
                                         totalParts));
            if (completedParts.size() == totalParts) {
                // We are done with all the parts, complete the whole request
                completed.set(true);
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

    private class PartsToRequest {
        int partNUmber;
        List<ByteBuffer> body;

        PartsToRequest(int partNUmber) {
            this.partNUmber = partNUmber;
            this.body = new ArrayList<>();
        }
    }
}
