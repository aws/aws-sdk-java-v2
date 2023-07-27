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


import static software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient.USER_AGENT_API_NAME;
import static software.amazon.awssdk.services.s3.internal.multipart.SdkPojoConversionUtils.toAbortMultipartUploadRequest;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SplitAsyncRequestBodyResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.UserAgentUtils;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * An internal helper class that automatically uses multipart upload based on the size of the object.
 */
@SdkInternalApi
public final class MultipartUploadHelper {
    private static final Logger log = Logger.loggerFor(MultipartUploadHelper.class);

    private final S3AsyncClient s3AsyncClient;
    private final long partSizeInBytes;
    private final GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper;

    private final long maxMemoryUsageInBytes;
    private final long multipartUploadThresholdInBytes;

    public MultipartUploadHelper(S3AsyncClient s3AsyncClient,
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
    }

    public CompletableFuture<PutObjectResponse> uploadObject(PutObjectRequest putObjectRequest,
                                                             AsyncRequestBody asyncRequestBody) {
        Long contentLength = asyncRequestBody.contentLength().orElseGet(putObjectRequest::contentLength);

        // TODO: support null content length. Need to determine whether to use single object or MPU based on the first
        //  AsyncRequestBody
        if (contentLength == null) {
            throw new IllegalArgumentException("Content-length is required");
        }

        CompletableFuture<PutObjectResponse> returnFuture = new CompletableFuture<>();

        try {
            if (contentLength > multipartUploadThresholdInBytes && contentLength > partSizeInBytes) {
                log.debug(() -> "Starting the upload as multipart upload request");
                uploadInParts(putObjectRequest, contentLength, asyncRequestBody, returnFuture);
            } else {
                log.debug(() -> "Starting the upload as a single upload part request");
                uploadInOneChunk(putObjectRequest, asyncRequestBody, returnFuture);
            }

        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return returnFuture;
    }

    private void uploadInParts(PutObjectRequest putObjectRequest, long contentLength, AsyncRequestBody asyncRequestBody,
                               CompletableFuture<PutObjectResponse> returnFuture) {

        CreateMultipartUploadRequest request = SdkPojoConversionUtils.toCreateMultipartUploadRequest(putObjectRequest);
        request = UserAgentUtils.applyUserAgentInfo(request, b -> b.addApiName(USER_AGENT_API_NAME));
        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            s3AsyncClient.createMultipartUpload(request);

        // Ensure cancellations are forwarded to the createMultipartUploadFuture future
        CompletableFutureUtils.forwardExceptionTo(returnFuture, createMultipartUploadFuture);

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
        if (optimalPartSize > partSizeInBytes) {
            log.info(() -> String.format("Configured partSize is %d, but using %d to prevent reaching maximum number of parts "
                                         + "allowed", partSizeInBytes, optimalPartSize));
        }
        int partCount = genericMultipartHelper.determinePartCount(contentLength, optimalPartSize);

        log.debug(() -> String.format("Starting multipart upload with partCount: %d, optimalPartSize: %d", partCount,
                                      optimalPartSize));

        // The list of completed parts must be sorted
        AtomicReferenceArray<CompletedPart> completedParts = new AtomicReferenceArray<>(partCount);

        PutObjectRequest putObjectRequest = request.left();

        Collection<CompletableFuture<CompletedPart>> futures = new ConcurrentLinkedQueue<>();

        MpuRequestContext mpuRequestContext = new MpuRequestContext(request, contentLength, optimalPartSize, uploadId);

        CompletableFuture<Void> requestsFuture = sendUploadPartRequests(mpuRequestContext,
                                                                        completedParts,
                                                                        returnFuture,
                                                                        futures);
        requestsFuture.whenComplete((r, t) -> {
            if (t != null) {
                genericMultipartHelper.handleException(returnFuture, () -> "Failed to send multipart upload requests", t);
                genericMultipartHelper.cleanUpParts(uploadId, toAbortMultipartUploadRequest(putObjectRequest));
                cancelingOtherOngoingRequests(futures, t);
                return;
            }
            CompletableFutureUtils.allOfExceptionForwarded(futures.toArray(new CompletableFuture[0]))
                                  .thenCompose(ignore -> genericMultipartHelper.completeMultipartUpload(putObjectRequest,
                                                                                                        uploadId,
                                                                                                        completedParts))
                                  .handle(genericMultipartHelper.handleExceptionOrResponse(putObjectRequest, returnFuture,
                                                                                           uploadId))
                                  .exceptionally(throwable -> {
                                      genericMultipartHelper.handleException(returnFuture, () -> "Unexpected exception occurred",
                                                                             throwable);
                                      return null;
                                  });
        });
    }

    private static void cancelingOtherOngoingRequests(Collection<CompletableFuture<CompletedPart>> futures, Throwable t) {
        log.trace(() -> "cancelling other ongoing requests " + futures.size());
        futures.forEach(f -> f.completeExceptionally(t));
    }

    private CompletableFuture<Void> sendUploadPartRequests(MpuRequestContext mpuRequestContext,
                                                           AtomicReferenceArray<CompletedPart> completedParts,
                                                           CompletableFuture<PutObjectResponse> returnFuture,
                                                           Collection<CompletableFuture<CompletedPart>> futures) {

        AsyncRequestBody asyncRequestBody = mpuRequestContext.request.right();

        SplitAsyncRequestBodyResponse result = asyncRequestBody.split(mpuRequestContext.partSize, maxMemoryUsageInBytes);

        CompletableFuture<Void> splittingPublisherFuture = result.future();

        result.asyncRequestBodyPublisher()
              .map(new BodyToRequestConverter(mpuRequestContext.request.left(),
                                              mpuRequestContext.uploadId))
              .subscribe(pair -> sendIndividualUploadPartRequest(mpuRequestContext.uploadId,
                                                                 completedParts,
                                                                 futures,
                                                                 pair,
                                                                 splittingPublisherFuture))
              .exceptionally(throwable -> {
                  returnFuture.completeExceptionally(throwable);
                  return null;
              });
        return splittingPublisherFuture;
    }

    private void sendIndividualUploadPartRequest(String uploadId,
                                                 AtomicReferenceArray<CompletedPart> completedParts,
                                                 Collection<CompletableFuture<CompletedPart>> futures,
                                                 Pair<UploadPartRequest, AsyncRequestBody> requestPair,
                                                 CompletableFuture<Void> sendUploadPartRequestsFuture) {
        UploadPartRequest uploadPartRequest = UserAgentUtils.applyUserAgentInfo(requestPair.left(),
                                                                                b -> b.addApiName(USER_AGENT_API_NAME));
        Integer partNumber = uploadPartRequest.partNumber();
        log.debug(() -> "Sending uploadPartRequest: " + uploadPartRequest.partNumber() + " uploadId: " + uploadId + " "
                        + "contentLength " + requestPair.right().contentLength());

        CompletableFuture<UploadPartResponse> uploadPartFuture = s3AsyncClient.uploadPart(uploadPartRequest, requestPair.right());

        CompletableFuture<CompletedPart> convertFuture =
            uploadPartFuture.thenApply(uploadPartResponse -> convertUploadPartResponse(completedParts, partNumber,
                                                                                       uploadPartResponse));
        futures.add(convertFuture);
        CompletableFutureUtils.forwardExceptionTo(convertFuture, uploadPartFuture);
        CompletableFutureUtils.forwardExceptionTo(uploadPartFuture, sendUploadPartRequestsFuture);
    }

    private static CompletedPart convertUploadPartResponse(AtomicReferenceArray<CompletedPart> completedParts,
                                                           Integer partNumber,
                                                           UploadPartResponse uploadPartResponse) {
        CompletedPart completedPart = SdkPojoConversionUtils.toCompletedPart(uploadPartResponse, partNumber);

        completedParts.set(partNumber - 1, completedPart);
        return completedPart;
    }

    private void uploadInOneChunk(PutObjectRequest putObjectRequest,
                                  AsyncRequestBody asyncRequestBody,
                                  CompletableFuture<PutObjectResponse> returnFuture) {
        putObjectRequest = UserAgentUtils.applyUserAgentInfo(putObjectRequest, b -> b.addApiName(USER_AGENT_API_NAME));
        CompletableFuture<PutObjectResponse> putObjectResponseCompletableFuture = s3AsyncClient.putObject(putObjectRequest,
                                                                                                          asyncRequestBody);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, putObjectResponseCompletableFuture);
        CompletableFutureUtils.forwardResultTo(putObjectResponseCompletableFuture, returnFuture);
    }

    private static final class BodyToRequestConverter implements Function<AsyncRequestBody, Pair<UploadPartRequest,
        AsyncRequestBody>> {
        private int partNumber = 1;
        private final PutObjectRequest putObjectRequest;
        private final String uploadId;

        BodyToRequestConverter(PutObjectRequest putObjectRequest, String uploadId) {
            this.putObjectRequest = putObjectRequest;
            this.uploadId = uploadId;
        }

        @Override
        public Pair<UploadPartRequest, AsyncRequestBody> apply(AsyncRequestBody asyncRequestBody) {
            log.trace(() -> "Generating uploadPartRequest for partNumber " + partNumber);
            UploadPartRequest uploadRequest =
                SdkPojoConversionUtils.toUploadPartRequest(putObjectRequest,
                                                           partNumber,
                                                           uploadId);
            ++partNumber;
            return Pair.of(uploadRequest, asyncRequestBody);
        }
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

}
