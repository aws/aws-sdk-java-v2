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


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.UploadPartCopyRequestIterable;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * An internal helper class that automatically uses multipart copy based on the size of the source object
 */
@SdkInternalApi
public final class CopyObjectHelper {
    private static final Logger log = Logger.loggerFor(S3AsyncClient.class);

    private final S3AsyncClient s3AsyncClient;
    private final long partSizeInBytes;
    private final GenericMultipartHelper<CopyObjectRequest, CopyObjectResponse> genericMultipartHelper;
    private final long uploadThreshold;

    public CopyObjectHelper(S3AsyncClient s3AsyncClient, long partSizeInBytes, long uploadThreshold) {
        this.s3AsyncClient = s3AsyncClient;
        this.partSizeInBytes = partSizeInBytes;
        this.genericMultipartHelper = new GenericMultipartHelper<>(s3AsyncClient,
                                                                   SdkPojoConversionUtils::toAbortMultipartUploadRequest,
                                                                   SdkPojoConversionUtils::toCopyObjectResponse);
        this.uploadThreshold = uploadThreshold;
    }

    public CompletableFuture<CopyObjectResponse> copyObject(CopyObjectRequest copyObjectRequest) {

        CompletableFuture<CopyObjectResponse> returnFuture = new CompletableFuture<>();

        try {
            CompletableFuture<HeadObjectResponse> headFuture =
                s3AsyncClient.headObject(SdkPojoConversionUtils.toHeadObjectRequest(copyObjectRequest));

            // Ensure cancellations are forwarded to the head future
            CompletableFutureUtils.forwardExceptionTo(returnFuture, headFuture);

            headFuture.whenComplete((headObjectResponse, throwable) -> {
                if (throwable != null) {
                    genericMultipartHelper.handleException(returnFuture, () -> "Failed to retrieve metadata from the source "
                                                                               + "object", throwable);
                } else {
                    doCopyObject(copyObjectRequest, returnFuture, headObjectResponse);
                }
            });
        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return returnFuture;
    }

    private void doCopyObject(CopyObjectRequest copyObjectRequest, CompletableFuture<CopyObjectResponse> returnFuture,
                              HeadObjectResponse headObjectResponse) {
        Long contentLength = headObjectResponse.contentLength();

        if (contentLength <= partSizeInBytes || contentLength <= uploadThreshold) {
            log.debug(() -> "Starting the copy as a single copy part request");
            copyInOneChunk(copyObjectRequest, returnFuture);
        } else {
            log.debug(() -> "Starting the copy as multipart copy request");
            copyInParts(copyObjectRequest, contentLength, returnFuture);
        }
    }

    private void copyInParts(CopyObjectRequest copyObjectRequest,
                             Long contentLength,
                             CompletableFuture<CopyObjectResponse> returnFuture) {

        CreateMultipartUploadRequest request = SdkPojoConversionUtils.toCreateMultipartUploadRequest(copyObjectRequest);
        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            s3AsyncClient.createMultipartUpload(request);

        // Ensure cancellations are forwarded to the createMultipartUploadFuture future
        CompletableFutureUtils.forwardExceptionTo(returnFuture, createMultipartUploadFuture);

        createMultipartUploadFuture.whenComplete((createMultipartUploadResponse, throwable) -> {
            if (throwable != null) {
                genericMultipartHelper.handleException(returnFuture, () -> "Failed to initiate multipart upload", throwable);
            } else {
                log.debug(() -> "Initiated new multipart upload, uploadId: " + createMultipartUploadResponse.uploadId());
                doCopyInParts(copyObjectRequest, contentLength, returnFuture, createMultipartUploadResponse.uploadId());
            }
        });
    }

    private void doCopyInParts(CopyObjectRequest copyObjectRequest,
                               Long contentLength,
                               CompletableFuture<CopyObjectResponse> returnFuture,
                               String uploadId) {

        long optimalPartSize = genericMultipartHelper.calculateOptimalPartSizeFor(contentLength, partSizeInBytes);

        int partCount = genericMultipartHelper.determinePartCount(contentLength, optimalPartSize);
        if (optimalPartSize > partSizeInBytes) {
            log.debug(() -> String.format("Configured partSize is %d, but using %d to prevent reaching maximum number of parts "
                                         + "allowed", partSizeInBytes, optimalPartSize));
        }

        log.debug(() -> String.format("Starting multipart copy with partCount: %s, optimalPartSize: %s",
                                      partCount, optimalPartSize));

        // The list of completed parts must be sorted
        AtomicReferenceArray<CompletedPart> completedParts = new AtomicReferenceArray<>(partCount);

        List<CompletableFuture<CompletedPart>> futures = sendUploadPartCopyRequests(copyObjectRequest,
                                                                                    contentLength,
                                                                                    uploadId,
                                                                                    completedParts,
                                                                                    optimalPartSize);
        CompletableFutureUtils.allOfExceptionForwarded(futures.toArray(new CompletableFuture[0]))
                              .thenCompose(ignore -> completeMultipartUpload(copyObjectRequest, uploadId, completedParts))
                              .handle(genericMultipartHelper.handleExceptionOrResponse(copyObjectRequest, returnFuture,
                                                                                       uploadId))
                              .exceptionally(throwable -> {
                                  genericMultipartHelper.handleException(returnFuture, () -> "Unexpected exception occurred",
                                                                         throwable);
                                  return null;
                              });
    }

    private CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(
        CopyObjectRequest copyObjectRequest, String uploadId, AtomicReferenceArray<CompletedPart> completedParts) {
        log.debug(() -> String.format("Sending completeMultipartUploadRequest, uploadId: %s",
                                      uploadId));
        CompletedPart[] parts =
            IntStream.range(0, completedParts.length())
                     .mapToObj(completedParts::get)
                     .toArray(CompletedPart[]::new);
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
            CompleteMultipartUploadRequest.builder()
                                          .bucket(copyObjectRequest.destinationBucket())
                                          .key(copyObjectRequest.destinationKey())
                                          .uploadId(uploadId)
                                          .multipartUpload(CompletedMultipartUpload.builder()
                                                                                   .parts(parts)
                                                                                   .build())
                                          .sseCustomerAlgorithm(copyObjectRequest.sseCustomerAlgorithm())
                                          .sseCustomerKey(copyObjectRequest.sseCustomerKey())
                                          .sseCustomerKeyMD5(copyObjectRequest.sseCustomerKeyMD5())
                                          .build();
        return s3AsyncClient.completeMultipartUpload(completeMultipartUploadRequest);
    }

    private List<CompletableFuture<CompletedPart>> sendUploadPartCopyRequests(CopyObjectRequest copyObjectRequest,
                                                                              long contentLength,
                                                                              String uploadId,
                                                                              AtomicReferenceArray<CompletedPart> completedParts,
                                                                              long optimalPartSize) {
        List<CompletableFuture<CompletedPart>> futures = new ArrayList<>();

        UploadPartCopyRequestIterable uploadPartCopyRequests = new UploadPartCopyRequestIterable(uploadId,
                                                                                                 optimalPartSize,
                                                                                                 copyObjectRequest,
                                                                                                 contentLength);

        uploadPartCopyRequests.forEach(uploadPartCopyRequest ->
                                           sendIndividualUploadPartCopy(uploadId, completedParts, futures,
                                                                        uploadPartCopyRequest));

        return futures;
    }

    private void sendIndividualUploadPartCopy(String uploadId,
                                              AtomicReferenceArray<CompletedPart> completedParts,
                                              List<CompletableFuture<CompletedPart>> futures,
                                              UploadPartCopyRequest uploadPartCopyRequest) {
        Integer partNumber = uploadPartCopyRequest.partNumber();
        log.debug(() -> "Sending uploadPartCopyRequest with range: " + uploadPartCopyRequest.copySourceRange() + " uploadId: "
                        + uploadId);

        CompletableFuture<UploadPartCopyResponse> uploadPartCopyFuture =
            s3AsyncClient.uploadPartCopy(uploadPartCopyRequest);

        CompletableFuture<CompletedPart> convertFuture =
            uploadPartCopyFuture.thenApply(uploadPartCopyResponse ->
                                               convertUploadPartCopyResponse(completedParts, partNumber, uploadPartCopyResponse));
        futures.add(convertFuture);

        CompletableFutureUtils.forwardExceptionTo(convertFuture, uploadPartCopyFuture);
    }

    private static CompletedPart convertUploadPartCopyResponse(AtomicReferenceArray<CompletedPart> completedParts,
                                                               Integer partNumber,
                                                               UploadPartCopyResponse uploadPartCopyResponse) {
        CopyPartResult copyPartResult = uploadPartCopyResponse.copyPartResult();
        CompletedPart completedPart =
            SdkPojoConversionUtils.toCompletedPart(copyPartResult,
                                                   partNumber);

        completedParts.set(partNumber - 1, completedPart);
        return completedPart;
    }

    private void copyInOneChunk(CopyObjectRequest copyObjectRequest,
                                CompletableFuture<CopyObjectResponse> returnFuture) {
        CompletableFuture<CopyObjectResponse> copyObjectFuture =
            s3AsyncClient.copyObject(copyObjectRequest);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, copyObjectFuture);
        CompletableFutureUtils.forwardResultTo(copyObjectFuture, returnFuture);
    }
}
