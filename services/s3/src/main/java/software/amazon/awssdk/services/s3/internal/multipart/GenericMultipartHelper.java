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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.services.s3.model.S3Response;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class GenericMultipartHelper<RequestT extends S3Request, ResponseT extends S3Response> {
    private static final Logger log = Logger.loggerFor(GenericMultipartHelper.class);
    /**
     * The max number of parts on S3 side is 10,000
     */
    private static final long MAX_UPLOAD_PARTS = 10_000;

    private final S3AsyncClient s3AsyncClient;
    private final Function<RequestT, AbortMultipartUploadRequest.Builder> abortMultipartUploadRequestConverter;
    private final Function<CompleteMultipartUploadResponse, ResponseT> responseConverter;

    public GenericMultipartHelper(S3AsyncClient s3AsyncClient,
                                  Function<RequestT, AbortMultipartUploadRequest.Builder> abortMultipartUploadRequestConverter,
                                  Function<CompleteMultipartUploadResponse, ResponseT> responseConverter) {
        this.s3AsyncClient = s3AsyncClient;
        this.abortMultipartUploadRequestConverter = abortMultipartUploadRequestConverter;
        this.responseConverter = responseConverter;
    }

    public void handleException(CompletableFuture<ResponseT> returnFuture,
                                Supplier<String> message,
                                Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;

        if (cause instanceof Error) {
            returnFuture.completeExceptionally(cause);
        } else {
            SdkClientException exception = SdkClientException.create(message.get(), cause);
            returnFuture.completeExceptionally(exception);
        }
    }

    public long calculateOptimalPartSizeFor(long contentLengthOfSource, long partSizeInBytes) {
        double optimalPartSize = contentLengthOfSource / (double) MAX_UPLOAD_PARTS;

        optimalPartSize = Math.ceil(optimalPartSize);
        return (long) Math.max(optimalPartSize, partSizeInBytes);
    }

    public int determinePartCount(long contentLength, long partSize) {
        return (int) Math.ceil(contentLength / (double) partSize);
    }

    public CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(
        RequestT request, String uploadId, CompletedPart[] parts) {
        log.debug(() -> String.format("Sending completeMultipartUploadRequest, uploadId: %s",
                                      uploadId));
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
            CompleteMultipartUploadRequest.builder()
                                          .bucket(request.getValueForField("Bucket", String.class).get())
                                          .key(request.getValueForField("Key", String.class).get())
                                          .uploadId(uploadId)
                                          .multipartUpload(CompletedMultipartUpload.builder()
                                                                                   .parts(parts)
                                                                                   .build())
                                          .build();
        return s3AsyncClient.completeMultipartUpload(completeMultipartUploadRequest);
    }

    public CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(
        RequestT request, String uploadId, AtomicReferenceArray<CompletedPart> completedParts) {
        CompletedPart[] parts =
            IntStream.range(0, completedParts.length())
                     .mapToObj(completedParts::get)
                     .toArray(CompletedPart[]::new);
        return completeMultipartUpload(request, uploadId, parts);
    }

    public BiFunction<CompleteMultipartUploadResponse, Throwable, Void> handleExceptionOrResponse(
        RequestT request,
        CompletableFuture<ResponseT> returnFuture,
        String uploadId) {

        return (completeMultipartUploadResponse, throwable) -> {
            if (throwable != null) {
                cleanUpParts(uploadId, abortMultipartUploadRequestConverter.apply(request));
                handleException(returnFuture, () -> "Failed to send multipart requests",
                                throwable);
            } else {
                returnFuture.complete(responseConverter.apply(
                    completeMultipartUploadResponse));
            }

            return null;
        };
    }

    public void cleanUpParts(String uploadId, AbortMultipartUploadRequest.Builder abortMultipartUploadRequest) {
        log.debug(() -> "Aborting multipart upload: " + uploadId);
        AbortMultipartUploadRequest request = abortMultipartUploadRequest.uploadId(uploadId).build();
        s3AsyncClient.abortMultipartUpload(request)
                     .exceptionally(throwable -> {
                         log.warn(() -> String.format("Failed to abort previous multipart upload "
                                                      + "(id: %s)"
                                                      + ". You may need to call "
                                                      + "S3AsyncClient#abortMultiPartUpload to "
                                                      + "free all storage consumed by"
                                                      + " all parts. ",
                                                      uploadId), throwable);
                         return null;
                     });
    }
}
