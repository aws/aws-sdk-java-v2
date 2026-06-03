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

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.Logger;

/**
 * A subscriber implementation that will download all individual parts for a multipart presigned URL download request.
 * It receives individual {@link AsyncResponseTransformer} instances which will be used to perform the individual
 * range-based part requests using presigned URLs. This is a 'one-shot' class, it should <em>NOT</em> be reused
 * for more than one multipart download.
 *
 * <p>Unlike the standard {@link MultipartDownloaderSubscriber} which uses S3's native multipart API with part numbers,
 * this subscriber uses HTTP range requests against presigned URLs to achieve multipart download functionality.
 * <p>This implementation is thread-safe and handles concurrent part downloads while maintaining proper
 * ordering and validation of responses.</p>
 */
@ThreadSafe
@SdkInternalApi
public class PresignedUrlMultipartDownloaderSubscriber
    implements Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {

    private static final Logger log = Logger.loggerFor(PresignedUrlMultipartDownloaderSubscriber.class);

    private final S3AsyncClient s3AsyncClient;
    private final PresignedUrlDownloadRequest presignedUrlDownloadRequest;
    private final Long configuredPartSizeInBytes;

    /**
     * Internal lifecycle future for this subscriber. Completed when all parts are downloaded
     * or when an error occurs.
     */
    private final CompletableFuture<Void> future;

    /**
     * The split transformer's completion future (from {@code SplitResult.resultFuture()}).
     * Completing this signals the download result to the caller. Must be completed exceptionally
     * before cancelling the subscription to prevent ByteArraySplittingTransformer from assembling
     * invalid data.
     */
    private final CompletableFuture<?> resultFuture;
    private final Object lock = new Object();
    private final AtomicInteger nextPartIndex;
    private final AtomicInteger requestsSent;

    /**
     * Store the GetObject futures so we can cancel them if onError() is invoked.
     */
    private final Queue<CompletableFuture<GetObjectResponse>> getObjectFutures = new ConcurrentLinkedQueue<>();

    private volatile Long totalContentLength;
    private volatile Integer totalParts;
    private volatile String eTag;
    private Subscription subscription;

    public PresignedUrlMultipartDownloaderSubscriber(
        S3AsyncClient s3AsyncClient,
        PresignedUrlDownloadRequest presignedUrlDownloadRequest,
        long configuredPartSizeInBytes,
        CompletableFuture<?> resultFuture) {
        this.s3AsyncClient = s3AsyncClient;
        this.presignedUrlDownloadRequest = presignedUrlDownloadRequest;
        this.configuredPartSizeInBytes = configuredPartSizeInBytes;
        this.nextPartIndex = new AtomicInteger(0);
        this.requestsSent = new AtomicInteger(0);
        this.future = new CompletableFuture<>();
        this.resultFuture = resultFuture;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (subscription != null) {
            s.cancel();
            return;
        }
        this.subscription = s;
        s.request(1);
    }

    @Override
    public void onNext(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        if (asyncResponseTransformer == null) {
            throw new NullPointerException("onNext must not be called with null asyncResponseTransformer");
        }

        int currentPartIndex;
        synchronized (lock) {
            currentPartIndex = nextPartIndex.get();
            if (totalParts != null && currentPartIndex >= totalParts) {
                log.debug(() -> String.format("Completing multipart download after a total of %d parts downloaded.", totalParts));
                subscription.cancel();
                return;
            }
            nextPartIndex.incrementAndGet();
        }
        makeRangeRequest(currentPartIndex, asyncResponseTransformer);
    }

    private void makeRangeRequest(int partIndex,
                                  AsyncResponseTransformer<GetObjectResponse,
                                      GetObjectResponse> asyncResponseTransformer) {
        PresignedUrlDownloadRequest partRequest = createRangedGetRequest(partIndex);
        log.debug(() -> "Sending range request for part " + partIndex + " with range=" + partRequest.range());

        requestsSent.incrementAndGet();
        CompletableFuture<GetObjectResponse> responseFuture = s3AsyncClient.presignedUrlExtension()
                                                                           .getObject(partRequest, asyncResponseTransformer);
        getObjectFutures.add(responseFuture);
        responseFuture.whenComplete((response, error) -> {
            if (error != null) {
                if (partIndex == 0 && PresignedUrlDownloadHelper.isRangeNotSatisfiable(error)) {
                    log.debug(() -> "Received 416 on first range request, object is empty");
                    resultFuture.completeExceptionally(
                        new PresignedUrlDownloadHelper.EmptyObjectRangeNotSatisfiableException(error));
                    synchronized (lock) {
                        subscription.cancel();
                    }
                } else {
                    handleError(error);
                }
                return;
            }
            if (validatePart(response, partIndex, asyncResponseTransformer)) {
                requestMoreIfNeeded(nextPartIndex.get());
            }
        });
    }

    private boolean validatePart(GetObjectResponse response, int partIndex,
                                 AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        int dispatched = nextPartIndex.get();
        log.debug(() -> String.format("Dispatched %d parts so far", dispatched));

        String responseETag = response.eTag();
        String responseContentRange = response.contentRange();
        if (eTag == null) {
            this.eTag = responseETag;
            log.debug(() -> String.format("Multipart object ETag: %s", this.eTag));
        }

        if (totalContentLength == null && responseContentRange != null) {
            Optional<Long> parsedContentLength = MultipartDownloadUtils.parseContentRangeForTotalSize(responseContentRange);
            if (!parsedContentLength.isPresent()) {
                SdkClientException error = PresignedUrlDownloadHelper.invalidContentRangeHeader(responseContentRange);
                log.debug(() -> "Failed to parse content range", error);
                asyncResponseTransformer.exceptionOccurred(error);
                handleError(error);
                return false;
            }

            this.totalContentLength = parsedContentLength.get();
            this.totalParts = MultipartDownloadUtils.calculateTotalParts(totalContentLength, configuredPartSizeInBytes);
            log.debug(() -> String.format("Total content length: %d, Total parts: %d", totalContentLength, totalParts));
        }

        Optional<SdkClientException> validationError = validateResponse(response, partIndex);
        if (validationError.isPresent()) {
            log.debug(() -> "Response validation failed", validationError.get());
            asyncResponseTransformer.exceptionOccurred(validationError.get());
            handleError(validationError.get());
            return false;
        }

        return true;
    }

    private void requestMoreIfNeeded(int dispatched) {
        synchronized (lock) {
            if (hasMoreParts(dispatched)) {
                subscription.request(1);
            } else {
                if (totalParts != null && requestsSent.get() != totalParts) {
                    handleError(new IllegalStateException(
                        "Request count mismatch. Expected: " + totalParts + ", sent: " + requestsSent.get()));
                    return;
                }
                log.debug(() -> String.format("Completing multipart download after a total of %d parts downloaded.", totalParts));
                subscription.cancel();
            }
        }
    }

    private Optional<SdkClientException> validateResponse(GetObjectResponse response, int partIndex) {
        return PresignedUrlDownloadHelper.validatePartResponse(
            response, partIndex, configuredPartSizeInBytes, totalContentLength, totalParts);
    }

    private boolean hasMoreParts(int dispatched) {
        return totalParts != null && dispatched < totalParts;
    }

    private PresignedUrlDownloadRequest createRangedGetRequest(int partIndex) {
        return PresignedUrlDownloadHelper.createRangedGetRequest(
            presignedUrlDownloadRequest, partIndex, configuredPartSizeInBytes, totalContentLength, eTag);
    }

    private void handleError(Throwable t) {
        future.completeExceptionally(t);
        if (resultFuture != null) {
            resultFuture.completeExceptionally(t);
        }
        synchronized (lock) {
            if (subscription != null) {
                subscription.cancel();
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        log.debug(() -> "Error in multipart download", t);
        CompletableFuture<GetObjectResponse> partFuture;
        while ((partFuture = getObjectFutures.poll()) != null) {
            partFuture.cancel(true);
        }
        future.completeExceptionally(t);
        if (resultFuture != null) {
            resultFuture.completeExceptionally(t);
        }
    }

    @Override
    public void onComplete() {
        future.complete(null);
    }

}