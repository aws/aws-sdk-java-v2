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
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

public class NonLinearMultipartDownloaderSubscriber
    implements Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {
    private static final Logger log = Logger.loggerFor(NonLinearMultipartDownloaderSubscriber.class);

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
     * The {@link GetObjectResponse} to be returned in the completed future to the user. It corresponds to the first part
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
     * The amount of in flights requests
     */
    private final AtomicInteger inFlights = new AtomicInteger();

    private final Object lock = new Object();

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
        if (asyncResponseTransformer == null) {
            subscription.cancel();
            throw new NullPointerException("onNext must not be called with null asyncResponseTransformer");
        }

        // Handle first request case
        if (completedParts.get() == 0) {
            synchronized (lock) {
                if (sendFirstRequest(asyncResponseTransformer)) {
                    // we don't know yet if we can request more. We can only request more if the object requested has multiple
                    // parts, and we can only know that by looking at the 'totalParts' field of the response.
                    // So we return for now without requesting more, and only request more when the first request completes.
                    // Note that this may be long, since calling GetObject with a FileAsyncResponseTransformer returns a future
                    // that is completed only when the body has been written to the file. This means the subsequent requests
                    // can only be sent after the first part is fully written to the file.
                    return;
                }

            }
        }
        sendNextRequest(asyncResponseTransformer);
        requestMoreIfNeeded();
    }

    private void sendNextRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        if (inFlights.get() + completedParts.get() >= totalParts) {
            // all requests are already being processed
            return;
        }

        int partToGet = nextPart();
        GetObjectRequest request = nextRequest(partToGet);
        inFlights.incrementAndGet();
        log.info(() -> "============== Sending next request for part: '" + partToGet + "' ==============");
        CompletableFuture<GetObjectResponse> response = s3.getObject(request, asyncResponseTransformer);
        response.whenComplete((res, e) -> {
            log.info(() -> "============== Completed part: '" + partToGet + "' ==============");
            inFlights.decrementAndGet();
            if (e != null) {
                resultFuture.completeExceptionally(e);
                return;
            }
            completedParts.incrementAndGet();
            if (completedParts.get() == totalParts) {
                subscription.cancel();
                resultFuture.complete(getObjectResponse);
            }
        });
    }

    // returns true if we actually sent the request
    private boolean sendFirstRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        // we could have completed the first request while waiting on synchronized block, check again
        if (completedParts.get() != 0) {
            // another thread sent the first request already
            return false;
        }
        log.info(() -> "============== SENDING FIRST REQUEST ==============");
        GetObjectRequest request = nextRequest(1);
        inFlights.incrementAndGet();
        CompletableFuture<GetObjectResponse> responseFuture = s3.getObject(request, asyncResponseTransformer);

        // Propagate cancellation from user
        CompletableFutureUtils.forwardExceptionTo(resultFuture, responseFuture);

        responseFuture.whenComplete((res, e) -> {
            inFlights.decrementAndGet();
            if (e != null) {
                // note on retries: When this future completed exceptionally, it means we did all retries and still failed
                // we need to report back the failure to the user.
                resultFuture.completeExceptionally(e);
                return;
            }
            completedParts.incrementAndGet();
            Integer partCount = res.partsCount();
            eTag = res.eTag();
            if (partCount != null && totalParts == null) {
                log.info(() -> String.format("Total amount of parts of the object to download: %d", partCount));
                // MultipartDownloadUtils.multipartDownloadResumeContext(getObjectRequest)
                //                       .ifPresent(ctx -> ctx.totalParts(partCount));
                totalParts = partCount;
            }

            if (totalParts == null || totalParts == 1) {
                // Single part object, skip multipart and complete everything now
                log.info(() -> "Single Part object detected, skipping multipart download");
                subscription.cancel();
                resultFuture.complete(res);
                return;
            }
            // it is a multipart object
            log.info(() -> "Multipart object detected, performing multipart download");
            // part 1 already completed, so skip it
            for (int i = 2; i <= totalParts; i++) {
                allRemainingParts.add(i);
            }
            getObjectResponse = res;
            // request the next part, which will start the whole multipart dl in parallel
            subscription.request(1);
        });
        return true;
    }

    private void requestMoreIfNeeded() {
        if (!allPartsCompletedOrInFlights()) {
            subscription.request(1);
        }
    }

    private Integer nextPart() {
        synchronized (allRemainingParts) {
            return allRemainingParts.poll();
        }
    }

    private boolean allPartsCompletedOrInFlights() {
        return inFlights.get() + completedParts.get() >= totalParts;
    }

    @Override
    public void onError(Throwable t) {

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
