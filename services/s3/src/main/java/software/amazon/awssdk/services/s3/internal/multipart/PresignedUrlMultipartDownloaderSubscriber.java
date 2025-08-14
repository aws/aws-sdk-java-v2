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
                requestMoreIfNeeded(response, partIndex);
            });
    }

    private void requestMoreIfNeeded(GetObjectResponse response, int partIndex) {
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
            handleError(validationError.get());
            return;
        }
        
        if (totalContentLength == null && responseContentRange != null) {
            Optional<Long> parsedContentLength = MultipartDownloadUtils.parseContentRangeForTotalSize(responseContentRange);
            if (!parsedContentLength.isPresent()) {
                SdkClientException error = PresignedUrlDownloadHelper.invalidContentRangeHeader(responseContentRange);
                log.debug(() -> "Failed to parse content range", error);
                handleError(error);
                return;
            }
            
            this.totalContentLength = parsedContentLength.get();
            this.totalParts = calculateTotalParts(totalContentLength, configuredPartSizeInBytes);
            log.debug(() -> String.format("Total content length: %d, Total parts: %d", totalContentLength, totalParts));
        }

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

        long expectedPartSize;
        if (totalContentLength != null && partIndex == totalParts - 1) {
            expectedPartSize = totalContentLength - (partIndex * configuredPartSizeInBytes);
        } else {
            expectedPartSize = configuredPartSizeInBytes;
        }
        
        if (!contentLength.equals(expectedPartSize)) {
            return Optional.of(SdkClientException.create(
                "Part content length validation failed for part " + partIndex + 
                ". Expected: " + expectedPartSize + ", but got: " + contentLength));
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
        return totalParts != null && totalParts > 1 && completedPartsCount < totalParts;
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
            log.debug(() -> "Setting IfMatch header to: " + eTag + " for part " + partIndex);
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

    public CompletableFuture<Void> future() {
        return future;
    }
}