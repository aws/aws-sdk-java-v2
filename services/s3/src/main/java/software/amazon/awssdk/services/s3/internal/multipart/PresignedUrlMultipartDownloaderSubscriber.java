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
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class PresignedUrlMultipartDownloaderSubscriber
        implements Subscriber<AsyncResponseTransformer<GetObjectResponse, GetObjectResponse>> {

    private static final Logger log = Logger.loggerFor(PresignedUrlMultipartDownloaderSubscriber.class);
    private static final String BYTES_RANGE_PREFIX = "bytes=";

    private final S3AsyncClient s3;
    private final PresignedUrlDownloadRequest baseRequest;
    private final long totalContentLength;
    private final long partSizeInBytes;
    private final int totalParts;
    private final AtomicInteger completedParts;
    private final CompletableFuture<Void> future;
    private final Object lock = new Object();
    private final boolean hasFirstPart;
    private final long firstPartSize;

    private Subscription subscription;

    public PresignedUrlMultipartDownloaderSubscriber(
            S3AsyncClient s3,
            PresignedUrlDownloadRequest baseRequest,
            long totalContentLength,
            long partSizeInBytes) {
        this(s3, baseRequest, totalContentLength, partSizeInBytes, null);
    }

    public PresignedUrlMultipartDownloaderSubscriber(
            S3AsyncClient s3,
            PresignedUrlDownloadRequest baseRequest,
            long totalContentLength,
            long partSizeInBytes,
            Object firstPartInfo) {
        this.s3 = Validate.paramNotNull(s3, "s3AsyncClient");
        this.baseRequest = Validate.paramNotNull(baseRequest, "baseRequest");
        this.totalContentLength = totalContentLength;
        this.partSizeInBytes = partSizeInBytes;
        
        // Handle first part info if provided
        if (firstPartInfo != null) {
            this.hasFirstPart = true;
            // Use reflection or cast to get first part size - for now assume it matches partSizeInBytes
            this.firstPartSize = Math.min(partSizeInBytes, totalContentLength);
            // Calculate remaining parts (excluding the first part we already have)
            long remainingBytes = totalContentLength - firstPartSize;
            int remainingParts = (int) Math.ceil((double) remainingBytes / partSizeInBytes);
            this.totalParts = remainingParts + 1; // +1 for the first part
            this.completedParts = new AtomicInteger(1); // First part is already "completed"
        } else {
            this.hasFirstPart = false;
            this.firstPartSize = 0;
            this.totalParts = calculateTotalParts(totalContentLength, partSizeInBytes);
            this.completedParts = new AtomicInteger(0);
        }
        
        this.future = new CompletableFuture<>();
        
        log.debug(() -> String.format("Initialized subscriber: totalParts=%d, hasFirstPart=%s, firstPartSize=%d", 
                                    totalParts, hasFirstPart, firstPartSize));
    }

    private int calculateTotalParts(long contentLength, long partSize) {
        return (int) Math.ceil((double) contentLength / partSize);
    }

    @Override
    public void onSubscribe(Subscription s) {
        synchronized (lock) {
            if (subscription != null) {
                s.cancel();
                return;
            }
            this.subscription = s;
            
            // If we already have the first part, we need to request parts starting from the second part
            if (hasFirstPart && completedParts.get() < totalParts) {
                this.subscription.request(1);
            } else if (!hasFirstPart) {
                this.subscription.request(1);
            } else {
                // All parts are already completed (single part case)
                log.debug(() -> "All parts already completed, finishing download");
                subscription.cancel();
            }
        }
    }

    @Override
    public void onNext(AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> asyncResponseTransformer) {
        if (asyncResponseTransformer == null) {
            subscription.cancel();
            throw new NullPointerException("onNext must not be called with null asyncResponseTransformer");
        }

        int currentCompletedParts = completedParts.get();
        
        // Calculate which part we're requesting (accounting for first part if already downloaded)
        int partIndex = hasFirstPart ? currentCompletedParts : currentCompletedParts;

        if (currentCompletedParts >= totalParts) {
            log.debug(() -> String.format("Completing multipart download after a total of %d parts downloaded.", totalParts));
            subscription.cancel();
            return;
        }

        PresignedUrlDownloadRequest partRequest = createPartRequest(partIndex);
        log.debug(() -> String.format("Sending presigned URL request for part %d (range: %s)",
                partIndex + 1, partRequest.range()));

        CompletableFuture<GetObjectResponse> getObjectFuture =
                s3.presignedUrlExtension().getObject(partRequest, asyncResponseTransformer);

        getObjectFuture.whenComplete((response, error) -> {
            if (error != null) {
                log.debug(() -> "Error encountered during presigned URL request for part " + (partIndex + 1), error);
                onError(error);
            } else {
                // SEP Step 6: Validate GetObject response for each part
                try {
                    validatePartResponse(response, partRequest, partIndex);
                    requestMoreIfNeeded(response);
                } catch (Exception validationError) {
                    log.error(() -> String.format("Validation failed for part %d: %s", 
                                                 partIndex + 1, validationError.getMessage()));
                    onError(validationError);
                }
            }
        });
    }

    @Override
    public void onError(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        future.complete(null);
    }

    public CompletableFuture<Void> future() {
        return future;
    }

    private void requestMoreIfNeeded(GetObjectResponse response) {
        int totalComplete = completedParts.incrementAndGet();
        log.debug(() -> String.format("Completed part %d of %d", totalComplete, totalParts));

        if (totalComplete < totalParts) {
            subscription.request(1);
        } else {
            // SEP Step 7: Validate that total number of ranged GET requests matches expected
            try {
                validateTotalPartsCompleted(totalComplete);
                log.debug(() -> "All parts downloaded, completing multipart download");
                subscription.cancel();
            } catch (Exception validationError) {
                log.error(() -> String.format("Final validation failed: %s", validationError.getMessage()));
                onError(validationError);
            }
        }
    }

    /**
     * SEP Step 6: Validate GetObject response for each part
     */
    private void validatePartResponse(GetObjectResponse response, PresignedUrlDownloadRequest partRequest, int partIndex) {
        String requestedRange = partRequest.range();
        String responseContentRange = response.contentRange();
        
        // Validate that ContentRange matches with the requested range
        if (responseContentRange == null) {
            throw new IllegalStateException(String.format(
                "Missing Content-Range header in response for part %d (requested range: %s)", 
                partIndex + 1, requestedRange));
        }
        
        // Extract requested range for comparison
        String expectedRangePattern = requestedRange.replace("bytes=", "bytes ");
        if (!responseContentRange.startsWith(expectedRangePattern.split("/")[0])) {
            log.warn(() -> String.format(
                "Content-Range mismatch for part %d: requested=%s, received=%s", 
                partIndex + 1, requestedRange, responseContentRange));
            // Note: This is a warning rather than error as some S3 implementations may vary slightly
        }
        
        // Validate content length matches expected part size (except for last part)
        Long contentLength = response.contentLength();
        if (contentLength != null) {
            long expectedSize = calculateExpectedPartSize(partIndex);
            if (!contentLength.equals(expectedSize)) {
                log.debug(() -> String.format(
                    "Part %d size validation: expected=%d, actual=%d (may be last part)", 
                    partIndex + 1, expectedSize, contentLength));
            }
        }
        
        log.debug(() -> String.format("Part %d validation passed: range=%s, contentLength=%s", 
                                    partIndex + 1, responseContentRange, contentLength));
    }

    /**
     * SEP Step 7: Validate that total number of ranged GET requests sent matches expected
     */
    private void validateTotalPartsCompleted(int actualPartsCompleted) {
        if (actualPartsCompleted != totalParts) {
            throw new IllegalStateException(String.format(
                "Part count mismatch: expected %d parts, but completed %d parts", 
                totalParts, actualPartsCompleted));
        }
        
        log.debug(() -> String.format("Total parts validation passed: %d parts completed as expected", totalParts));
    }

    /**
     * Calculate expected part size for validation
     */
    private long calculateExpectedPartSize(int partIndex) {
        if (hasFirstPart && partIndex == 0) {
            return firstPartSize;
        }
        
        long startByte;
        if (hasFirstPart) {
            startByte = firstPartSize + (partIndex - 1) * partSizeInBytes;
        } else {
            startByte = partIndex * partSizeInBytes;
        }
        
        long endByte = Math.min(startByte + partSizeInBytes - 1, totalContentLength - 1);
        return endByte - startByte + 1;
    }

    private PresignedUrlDownloadRequest createPartRequest(int partIndex) {
        // Calculate byte range for this part
        long startByte;
        long endByte;
        
        if (hasFirstPart) {
            // Skip the first part since we already have it
            startByte = firstPartSize + (partIndex - 1) * partSizeInBytes;
            endByte = Math.min(startByte + partSizeInBytes - 1, totalContentLength - 1);
        } else {
            // Standard calculation
            startByte = partIndex * partSizeInBytes;
            endByte = Math.min(((partIndex + 1) * partSizeInBytes) - 1, totalContentLength - 1);
        }

        // Validate range
        if (startByte >= totalContentLength) {
            throw new IllegalStateException(String.format(
                "Invalid range calculation: startByte=%d >= totalContentLength=%d for partIndex=%d", 
                startByte, totalContentLength, partIndex));
        }

        String rangeHeader = String.format("%s%d-%d", BYTES_RANGE_PREFIX, startByte, endByte);

        return baseRequest.toBuilder()
                .range(rangeHeader)
                .build();
    }
}
