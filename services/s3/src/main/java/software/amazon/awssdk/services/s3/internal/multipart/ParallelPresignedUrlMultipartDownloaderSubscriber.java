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
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * A parallel subscriber for multipart presigned URL downloads that writes parts concurrently.
 * Used with {@link software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformerPublisher}
 * when {@code parallelSplitSupported() == true} (i.e., toFile() downloads).
 *
 * <p>Unlike {@link PresignedUrlMultipartDownloaderSubscriber} which requests one part at a time,
 * this subscriber requests up to {@code maxInFlightParts} concurrently, similar to
 * {@link ParallelMultipartDownloaderSubscriber} for regular multipart downloads.</p>
 */
@SdkInternalApi
public class ParallelPresignedUrlMultipartDownloaderSubscriber
    implements Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {

    private static final Logger log = Logger.loggerFor(ParallelPresignedUrlMultipartDownloaderSubscriber.class);

    private final S3AsyncClient s3AsyncClient;
    private final PresignedUrlDownloadRequest presignedUrlDownloadRequest;
    private final long configuredPartSizeInBytes;
    /**
     * The future returned by the SplitResult, representing the overall download completion.
     * Completed exceptionally on error (before cancel) to prevent ByteArraySplittingTransformer
     * from assembling invalid data.
     */
    private final CompletableFuture<GetObjectResponse> resultFuture;
    private final int maxInFlightParts;

    private final AtomicLong partNumber = new AtomicLong(0);
    private final AtomicLong completedParts = new AtomicLong(0);
    private final Semaphore inFlightPermits;
    /**
     * CAS gate ensuring only the first part failure triggers error handling and cancellation.
     */
    private final AtomicBoolean downloadFailed = new AtomicBoolean(false);
    private final AtomicBoolean processingPending = new AtomicBoolean(false);
    private final Map<Long, CompletableFuture<GetObjectResponse>> inFlightRequests = new ConcurrentHashMap<>();
    private final Queue<Pair<Long, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>>> pendingTransformers =
        new ConcurrentLinkedQueue<>();

    private final Object subscriptionLock = new Object();
    private Subscription subscription;

    private volatile Long totalContentLength;
    private volatile Long totalParts;
    private volatile String eTag;
    private volatile GetObjectResponse firstResponse;

    public ParallelPresignedUrlMultipartDownloaderSubscriber(
        S3AsyncClient s3AsyncClient,
        PresignedUrlDownloadRequest presignedUrlDownloadRequest,
        long configuredPartSizeInBytes,
        CompletableFuture<GetObjectResponse> resultFuture,
        int maxInFlightParts) {
        this.s3AsyncClient = s3AsyncClient;
        this.presignedUrlDownloadRequest = presignedUrlDownloadRequest;
        this.configuredPartSizeInBytes = configuredPartSizeInBytes;
        this.resultFuture = resultFuture;
        this.maxInFlightParts = maxInFlightParts;
        this.inFlightPermits = new Semaphore(maxInFlightParts);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (this.subscription != null) {
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

        long currentPart = partNumber.getAndIncrement();

        if (currentPart == 0) {
            sendFirstRequest(asyncResponseTransformer);
        } else {
            if (totalParts != null && currentPart >= totalParts) {
                return;
            }
            if (totalParts != null) {
                processRequest(asyncResponseTransformer, currentPart);
            } else {
                pendingTransformers.offer(Pair.of(currentPart, asyncResponseTransformer));
            }
        }
    }

    private void sendFirstRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer) {
        PresignedUrlDownloadRequest partRequest = createRangedGetRequest(0L);
        log.debug(() -> "Sending first range request with range=" + partRequest.range());

        if (!inFlightPermits.tryAcquire()) {
            throw new IllegalStateException("Failed to acquire permit for first request");
        }

        CompletableFuture<GetObjectResponse> response =
            s3AsyncClient.presignedUrlExtension().getObject(partRequest, transformer);

        inFlightRequests.put(0L, response);
        CompletableFutureUtils.forwardExceptionTo(resultFuture, response);

        response.whenComplete((res, error) -> {
            inFlightRequests.remove(0L);
            inFlightPermits.release();

            if (error != null) {
                if (PresignedUrlDownloadHelper.isRangeNotSatisfiable(error)) {
                    resultFuture.completeExceptionally(
                        new PresignedUrlDownloadHelper.EmptyObjectRangeNotSatisfiableException(error));
                    synchronized (subscriptionLock) {
                        subscription.cancel();
                    }
                } else {
                    handlePartError(error, 0L);
                }
                return;
            }

            if (downloadFailed.get()) {
                return;
            }

            completedParts.incrementAndGet();

            this.eTag = res.eTag();
            this.firstResponse = res;

            String contentRange = res.contentRange();
            if (contentRange == null) {
                handlePartError(PresignedUrlDownloadHelper.missingContentRangeHeader(), 0L);
                return;
            }

            Optional<Long> parsedTotal = MultipartDownloadUtils.parseContentRangeForTotalSize(contentRange);
            if (!parsedTotal.isPresent()) {
                handlePartError(PresignedUrlDownloadHelper.invalidContentRangeHeader(contentRange), 0L);
                return;
            }

            this.totalContentLength = parsedTotal.get();
            this.totalParts = MultipartDownloadUtils.calculateTotalParts(totalContentLength, configuredPartSizeInBytes);
            log.debug(() -> String.format("Total content length: %d, Total parts: %d", totalContentLength, totalParts));

            Optional<SdkClientException> validationError = validatePartResponse(res, 0L);
            if (validationError.isPresent()) {
                handlePartError(validationError.get(), 0L);
                return;
            }

            if (totalParts <= 1) {
                resultFuture.complete(firstResponse);
                synchronized (subscriptionLock) {
                    subscription.cancel();
                }
                return;
            }

            processPendingTransformers();

            long remainingParts = totalParts - 1;
            long toRequest = Math.min(remainingParts, maxInFlightParts);
            synchronized (subscriptionLock) {
                subscription.request(toRequest);
            }
        });
    }

    private void processRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer,
                                long currentPart) {
        if (currentPart >= totalParts) {
            return;
        }

        if (!inFlightPermits.tryAcquire()) {
            pendingTransformers.offer(Pair.of(currentPart, transformer));
            return;
        }

        sendPartRequest(transformer, currentPart);
        processPendingTransformers();
    }

    private void sendPartRequest(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer,
                                 long partIndex) {
        if (downloadFailed.get()) {
            inFlightPermits.release();
            return;
        }

        PresignedUrlDownloadRequest partRequest = createRangedGetRequest(partIndex);
        log.debug(() -> "Sending range request for part " + partIndex + " with range=" + partRequest.range());

        CompletableFuture<GetObjectResponse> response =
            s3AsyncClient.presignedUrlExtension().getObject(partRequest, transformer);

        inFlightRequests.put(partIndex, response);
        CompletableFutureUtils.forwardExceptionTo(resultFuture, response);

        response.whenComplete((res, error) -> {
            inFlightRequests.remove(partIndex);
            inFlightPermits.release();

            if (error != null) {
                handlePartError(error, partIndex);
                return;
            }
            if (downloadFailed.get()) {
                log.debug(() -> "Ignoring late completion for part " + partIndex + ", download already failed");
                return;
            }

            Optional<SdkClientException> validationError = validatePartResponse(res, partIndex);
            if (validationError.isPresent()) {
                handlePartError(validationError.get(), partIndex);
                return;
            }

            log.debug(() -> "Completed part: " + partIndex);
            long totalComplete = completedParts.incrementAndGet();

            if (totalComplete == totalParts) {
                resultFuture.complete(firstResponse);
                synchronized (subscriptionLock) {
                    subscription.cancel();
                }
            } else {
                processPendingTransformers();
                synchronized (subscriptionLock) {
                    subscription.request(1);
                }
            }
        });
    }

    private void processPendingTransformers() {
        // Re-check after releasing the gate to catch permits that arrived
        // while exiting — prevents "missed signal" where no thread drains the queue.
        do {
            if (!processingPending.compareAndSet(false, true)) {
                return;
            }
            try {
                // Drain pending queue while permits are available
                while (!pendingTransformers.isEmpty() && inFlightPermits.tryAcquire()) {
                    Pair<Long, AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> pendingPart =
                        pendingTransformers.poll();
                    if (pendingPart != null && pendingPart.left() < totalParts) {
                        sendPartRequest(pendingPart.right(), pendingPart.left());
                    } else {
                        inFlightPermits.release();
                    }
                }
            } finally {
                processingPending.set(false);
            }
        } while (!pendingTransformers.isEmpty() && inFlightPermits.availablePermits() > 0);
    }

    private Optional<SdkClientException> validatePartResponse(GetObjectResponse response, long partIndex) {
        return PresignedUrlDownloadHelper.validatePartResponse(
            response, partIndex, configuredPartSizeInBytes, totalContentLength, totalParts);
    }

    private void handlePartError(Throwable error, long partIndex) {
        if (downloadFailed.compareAndSet(false, true)) {
            log.debug(() -> "Error on part " + partIndex, error);
            resultFuture.completeExceptionally(error);
            inFlightRequests.values().forEach(future -> future.cancel(true));
            synchronized (subscriptionLock) {
                if (subscription != null) {
                    subscription.cancel();
                }
            }
        }
    }

    private PresignedUrlDownloadRequest createRangedGetRequest(long partIndex) {
        return PresignedUrlDownloadHelper.createRangedGetRequest(
            presignedUrlDownloadRequest, partIndex, configuredPartSizeInBytes, totalContentLength, eTag);
    }

    @Override
    public void onError(Throwable t) {
        log.debug(() -> "Error in parallel multipart download", t);
        resultFuture.completeExceptionally(t);
        inFlightRequests.values().forEach(future -> future.cancel(true));
    }

    @Override
    public void onComplete() {
        // Completion is handled by resultFuture
    }
}