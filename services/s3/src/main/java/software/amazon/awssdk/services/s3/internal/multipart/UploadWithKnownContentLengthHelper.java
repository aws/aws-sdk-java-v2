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

import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.PAUSE_OBSERVABLE;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.RESUME_TOKEN;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.services.s3.paginators.ListPartsPublisher;
import software.amazon.awssdk.utils.CompletableFutureUtils;
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

        long numPartsCompleted = 0;
        long partSize = genericMultipartHelper.calculateOptimalPartSizeFor(contentLength, partSizeInBytes);
        int partCount = genericMultipartHelper.determinePartCount(contentLength, partSize);

        if (partSize > partSizeInBytes) {
            log.debug(() -> String.format("Configured partSize is %d, but using %d to prevent reaching maximum number of "
                                          + "parts allowed", partSizeInBytes, partSize));
        }

        log.debug(() -> String.format("Starting multipart upload with partCount: %d, optimalPartSize: %d", partCount,
                                      partSize));

        MpuRequestContext mpuRequestContext = MpuRequestContext.builder()
                                                               .request(request)
                                                               .contentLength(contentLength)
                                                               .partSize(partSize)
                                                               .uploadId(uploadId)
                                                               .numPartsCompleted(numPartsCompleted)
                                                               .build();

        splitAndSubscribe(mpuRequestContext, returnFuture);
    }

    private void resumePausedUpload(ResumeRequestContext resumeContext) {
        S3ResumeToken resumeToken = resumeContext.resumeToken;
        String uploadId = resumeToken.uploadId();
        PutObjectRequest putObjectRequest = resumeContext.putObjectRequest;
        Map<Integer, CompletedPart> existingParts = new ConcurrentHashMap<>();
        CompletableFuture<Void> listPartsFuture = identifyExistingPartsForResume(uploadId, putObjectRequest, existingParts);

        int remainingParts = (int) (resumeToken.totalNumParts() - resumeToken.numPartsCompleted());
        log.debug(() -> String.format("Resuming a paused multipart upload, uploadId: %s, completedPartCount: %d, "
                                  + "remainingPartCount: %d, partSize: %d",
                                      uploadId, resumeToken.numPartsCompleted(), remainingParts, resumeToken.partSize()));

        CompletableFutureUtils.forwardExceptionTo(resumeContext.returnFuture, listPartsFuture);

        listPartsFuture.whenComplete((r, t) -> {
            if (t != null) {
                genericMultipartHelper.handleException(resumeContext.returnFuture,
                                                       () -> "Failed to resume because listParts failed", t);
                return;
            }

            Pair<PutObjectRequest, AsyncRequestBody> request = Pair.of(putObjectRequest, resumeContext.asyncRequestBody);
            MpuRequestContext mpuRequestContext = MpuRequestContext.builder()
                                                                   .request(request)
                                                                   .contentLength(resumeContext.contentLength)
                                                                   .partSize(resumeToken.partSize())
                                                                   .uploadId(uploadId)
                                                                   .existingParts(existingParts)
                                                                   .numPartsCompleted(resumeToken.numPartsCompleted())
                                                                   .build();

            splitAndSubscribe(mpuRequestContext, resumeContext.returnFuture);
        });
    }

    private void splitAndSubscribe(MpuRequestContext mpuRequestContext, CompletableFuture<PutObjectResponse> returnFuture) {
        KnownContentLengthAsyncRequestBodySubscriber subscriber =
            new KnownContentLengthAsyncRequestBodySubscriber(mpuRequestContext, returnFuture, multipartUploadHelper);

        attachSubscriberToObservable(subscriber, mpuRequestContext.request().left());

        System.out.println("Split and subscribe started");
        mpuRequestContext.request().right()
            .split(b -> b.chunkSizeInBytes(mpuRequestContext.partSize())
                         .bufferSizeInBytes(maxMemoryUsageInBytes))
            .subscribe(subscriber);
    }

    private CompletableFuture<Void> identifyExistingPartsForResume(String uploadId, PutObjectRequest putObjectRequest,
                                                                   Map<Integer, CompletedPart> existingParts) {
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

        private ResumeRequestContext(S3ResumeToken resumeToken,
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
}
