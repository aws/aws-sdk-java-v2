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

import static software.amazon.awssdk.services.s3.multipart.S3PauseResumeExecutionAttribute.PAUSE_OBSERVABLE;
import static software.amazon.awssdk.services.s3.multipart.S3PauseResumeExecutionAttribute.RESUME_TOKEN;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.services.s3.paginators.ListPartsPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * An internal helper class that automatically uses multipart upload based on the size of the object.
 */
@SdkInternalApi
public final class UploadWithKnownContentLengthHelper {
    private static final Logger log = Logger.loggerFor(UploadWithKnownContentLengthHelper.class);

    private final Map<Integer, CompletedPart> existingParts = new ConcurrentHashMap<>();
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
        S3ResumeToken resumeToken = putObjectRequest.overrideConfiguration()
                                                    .map(c -> c.executionAttributes()
                                                               .getAttribute(RESUME_TOKEN)).orElse(null);

        if (resumeToken == null) {
            initiateNewUpload(putObjectRequest, contentLength, asyncRequestBody, returnFuture);
        } else {
            ResumeRequestContext resumeRequestContext = new ResumeRequestContext(resumeToken, putObjectRequest, contentLength,
                                                                                 asyncRequestBody, returnFuture);
            resumePausedUpload(resumeRequestContext);
        }
    }

    private void initiateNewUpload(PutObjectRequest putObjectRequest, long contentLength, AsyncRequestBody asyncRequestBody,
                                   CompletableFuture<PutObjectResponse> returnFuture) {
        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            multipartUploadHelper.createMultipartUpload(putObjectRequest, returnFuture);

        createMultipartUploadFuture.whenComplete((createMultipartUploadResponse, throwable) -> {
            if (throwable != null) {
                genericMultipartHelper.handleException(returnFuture, () -> "Failed to initiate multipart upload", throwable);
            } else {
                log.debug(() -> "Initiated a new multipart upload, uploadId: " + createMultipartUploadResponse.uploadId());
                uploadFromBeginning(Pair.of(putObjectRequest, asyncRequestBody), contentLength, returnFuture,
                                    createMultipartUploadResponse.uploadId());
            }
        });
    }

    private void uploadFromBeginning(Pair<PutObjectRequest, AsyncRequestBody> request, long contentLength,
                                     CompletableFuture<PutObjectResponse> returnFuture, String uploadId) {

        int numPartsCompleted = 0;
        long partSize = genericMultipartHelper.calculateOptimalPartSizeFor(contentLength, partSizeInBytes);
        int partCount = genericMultipartHelper.determinePartCount(contentLength, partSize);

        if (partSize > partSizeInBytes) {
            log.debug(() -> String.format("Configured partSize is %d, but using %d to prevent reaching maximum number of "
                                          + "parts allowed", partSizeInBytes, partSize));
        }

        log.debug(() -> String.format("Starting multipart upload with partCount: %d, optimalPartSize: %d", partCount,
                                      partSize));

        MpuRequestContext mpuRequestContext =
            new MpuRequestContext(request, contentLength, partSize, uploadId, existingParts, numPartsCompleted);

        splitAndSubscribe(mpuRequestContext, returnFuture);
    }

    private void resumePausedUpload(ResumeRequestContext resumeContext) {
        S3ResumeToken resumeToken = resumeContext.resumeToken;
        String uploadId = resumeToken.uploadId();
        PutObjectRequest putObjectRequest = resumeContext.putObjectRequest;
        CompletableFuture<Void> listPartsFuture = identifyExistingPartsForResume(uploadId, putObjectRequest);

        int remainingParts = (int) (resumeToken.totalNumParts() - resumeToken.numPartsCompleted());
        log.debug(() -> String.format("Resuming a paused multipart upload, uploadId: %s, completedPartCount: %d, "
                                  + "remainingPartCount: %d, partSize: %d",
                                      uploadId, resumeToken.numPartsCompleted(), remainingParts, resumeToken.partSize()));

        listPartsFuture.whenComplete((r, t) -> {
            if (t != null) {
                genericMultipartHelper.handleException(resumeContext.returnFuture,
                                                       () -> "Failed to resume because listParts failed", t);
                return;
            }

            MpuRequestContext mpuRequestContext =
                new MpuRequestContext(Pair.of(putObjectRequest, resumeContext.asyncRequestBody),
                                      resumeContext.contentLength,
                                      resumeToken.partSize(), uploadId, existingParts,
                                      resumeToken.numPartsCompleted());

            splitAndSubscribe(mpuRequestContext, resumeContext.returnFuture);
        });
    }

    private void splitAndSubscribe(MpuRequestContext mpuRequestContext, CompletableFuture<PutObjectResponse> returnFuture) {
        KnownContentLengthAsyncRequestBodySubscriber subscriber =
            new KnownContentLengthAsyncRequestBodySubscriber(mpuRequestContext, returnFuture);

        attachSubscriberToObservable(subscriber, mpuRequestContext.request.left());

        mpuRequestContext.request.right()
            .split(b -> b.chunkSizeInBytes(mpuRequestContext.partSize)
                         .bufferSizeInBytes(maxMemoryUsageInBytes))
            .subscribe(subscriber);
    }

    private CompletableFuture<Void> identifyExistingPartsForResume(String uploadId, PutObjectRequest putObjectRequest) {
        ListPartsRequest request = SdkPojoConversionUtils.toListPartsRequest(uploadId, putObjectRequest);
        ListPartsPublisher listPartsPublisher = s3AsyncClient.listPartsPaginator(request);
        SdkPublisher<Part> partsPublisher = listPartsPublisher.parts();
        return partsPublisher.subscribe(part ->
                                            existingParts.put(part.partNumber(), SdkPojoConversionUtils.toCompletedPart(part)));
    }

    private void attachSubscriberToObservable(KnownContentLengthAsyncRequestBodySubscriber subscriber,
                                              PutObjectRequest putObjectRequest) {
        // observable will be present if TransferManager is used
        putObjectRequest.overrideConfiguration().map(c -> c.executionAttributes().getAttribute(PAUSE_OBSERVABLE))
                        .ifPresent(p -> p.setPausableUpload(new DefaultPausableUpload(subscriber)));
    }

    private static final class ResumeRequestContext {
        private final S3ResumeToken resumeToken;
        private final PutObjectRequest putObjectRequest;
        private final long contentLength;
        private final AsyncRequestBody asyncRequestBody;
        private final CompletableFuture<PutObjectResponse> returnFuture;

        private ResumeRequestContext (S3ResumeToken resumeToken,
                                      PutObjectRequest putObjectRequest,
                                      long contentLength,
                                      AsyncRequestBody asyncRequestBody,
                                      CompletableFuture<PutObjectResponse> returnFuture) {
            this.resumeToken = resumeToken;
            this.putObjectRequest = putObjectRequest;
            this.contentLength = contentLength;
            this.asyncRequestBody = asyncRequestBody;
            this.returnFuture = returnFuture;
        }
    }

    private static final class MpuRequestContext {
        private final Pair<PutObjectRequest, AsyncRequestBody> request;
        private final long contentLength;
        private final long partSize;
        private final long numPartsCompleted;
        private final String uploadId;
        private final Map<Integer, CompletedPart> existingParts;

        private MpuRequestContext(Pair<PutObjectRequest, AsyncRequestBody> request,
                                  long contentLength,
                                  long partSize,
                                  String uploadId,
                                  Map<Integer, CompletedPart> existingParts,
                                  long numPartsCompleted) {
            this.request = request;
            this.contentLength = contentLength;
            this.partSize = partSize;
            this.uploadId = uploadId;
            this.existingParts = existingParts;
            this.numPartsCompleted = numPartsCompleted;
        }
    }

    private static final class DefaultPausableUpload implements PausableUpload {

        private KnownContentLengthAsyncRequestBodySubscriber subscriber;

        private DefaultPausableUpload(KnownContentLengthAsyncRequestBodySubscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public S3ResumeToken pause() {
            return subscriber.pause();
        }
    }

    @SdkInternalApi
    public class KnownContentLengthAsyncRequestBodySubscriber implements Subscriber<AsyncRequestBody> {

        /**
         * The number of AsyncRequestBody has been received but yet to be processed
         */
        private final AtomicInteger asyncRequestBodyInFlight = new AtomicInteger(0);
        private final AtomicBoolean failureActionInitiated = new AtomicBoolean(false);
        private final AtomicInteger partNumber = new AtomicInteger(1);
        private final AtomicReferenceArray<CompletedPart> completedParts;
        private final int partCount;
        private final int numExistingParts;
        private final String uploadId;
        private final Collection<CompletableFuture<CompletedPart>> futures = new ConcurrentLinkedQueue<>();
        private final PutObjectRequest putObjectRequest;
        private final CompletableFuture<PutObjectResponse> returnFuture;
        private final Map<Integer, CompletedPart> existingParts;
        private Subscription subscription;
        private volatile boolean isDone;
        private volatile boolean isPaused;
        private volatile CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture;

        KnownContentLengthAsyncRequestBodySubscriber(MpuRequestContext mpuRequestContext,
                                                     CompletableFuture<PutObjectResponse> returnFuture) {
            this.partCount =
                genericMultipartHelper.determinePartCount(mpuRequestContext.contentLength, mpuRequestContext.partSize);
            this.putObjectRequest = mpuRequestContext.request.left();
            this.returnFuture = returnFuture;
            this.uploadId = mpuRequestContext.uploadId;
            this.existingParts = mpuRequestContext.existingParts;
            this.numExistingParts = (int) mpuRequestContext.numPartsCompleted;
            this.completedParts = new AtomicReferenceArray<>(partCount - numExistingParts);
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
                                .partSize(partSizeInBytes)
                                .totalNumParts(partCount)
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
                asyncRequestBody.subscribe(new CancelledSubscriber<>());
                subscription.request(1);
                return;
            }

            asyncRequestBodyInFlight.incrementAndGet();
            UploadPartRequest uploadRequest = SdkPojoConversionUtils.toUploadPartRequest(putObjectRequest,
                                                                                         partNumber.getAndIncrement(),
                                                                                         uploadId);

            Consumer<CompletedPart> completedPartConsumer =
                completedPart -> completedParts.set(completedPart.partNumber() - 1 - numExistingParts, completedPart);
            multipartUploadHelper.sendIndividualUploadPartRequest(uploadId, completedPartConsumer, futures,
                                                                  Pair.of(uploadRequest, asyncRequestBody))
                                 .whenComplete((r, t) -> {
                                     if (t != null) {
                                         if (shouldFailRequest()) {
                                             multipartUploadHelper.failRequestsElegantly(futures, t, uploadId, returnFuture,
                                                                                         putObjectRequest);
                                         }
                                     } else {
                                         completeMultipartUploadIfFinish(asyncRequestBodyInFlight.decrementAndGet());
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
                completeMultipartUploadIfFinish(asyncRequestBodyInFlight.get());
            }
        }

        private void completeMultipartUploadIfFinish(int requestsInFlight) {
            if (isDone && requestsInFlight == 0) {
                CompletedPart[] parts;
                if (existingParts.isEmpty()) {
                    parts = IntStream.range(0, completedParts.length())
                                     .mapToObj(completedParts::get)
                                     .toArray(CompletedPart[]::new);
                } else if (completedParts.length() != 0) {
                    // List of CompletedParts needs to be in ascending order
                    parts = mergeCompletedParts();
                } else {
                    parts = existingParts.values().toArray(new CompletedPart[0]);
                }
                completeMpuFuture = multipartUploadHelper.completeMultipartUpload(returnFuture, uploadId, parts,
                                                                                  putObjectRequest);
            }
        }

        private CompletedPart[] mergeCompletedParts() {
            CompletedPart[] merged = new CompletedPart[partCount];
            int currPart = 1;
            while (currPart < partCount + 1) {
                if (existingParts.containsKey(currPart)) {
                    merged[currPart - 1] = existingParts.get(currPart);
                } else {
                    merged[currPart - 1] = completedParts.get(currPart - 1 - numExistingParts);
                }
                currPart++;
            }
            return merged;
        }
    }

    @SdkTestInternalApi
    public static DefaultPausableUpload defaultPausableUpload(KnownContentLengthAsyncRequestBodySubscriber subscriber) {
        return new DefaultPausableUpload(subscriber);
    }
}
