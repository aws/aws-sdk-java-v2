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

/*
 * right Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A  of the License is located at
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
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReferenceArray;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.crt.RequestConversionUtils;
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

    private final int numOfPartsBuffered;
    private final Executor executor;

    public MultipartUploadHelper(S3AsyncClient s3AsyncClient, long partSizeInBytes, int numOfPartsBuffered, Executor executor) {
        this.s3AsyncClient = s3AsyncClient;
        this.partSizeInBytes = partSizeInBytes;
        this.genericMultipartHelper = new GenericMultipartHelper<>(s3AsyncClient,
                                                                   RequestConversionUtils::toAbortMultipartUploadRequest,
                                                                   RequestConversionUtils::toPutObjectResponse);
        this.executor = executor;
        this.numOfPartsBuffered = numOfPartsBuffered;
    }

    public CompletableFuture<PutObjectResponse> uploadObject(PutObjectRequest putObjectRequest,
                                                             AsyncRequestBody asyncRequestBody) {
        Long contentLength = asyncRequestBody.contentLength().orElseGet(putObjectRequest::contentLength);

        if (contentLength == null) {
            throw new IllegalArgumentException("Content-length is required");
        }

        CompletableFuture<PutObjectResponse> returnFuture = new CompletableFuture<>();

        try {
            if (contentLength <= partSizeInBytes) {
                log.debug(() -> "Starting the upload as a single upload part request");
                uploadInOneChunk(putObjectRequest, asyncRequestBody, returnFuture);
            } else {
                log.debug(() -> "Starting the upload as multipart upload request");
                uploadInParts(putObjectRequest, contentLength, asyncRequestBody, returnFuture);
            }

        } catch (Throwable throwable) {
            returnFuture.completeExceptionally(throwable);
        }

        return returnFuture;
    }

    private void uploadInParts(PutObjectRequest putObjectRequest, long contentLength, AsyncRequestBody asyncRequestBody,
                               CompletableFuture<PutObjectResponse> returnFuture) {

        CreateMultipartUploadRequest request = RequestConversionUtils.toCreateMultipartUploadRequest(putObjectRequest);
        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            s3AsyncClient.createMultipartUpload(request);

        // Ensure cancellations are forwarded to the createMultipartUploadFuture future
        CompletableFutureUtils.forwardExceptionTo(returnFuture, createMultipartUploadFuture);

        createMultipartUploadFuture.whenComplete((createMultipartUploadResponse, throwable) -> {
            if (throwable != null) {
                genericMultipartHelper.handleException(returnFuture, () -> "Failed to initiate multipart upload", throwable);
            } else {
                log.debug(() -> "Initiated a new multipart upload, uploadId: " + createMultipartUploadResponse.uploadId());
                doUploadInParts(putObjectRequest, asyncRequestBody, contentLength, returnFuture,
                                createMultipartUploadResponse.uploadId());
            }
        });
    }

    private void doUploadInParts(PutObjectRequest putObjectRequest,
                                 AsyncRequestBody asyncRequestBody,
                                 long contentLength,
                                 CompletableFuture<PutObjectResponse> returnFuture,
                                 String uploadId) {
        long optimalPartSize = genericMultipartHelper.calculateOptimalPartSizeFor(contentLength, partSizeInBytes);

        int partCount = genericMultipartHelper.determinePartCount(contentLength, optimalPartSize);

        log.debug(() -> String.format("Starting multipart upload with partCount: %d, optimalPartSize: %d", partCount,
                                      optimalPartSize));

        // The list of completed parts must be sorted
        AtomicReferenceArray<CompletedPart> completedParts = new AtomicReferenceArray<>(partCount);

        UploadPartRequestPublisher uploadPartRequestPublisher =
            UploadPartRequestPublisher.builder()
                                      .partSize(partSizeInBytes)
                                      .asyncRequestBody(asyncRequestBody)
                                      .putObjectRequest(putObjectRequest)
                                      .numOfPartsBuffered(numOfPartsBuffered)
                                      .uploadId(uploadId)
                                      .build();

        List<CompletableFuture<CompletedPart>> futures = sendUploadPartRequests(uploadId, uploadPartRequestPublisher,
                                                                                completedParts, returnFuture);

        CompletableFutureUtils.allOfExceptionForwarded(futures.toArray(new CompletableFuture[0]))
                              .thenCompose(ignore -> genericMultipartHelper.completeMultipartUpload(putObjectRequest, uploadId,
                                                                                                    completedParts))
                              .handle(genericMultipartHelper.handleExceptionOrResponse(putObjectRequest, returnFuture, uploadId))
                              .exceptionally(throwable -> {
                                  genericMultipartHelper.handleException(returnFuture, () -> "Unexpected exception occurred",
                                                                         throwable);
                                  return null;
                              });
    }

    private List<CompletableFuture<CompletedPart>> sendUploadPartRequests(String uploadId,
                                                                          UploadPartRequestPublisher publisher,
                                                                          AtomicReferenceArray<CompletedPart> completedParts,
                                                                          CompletableFuture<PutObjectResponse> returnFuture) {
        List<CompletableFuture<CompletedPart>> futures = new ArrayList<>();

        publisher.subscribe(pair -> sendIndividualUploadPartRequest(uploadId, completedParts, futures, pair))
                 .exceptionally(throwable -> {
                     returnFuture.completeExceptionally(throwable);
                     return null;
                 });

        return futures;
    }

    private void sendIndividualUploadPartRequest(String uploadId,
                                                 AtomicReferenceArray<CompletedPart> completedParts,
                                                 List<CompletableFuture<CompletedPart>> futures,
                                                 Pair<UploadPartRequest, AsyncRequestBody> requestPair) {
        UploadPartRequest uploadPartRequest = requestPair.left();
        Integer partNumber = uploadPartRequest.partNumber();
        log.debug(() -> "Sending uploadPartRequest: " + uploadPartRequest.partNumber() + " uploadId: " + uploadId + " "
                        + "contentLength " + requestPair.right().contentLength());

        CompletableFuture<UploadPartResponse> uploadPartFuture = s3AsyncClient.uploadPart(uploadPartRequest, requestPair.right());

        CompletableFuture<CompletedPart> convertFuture =
            uploadPartFuture.thenApply(uploadPartResponse -> convertUploadPartResponse(completedParts, partNumber,
                                                                                       uploadPartResponse));
        futures.add(convertFuture);

        CompletableFutureUtils.forwardExceptionTo(convertFuture, uploadPartFuture);
    }

    private static CompletedPart convertUploadPartResponse(AtomicReferenceArray<CompletedPart> completedParts,
                                                           Integer partNumber,
                                                           UploadPartResponse uploadPartResponse) {
        CompletedPart completedPart = RequestConversionUtils.toCompletedPart(uploadPartResponse, partNumber);

        completedParts.set(partNumber - 1, completedPart);
        return completedPart;
    }

    private void uploadInOneChunk(PutObjectRequest putObjectRequest, AsyncRequestBody asyncRequestBody,
                                  CompletableFuture<PutObjectResponse> returnFuture) {
        CompletableFuture<PutObjectResponse> putObjectResponseCompletableFuture = s3AsyncClient.putObject(putObjectRequest,
                                                                                                          asyncRequestBody);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, putObjectResponseCompletableFuture);
        CompletableFutureUtils.forwardResultTo(putObjectResponseCompletableFuture, returnFuture);
    }
}
