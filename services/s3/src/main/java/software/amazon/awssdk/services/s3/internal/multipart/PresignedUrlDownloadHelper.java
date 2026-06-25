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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class PresignedUrlDownloadHelper {
    private static final Logger log = Logger.loggerFor(PresignedUrlDownloadHelper.class);
    private static final int DEFAULT_MAX_IN_FLIGHT_PARTS = 10;

    private final S3AsyncClient s3AsyncClient;
    private final AsyncPresignedUrlExtension asyncPresignedUrlExtension;
    private final long bufferSizeInBytes;
    private final long configuredPartSizeInBytes;

    public PresignedUrlDownloadHelper(S3AsyncClient s3AsyncClient,
                                      AsyncPresignedUrlExtension asyncPresignedUrlExtension,
                                      long bufferSizeInBytes,
                                      long configuredPartSizeInBytes) {
        this.s3AsyncClient = Validate.paramNotNull(s3AsyncClient, "s3AsyncClient");
        this.asyncPresignedUrlExtension = Validate.paramNotNull(asyncPresignedUrlExtension, "asyncPresignedUrlExtension");
        this.bufferSizeInBytes = Validate.isPositive(bufferSizeInBytes, "bufferSizeInBytes");
        this.configuredPartSizeInBytes = Validate.isPositive(configuredPartSizeInBytes, "configuredPartSizeInBytes");
    }

    public <T> CompletableFuture<T> downloadObject(
        PresignedUrlDownloadRequest presignedRequest,
        AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {

        Validate.paramNotNull(presignedRequest, "presignedRequest");
        Validate.paramNotNull(asyncResponseTransformer, "asyncResponseTransformer");

        if (presignedRequest.range() != null) {
            log.debug(() -> "Using single part download because presigned URL request range is included in the request. range = "
                            + presignedRequest.range());
            return asyncPresignedUrlExtension.getObject(presignedRequest, asyncResponseTransformer);
        }

        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        doMultipartDownload(presignedRequest, asyncResponseTransformer)
            .whenComplete((result, error) -> {
                Throwable cause = error instanceof CompletionException ? error.getCause() : error;
                // Parallel path wraps it as EmptyObjectRangeNotSatisfiableException;
                // serial path (toBytes, custom transformers) surfaces raw S3Exception.
                if (cause instanceof EmptyObjectRangeNotSatisfiableException
                    || isRangeNotSatisfiable(cause)) {
                    log.debug(() -> "Received 416 on first request, falling back to non-range GET for empty object");
                    asyncPresignedUrlExtension.getObject(presignedRequest, asyncResponseTransformer)
                                              .whenComplete((r, e) -> {
                                                  if (e != null) {
                                                      resultFuture.completeExceptionally(e);
                                                  } else {
                                                      resultFuture.complete(r);
                                                  }
                                              });
                } else if (error != null) {
                    resultFuture.completeExceptionally(error);
                } else {
                    resultFuture.complete(result);
                }
            });
        return resultFuture;
    }

    private <T> CompletableFuture<T> doMultipartDownload(
        PresignedUrlDownloadRequest presignedRequest,
        AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {

        SplittingTransformerConfiguration splittingConfig = SplittingTransformerConfiguration.builder()
                                                                                             .bufferSizeInBytes(bufferSizeInBytes)
                                                                                             .build();
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split =
            asyncResponseTransformer.split(splittingConfig);

        if (split.parallelSplitSupported()) {
            return downloadPartsInParallel(presignedRequest, split);
        }
        // Serial path: split with response mapper to convert part response to full-object response
        split = MultipartDownloadUtils.splitWithResponseRewrite(asyncResponseTransformer, splittingConfig);
        return downloadPartsSerially(presignedRequest, split);
    }

    private <T> CompletableFuture<T> downloadPartsInParallel(
        PresignedUrlDownloadRequest presignedRequest,
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split) {
        log.debug(() -> "Using parallel multipart download for presigned URL");
        ParallelPresignedUrlMultipartDownloaderSubscriber subscriber =
            new ParallelPresignedUrlMultipartDownloaderSubscriber(
                s3AsyncClient,
                presignedRequest,
                configuredPartSizeInBytes,
                (CompletableFuture<GetObjectResponse>) split.resultFuture(),
                DEFAULT_MAX_IN_FLIGHT_PARTS);
        split.publisher().subscribe(subscriber);
        return split.resultFuture();
    }

    private <T> CompletableFuture<T> downloadPartsSerially(
        PresignedUrlDownloadRequest presignedRequest,
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split) {
        log.debug(() -> "Using serial multipart download for presigned URL");
        PresignedUrlMultipartDownloaderSubscriber subscriber =
            new PresignedUrlMultipartDownloaderSubscriber(
                s3AsyncClient,
                presignedRequest,
                configuredPartSizeInBytes,
                split.resultFuture());
        split.publisher().subscribe(subscriber);
        return split.resultFuture();
    }

    static SdkClientException invalidContentRangeHeader(String contentRange) {
        return SdkClientException.create("Invalid Content-Range header: " + contentRange);
    }

    static SdkClientException missingContentRangeHeader() {
        return SdkClientException.create("No Content-Range header in response");
    }

    static SdkClientException invalidContentLength() {
        return SdkClientException.create("Invalid or missing Content-Length in response");
    }

    /**
     * Validates a part response for data integrity. Checks that Content-Range and Content-Length
     * match the expected values based on part index, part size, and total object size.
     *
     * @param response           the GetObject response to validate
     * @param partIndex          zero-based index of this part
     * @param partSizeInBytes    configured part size
     * @param totalContentLength total object size (from Content-Range), or null if not yet known
     * @param totalParts         total number of parts, or null if not yet known
     * @return empty if valid, or an SdkClientException describing the mismatch
     */
    static Optional<SdkClientException> validatePartResponse(GetObjectResponse response,
                                                             long partIndex,
                                                             long partSizeInBytes,
                                                             Long totalContentLength,
                                                             Long totalParts) {
        String contentRange = response.contentRange();
        if (contentRange == null) {
            return Optional.of(missingContentRangeHeader());
        }

        Long contentLength = response.contentLength();
        if (contentLength == null || contentLength < 0) {
            return Optional.of(invalidContentLength());
        }

        long expectedStartByte = partIndex * partSizeInBytes;
        long[] parsedRange = MultipartDownloadUtils.parseContentRange(contentRange);
        if (parsedRange == null) {
            return Optional.of(invalidContentRangeHeader(contentRange));
        }
        long actualStartByte = parsedRange[0];
        long actualEndByte = parsedRange[1];
        if (actualStartByte != expectedStartByte) {
            return Optional.of(SdkClientException.create(
                String.format("Content-Range mismatch for part %d. Expected start byte: %d, but got: bytes %d-%d",
                              partIndex, expectedStartByte, actualStartByte, actualEndByte)));
        }
        if (totalContentLength != null) {
            long expectedEndByte = Math.min(expectedStartByte + partSizeInBytes - 1, totalContentLength - 1);
            if (actualEndByte != expectedEndByte) {
                return Optional.of(SdkClientException.create(
                    String.format("Content-Range mismatch for part %d. Expected: bytes %d-%d, but got: bytes %d-%d",
                                  partIndex, expectedStartByte, expectedEndByte, actualStartByte, actualEndByte)));
            }
        }

        if (totalContentLength != null && totalParts != null) {
            long expectedPartSize = (partIndex == totalParts - 1)
                                    ? totalContentLength - (partIndex * partSizeInBytes)
                                    : partSizeInBytes;
            if (!contentLength.equals(expectedPartSize)) {
                return Optional.of(SdkClientException.create(
                    String.format("Part content length validation failed for part %d. Expected: %d, but got: %d",
                                  partIndex, expectedPartSize, contentLength)));
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a range-based GET request for a specific part of a presigned URL download.
     *
     * @param originalRequest    the original presigned URL request
     * @param partIndex          zero-based index of this part
     * @param partSizeInBytes    configured part size
     * @param totalContentLength total object size, or null if not yet known (first part)
     * @param eTag               ETag from first response, used for If-Match on parts 1+
     * @return a new PresignedUrlDownloadRequest with the appropriate Range and If-Match headers
     */
    static PresignedUrlDownloadRequest createRangedGetRequest(PresignedUrlDownloadRequest originalRequest,
                                                              long partIndex,
                                                              long partSizeInBytes,
                                                              Long totalContentLength,
                                                              String eTag) {
        long startByte = partIndex * partSizeInBytes;
        long endByte = totalContentLength != null
                       ? Math.min(startByte + partSizeInBytes - 1, totalContentLength - 1)
                       : startByte + partSizeInBytes - 1;
        PresignedUrlDownloadRequest.Builder builder = originalRequest.toBuilder()
                                                                     .range("bytes=" + startByte + "-" + endByte);
        if (partIndex > 0 && eTag != null) {
            builder.ifMatch(eTag);
        }
        return builder.build();
    }

    /**
     * Returns true if the error is a 416 Range Not Satisfiable response from S3.
     * Used by subscribers to detect empty object responses on the first range request.
     */
    static boolean isRangeNotSatisfiable(Throwable error) {
        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
        return cause instanceof S3Exception && ((S3Exception) cause).statusCode() == 416;
    }

    /**
     * Marker exception wrapping a 416 on the first range request, signaling an empty object.
     * Used to distinguish from 416 errors on subsequent requests which should propagate as failures.
     */
    static class EmptyObjectRangeNotSatisfiableException extends RuntimeException {
        EmptyObjectRangeNotSatisfiableException(Throwable cause) {
            super("Object is empty (416 on first range request)", cause);
        }
    }
}