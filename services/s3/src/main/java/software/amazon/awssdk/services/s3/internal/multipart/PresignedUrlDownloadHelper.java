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
                if (cause instanceof EmptyObjectRangeNotSatisfiableException) {
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