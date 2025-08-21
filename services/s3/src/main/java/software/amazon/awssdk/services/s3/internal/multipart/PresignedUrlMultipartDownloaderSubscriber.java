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
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.Logger;

/**
 * A subscriber implementation that will download all individual parts for a multipart presigned URL download request.
 * It receives individual {@link AsyncResponseTransformer} instances which will be used to perform the individual
 * range-based part requests using presigned URLs. This is a 'one-shot' class, it should <em>NOT</em> be reused
 * for more than one multipart download.
 *
 * <p>Unlike the standard {@link MultipartDownloaderSubscriber} which uses S3's native multipart API with part numbers,
 * this subscriber uses HTTP range requests against presigned URLs to achieve multipart download functionality.
 * <p>This implementation is thread-safe and handles concurrent part downloads while maintaining proper
 * ordering and validation of responses.</p>
 */
@ThreadSafe
@Immutable
@SdkInternalApi
public class PresignedUrlMultipartDownloaderSubscriber
    implements Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {

    private static final Logger log = Logger.loggerFor(PresignedUrlMultipartDownloaderSubscriber.class);
    private static final String BYTES_RANGE_PREFIX = "bytes=";

    private final S3AsyncClient s3AsyncClient;
    private final PresignedUrlDownloadRequest presignedUrlDownloadRequest;
    private final Long configuredPartSizeInBytes;
    private final CompletableFuture<Void> future;
    private final Object lock = new Object();
    private final AtomicInteger completedParts;
    private final AtomicInteger requestsSent;

    private volatile Long totalContentLength;
    private volatile Integer totalParts;
    private volatile String eTag;
    private Subscription subscription;

    public PresignedUrlMultipartDownloaderSubscriber(
        S3AsyncClient s3AsyncClient,
        PresignedUrlDownloadRequest presignedUrlDownloadRequest,
        long configuredPartSizeInBytes) {
        this.s3AsyncClient = s3AsyncClient;
        this.presignedUrlDownloadRequest = presignedUrlDownloadRequest;
        this.configuredPartSizeInBytes = configuredPartSizeInBytes;
        this.completedParts = new AtomicInteger(0);
        this.requestsSent = new AtomicInteger(0);
        this.future = new CompletableFuture<>();
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (subscription != null) {
            s.cancel();
            return;
        }
        this.subscription = s;
        s.request(1);
    }

    @Override
    public void onNext(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        if (asyncResponseTransformer == null) {
            throw new NullPointerException("onNext must not be called with null asyncResponseTransformer");
        }

        int nextPartIndex;
        synchronized (lock) {
            nextPartIndex = completedParts.get();
            if (totalParts != null && nextPartIndex >= totalParts) {
                log.debug(() -> String.format("Completing multipart download after a total of %d parts downloaded.", totalParts));
                subscription.cancel();
                return;
            }
            completedParts.incrementAndGet();
        }
        makeRangeRequest(nextPartIndex, asyncResponseTransformer);
    }

    private void makeRangeRequest(int partIndex,
                                  AsyncResponseTransformer<GetObjectResponse,
                                      GetObjectResponse> asyncResponseTransformer) {
        PresignedUrlDownloadRequest partRequest = createRangedGetRequest(partIndex);
        log.debug(() -> "Sending range request for part " + partIndex + " with range=" + partRequest.range());

        requestsSent.incrementAndGet();
        s3AsyncClient.presignedUrlExtension()
                     .getObject(partRequest, asyncResponseTransformer)
                     .whenComplete((response, error) -> {
                         if (error != null) {
                             log.debug(() -> "Error encountered during part request for part " + partIndex);
                             handleError(error);
                             return;
                         }
                         if (validatePart(response, partIndex, asyncResponseTransformer)) {
                             requestMoreIfNeeded(completedParts.get());
                         }
                     });
    }

    private boolean validatePart(GetObjectResponse response, int partIndex,
                                 AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        int totalComplete = completedParts.get();
        log.debug(() -> String.format("Completed part %d", totalComplete));

        String responseETag = response.eTag();
        String responseContentRange = response.contentRange();
        if (eTag == null) {
            this.eTag = responseETag;
            log.debug(() -> String.format("Multipart object ETag: %s", this.eTag));
        }

        Optional<SdkClientException> validationError = validateResponse(response, partIndex);
        if (validationError.isPresent()) {
            log.debug(() -> "Response validation failed", validationError.get());
            asyncResponseTransformer.exceptionOccurred(validationError.get());
            handleError(validationError.get());
            return false;
        }

        if (totalContentLength == null && responseContentRange != null) {
            Optional<Long> parsedContentLength = MultipartDownloadUtils.parseContentRangeForTotalSize(responseContentRange);
            if (!parsedContentLength.isPresent()) {
                SdkClientException error = PresignedUrlDownloadHelper.invalidContentRangeHeader(responseContentRange);
                log.debug(() -> "Failed to parse content range", error);
                asyncResponseTransformer.exceptionOccurred(error);
                handleError(error);
                return false;
            }

            this.totalContentLength = parsedContentLength.get();
            this.totalParts = calculateTotalParts(totalContentLength, configuredPartSizeInBytes);
            log.debug(() -> String.format("Total content length: %d, Total parts: %d", totalContentLength, totalParts));
        }
        return true;
    }

    private void requestMoreIfNeeded(int totalComplete) {
        synchronized (lock) {
            if (hasMoreParts(totalComplete)) {
                subscription.request(1);
            } else {
                if (totalParts != null && requestsSent.get() != totalParts) {
                    handleError(new IllegalStateException(
                        "Request count mismatch. Expected: " + totalParts + ", sent: " + requestsSent.get()));
                    return;
                }
                log.debug(() -> String.format("Completing multipart download after a total of %d parts downloaded.", totalParts));
                subscription.cancel();
            }
        }
    }

    private Optional<SdkClientException> validateResponse(GetObjectResponse response, int partIndex) {
        if (response == null) {
            return Optional.of(SdkClientException.create("Response cannot be null"));
        }
        String contentRange = response.contentRange();
        if (contentRange == null) {
            return Optional.of(PresignedUrlDownloadHelper.missingContentRangeHeader());
        }

        Long contentLength = response.contentLength();
        if (contentLength == null || contentLength < 0) {
            return Optional.of(PresignedUrlDownloadHelper.invalidContentLength());
        }

        long expectedStartByte = partIndex * configuredPartSizeInBytes;

        // For the first part, we need to determine the actual object size from the response
        if (partIndex == 0 && totalContentLength == null) {
            // Parse total content length from the Content-Range header for validation
            Optional<Long> parsedContentLength = MultipartDownloadUtils.parseContentRangeForTotalSize(contentRange);
            if (parsedContentLength.isPresent()) {
                long actualTotalLength = parsedContentLength.get();
                // If the object is smaller than our part size, we should expect the full object
                if (actualTotalLength <= configuredPartSizeInBytes) {
                    String expectedRange = "bytes 0-" + (actualTotalLength - 1) + "/";
                    if (!contentRange.startsWith(expectedRange)) {
                        return Optional.of(SdkClientException.create(
                            "Content-Range mismatch for small object. Expected range starting with: " + expectedRange +
                            ", but got: " + contentRange));
                    }
                    return Optional.empty(); // Skip further validation for small objects
                }
            }
        }

        long expectedEndByte;
        if (totalContentLength != null) {
            expectedEndByte = Math.min(expectedStartByte + configuredPartSizeInBytes - 1, totalContentLength - 1);
        } else {
            expectedEndByte = expectedStartByte + configuredPartSizeInBytes - 1;
        }
        String expectedRange = "bytes " + expectedStartByte + "-" + expectedEndByte + "/";
        if (!contentRange.startsWith(expectedRange)) {
            return Optional.of(SdkClientException.create(
                "Content-Range mismatch. Expected range starting with: " + expectedRange +
                ", but got: " + contentRange));
        }

        // Skip part size validation if we already handled small object case above
        if (partIndex == 0 && totalContentLength == null) {
            Optional<Long> parsedContentLength = MultipartDownloadUtils.parseContentRangeForTotalSize(contentRange);
            if (parsedContentLength.isPresent() && parsedContentLength.get() <= configuredPartSizeInBytes) {
                // For small objects, the content length should match the actual object size
                if (!contentLength.equals(parsedContentLength.get())) {
                    return Optional.of(SdkClientException.create(
                        String.format("Small object content length validation failed. Expected: %d, but got: %d",
                                      parsedContentLength.get(), contentLength)));
                }
                return Optional.empty(); // Skip remaining validation
            }
        }

        long expectedPartSize;
        if (totalContentLength != null && partIndex == totalParts - 1) {
            expectedPartSize = totalContentLength - (partIndex * configuredPartSizeInBytes);
        } else {
            expectedPartSize = configuredPartSizeInBytes;
        }
        if (!contentLength.equals(expectedPartSize)) {
            return Optional.of(SdkClientException.create(
                String.format("Part content length validation failed for part %d. Expected: %d, but got: %d",
                              partIndex, expectedPartSize, contentLength)));
        }

        long actualStartByte = MultipartDownloadUtils.parseStartByteFromContentRange(contentRange);
        if (actualStartByte != expectedStartByte) {
            return Optional.of(SdkClientException.create(
                "Content range offset mismatch for part " + partIndex +
                ". Expected start: " + expectedStartByte + ", but got: " + actualStartByte));
        }

        return Optional.empty();
    }

    private int calculateTotalParts(long contentLength, long partSize) {
        return (int) Math.ceil((double) contentLength / partSize);
    }

    private boolean hasMoreParts(int completedPartsCount) {
        return totalParts != null && completedPartsCount < totalParts;
    }

    private PresignedUrlDownloadRequest createRangedGetRequest(int partIndex) {
        long startByte = partIndex * configuredPartSizeInBytes;
        long endByte;
        if (totalContentLength != null) {
            endByte = Math.min(startByte + configuredPartSizeInBytes - 1, totalContentLength - 1);
        } else {
            endByte = startByte + configuredPartSizeInBytes - 1;
        }
        String rangeHeader = BYTES_RANGE_PREFIX + startByte + "-" + endByte;
        PresignedUrlDownloadRequest.Builder builder = presignedUrlDownloadRequest.toBuilder()
                                                                                 .range(rangeHeader);
        if (partIndex > 0 && eTag != null) {
            builder.ifMatch(eTag);
        }
        return builder.build();
    }

    private void handleError(Throwable t) {
        synchronized (lock) {
            if (subscription != null) {
                subscription.cancel();
            }
        }
        onError(t);
    }

    @Override
    public void onError(Throwable t) {
        log.debug(() -> "Error in multipart download", t);
        future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        future.complete(null);
    }

}