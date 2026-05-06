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

import static software.amazon.awssdk.services.s3.internal.multipart.MultipartUploadHelper.contentLengthMismatchForPart;
import static software.amazon.awssdk.services.s3.internal.multipart.MultipartUploadHelper.contentLengthMissingForPart;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.JAVA_PROGRESS_LISTENER;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class UnknownContentLengthAsyncRequestBodySubscriber implements Subscriber<CloseableAsyncRequestBody> {
    private static final Logger log = Logger.loggerFor(UnknownContentLengthAsyncRequestBodySubscriber.class);

    /**
     * Indicates whether this is the first async request body or not.
     */
    private final AtomicBoolean firstAsyncRequestBodyReceived = new AtomicBoolean(false);

    /**
     * Indicates whether CreateMultipartUpload has been initiated or not
     */
    private final AtomicBoolean createMultipartUploadInitiated = new AtomicBoolean(false);

    /**
     * Indicates whether CompleteMultipart has been initiated or not.
     */
    private final AtomicBoolean completedMultipartInitiated = new AtomicBoolean(false);

    /**
     * The number of AsyncRequestBody has been received but yet to be processed
     */
    private final AtomicInteger asyncRequestBodyInFlight = new AtomicInteger(0);

    private final AtomicBoolean failureActionInitiated = new AtomicBoolean(false);

    private final AtomicInteger partNumber = new AtomicInteger(0);
    private final AtomicLong contentLength = new AtomicLong(0);

    private final Queue<CompletedPart> completedParts = new ConcurrentLinkedQueue<>();
    private final Collection<CompletableFuture<CompletedPart>> futures = new ConcurrentLinkedQueue<>();

    private final CompletableFuture<String> uploadIdFuture = new CompletableFuture<>();

    private final long partSizeInBytes;
    private final PutObjectRequest putObjectRequest;
    private final CompletableFuture<PutObjectResponse> returnFuture;
    private final PublisherListener<Long> progressListener;
    private final MultipartUploadHelper multipartUploadHelper;
    private final GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper;
    private final int maxInFlightParts;

    private final Object subscriptionLock = new Object();
    private Subscription subscription;
    private CloseableAsyncRequestBody firstRequestBody;
    private String uploadId;
    private volatile boolean isDone;

    UnknownContentLengthAsyncRequestBodySubscriber(
        long partSizeInBytes,
        PutObjectRequest putObjectRequest,
        CompletableFuture<PutObjectResponse> returnFuture,
        MultipartUploadHelper multipartUploadHelper,
        GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper,
        int maxInFlightParts) {
        this.partSizeInBytes = partSizeInBytes;
        this.putObjectRequest = putObjectRequest;
        this.returnFuture = returnFuture;
        this.multipartUploadHelper = multipartUploadHelper;
        this.genericMultipartHelper = genericMultipartHelper;
        this.maxInFlightParts = maxInFlightParts;
        this.progressListener = putObjectRequest.overrideConfiguration()
                                                .map(c -> c.executionAttributes().getAttribute(JAVA_PROGRESS_LISTENER))
                                                .orElseGet(PublisherListener::noOp);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (this.subscription != null) {
            log.warn(() -> "The subscriber has already been subscribed. Cancelling the incoming subscription");
            subscription.cancel();
            return;
        }
        this.subscription = s;
        s.request(1);
        returnFuture.whenComplete((r, t) -> {
            if (t != null) {
                s.cancel();
                MultipartUploadHelper.cancelingOtherOngoingRequests(futures, t);
            }
        });
    }

    @Override
    public void onNext(CloseableAsyncRequestBody asyncRequestBody) {
        if (asyncRequestBody == null) {
            NullPointerException exception = new NullPointerException("asyncRequestBody passed to onNext MUST NOT be null.");
            multipartUploadHelper.failRequestsElegantly(futures, exception, uploadId, returnFuture, putObjectRequest);
            throw exception;
        }

        if (isDone) {
            return;
        }

        int currentPartNum = partNumber.incrementAndGet();
        log.trace(() -> "Received asyncRequestBody " + asyncRequestBody.contentLength());
        asyncRequestBodyInFlight.incrementAndGet();

        Optional<SdkClientException> sdkClientException = validatePart(asyncRequestBody, currentPartNum);
        if (sdkClientException.isPresent()) {
            multipartUploadHelper.failRequestsElegantly(
                futures, sdkClientException.get(), uploadId, returnFuture, putObjectRequest);
            subscription.cancel();
            return;
        }

        if (firstAsyncRequestBodyReceived.compareAndSet(false, true)) {
            log.trace(() -> "Received first async request body");
            firstRequestBody = asyncRequestBody;
            // If this is the first AsyncRequestBody received, request another one because we don't know if there is more
            synchronized (subscriptionLock) {
                subscription.request(1);
            }
            return;
        }

        // If there are more than 1 AsyncRequestBodies, then we know we need to upload using MPU
        if (createMultipartUploadInitiated.compareAndSet(false, true)) {
            log.debug(() -> "Starting the upload as multipart upload request");
            CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture = multipartUploadHelper
                .createMultipartUpload(putObjectRequest, returnFuture);

            createMultipartUploadFuture.whenComplete((createMultipartUploadResponse, throwable) -> {
                if (throwable != null) {
                    genericMultipartHelper.handleException(returnFuture, () -> "Failed to initiate multipart upload", throwable);
                    subscription.cancel();
                } else {
                    uploadId = createMultipartUploadResponse.uploadId();
                    log.debug(() -> "Initiated a new multipart upload, uploadId: " + uploadId);

                    sendUploadPartRequest(uploadId, firstRequestBody, 1);
                    sendUploadPartRequest(uploadId, asyncRequestBody, 2);
                    uploadIdFuture.complete(uploadId);

                    // 2 parts already in flight, request the rest of our max in flight
                    int additionalDemand = maxInFlightParts - 2;
                    if (additionalDemand > 0) {
                        synchronized (subscriptionLock) {
                            subscription.request(additionalDemand);
                        }
                    }
                }
            });
            CompletableFutureUtils.forwardExceptionTo(returnFuture, createMultipartUploadFuture);
        } else {
            uploadIdFuture.whenComplete((r, t) -> {
                sendUploadPartRequest(uploadId, asyncRequestBody, currentPartNum);
            });
        }
    }

    private Optional<SdkClientException> validatePart(AsyncRequestBody asyncRequestBody, int currentPartNum) {
        Optional<Long> contentLength = asyncRequestBody.contentLength();
        if (!contentLength.isPresent()) {
            return Optional.of(contentLengthMissingForPart(currentPartNum));
        }

        Long contentLengthCurrentPart = contentLength.get();
        if (contentLengthCurrentPart > partSizeInBytes) {
            return Optional.of(contentLengthMismatchForPart(partSizeInBytes, contentLengthCurrentPart, currentPartNum));
        }
        return Optional.empty();
    }

    private void sendUploadPartRequest(String uploadId,
            CloseableAsyncRequestBody asyncRequestBody,
            int currentPartNum) {
        Long contentLengthCurrentPart = asyncRequestBody.contentLength().get();
        this.contentLength.getAndAdd(contentLengthCurrentPart);

        multipartUploadHelper
            .sendIndividualUploadPartRequest(uploadId, completedParts::add, futures,
                                             uploadPart(asyncRequestBody, currentPartNum), progressListener)
            .whenComplete((r, t) -> {
                asyncRequestBody.close();
                if (t != null) {
                    if (failureActionInitiated.compareAndSet(false, true)) {
                        multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture,
                                                                    putObjectRequest);
                    }
                } else {
                    int inFlight = asyncRequestBodyInFlight.decrementAndGet();
                    if (!isDone && inFlight < maxInFlightParts) {
                        synchronized (subscriptionLock) {
                            subscription.request(1);
                        }
                    }
                    completeMultipartUploadIfFinish(inFlight);
                }
            });
    }

    private Pair<UploadPartRequest, AsyncRequestBody> uploadPart(AsyncRequestBody asyncRequestBody, int partNum) {
        UploadPartRequest uploadRequest = SdkPojoConversionUtils.toUploadPartRequest(putObjectRequest,
                                                                                     partNum,
                                                                                     uploadId);

        return Pair.of(uploadRequest, asyncRequestBody);
    }

    @Override
    public void onError(Throwable t) {
        log.debug(() -> "Received onError() ", t);
        if (failureActionInitiated.compareAndSet(false, true)) {
            isDone = true;
            multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture, putObjectRequest);
        }
    }

    @Override
    public void onComplete() {
        log.debug(() -> "Received onComplete()");
        // If CreateMultipartUpload has not been initiated at this point, we know this
        // is a single object upload, and if no async request body has been received, it's an empty stream
        if (createMultipartUploadInitiated.get() == false) {
            log.debug(() -> "Starting the upload as a single object upload request");
            AsyncRequestBody entireRequestBody = firstAsyncRequestBodyReceived.get() ? firstRequestBody
                    : AsyncRequestBody.empty();
            multipartUploadHelper.uploadInOneChunk(putObjectRequest, entireRequestBody, returnFuture);
        } else {
            isDone = true;
            completeMultipartUploadIfFinish(asyncRequestBodyInFlight.get());
        }
    }

    private void completeMultipartUploadIfFinish(int requestsInFlight) {
        if (isDone && requestsInFlight == 0 && completedMultipartInitiated.compareAndSet(false, true)) {
            CompletedPart[] parts = completedParts.stream()
                                                  .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                                                  .toArray(CompletedPart[]::new);

            long totalLength = contentLength.get();
            int expectedNumParts = genericMultipartHelper.determinePartCount(totalLength, partSizeInBytes);
            if (parts.length != expectedNumParts) {
                SdkClientException exception = SdkClientException.create(
                    String.format(
                        "The number of UploadParts requests is not equal to the expected number of parts. "
                        + "Expected: %d, Actual: %d",
                        expectedNumParts, parts.length));
                multipartUploadHelper.failRequestsElegantly(futures, exception, uploadId, returnFuture, putObjectRequest);
                return;
            }

            multipartUploadHelper.completeMultipartUpload(returnFuture, uploadId, parts, putObjectRequest, totalLength);
        }
    }
}
