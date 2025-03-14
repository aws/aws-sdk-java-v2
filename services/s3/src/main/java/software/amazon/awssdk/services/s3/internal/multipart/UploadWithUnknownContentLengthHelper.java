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
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * An internal helper class that uploads streams with unknown content length.
 */
@SdkInternalApi
public final class UploadWithUnknownContentLengthHelper {
    private static final Logger log = Logger.loggerFor(UploadWithUnknownContentLengthHelper.class);

    private final S3AsyncClient s3AsyncClient;
    private final long partSizeInBytes;
    private final GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper;

    private final long maxMemoryUsageInBytes;
    private final long multipartUploadThresholdInBytes;

    private final MultipartUploadHelper multipartUploadHelper;

    public UploadWithUnknownContentLengthHelper(S3AsyncClient s3AsyncClient,
                                                long partSizeInBytes,
                                                long multipartUploadThresholdInBytes,
                                                long maxMemoryUsageInBytes) {
        this.s3AsyncClient = s3AsyncClient;
        this.partSizeInBytes = partSizeInBytes;
        this.genericMultipartHelper = new GenericMultipartHelper<>(s3AsyncClient,
                                                                   SdkPojoConversionUtils::toAbortMultipartUploadRequest,
                                                                   SdkPojoConversionUtils::toPutObjectResponse);
        this.maxMemoryUsageInBytes = maxMemoryUsageInBytes;
        this.multipartUploadThresholdInBytes = multipartUploadThresholdInBytes;
        this.multipartUploadHelper = new MultipartUploadHelper(s3AsyncClient, partSizeInBytes, multipartUploadThresholdInBytes,
                                                               maxMemoryUsageInBytes);
    }

    public CompletableFuture<PutObjectResponse> uploadObject(PutObjectRequest putObjectRequest,
                                                             AsyncRequestBody asyncRequestBody) {
        CompletableFuture<PutObjectResponse> returnFuture = new CompletableFuture<>();

        SdkPublisher<AsyncRequestBody> splitAsyncRequestBodyResponse =
            asyncRequestBody.split(b -> b.chunkSizeInBytes(partSizeInBytes)
                                         .bufferSizeInBytes(maxMemoryUsageInBytes));

        splitAsyncRequestBodyResponse.subscribe(new UnknownContentLengthAsyncRequestBodySubscriber(partSizeInBytes,
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

        private final AtomicBoolean failureActionInitiated = new AtomicBoolean(false);

        private AtomicInteger partNumber = new AtomicInteger(0);
        private AtomicLong contentLength = new AtomicLong(0);

        private final Queue<CompletedPart> completedParts = new ConcurrentLinkedQueue<>();
        private final Collection<CompletableFuture<CompletedPart>> futures = new ConcurrentLinkedQueue<>();

        private final CompletableFuture<String> uploadIdFuture = new CompletableFuture<>();

        private final long maximumChunkSizeInByte;
        private final PutObjectRequest putObjectRequest;
        private final CompletableFuture<PutObjectResponse> returnFuture;
        private final PublisherListener<Long> progressListener;
        private Subscription subscription;
        private AsyncRequestBody firstRequestBody;

        private String uploadId;
        private volatile boolean isDone;

        UnknownContentLengthAsyncRequestBodySubscriber(long maximumChunkSizeInByte,
                                                       PutObjectRequest putObjectRequest,
                                                       CompletableFuture<PutObjectResponse> returnFuture) {
            this.maximumChunkSizeInByte = maximumChunkSizeInByte;
            this.putObjectRequest = putObjectRequest;
            this.returnFuture = returnFuture;
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
                    multipartUploadHelper.cancelingOtherOngoingRequests(futures, t);
                }
            });
        }

        @Override
        public void onNext(AsyncRequestBody asyncRequestBody) {
            int currentPartNum = partNumber.incrementAndGet();
            log.trace(() -> "Received asyncRequestBody " + asyncRequestBody.contentLength());
            asyncRequestBodyInFlight.incrementAndGet();

            if (isFirstAsyncRequestBody.compareAndSet(true, false)) {
                log.trace(() -> "Received first async request body");
                // If this is the first AsyncRequestBody received, request another one because we don't know if there is more
                firstRequestBody = asyncRequestBody;
                subscription.request(1);
                return;
            }

            // If there are more than 1 AsyncRequestBodies, then we know we need to upload this
            // object using MPU
            if (createMultipartUploadInitiated.compareAndSet(false, true)) {
                log.debug(() -> "Starting the upload as multipart upload request");
                CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
                    multipartUploadHelper.createMultipartUpload(putObjectRequest, returnFuture);

                createMultipartUploadFuture.whenComplete((createMultipartUploadResponse, throwable) -> {
                    if (throwable != null) {
                        genericMultipartHelper.handleException(returnFuture, () -> "Failed to initiate multipart upload",
                                                               throwable);
                        subscription.cancel();
                    } else {
                        uploadId = createMultipartUploadResponse.uploadId();
                        log.debug(() -> "Initiated a new multipart upload, uploadId: " + uploadId);

                        sendUploadPartRequest(uploadId, firstRequestBody, 1);
                        sendUploadPartRequest(uploadId, asyncRequestBody, 2);

                        // We need to complete the uploadIdFuture *after* the first two requests have been sent
                        uploadIdFuture.complete(uploadId);
                    }
                });
                CompletableFutureUtils.forwardExceptionTo(returnFuture, createMultipartUploadFuture);
            } else {
                uploadIdFuture.whenComplete((r, t) -> {
                    sendUploadPartRequest(uploadId, asyncRequestBody, currentPartNum);
                });
            }
        }

        private void sendUploadPartRequest(String uploadId,
                                           AsyncRequestBody asyncRequestBody,
                                           int currentPartNum) {
            Optional<Long> contentLength = asyncRequestBody.contentLength();
            if (!contentLength.isPresent()) {
                SdkClientException e = SdkClientException.create("Content length must be present on the AsyncRequestBody");
                multipartUploadHelper.failRequestsElegantly(futures, e, uploadId, returnFuture, putObjectRequest);
            }
            this.contentLength.getAndAdd(contentLength.get());

            multipartUploadHelper.sendIndividualUploadPartRequest(uploadId, completedParts::add, futures,
                                                                  uploadPart(asyncRequestBody, currentPartNum), progressListener)
                                 .whenComplete((r, t) -> {
                                     if (t != null) {
                                         if (failureActionInitiated.compareAndSet(false, true)) {
                                             multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture, putObjectRequest);
                                         }
                                     } else {
                                         completeMultipartUploadIfFinish(asyncRequestBodyInFlight.decrementAndGet());
                                     }
                                 });
            synchronized (this) {
                subscription.request(1);
            };
        }

        private Pair<UploadPartRequest, AsyncRequestBody> uploadPart(AsyncRequestBody asyncRequestBody, int partNum) {
            UploadPartRequest uploadRequest =
                SdkPojoConversionUtils.toUploadPartRequest(putObjectRequest,
                                                           partNum,
                                                           uploadId);
            return Pair.of(uploadRequest, asyncRequestBody);
        }

        @Override
        public void onError(Throwable t) {
            log.debug(() -> "Received onError() ", t);
            if (failureActionInitiated.compareAndSet(false, true)) {
                multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture, putObjectRequest);
            }
        }

        @Override
        public void onComplete() {
            log.debug(() -> "Received onComplete()");
            // If CreateMultipartUpload has not been initiated at this point, we know this is a single object upload
            if (createMultipartUploadInitiated.get() == false) {
                log.debug(() -> "Starting the upload as a single object upload request");
                multipartUploadHelper.uploadInOneChunk(putObjectRequest, firstRequestBody, returnFuture);
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
                multipartUploadHelper.completeMultipartUpload(returnFuture, uploadId, parts, putObjectRequest,
                                                              this.contentLength.get());
            }
        }
    }
}
