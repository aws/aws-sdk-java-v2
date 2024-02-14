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
import java.util.concurrent.atomic.AtomicBoolean;
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
 * individual {@link AsyncResponseTransformer} which will be used to perform the individual part requests.
 * This is a 'one-shot' class, it should <em>NOT</em> be reused for more than one multipart download
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
     * Set to true when we know the total part number of the object to get.
     */
    private final AtomicBoolean totalPartKnown = new AtomicBoolean(false);

    /**
     * Once total part is know, this value indicates the total number of parts of the object to get.
     */
    private final AtomicInteger totalParts = new AtomicInteger();

    /**
     * The total number of completed parts. A part is considered complete once the completable future associated with its request
     * completes successfully.
     */
    private final AtomicInteger completedParts = new AtomicInteger(0);

    /**
     * The subscription received from the publisher this subscriber subscribes to.
     */
    private Subscription subscription;

    /**
     * This future will be completed once this subscriber reaches a terminal state, failed or successfully, and will be
     * completed accordingly.
     */
    private CompletableFuture<Void> future = new CompletableFuture<>();

    public MultipartDownloaderSubscriber(S3AsyncClient s3, GetObjectRequest getObjectRequest) {
        this.s3 = s3;
        this.getObjectRequest = getObjectRequest;
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
            throw new NullPointerException("onNext must not be called with null asyncResponseTransformer");
        }

        int nextPartToGet = completedParts.get() + 1;
        if (totalPartKnown.get() && nextPartToGet > totalParts.get()) {
            subscription.cancel();
            return;
        }

        GetObjectRequest actualRequest = getObjectRequest.copy(req -> req.partNumber(nextPartToGet));
        CompletableFuture<GetObjectResponse> future = s3.getObject(actualRequest, asyncResponseTransformer);
        future.whenComplete((response, error) -> {
            if (error != null) {
                onError(error);
                return;
            }
            int totalComplete = completedParts.incrementAndGet();
            log.info(() -> String.format("completed part: %s", totalComplete));

            Integer partCount = response.partsCount();
            if (partCount != null && totalPartKnown.compareAndSet(false, true)) {
                log.info(() -> String.format("total parts: %s", partCount));
                totalParts.set(partCount);
                totalPartKnown.set(true);
            }
            if (partCount != null && requiresMultipart()) {
                subscription.request(1);
            } else {
                subscription.cancel();
            }
        });
    }

    private boolean requiresMultipart() {
        return totalParts.get() > 1;
    }

    @Override
    public void onError(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        future.complete(null);
    }
}
