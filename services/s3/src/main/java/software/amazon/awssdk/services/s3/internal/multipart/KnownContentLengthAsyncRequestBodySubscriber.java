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

import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.JAVA_PROGRESS_LISTENER;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class KnownContentLengthAsyncRequestBodySubscriber implements Subscriber<AsyncRequestBody>  {

    private static final Logger log = Logger.loggerFor(KnownContentLengthAsyncRequestBodySubscriber.class);

    /**
     * The number of AsyncRequestBody has been received but yet to be processed
     */
    private final AtomicInteger asyncRequestBodyInFlight = new AtomicInteger(0);
    private final AtomicBoolean failureActionInitiated = new AtomicBoolean(false);
    private final AtomicInteger partNumber = new AtomicInteger(1);
    private final MultipartUploadHelper multipartUploadHelper;
    private final long contentLength;
    private final long partSize;
    private final int partCount;
    private final int numExistingParts;
    private final String uploadId;
    private final Collection<CompletableFuture<CompletedPart>> futures = new ConcurrentLinkedQueue<>();
    private final PutObjectRequest putObjectRequest;
    private final CompletableFuture<PutObjectResponse> returnFuture;
    private final AtomicReferenceArray<CompletedPart> completedParts;
    private final Map<Integer, CompletedPart> existingParts;
    private final PublisherListener<Long> progressListener;
    private Subscription subscription;
    private volatile boolean isDone;
    private volatile boolean isPaused;
    /**
     * Indicates whether CompleteMultipart has been initiated or not.
     */
    private final AtomicBoolean completedMultipartInitiated = new AtomicBoolean(false);
    private volatile CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture;
    private volatile CompletableFuture<String> checksumFuture;

    KnownContentLengthAsyncRequestBodySubscriber(MpuRequestContext mpuRequestContext,
                                                 CompletableFuture<PutObjectResponse> returnFuture,
                                                 MultipartUploadHelper multipartUploadHelper) {
        this.contentLength = mpuRequestContext.contentLength();
        this.partSize = mpuRequestContext.partSize();
        this.partCount = determinePartCount(contentLength, partSize);
        this.putObjectRequest = mpuRequestContext.request().left();
        this.returnFuture = returnFuture;
        this.uploadId = mpuRequestContext.uploadId();
        this.existingParts = mpuRequestContext.existingParts() == null ? new HashMap<>() : mpuRequestContext.existingParts();
        this.numExistingParts = NumericUtils.saturatedCast(mpuRequestContext.numPartsCompleted());
        this.completedParts = new AtomicReferenceArray<>(partCount);
        this.multipartUploadHelper = multipartUploadHelper;
        this.progressListener = putObjectRequest.overrideConfiguration().map(c -> c.executionAttributes()
                                                                                   .getAttribute(JAVA_PROGRESS_LISTENER))
                                                .orElseGet(PublisherListener::noOp);
    }

    private int determinePartCount(long contentLength, long partSize) {
        return (int) Math.ceil(contentLength / (double) partSize);
    }

    public S3ResumeToken pause() {
        isPaused = true;

        if (completeMpuFuture != null && completeMpuFuture.isDone()) {
            return null;
        }

        if (completeMpuFuture != null && !completeMpuFuture.isDone()) {
            completeMpuFuture.cancel(true);
        }

        long numPartsCompleted = 0;
        for (CompletableFuture<CompletedPart> cf : futures) {
            if (!cf.isDone()) {
                cf.cancel(true);
            } else {
                numPartsCompleted++;
            }
        }

        return S3ResumeToken.builder()
                            .uploadId(uploadId)
                            .partSize(partSize)
                            .totalNumParts((long) partCount)
                            .numPartsCompleted(numPartsCompleted + numExistingParts)
                            .build();
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
                if (shouldFailRequest()) {
                    multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture, putObjectRequest);
                }
            }
        });
    }

    @Override
    public void onNext(AsyncRequestBody asyncRequestBody) {
        if (isPaused) {
            return;
        }

        if (existingParts.containsKey(partNumber.get())) {
            partNumber.getAndIncrement();
            asyncRequestBody.subscribe(new CancelledSubscriber<>());
            subscription.request(1);
            asyncRequestBody.contentLength().ifPresent(progressListener::subscriberOnNext);
            return;
        }

        asyncRequestBodyInFlight.incrementAndGet();
        UploadPartRequest uploadRequest = SdkPojoConversionUtils.toUploadPartRequest(putObjectRequest,
                                                                                     partNumber.getAndIncrement(),
                                                                                     uploadId);

        Consumer<CompletedPart> completedPartConsumer = completedPart -> completedParts.set(completedPart.partNumber() - 1,
                                                                                            completedPart);
        multipartUploadHelper.sendIndividualUploadPartRequest(uploadId, completedPartConsumer, futures,
                                                              Pair.of(uploadRequest, asyncRequestBody), progressListener)
                             .whenComplete((r, t) -> {
                                 if (t != null) {
                                     if (shouldFailRequest()) {
                                         multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture,
                                                                                     putObjectRequest);
                                     }
                                 } else {
                                     completeMultipartUploadIfFinished(asyncRequestBodyInFlight.decrementAndGet());
                                 }
                             });
        subscription.request(1);
    }

    private boolean shouldFailRequest() {
        return failureActionInitiated.compareAndSet(false, true) && !isPaused;
    }

    @Override
    public void onError(Throwable t) {
        log.debug(() -> "Received onError ", t);
        if (failureActionInitiated.compareAndSet(false, true)) {
            multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture, putObjectRequest);
        }
    }

    @Override
    public void onComplete() {
        log.debug(() -> "Received onComplete()");
        isDone = true;
        if (!isPaused) {
            completeMultipartUploadIfFinished(asyncRequestBodyInFlight.get());
        }
    }

    private void completeMultipartUploadIfFinished(int requestsInFlight) {
        if (isDone && requestsInFlight == 0 && completedMultipartInitiated.compareAndSet(false, true)) {
            CompletedPart[] parts;
            if (existingParts.isEmpty()) {
                parts =
                    IntStream.range(0, completedParts.length())
                             .mapToObj(completedParts::get)
                             .toArray(CompletedPart[]::new);
            } else {
                // List of CompletedParts needs to be in ascending order
                parts = mergeCompletedParts();
            }

            PutObjectRequest newPutObjectRequest =
                putObjectRequest.toBuilder().checksumCRC32(checksumFuture.join()).build();
            completeMpuFuture = multipartUploadHelper.completeMultipartUpload(returnFuture, uploadId, parts, newPutObjectRequest,
                                                                              contentLength);
        }
    }

    private CompletedPart[] mergeCompletedParts() {
        CompletedPart[] merged = new CompletedPart[partCount];
        int currPart = 1;
        while (currPart < partCount + 1) {
            CompletedPart completedPart = existingParts.containsKey(currPart) ? existingParts.get(currPart) :
                                          completedParts.get(currPart - 1);
            merged[currPart - 1] = completedPart;
            currPart++;
        }
        return merged;
    }

    public void setChecksumFuture(CompletableFuture<String> checksumFuture) {
        this.checksumFuture = checksumFuture;
    }
}
