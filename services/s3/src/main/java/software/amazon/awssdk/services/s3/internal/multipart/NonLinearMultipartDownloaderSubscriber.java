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

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class NonLinearMultipartDownloaderSubscriber
    implements Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {
    private static final Logger log = Logger.loggerFor(NonLinearMultipartDownloaderSubscriber.class);
    private static final int MAX_IN_FLIGHT = 16;

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
     * Queue of all remaining parts in ascending order. i.e. 4, 5, 6, 8, 11
     */
    private final Queue<Integer> allRemainingParts = new ArrayDeque<>();

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
    private volatile Integer totalParts;

    /**
     * The etag of the object being downloaded.
     */
    private volatile String eTag;

    /**
     * Tracks request that are currently in flights, waiting to be completed. Once completed, future are removed from the map
     */
    private final Map<Integer, CompletableFuture<GetObjectResponse>> inFlightRequests = new ConcurrentHashMap<>();

    /**
     * Lock used during first part, so that the first batch of requests wait to be executed until we know if the s3 object is
     * multipart or not.
     */
    private final ReentrantLock firstPartLock = new ReentrantLock();

    /**
     * Pending transformers waiting for the first request to be executed. We cant execute multiple part get until the first
     * requests completes, and we know the object in s3 is a multipart object.
     */
    private final Queue<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> pendingTransformers = new ArrayDeque<>();

    /**
     * Tracks if the first request was sent
     */
    private final AtomicBoolean firstRequestSent = new AtomicBoolean(false);

    public NonLinearMultipartDownloaderSubscriber(S3AsyncClient s3, GetObjectRequest getObjectRequest,
                                                  CompletableFuture<GetObjectResponse> resultFuture) {
        this.s3 = s3;
        this.getObjectRequest = getObjectRequest;
        this.resultFuture = resultFuture;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (this.subscription != null) {
            s.cancel();
            return;
        }
        this.subscription = s;
        this.subscription.request(1);
    }

    @Override
    public void onNext(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        log.info(() -> "onNext");
        if (asyncResponseTransformer == null) {
            subscription.cancel();
            throw new NullPointerException("onNext must not be called with null asyncResponseTransformer");
        }

        if (handleFirstRequest(asyncResponseTransformer)) {
            return;
        }

        sendNextRequest(asyncResponseTransformer);
        requestMoreIfNeeded();
    }

    // Handle first request.
    // We need to wait for the first request to finish so we know if it is aa multipart object or not.
    // While we don't know yet, additional onNext signal receives are stored in pendingTransformers.
    private boolean handleFirstRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        if (completedParts.get() != 0) {
            return false;
        }

        firstPartLock.lock();
        try {
            if (!firstRequestSent.get()) {
                sendFirstRequest(asyncResponseTransformer);
                firstRequestSent.set(true);
                return true;
            }

            // First request already sent, queue this transformer
            synchronized (pendingTransformers) {
                pendingTransformers.offer(asyncResponseTransformer);
            }
        } finally {
            firstPartLock.unlock();
        }
        return true;
    }

    private void sendNextRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        // guardrails: we shouldn't send more requests than
        if (inFlightRequests.size() + completedParts.get() >= totalParts) {
            return;
        }

        int partToGet = nextPart();
        GetObjectRequest request = nextRequest(partToGet);
        // inFlights.incrementAndGet();
        log.info(() -> "Sending next request for part: '" + partToGet);
        CompletableFuture<GetObjectResponse> response = s3.getObject(request, asyncResponseTransformer);
        inFlightRequests.put(partToGet, response);
        log.info(() -> "Total in flight parts: " + inFlightRequests.size());
        response.whenComplete((res, e) -> {
            log.info(() -> "Completed part: '" + partToGet);
            inFlightRequests.remove(partToGet);
            log.info(() -> "Total in flight parts: " + inFlightRequests.size());
            if (e != null) {
                resultFuture.completeExceptionally(e);
                inFlightRequests.values().forEach(future -> future.cancel(true));
                return;
            }
            completedParts.incrementAndGet();
            if (completedParts.get() == totalParts) {
                subscription.cancel();
                resultFuture.complete(getObjectResponse);
            } else {
                requestMoreIfNeeded();
            }
        });
    }

    // returns true if the first request was sent and is still in flight
    private void sendFirstRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        log.info(() -> "Sending first request");
        GetObjectRequest request = nextRequest(1);
        // inFlights.incrementAndGet();
        CompletableFuture<GetObjectResponse> responseFuture = s3.getObject(request, asyncResponseTransformer);
        inFlightRequests.put(1, responseFuture);

        // Propagate cancellation from user
        CompletableFutureUtils.forwardExceptionTo(resultFuture, responseFuture);

        responseFuture.whenComplete((res, e) -> {
            inFlightRequests.remove(1);
            if (e != null) {
                // Note on retries: When this future completed exceptionally, it means we did all retries and still failed for
                // that part. We need to report back the failure to the user.
                resultFuture.completeExceptionally(e);
                inFlightRequests.values().forEach(future -> future.cancel(true));
                return;
            }
            Integer partCount = res.partsCount();
            eTag = res.eTag();
            if (partCount != null && totalParts == null) {
                log.info(() -> String.format("Total amount of parts of the object to download: %d", partCount));
                // MultipartDownloadUtils.multipartDownloadResumeContext(getObjectRequest)
                //                       .ifPresent(ctx -> ctx.totalParts(partCount));
                totalParts = partCount;
            }

            completedParts.incrementAndGet();
            // it is a multipart object?
            if (totalParts == null || totalParts == 1) {
                // Single part object detected, skip multipart and complete everything now
                log.info(() -> "Single Part object detected, skipping multipart download");
                subscription.cancel();
                resultFuture.complete(res);
                return;
            }

            log.info(() -> "Multipart object detected, performing multipart download");
            // part 1 already completed, so skip it
            for (int i = 2; i <= totalParts; i++) {
                allRemainingParts.add(i);
            }
            getObjectResponse = res;

            // Process pending transformers
            processPendingTransformers();

            subscription.request(1);
        });
    }

    private void processPendingTransformers() {
        synchronized (pendingTransformers) {
            AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer = pendingTransformers.poll();
            while (transformer != null) {
                sendNextRequest(transformer);
                transformer = pendingTransformers.poll();
            }
        }
    }

    private void requestMoreIfNeeded() {
        if (!allPartsCompletedOrInFlights() && inFlightRequests.size() < MAX_IN_FLIGHT) {
            log.info(() -> "Requesting next part");
            subscription.request(1);
        }
    }

    private Integer nextPart() {
        synchronized (allRemainingParts) {
            return allRemainingParts.poll();
        }
    }

    private boolean allPartsCompletedOrInFlights() {
        return inFlightRequests.size() + completedParts.get() >= totalParts;
    }

    @Override
    public void onError(Throwable t) {
        // signal received from the publisher this is subscribed to
        // failed state, something really wrong has happened, cancel everything
        resultFuture.completeExceptionally(t);
    }

    @Override
    public void onComplete() {

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
