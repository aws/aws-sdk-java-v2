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

package software.amazon.awssdk.custom.s3.transfer.internal;

import static software.amazon.awssdk.custom.s3.transfer.internal.RequestConversionUtils.toAbortPartRequest;
import static software.amazon.awssdk.custom.s3.transfer.internal.RequestConversionUtils.toCompleteMultipartUploadRequest;
import static software.amazon.awssdk.custom.s3.transfer.internal.RequestConversionUtils.toCreateMultipartUploadRequest;
import static software.amazon.awssdk.custom.s3.transfer.internal.RequestConversionUtils.toUploadPartRequest;
import static software.amazon.awssdk.utils.CompletableFutureUtils.allOfCancelForwarded;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReferenceArray;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.custom.s3.transfer.MultipartUploadConfiguration;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.Logger;

/**
 * UploadManager to handle multipart upload.
 */
@SdkInternalApi
@ThreadSafe
final class MultiPartUploadManager {
    private static final Logger log = Logger.loggerFor(MultiPartUploadManager.class);
    private final S3AsyncClient s3Client;

    MultiPartUploadManager(S3AsyncClient s3Client) {
        this.s3Client = s3Client;
    }

    CompletableFuture<Void> apiRequestUpload(Long objectSize,
                                             PutObjectRequest request,
                                             TransferRequestBody requestBody,
                                             MultipartUploadConfiguration resolvedConfiguration) {

        CreateMultipartUploadRequest createMultipartUploadRequest = toCreateMultipartUploadRequest(request);

        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            s3Client.createMultipartUpload(createMultipartUploadRequest);

        return createMultipartUploadFuture.thenCompose(response -> uploadParts(objectSize, request, requestBody,
                                                                               resolvedConfiguration, response));
    }

    private CompletionStage<Void> uploadParts(Long objectSize,
                                              PutObjectRequest request,
                                              TransferRequestBody requestBody,
                                              MultipartUploadConfiguration resolvedConfiguration,
                                              CreateMultipartUploadResponse createMultipartUploadResponse) {

        String uploadId = createMultipartUploadResponse.uploadId();
        log.debug(() -> "Finished createMultipartUpload, starting to upload object. UploadId: " + uploadId);
        List<MultipartUploadContext> contexts = multipartContexts(objectSize, request, resolvedConfiguration, uploadId);
        AtomicReferenceArray<CompletedPart> completedParts = new AtomicReferenceArray<>(contexts.size());

        CompletableFuture<?>[] completableFutures = contexts.stream()
                                                            .map(ctx -> uploadSinglePart(requestBody, completedParts, ctx))
                                                            .toArray(CompletableFuture[]::new);

        CompletionStage<Void> resultFuture = completeMultipartUpload(allOfCancelForwarded(completableFutures), request,
                                                                     uploadId, completedParts);

        resultFuture.whenComplete((r, t) -> {
            if (t != null) {
                cleanUpIfNeeded(request, uploadId);
            }
        });
        return resultFuture;
    }

    private CompletionStage<Void> uploadSinglePart(TransferRequestBody requestBody,
                                                   AtomicReferenceArray<CompletedPart> completedParts,
                                                   MultipartUploadContext ctx) {
        AsyncRequestBody asyncRequestBody = requestBody.requestBodyForPart(ctx);
        return s3Client.uploadPart(ctx.uploadPartRequest(), asyncRequestBody)
                       .thenAccept(uploadPartResponse -> {
                           String etag = uploadPartResponse.eTag();
                           // The list of parts has to be sorted
                           completedParts.set(ctx.partNumber() - 1, CompletedPart.builder()
                                                                                 .eTag(etag)
                                                                                 .partNumber(ctx.partNumber())
                                                                                 .build());
                       });
    }

    /**
     * Cleaning up resources if multipart upload fails by invoking abortMultiPart request
     */
    private void cleanUpIfNeeded(PutObjectRequest request, String uploadId) {
        CompletableFuture<AbortMultipartUploadResponse> abortMultipartResponseFuture = abortMultipart(request, uploadId);
        abortMultipartResponseFuture.whenComplete((r, t) -> {
            if (t != null) {
                log.warn(() -> String.format("Failed to abort multipartUpload. You should invoke abortMultipart to avoid "
                               + "being charged for storage of the uploaded parts. (uploadId: %s)", uploadId), t);
            } else {
                log.debug(() -> "Upload " + uploadId + " has been aborted");
            }
        });
    }

    private CompletableFuture<AbortMultipartUploadResponse> abortMultipart(PutObjectRequest request, String uploadId) {
        AbortMultipartUploadRequest abortMultipartUploadRequest = toAbortPartRequest(request, uploadId);
        return s3Client.abortMultipartUpload(abortMultipartUploadRequest);
    }

    private CompletionStage<Void> completeMultipartUpload(CompletionStage<Void> completedMultiPartsFuture,
                                                          PutObjectRequest request,
                                                          String uploadId,
                                                          AtomicReferenceArray<CompletedPart> completedParts) {
        return completedMultiPartsFuture.thenCompose(ignore -> {
            CompletedPart[] parts = new CompletedPart[completedParts.length()];
            for (int i = 0; i < completedParts.length(); i++) {
                parts[i] = completedParts.get(i);
            }
            CompleteMultipartUploadRequest completeMultipartUploadRequest = toCompleteMultipartUploadRequest(request,
                                                                                                             uploadId,
                                                                                                             parts);
            log.debug(() -> "Finished uploading all parts, starting to completeMultipartUpload ");

            return s3Client.completeMultipartUpload(completeMultipartUploadRequest)
                           .thenApply(i -> null);
        });
    }

    private List<MultipartUploadContext> multipartContexts(long objectSize,
                                                           PutObjectRequest putObjectRequest,
                                                           MultipartUploadConfiguration resolvedConfiguration,
                                                           String uploadId) {
        long numberOfParts = determinePartCount(objectSize, resolvedConfiguration);

        long partSize = (long) Math.ceil((double) objectSize / numberOfParts);

        log.debug(() -> String.format("Uploading %s in %d parts of size %d", putObjectRequest, numberOfParts, partSize));

        List<MultipartUploadContext> contexts = new ArrayList<>((int) numberOfParts);

        long startByte = 0L;
        long remaining = objectSize;
        for (int p = 0; p < numberOfParts; ++p) {
            int partNum = p + 1;
            boolean lastPart = partNum == numberOfParts;
            long size = remaining > partSize ? partSize : remaining;

            long offset = startByte;

            log.debug(() -> "PartNumber " + partNum + " partOffset " + offset + " size " + size);
            UploadPartRequest uploadPartRequest = toUploadPartRequest(putObjectRequest, size, uploadId, partNum);
            MultipartUploadContext context = MultipartUploadContext.builder()
                                                                   .partNumber(partNum)
                                                                   .uploadPartRequest(uploadPartRequest)
                                                                   .partOffset(offset)
                                                                   .isLastPart(lastPart)
                                                                   .build();
            contexts.add(context);

            startByte = offset + size;
            remaining -= size;
        }

        return contexts;
    }

    private long determinePartCount(long size, MultipartUploadConfiguration resolvedConfiguration) {
        int maxPartCount = resolvedConfiguration.maxUploadPartCount();

        // First calculate maximum part count based on min part size. This
        // should always be positive since we enforce that min part size is <=
        // threshold
        long partCount = size / resolvedConfiguration.minimumUploadPartSize();

        // Clamp to configured max part count
        return Math.min(partCount, maxPartCount);
    }
}
