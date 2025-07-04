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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Logger;

/**
 * A subscriber implementation that will download all individual parts for a multipart get-object request. It receives the
 * individual {@link AsyncResponseTransformer} which will be used to perform the individual part requests. This is a 'one-shot'
 * class, it should <em>NOT</em> be reused for more than one multipart download
 */
@SdkInternalApi
public class MultipartDownloaderSubscriber implements Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {
    private static final Logger log = Logger.loggerFor(MultipartDownloaderSubscriber.class);

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
     * This value indicates the total number of parts of the object to get. If null, it means we don't know the total amount of
     * parts, either because we haven't received a response from s3 yet to set it, or the object to get is not multipart.
     */
    private volatile Integer totalParts;

    /**
     * The total number of completed parts. A part is considered complete once the completable future associated with its request
     * completes successfully.
     */
    private final AtomicInteger completedParts;

    /**
     * The subscription received from the publisher this subscriber subscribes to.
     */
    private Subscription subscription;

    /**
     * This future will be completed once this subscriber reaches a terminal state, failed or successfully, and will be completed
     * accordingly.
     */
    private final CompletableFuture<Void> future = new CompletableFuture<>();

    /**
     * The etag of the object being downloaded.
     */
    private volatile String eTag;

    /**
     * The Subscription lock.
     */
    private final Object lock = new Object();

    public MultipartDownloaderSubscriber(S3AsyncClient s3, GetObjectRequest getObjectRequest) {
        this(s3, getObjectRequest, 0);
    }

    public MultipartDownloaderSubscriber(S3AsyncClient s3, GetObjectRequest getObjectRequest, int completedParts) {
        this.s3 = s3;
        this.getObjectRequest = getObjectRequest;
        this.completedParts = new AtomicInteger(completedParts);
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

        int nextPartToGet = completedParts.get() + 1;

        synchronized (lock) {
            if (totalParts != null && nextPartToGet > totalParts) {
                log.debug(() -> String.format("Completing multipart download after a total of %d parts downloaded.", totalParts));
                subscription.cancel();
                return;
            }
        }

        GetObjectRequest actualRequest = nextRequest(nextPartToGet);
        log.debug(() -> "Sending GetObjectRequest for next part with partNumber=" + nextPartToGet);
        CompletableFuture<GetObjectResponse> getObjectFuture = s3.getObject(actualRequest, asyncResponseTransformer);
        getObjectFuture.whenComplete((response, error) -> {
            if (error != null) {
                log.debug(() -> "Error encountered during GetObjectRequest with partNumber=" + nextPartToGet);
                onError(error);
                return;
            }
            requestMoreIfNeeded(response);
        });
    }

    private void requestMoreIfNeeded(GetObjectResponse response) {
        int totalComplete = completedParts.incrementAndGet();
        MultipartDownloadUtils.multipartDownloadResumeContext(getObjectRequest)
                              .ifPresent(ctx -> {
                                  ctx.addCompletedPart(totalComplete);
                                  ctx.addToBytesToLastCompletedParts(response.contentLength());
                                  if (ctx.response() == null) {
                                      ctx.response(response);
                                  }
                              });
        log.debug(() -> String.format("Completed part %d", totalComplete));

        if (eTag == null) {
            this.eTag = response.eTag();
            log.debug(() -> String.format("Multipart object ETag: %s", this.eTag));
        }

        Integer partCount = response.partsCount();
        if (partCount != null && totalParts == null) {
            log.debug(() -> String.format("Total amount of parts of the object to download: %d", partCount));
            MultipartDownloadUtils.multipartDownloadResumeContext(getObjectRequest)
                                  .ifPresent(ctx -> ctx.totalParts(partCount));
            totalParts = partCount;
        }

        synchronized (lock) {
            if (totalParts != null && totalParts > 1 && totalComplete < totalParts) {
                subscription.request(1);
            } else {
                log.debug(() -> String.format("Completing multipart download after a total of %d parts downloaded.", totalParts));
                subscription.cancel();
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        future.complete(null);
    }

    public CompletableFuture<Void> future() {
        return this.future;
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
