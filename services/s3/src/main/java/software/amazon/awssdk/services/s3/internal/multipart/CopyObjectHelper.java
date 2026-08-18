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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.UploadPartCopyRequestIterable;
import software.amazon.awssdk.services.s3.model.AnnotationDirective;
import software.amazon.awssdk.services.s3.model.AnnotationEntry;
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
import software.amazon.awssdk.services.s3.model.ListObjectAnnotationsResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.TaggingDirective;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.utils.CollectionUtils;
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
            String sourceVersionId = headObjectResponse.versionId();
            String sourceETag = headObjectResponse.eTag();
            copyInParts(copyObjectRequest, contentLength, returnFuture, headObjectResponse, sourceVersionId, sourceETag);
        }
    }

    /**
     * Performs a multipart copy with the following property-copy behavior:
     * <ul>
     *   <li>Metadata: always copied from source unless {@code MetadataDirective.REPLACE} is set</li>
     *   <li>Tags: copied when {@code TaggingDirective.COPY} is set (via {@code PutObjectTagging} post-completion)</li>
     *   <li>Annotations: copied when {@code AnnotationDirective.COPY} is set (via {@code PutObjectAnnotation} post-completion)
     *   </li>
     * </ul>
     */
    private void copyInParts(CopyObjectRequest copyObjectRequest,
                             Long contentLength,
                             CompletableFuture<CopyObjectResponse> returnFuture,
                             HeadObjectResponse headObjectResponse,
                             String sourceVersionId,
                             String sourceETag) {

        CompletableFuture<List<Tag>> tagsFuture;
        if (copyObjectRequest.taggingDirective() == TaggingDirective.COPY) {
            tagsFuture = fetchSourceTagging(copyObjectRequest, sourceVersionId);
        } else {
            tagsFuture = CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Map<String, byte[]>> annotationsFuture;
        if (copyObjectRequest.annotationDirective() == AnnotationDirective.COPY) {
            annotationsFuture = fetchSourceAnnotations(copyObjectRequest, sourceVersionId);
        } else {
            annotationsFuture = CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> sourcePropertiesFuture =
            CompletableFutureUtils.allOfExceptionForwarded(
                new CompletableFuture<?>[] {tagsFuture, annotationsFuture});
        CompletableFutureUtils.forwardExceptionTo(returnFuture, sourcePropertiesFuture);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, tagsFuture);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, annotationsFuture);

        sourcePropertiesFuture.whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                genericMultipartHelper.handleException(returnFuture,
                                                       () -> "Failed to read source object properties", throwable);
            } else {
                MultipartCopyContext ctx = MultipartCopyContext.builder()
                                                               .copyObjectRequest(copyObjectRequest)
                                                               .contentLength(contentLength)
                                                               .returnFuture(returnFuture)
                                                               .sourceVersionId(sourceVersionId)
                                                               .sourceETag(sourceETag)
                                                               .sourceTags(tagsFuture.join())
                                                               .annotations(annotationsFuture.join())
                                                               .build();
                doCreateMultipartUploadAndCopy(ctx, headObjectResponse);
            }
        });
    }

    private CompletableFuture<List<Tag>> fetchSourceTagging(CopyObjectRequest copyObjectRequest, String sourceVersionId) {
        return s3AsyncClient.getObjectTagging(r -> r.bucket(copyObjectRequest.sourceBucket())
                                                    .key(copyObjectRequest.sourceKey())
                                                    .versionId(sourceVersionId))
                            .thenApply(response -> CollectionUtils.isNullOrEmpty(response.tagSet())
                                                   ? null : response.tagSet());
    }

    /**
     * Fetches all annotations from the source object. Starts downloading annotation bodies
     * as each page of ListObjectAnnotations arrives, rather than waiting for full listing.
     */
    private CompletableFuture<Map<String, byte[]>> fetchSourceAnnotations(CopyObjectRequest copyObjectRequest,
                                                                          String sourceVersionId) {
        Map<String, byte[]> annotationBodies = new ConcurrentHashMap<>();
        Queue<CompletableFuture<Void>> fetchFutures = new ConcurrentLinkedQueue<>();
        CompletableFuture<Map<String, byte[]>> result = new CompletableFuture<>();

        s3AsyncClient.listObjectAnnotationsPaginator(r -> r.bucket(copyObjectRequest.sourceBucket())
                                                           .key(copyObjectRequest.sourceKey())
                                                           .versionId(sourceVersionId))
                     .subscribe(response -> fetchAnnotationBodies(copyObjectRequest, sourceVersionId, response, annotationBodies,
                                                                  fetchFutures))
                     .whenComplete((v, t) -> {
                         if (t != null) {
                             result.completeExceptionally(t);
                         } else {
                             CompletableFutureUtils.allOfExceptionForwarded(fetchFutures.toArray(new CompletableFuture[0]))
                                                   .whenComplete((ignored, fetchError) -> {
                                                       if (fetchError != null) {
                                                           result.completeExceptionally(fetchError);
                                                       } else {
                                                           result.complete(annotationBodies);
                                                       }
                                                   });
                         }
                     });

        return result;
    }

    private void fetchAnnotationBodies(CopyObjectRequest copyObjectRequest,
                                       String sourceVersionId,
                                       ListObjectAnnotationsResponse response,
                                       Map<String, byte[]> annotationBodies,
                                       Queue<CompletableFuture<Void>> fetchFutures) {
        for (AnnotationEntry entry : response.annotations()) {
            CompletableFuture<Void> fetchFuture =
                fetchSingleAnnotation(copyObjectRequest, sourceVersionId, entry.annotationName())
                    .thenAccept(body -> annotationBodies.put(entry.annotationName(), body));
            fetchFutures.add(fetchFuture);
        }
    }

    private CompletableFuture<byte[]> fetchSingleAnnotation(CopyObjectRequest copyObjectRequest,
                                                            String sourceVersionId,
                                                            String annotationName) {
        return s3AsyncClient.getObjectAnnotation(r -> r.bucket(copyObjectRequest.sourceBucket())
                                                       .key(copyObjectRequest.sourceKey())
                                                       .versionId(sourceVersionId)
                                                       .annotationName(annotationName),
                                                 AsyncResponseTransformer.toBytes())
                            .thenApply(ResponseBytes::asByteArray);
    }

    private CompletableFuture<CompleteMultipartUploadResponse> putTaggingPostCompletion(
        CopyObjectRequest copyObjectRequest,
        CompleteMultipartUploadResponse completeResponse,
        List<Tag> tags) {

        return s3AsyncClient.putObjectTagging(r -> r.bucket(copyObjectRequest.destinationBucket())
                                                    .key(copyObjectRequest.destinationKey())
                                                    .versionId(completeResponse.versionId())
                                                    .tagging(Tagging.builder().tagSet(tags).build()))
                            .thenApply(r -> completeResponse);
    }

    /**
     * Writes annotations to the destination object after CompleteMultipartUpload.
     * Annotations are written sequentially; the chain fails fast on the first error.
     * On failure, the destination object is NOT deleted - partial results may be useful.
     */
    private CompletableFuture<CompleteMultipartUploadResponse> writeAnnotationsToDestination(
        CopyObjectRequest copyObjectRequest,
        CompleteMultipartUploadResponse completeResponse,
        Map<String, byte[]> annotations) {

        String destVersionId = completeResponse.versionId();
        String destETag = completeResponse.eTag();

        log.debug(() -> String.format("Writing %d annotations to destination object", annotations.size()));

        List<String> succeededAnnotations = new ArrayList<>();
        List<Map.Entry<String, byte[]>> entries = new ArrayList<>(annotations.entrySet());

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (Map.Entry<String, byte[]> entry : entries) {
            chain = chain.thenCompose(ignored ->
                                          s3AsyncClient.putObjectAnnotation(r -> r.bucket(copyObjectRequest.destinationBucket())
                                                                                  .key(copyObjectRequest.destinationKey())
                                                                                  .versionId(destVersionId)
                                                                                  .objectIfMatch(destETag)
                                                                                  .annotationName(entry.getKey()),
                                                                            AsyncRequestBody.fromBytes(entry.getValue()))
                                                       .thenRun(() -> succeededAnnotations.add(entry.getKey())));
        }

        return chain.thenApply(ignored -> completeResponse)
                    .exceptionally(t -> {
                        Throwable cause = t instanceof CompletionException ? t.getCause() : t;
                        List<String> failedAnnotations = entries.stream()
                                                                .map(Map.Entry::getKey)
                                                                .filter(name -> !succeededAnnotations.contains(name))
                                                                .collect(Collectors.toList());
                        log.warn(() -> String.format("Annotation copy partial failure. SUCCEEDED (%d): %s, FAILED (%d): %s",
                                                     succeededAnnotations.size(), succeededAnnotations,
                                                     failedAnnotations.size(), failedAnnotations));
                        throw SdkClientException.create(
                            String.format("Failed to copy %d of %d annotations to destination object. "
                                          + "The destination object was NOT deleted.",
                                          failedAnnotations.size(), entries.size()), cause);
                    });
    }

    private void doCreateMultipartUploadAndCopy(MultipartCopyContext ctx, HeadObjectResponse headObjectResponse) {
        CreateMultipartUploadRequest request = SdkPojoConversionUtils.toCreateMultipartUploadRequest(ctx.copyObjectRequest);

        // Forward source metadata unless the user explicitly set REPLACE with their own values.
        if (ctx.copyObjectRequest.metadataDirective() != MetadataDirective.REPLACE) {
            request = request.toBuilder()
                             .metadata(headObjectResponse.metadata())
                             .contentType(headObjectResponse.contentType())
                             .cacheControl(headObjectResponse.cacheControl())
                             .contentDisposition(headObjectResponse.contentDisposition())
                             .contentEncoding(headObjectResponse.contentEncoding())
                             .contentLanguage(headObjectResponse.contentLanguage())
                             .expires(headObjectResponse.expires())
                             .build();
        }

        CompletableFuture<CreateMultipartUploadResponse> createMpuFuture = s3AsyncClient.createMultipartUpload(request);

        CompletableFutureUtils.forwardExceptionTo(ctx.returnFuture, createMpuFuture);

        createMpuFuture.whenComplete((createMultipartUploadResponse, throwable) -> {
            if (throwable != null) {
                genericMultipartHelper.handleException(ctx.returnFuture, () -> "Failed to initiate multipart upload", throwable);
            } else {
                log.debug(() -> "Initiated new multipart upload, uploadId: " + createMultipartUploadResponse.uploadId());
                doCopyInParts(ctx, createMultipartUploadResponse.uploadId());
            }
        });
    }

    private void doCopyInParts(MultipartCopyContext ctx, String uploadId) {

        long optimalPartSize = genericMultipartHelper.calculateOptimalPartSizeFor(ctx.contentLength, partSizeInBytes);

        int partCount = genericMultipartHelper.determinePartCount(ctx.contentLength, optimalPartSize);
        if (optimalPartSize > partSizeInBytes) {
            log.debug(() -> String.format("Configured partSize is %d, but using %d to prevent reaching maximum number of parts "
                                          + "allowed", partSizeInBytes, optimalPartSize));
        }

        log.debug(() -> String.format("Starting multipart copy with partCount: %s, optimalPartSize: %s",
                                      partCount, optimalPartSize));

        CopyObjectRequest copyRequestWithPinnedSource = pinSourceETag(ctx.copyObjectRequest, ctx.sourceETag);

        // The list of completed parts must be sorted
        AtomicReferenceArray<CompletedPart> completedParts = new AtomicReferenceArray<>(partCount);

        List<CompletableFuture<CompletedPart>> futures = sendUploadPartCopyRequests(copyRequestWithPinnedSource,
                                                                                    ctx.contentLength,
                                                                                    uploadId,
                                                                                    completedParts,
                                                                                    optimalPartSize);

        AtomicBoolean mpuCompleted = new AtomicBoolean(false);

        CompletableFutureUtils.allOfExceptionForwarded(futures.toArray(new CompletableFuture[0]))
                              .thenCompose(ignore -> completeMultipartUpload(ctx.copyObjectRequest, uploadId,
                                                                             completedParts))
                              .thenApply(response -> {
                                  mpuCompleted.set(true);
                                  return response;
                              })
                              .thenCompose(response -> applyPostCompletionProperties(ctx, response))
                              .whenComplete(handleCopyCompletion(ctx, uploadId, mpuCompleted));
    }

    private CompletableFuture<CompleteMultipartUploadResponse> applyPostCompletionProperties(
        MultipartCopyContext ctx,
        CompleteMultipartUploadResponse completeResponse) {

        CompletableFuture<CompleteMultipartUploadResponse> result;

        if (!CollectionUtils.isNullOrEmpty(ctx.sourceTags)) {
            result = putTaggingPostCompletion(ctx.copyObjectRequest, completeResponse, ctx.sourceTags);
        } else {
            result = CompletableFuture.completedFuture(completeResponse);
        }

        if (!CollectionUtils.isNullOrEmpty(ctx.annotations)) {
            result = result.thenCompose(resp -> writeAnnotationsToDestination(ctx.copyObjectRequest, resp, ctx.annotations));
        }
        return result;
    }

    private BiConsumer<CompleteMultipartUploadResponse, Throwable> handleCopyCompletion(
        MultipartCopyContext ctx,
        String uploadId,
        AtomicBoolean mpuCompleted) {

        PublisherListener<Long> progressListener = ctx.copyObjectRequest.overrideConfiguration()
                                                                        .map(c -> c.executionAttributes()
                                                                                   .getAttribute(JAVA_PROGRESS_LISTENER))
                                                                        .orElseGet(PublisherListener::noOp);

        return (completeResponse, throwable) -> {
            if (throwable != null) {
                if (!mpuCompleted.get()) {
                    genericMultipartHelper.cleanUpParts(uploadId,
                                                        SdkPojoConversionUtils.toAbortMultipartUploadRequest(
                                                            ctx.copyObjectRequest));
                    genericMultipartHelper.handleException(ctx.returnFuture,
                                                           () -> "Failed to send multipart copy requests", throwable);
                } else {
                    log.warn(() -> "Multipart copy completed but failed to copy source properties to destination. "
                                   + "The destination object was NOT deleted.");
                    genericMultipartHelper.handleException(ctx.returnFuture,
                                                           () -> "Failed to copy source properties to destination", throwable);
                }
            } else {
                ctx.returnFuture.complete(SdkPojoConversionUtils.toCopyObjectResponse(completeResponse));
                progressListener.subscriberOnComplete();
            }
        };
    }

    /**
     * Returns a CopyObjectRequest with copySourceIfMatch set to the ETag from HeadObject.
     * This detects source mutation via ETag conditional without requiring s3:GetObjectVersion.
     * If the user explicitly provided sourceVersionId on the original request, it is already
     * present on the builder and will be preserved.
     */
    private CopyObjectRequest pinSourceETag(CopyObjectRequest request, String sourceETag) {
        CopyObjectRequest.Builder builder = request.toBuilder();
        if (sourceETag != null) {
            builder.copySourceIfMatch(sourceETag);
        }
        return builder.build();
    }

    private CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(
        CopyObjectRequest copyObjectRequest, String uploadId, AtomicReferenceArray<CompletedPart> completedParts) {
        log.debug(() -> String.format("Sending completeMultipartUploadRequest, uploadId: %s",
                                      uploadId));
        CompletedPart[] parts =
            IntStream.range(0, completedParts.length())
                     .mapToObj(completedParts::get)
                     .toArray(CompletedPart[]::new);
        CompleteMultipartUploadRequest.Builder builder =
            CompleteMultipartUploadRequest.builder()
                                          .bucket(copyObjectRequest.destinationBucket())
                                          .key(copyObjectRequest.destinationKey())
                                          .uploadId(uploadId)
                                          .multipartUpload(CompletedMultipartUpload.builder()
                                                                                   .parts(parts)
                                                                                   .build())
                                          .sseCustomerAlgorithm(copyObjectRequest.sseCustomerAlgorithm())
                                          .sseCustomerKey(copyObjectRequest.sseCustomerKey())
                                          .sseCustomerKeyMD5(copyObjectRequest.sseCustomerKeyMD5());
        copyObjectRequest.overrideConfiguration().ifPresent(builder::overrideConfiguration);
        CompleteMultipartUploadRequest completeMultipartUploadRequest = builder.build();
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

    private static final class MultipartCopyContext {
        private final CopyObjectRequest copyObjectRequest;
        private final long contentLength;
        private final CompletableFuture<CopyObjectResponse> returnFuture;
        private final String sourceVersionId;
        private final String sourceETag;
        private final List<Tag> sourceTags;
        private final Map<String, byte[]> annotations;

        private MultipartCopyContext(Builder builder) {
            this.copyObjectRequest = builder.copyObjectRequest;
            this.contentLength = builder.contentLength;
            this.returnFuture = builder.returnFuture;
            this.sourceVersionId = builder.sourceVersionId;
            this.sourceETag = builder.sourceETag;
            this.sourceTags = builder.sourceTags;
            this.annotations = builder.annotations;
        }

        static Builder builder() {
            return new Builder();
        }

        static final class Builder {
            private CopyObjectRequest copyObjectRequest;
            private long contentLength;
            private CompletableFuture<CopyObjectResponse> returnFuture;
            private String sourceVersionId;
            private String sourceETag;
            private List<Tag> sourceTags;
            private Map<String, byte[]> annotations;

            Builder copyObjectRequest(CopyObjectRequest copyObjectRequest) {
                this.copyObjectRequest = copyObjectRequest;
                return this;
            }

            Builder contentLength(long contentLength) {
                this.contentLength = contentLength;
                return this;
            }

            Builder returnFuture(CompletableFuture<CopyObjectResponse> returnFuture) {
                this.returnFuture = returnFuture;
                return this;
            }

            Builder sourceVersionId(String sourceVersionId) {
                this.sourceVersionId = sourceVersionId;
                return this;
            }

            Builder sourceETag(String sourceETag) {
                this.sourceETag = sourceETag;
                return this;
            }

            Builder sourceTags(List<Tag> sourceTags) {
                this.sourceTags = sourceTags;
                return this;
            }

            Builder annotations(Map<String, byte[]> annotations) {
                this.annotations = annotations;
                return this;
            }

            MultipartCopyContext build() {
                return new MultipartCopyContext(this);
            }
        }
    }
}
