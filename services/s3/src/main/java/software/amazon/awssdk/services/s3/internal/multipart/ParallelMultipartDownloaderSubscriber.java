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

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * A subscriber implementation that will download all individual parts for a multipart get-object request in parallel,
 * concurrently. The amount of concurrent get-object is limited by the {@code maxInFlightParts} configuration. It receives the
 * individual {@link AsyncResponseTransformer} which will be used to perform the individual part requests. These
 * AsyncResponseTransformer should be able to handle receiving data in parts potentially out of order, For example, the
 * AsyncResponseTransformer for part 4 might may have any of its callback called before part 1, 2 or 3 if it finishes before. This
 * is a 'one-shot' class, it should <em>NOT</em> be reused for more than one multipart download.
 */
@SdkInternalApi
public class ParallelMultipartDownloaderSubscriber
    implements Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {
    private static final Logger log = Logger.loggerFor(ParallelMultipartDownloaderSubscriber.class);

    /**
     * Maximum number of concurrent GetObject requests
     */
    private final int maxInFlightParts;

    /**
     * The s3 client used to make the individual part requests
     */
    private final S3AsyncClient s3;

    /**
     * The GetObjectRequest that was provided when calling s3.getObject(...). It is copied for each individual request, and the
     * copy has the partNumber field updated as more parts are downloaded.
     */
    private final GetObjectRequest getObjectRequest;

    /**
     * The total number of completed parts. A part is considered complete once the completable future associated with its request
     * completes successfully.
     */
    private final AtomicInteger completedParts = new AtomicInteger();

    /**
     * The future returned to the user when calling
     * {@link S3AsyncClient#getObject(GetObjectRequest, AsyncResponseTransformer) getObject}. This will be completed once the last
     * part finishes. Contrary to the linear code path, the future returned to the user is handled here so that we can complete it
     * once the last part writting to the file is completed.
     */
    private final CompletableFuture<GetObjectResponse> resultFuture;

    /**
     * The {@link GetObjectResponse} to be returned in the completed future to the user. It corresponds to the response of first
     * part GetObject
     */
    private GetObjectResponse getObjectResponse;

    /**
     * The subscription received from the publisher this subscriber subscribes to.
     */
    private Subscription subscription;

    /**
     * This value indicates the total number of parts of the object to get. If null, it means we don't know the total amount of
     * parts, either because we haven't received a response from s3 yet to set it, or the object to get is not multipart.
     */
    private CompletableFuture<Integer> totalPartsFuture = new CompletableFuture<>();

    /**
     * The etag of the object being downloaded.
     */
    private volatile String eTag;

    /**
     * Lock around calls to the subscription
     */
    private final Object subscriptionLock = new Object();

    /**
     * Tracks request that are currently in flights, waiting to be completed. Once completed, future are removed from the map
     */
    private final Map<Integer, CompletableFuture<GetObjectResponse>> inFlightRequests = new ConcurrentHashMap<>();

    /**
     * Trasck the amount of in flight requests
     */
    private final AtomicInteger inFlightRequestsNum = new AtomicInteger(0);

    /**
     * Pending transformers received through onNext that are waiting to be executed.
     */
    private final Queue<Pair<Integer, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>> pendingTransformers =
        new ConcurrentLinkedQueue<>();

    /**
     * Amount of demand requested but not yet fulfilled by the subscription
     */
    private final AtomicInteger outstandingDemand = new AtomicInteger(0);

    /**
     * Indicates whether this is the first response transformer or not.
     */
    private final AtomicBoolean isFirstResponseTransformer = new AtomicBoolean(true);

    /**
     * Indicates if we are currently processing pending transformer, which are waiting to be used to send requests
     */
    private final AtomicBoolean processingPendingTransformers = new AtomicBoolean(false);

    /**
     * The current part of the object to get
     */
    private final AtomicInteger partNumber = new AtomicInteger(0);

    /**
     * Tracks if one of the parts requests future completed exceptionally. If this occurs, it means all retries were
     * attempted for that part, but it still failed. This is a failure state, the error should be reported back to the user
     * and any more request should be ignored.
     */
    private final AtomicBoolean isCompletedExceptionally = new AtomicBoolean(false);

    public ParallelMultipartDownloaderSubscriber(S3AsyncClient s3,
                                                 GetObjectRequest getObjectRequest,
                                                 CompletableFuture<GetObjectResponse> resultFuture,
                                                 int maxInFlightParts) {
        this.s3 = s3;
        this.getObjectRequest = getObjectRequest;
        this.resultFuture = resultFuture;
        this.maxInFlightParts = maxInFlightParts;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (this.subscription != null) {
            s.cancel();
            return;
        }
        this.subscription = s;
        subscription.request(maxInFlightParts);
    }

    @Override
    public void onNext(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        if (asyncResponseTransformer == null) {
            subscription.cancel();
            throw new NullPointerException("onNext must not be called with null asyncResponseTransformer");
        }

        log.trace(() -> "On Next - Total in flight parts: " + inFlightRequests.size()
                        + " - Demand : " + outstandingDemand.get()
                        + " - Total completed parts: " + completedParts
                        + " - Total pending transformers: " + pendingTransformers.size()
                        + " - Current in flight requests: " + inFlightRequests.keySet());

        int currentPartNum = partNumber.incrementAndGet();

        if (isFirstResponseTransformer.compareAndSet(true, false)) {
            sendFirstRequest(asyncResponseTransformer);
        } else {
            pendingTransformers.offer(Pair.of(currentPartNum, asyncResponseTransformer));
            totalPartsFuture.thenAccept(
                totalParts -> processingRequests(asyncResponseTransformer, currentPartNum, totalParts));
        }
    }

    private void processingRequests(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer,
                                    int currentPartNum, Integer totalParts) {

        if (currentPartNum > totalParts) {
            // Do not process requests above total parts.
            // Since we request for maxInFlight during onSubscribe, and the object might actually have less part than maxInFlight,
            // there may be situations where we received more onNext signals than the amount of GetObjectRequest required to be
            // made.
            return;
        }

        if (inFlightRequests.size() >= maxInFlightParts) {
            pendingTransformers.offer(Pair.of(currentPartNum, asyncResponseTransformer));
            return;
        }

        processPendingTransformers(totalParts);
    }

    private void sendNextRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer,
                                 int currentPartNumber, int totalParts) {
        if (inFlightRequestsNum.get() + completedParts.get() >= totalParts) {
            return;
        }

        GetObjectRequest request = nextRequest(currentPartNumber);
        log.debug(() -> "Sending next request for part: " + currentPartNumber);

        CompletableFuture<GetObjectResponse> response = s3.getObject(request, asyncResponseTransformer);

        inFlightRequestsNum.incrementAndGet();
        inFlightRequests.put(currentPartNumber, response);
        CompletableFutureUtils.forwardExceptionTo(resultFuture, response);

        response.whenComplete((res, e) -> {
            if (e != null || isCompletedExceptionally.get()) {
                // Note on retries: When this future completes exceptionally, it means we did all retries and still failed for
                // that part. We need to report back the failure to the user.
                handlePartError(e, currentPartNumber);
                return;
            }
            log.debug(() -> "Completed part: " + currentPartNumber);

            inFlightRequests.remove(currentPartNumber);
            inFlightRequestsNum.decrementAndGet();
            completedParts.incrementAndGet();

            if (completedParts.get() >= totalParts) {
                if (completedParts.get() > totalParts) {
                    resultFuture.completeExceptionally(new IllegalStateException("Total parts exceeded"));
                } else {
                    resultFuture.complete(getObjectResponse);
                }

                synchronized (subscriptionLock) {
                    subscription.cancel();
                }

            } else {
                processPendingTransformers(res.partsCount());
                synchronized (subscriptionLock) {
                    subscription.request(1);
                }
            }
        });
    }

    private void sendFirstRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        log.debug(() -> "Sending first request");
        GetObjectRequest request = nextRequest(1);
        CompletableFuture<GetObjectResponse> responseFuture = s3.getObject(request, asyncResponseTransformer);

        // Propagate cancellation from user
        CompletableFutureUtils.forwardExceptionTo(resultFuture, responseFuture);

        responseFuture.whenComplete((res, e) -> {
            if (e != null || isCompletedExceptionally.get()) {
                // Note on retries: When this future completes exceptionally, it means we did all retries and still failed for
                // that part. We need to report back the failure to the user.
                handlePartError(e, 1);
                return;
            }

            log.debug(() -> "Completed part: 1");
            completedParts.incrementAndGet();
            setInitialPartCountAndEtag(res);

            if (!isMultipartObject(res)) {
                return;
            }

            log.debug(() -> "Multipart object detected, performing multipart download");
            getObjectResponse = res;

            processPendingTransformers(res.partsCount());
            synchronized (subscriptionLock) {
                subscription.request(1);
            }
        });
    }

    private boolean isMultipartObject(GetObjectResponse response) {
        if (response.partsCount() == null || response.partsCount() == 1) {
            // Single part object detected, skip multipart and complete everything now
            log.debug(() -> "Single Part object detected, skipping multipart download");
            subscription.cancel();
            resultFuture.complete(response);
            return false;
        }
        return true;
    }

    private void setInitialPartCountAndEtag(GetObjectResponse response) {
        Integer partCount = response.partsCount();
        eTag = response.eTag();
        if (partCount != null) {
            log.debug(() -> String.format("Total amount of parts of the object to download: %d", partCount));
            totalPartsFuture.complete(partCount);
        } else {
            totalPartsFuture.complete(1);
        }
    }

    private void handlePartError(Throwable e, int part) {
        isCompletedExceptionally.set(true);
        log.error(() -> "Error on part " + part,  e);
        resultFuture.completeExceptionally(e);
        inFlightRequests.values().forEach(future -> future.cancel(true));
    }

    private void processPendingTransformers(int totalParts) {
        do {
            if (!processingPendingTransformers.compareAndSet(false, true)) {
                return;
            }
            try {
                doProcessPendingTransformers(totalParts);
            } finally {
                processingPendingTransformers.set(false);
            }

        } while (shouldProcessPendingTransformers());

    }

    private void doProcessPendingTransformers(int totalParts) {
        while (shouldProcessPendingTransformers()) {
            Pair<Integer, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> transformer =
                pendingTransformers.poll();
            sendNextRequest(transformer.right(), transformer.left(), totalParts);
        }
    }

    private boolean shouldProcessPendingTransformers() {
        if (pendingTransformers.isEmpty()) {
            return false;
        }
        return maxInFlightParts - inFlightRequestsNum.get() > 0;
    }

    @Override
    public void onError(Throwable t) {
        // Signal received from the publisher this is subscribed to
        // (in the case of file download, that's FileAsyncResponseTransformerPublisher)
        // Failed state, something really wrong has happened, cancel everything
        inFlightRequests.values().forEach(future -> future.cancel(true));
        inFlightRequests.clear();
        resultFuture.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        // We check for completion state when we receive the GetObjectResponse for last part.
        // This Subscriber is responsible for its completed state, so we do nothing here.
    }

    private GetObjectRequest nextRequest(int nextPartToGet) {
        return getObjectRequest.copy(req -> {
            req.partNumber(nextPartToGet);
            if (eTag != null) {
                req.ifMatch(eTag);
            }
        });
    }

}
