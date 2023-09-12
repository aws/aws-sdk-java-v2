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


import java.util.Collection;
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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * An internal helper class that automatically uses multipart upload based on the size of the object.
 */
@SdkInternalApi
public final class UploadWithKnownContentLengthHelper {
    private static final Logger log = Logger.loggerFor(UploadWithKnownContentLengthHelper.class);

    private final S3AsyncClient s3AsyncClient;
    private final long partSizeInBytes;
    private final GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper;

    private final long maxMemoryUsageInBytes;
    private final long multipartUploadThresholdInBytes;
    private final MultipartUploadHelper multipartUploadHelper;

    public UploadWithKnownContentLengthHelper(S3AsyncClient s3AsyncClient,
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
                                                             AsyncRequestBody asyncRequestBody,
                                                             long contentLength) {
        CompletableFuture<PutObjectResponse> returnFuture = new CompletableFuture<>();

        try {
            if (contentLength > multipartUploadThresholdInBytes && contentLength > partSizeInBytes) {
                log.debug(() -> "Starting the upload as multipart upload request");
                uploadInParts(putObjectRequest, contentLength, asyncRequestBody, returnFuture);
            } else {
                log.debug(() -> "Starting the upload as a single upload part request");
                multipartUploadHelper.uploadInOneChunk(putObjectRequest, asyncRequestBody, returnFuture);
            }

        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return returnFuture;
    }

    private void uploadInParts(PutObjectRequest putObjectRequest, long contentLength, AsyncRequestBody asyncRequestBody,
                               CompletableFuture<PutObjectResponse> returnFuture) {

        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            multipartUploadHelper.createMultipartUpload(putObjectRequest, returnFuture);

        createMultipartUploadFuture.whenComplete((createMultipartUploadResponse, throwable) -> {
            if (throwable != null) {
                genericMultipartHelper.handleException(returnFuture, () -> "Failed to initiate multipart upload", throwable);
            } else {
                log.debug(() -> "Initiated a new multipart upload, uploadId: " + createMultipartUploadResponse.uploadId());
                doUploadInParts(Pair.of(putObjectRequest, asyncRequestBody), contentLength, returnFuture,
                                createMultipartUploadResponse.uploadId());
            }
        });
    }

    private void doUploadInParts(Pair<PutObjectRequest, AsyncRequestBody> request,
                                 long contentLength,
                                 CompletableFuture<PutObjectResponse> returnFuture,
                                 String uploadId) {

        long optimalPartSize = genericMultipartHelper.calculateOptimalPartSizeFor(contentLength, partSizeInBytes);
        int partCount = genericMultipartHelper.determinePartCount(contentLength, optimalPartSize);
        if (optimalPartSize > partSizeInBytes) {
            log.debug(() -> String.format("Configured partSize is %d, but using %d to prevent reaching maximum number of parts "
                                          + "allowed", partSizeInBytes, optimalPartSize));
        }

        log.debug(() -> String.format("Starting multipart upload with partCount: %d, optimalPartSize: %d", partCount,
                                      optimalPartSize));

        MpuRequestContext mpuRequestContext = new MpuRequestContext(request, contentLength, optimalPartSize, uploadId);

        request.right()
               .split(b -> b.chunkSizeInBytes(mpuRequestContext.partSize)
                            .bufferSizeInBytes(maxMemoryUsageInBytes))
               .subscribe(new KnownContentLengthAsyncRequestBodySubscriber(mpuRequestContext,
                                                                           returnFuture));
    }

    private static final class MpuRequestContext {
        private final Pair<PutObjectRequest, AsyncRequestBody> request;
        private final long contentLength;
        private final long partSize;

        private final String uploadId;

        private MpuRequestContext(Pair<PutObjectRequest, AsyncRequestBody> request,
                                  long contentLength,
                                  long partSize,
                                  String uploadId) {
            this.request = request;
            this.contentLength = contentLength;
            this.partSize = partSize;
            this.uploadId = uploadId;
        }
    }

    private class KnownContentLengthAsyncRequestBodySubscriber implements Subscriber<AsyncRequestBody> {

        /**
         * The number of AsyncRequestBody has been received but yet to be processed
         */
        private final AtomicInteger asyncRequestBodyInFlight = new AtomicInteger(0);

        /**
         * Indicates whether CompleteMultipart has been initiated or not.
         */
        private final AtomicBoolean completedMultipartInitiated = new AtomicBoolean(false);

        private final AtomicBoolean failureActionInitiated = new AtomicBoolean(false);

        private final AtomicInteger partNumber = new AtomicInteger(1);

        private final AtomicReferenceArray<CompletedPart> completedParts;
        private final String uploadId;
        private final Collection<CompletableFuture<CompletedPart>> futures = new ConcurrentLinkedQueue<>();

        private final PutObjectRequest putObjectRequest;
        private final CompletableFuture<PutObjectResponse> returnFuture;
        private Subscription subscription;

        private volatile boolean isDone;

        KnownContentLengthAsyncRequestBodySubscriber(MpuRequestContext mpuRequestContext,
                                                     CompletableFuture<PutObjectResponse> returnFuture) {
            long optimalPartSize = genericMultipartHelper.calculateOptimalPartSizeFor(mpuRequestContext.contentLength,
                                                                                      partSizeInBytes);
            int partCount = genericMultipartHelper.determinePartCount(mpuRequestContext.contentLength, optimalPartSize);
            this.putObjectRequest = mpuRequestContext.request.left();
            this.returnFuture = returnFuture;
            this.completedParts = new AtomicReferenceArray<>(partCount);
            this.uploadId = mpuRequestContext.uploadId;
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
                    if (failureActionInitiated.compareAndSet(false, true)) {
                        multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture, putObjectRequest);
                    }
                }
            });
        }

        @Override
        public void onNext(AsyncRequestBody asyncRequestBody) {
            log.trace(() -> "Received asyncRequestBody " + asyncRequestBody.contentLength());
            asyncRequestBodyInFlight.incrementAndGet();
            UploadPartRequest uploadRequest =
                SdkPojoConversionUtils.toUploadPartRequest(putObjectRequest,
                                                           partNumber.getAndIncrement(),
                                                           uploadId);

            Consumer<CompletedPart> completedPartConsumer = completedPart -> completedParts.set(completedPart.partNumber() - 1,
                                                                                                completedPart);
            multipartUploadHelper.sendIndividualUploadPartRequest(uploadId, completedPartConsumer, futures,
                                                                  Pair.of(uploadRequest, asyncRequestBody))
                                 .whenComplete((r, t) -> {
                                     if (t != null) {
                                         if (failureActionInitiated.compareAndSet(false, true)) {
                                             multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture,
                                                                                         putObjectRequest);
                                         }
                                     } else {
                                         completeMultipartUploadIfFinish(asyncRequestBodyInFlight.decrementAndGet());
                                     }
                                 });
            subscription.request(1);
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
            completeMultipartUploadIfFinish(asyncRequestBodyInFlight.get());
        }

        private void completeMultipartUploadIfFinish(int requestsInFlight) {
            if (isDone && requestsInFlight == 0 && completedMultipartInitiated.compareAndSet(false, true)) {
                CompletedPart[] parts =
                    IntStream.range(0, completedParts.length())
                             .mapToObj(completedParts::get)
                             .toArray(CompletedPart[]::new);
                multipartUploadHelper.completeMultipartUpload(returnFuture, uploadId, parts, putObjectRequest);
            }
        }

    }
}
