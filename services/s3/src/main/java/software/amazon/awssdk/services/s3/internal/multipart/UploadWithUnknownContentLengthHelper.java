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


import static software.amazon.awssdk.services.s3.internal.multipart.SdkPojoConversionUtils.toAbortMultipartUploadRequest;

import java.util.Collection;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SplitAsyncRequestBodyResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * An internal helper class that uploads streams with unknown content length.
 */
@SdkInternalApi
public final class UploadWithUnknownContentLengthHelper extends BaseMultipartUploadHelper {
    private static final Logger log = Logger.loggerFor(UploadWithUnknownContentLengthHelper.class);

    private final S3AsyncClient s3AsyncClient;
    private final long partSizeInBytes;
    private final GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper;

    private final long maxMemoryUsageInBytes;
    private final long multipartUploadThresholdInBytes;

    public UploadWithUnknownContentLengthHelper(S3AsyncClient s3AsyncClient,
                                                long partSizeInBytes,
                                                long multipartUploadThresholdInBytes,
                                                long maxMemoryUsageInBytes) {
        super(s3AsyncClient, partSizeInBytes, multipartUploadThresholdInBytes, maxMemoryUsageInBytes);
        this.s3AsyncClient = s3AsyncClient;
        this.partSizeInBytes = partSizeInBytes;
        this.genericMultipartHelper = new GenericMultipartHelper<>(s3AsyncClient,
                                                                   SdkPojoConversionUtils::toAbortMultipartUploadRequest,
                                                                   SdkPojoConversionUtils::toPutObjectResponse);
        this.maxMemoryUsageInBytes = maxMemoryUsageInBytes;
        this.multipartUploadThresholdInBytes = multipartUploadThresholdInBytes;
    }

    public CompletableFuture<PutObjectResponse> uploadObject(PutObjectRequest putObjectRequest,
                                                             AsyncRequestBody asyncRequestBody) {
        CompletableFuture<PutObjectResponse> returnFuture = new CompletableFuture<>();
        Long contentLength = asyncRequestBody.contentLength().orElseGet(putObjectRequest::contentLength);

        SplitAsyncRequestBodyResponse splitAsyncRequestBodyResponse = asyncRequestBody.split(partSizeInBytes, maxMemoryUsageInBytes);

        splitAsyncRequestBodyResponse.asyncRequestBodyPublisher()
                                     .subscribe(
                                         new UnknownContentLengthAsyncRequestBodySubscriber(partSizeInBytes,
                                                                                            putObjectRequest,
                                                                                            returnFuture));

        return returnFuture;
    }

    private class UnknownContentLengthAsyncRequestBodySubscriber implements Subscriber<AsyncRequestBody> {
        /**
         * Indicates whether this is the first async request body or not.
         */
        private final AtomicBoolean isFirstAsyncRequestBody = new AtomicBoolean(true);

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

        private AtomicInteger partNumber = new AtomicInteger(1);

        private final Queue<CompletedPart> completedParts = new ConcurrentLinkedQueue<>();
        private final Collection<CompletableFuture<CompletedPart>> futures = new ConcurrentLinkedQueue<>();

        private final CompletableFuture<String> uploadIdFuture = new CompletableFuture<>();

        private final long maximumChunkSizeInByte;
        private final PutObjectRequest putObjectRequest;
        private final CompletableFuture<PutObjectResponse> returnFuture;
        private Subscription subscription;
        private AsyncRequestBody firstRequestBody;

        private String uploadId;
        private volatile boolean isDone;

        public UnknownContentLengthAsyncRequestBodySubscriber(long maximumChunkSizeInByte,
                                                              PutObjectRequest putObjectRequest,
                                                              CompletableFuture<PutObjectResponse> returnFuture) {
            this.maximumChunkSizeInByte = maximumChunkSizeInByte;
            this.putObjectRequest = putObjectRequest;
            this.returnFuture = returnFuture;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(AsyncRequestBody asyncRequestBody) {
            log.debug(() -> "Received asyncRequestBody " + asyncRequestBody.contentLength());
            asyncRequestBodyInFlight.incrementAndGet();

            if (isFirstAsyncRequestBody.compareAndSet(true, false)) {
                log.debug(() -> "Received first async request body");
                // If this is the first AsyncRequestBody received, request another one because we don't know if there is more
                firstRequestBody = asyncRequestBody;
                subscription.request(1);
                log.debug(() -> "requesting");
                return;
            }

            // If there are more than 1 AsyncRequestBodies, then we know we need to upload this
            // object using MPU
            if (createMultipartUploadInitiated.compareAndSet(false, true)) {
                log.debug(() -> "Starting the upload as multipart upload request");
                CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
                    createMultipartUpload(putObjectRequest, returnFuture);

                createMultipartUploadFuture.whenComplete((createMultipartUploadResponse, throwable) -> {
                    if (throwable != null) {
                        genericMultipartHelper.handleException(returnFuture, () -> "Failed to initiate multipart upload",
                                                               throwable);
                        subscription.cancel();
                    } else {
                        uploadId = createMultipartUploadResponse.uploadId();
                        uploadIdFuture.complete(uploadId);
                        log.debug(() -> "Initiated a new multipart upload, uploadId: " + createMultipartUploadResponse.uploadId());

                        sendIndividualUploadPartRequest(uploadId, completedParts, futures, uploadPart(firstRequestBody));
                        sendIndividualUploadPartRequest(uploadId, completedParts, futures, uploadPart(asyncRequestBody));
                    }
                });
            } else {
                uploadIdFuture.whenComplete((r, t) -> sendIndividualUploadPartRequest(uploadId, completedParts, futures,
                                                                                      uploadPart(asyncRequestBody)));
            }
        }

        private Pair<UploadPartRequest, AsyncRequestBody> uploadPart(AsyncRequestBody asyncRequestBody) {
            UploadPartRequest uploadRequest =
                SdkPojoConversionUtils.toUploadPartRequest(putObjectRequest,
                                                           partNumber.getAndIncrement(),
                                                           uploadId);
            return Pair.of(uploadRequest, asyncRequestBody);
        }

        @Override
        public void onError(Throwable t) {
            genericMultipartHelper.handleException(returnFuture, () -> "Failed to send multipart upload requests", t);
            returnFuture.completeExceptionally(t);
            if (uploadId != null) {
                genericMultipartHelper.cleanUpParts(uploadId, toAbortMultipartUploadRequest(putObjectRequest));
            }
            cancelingOtherOngoingRequests(futures, t);
        }

        @Override
        public void onComplete() {
            log.debug(() -> "Received onComplete()");
            // If CreateMultipartUpload has not been initiated at this point, we know this is a single object upload
            if (createMultipartUploadInitiated.get() == false) {
                log.debug(() -> "Starting the upload as a single object upload request");
                uploadInOneChunk(putObjectRequest, firstRequestBody, returnFuture);
            } else {
                isDone = true;
                completeMultipartUploadIfFinish(asyncRequestBodyInFlight.get());
            }
        }

        private void sendIndividualUploadPartRequest(String uploadId,
                                                     Queue<CompletedPart> completedParts,
                                                     Collection<CompletableFuture<CompletedPart>> futures,
                                                     Pair<UploadPartRequest, AsyncRequestBody> requestPair) {
            UploadPartRequest uploadPartRequest = requestPair.left();
            Integer partNumber = uploadPartRequest.partNumber();
            log.debug(() -> "Sending uploadPartRequest: " + uploadPartRequest.partNumber() + " uploadId: " + uploadId + " "
                            + "contentLength " + requestPair.right().contentLength());

            CompletableFuture<UploadPartResponse> uploadPartFuture = s3AsyncClient.uploadPart(uploadPartRequest, requestPair.right());

            CompletableFuture<CompletedPart> convertFuture =
                uploadPartFuture.thenApply(uploadPartResponse -> convertUploadPartResponse(completedParts, partNumber,
                                                                                           uploadPartResponse))
                                .whenComplete((r, t) -> {
                                    int numRequests = asyncRequestBodyInFlight.decrementAndGet();
                                    completeMultipartUploadIfFinish(numRequests);
                                });
            futures.add(convertFuture);
            CompletableFutureUtils.forwardExceptionTo(convertFuture, uploadPartFuture);
            synchronized (this) {
                subscription.request(1);
            }
        }

        private void completeMultipartUploadIfFinish(int requestsInFlight) {
            if (isDone && requestsInFlight == 0 && completedMultipartInitiated.compareAndSet(false, true)) {
                    CompletedPart[] parts = completedParts.stream()
                                                          .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                                                          .toArray(CompletedPart[]::new);
                    completeMultipartUpload(returnFuture, uploadId, parts, putObjectRequest, futures);
            }
        }
    }
}
