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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
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
    private static final Pattern CONTENT_RANGE_PATTERN = Pattern.compile("bytes\\s+(\\d+)-(\\d+)/(\\d+)");

    private final S3AsyncClient s3AsyncClient;
    private final PresignedUrlDownloadRequest presignedUrlDownloadRequest;
    private final long configuredPartSizeInBytes;
    private final CompletableFuture<Void> future;
    private final Object lock = new Object();
    private final AtomicInteger completedParts;

    private volatile Long totalContentLength;
    private volatile Integer totalParts;
    private volatile String eTag;
    private volatile Subscription subscription;

    public PresignedUrlMultipartDownloaderSubscriber(
        S3AsyncClient s3AsyncClient,
        PresignedUrlDownloadRequest presignedUrlDownloadRequest,
        long configuredPartSizeInBytes) {
        this.s3AsyncClient = s3AsyncClient;
        this.presignedUrlDownloadRequest = presignedUrlDownloadRequest;
        this.configuredPartSizeInBytes = configuredPartSizeInBytes;
        this.completedParts = new AtomicInteger(0);
        this.future = new CompletableFuture<>();
    }

    @Override
    public void onSubscribe(Subscription s) {
        synchronized (lock) {
            if (subscription != null) {
                s.cancel();
                return;
            }
            this.subscription = s;
            s.request(1);
        }
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
        PresignedUrlDownloadRequest partRequest = createPartRequest(partIndex);
        log.debug(() -> "Sending range request for part " + partIndex + " with range=" + partRequest.range());
        
        s3AsyncClient.presignedUrlExtension()
            .getObject(partRequest, asyncResponseTransformer)
            .whenComplete((response, error) -> {
                if (error != null) {
                    log.debug(() -> "Error encountered during part request for part " + partIndex);
                    handleError(error);
                    return;
                }
                requestMoreIfNeeded(response);
            });
    }

    private void requestMoreIfNeeded(GetObjectResponse response) {
        int totalComplete = completedParts.get();
        log.debug(() -> String.format("Completed part %d", totalComplete));
        
        synchronized (lock) {
            if (eTag == null) {
                this.eTag = response.eTag();
                log.debug(() -> String.format("Multipart object ETag: %s", this.eTag));
            } else if (response.eTag() != null && !eTag.equals(response.eTag())) {
                handleError(new IllegalStateException("ETag mismatch - object may have changed during download"));
                return;
            }
            if (totalContentLength == null && response.contentRange() != null) {
                try {
                    validateResponse(response);
                    long totalSize = parseContentRangeForTotalSize(response.contentRange());
                    int calculatedTotalParts = calculateTotalParts(totalSize, configuredPartSizeInBytes);
                    this.totalContentLength = totalSize;
                    this.totalParts = calculatedTotalParts;
                    log.debug(() -> String.format("Total content length: %d, Total parts: %d", totalSize, calculatedTotalParts));
                } catch (Exception e) {
                    log.debug(() -> "Failed to parse content range", e);
                    handleError(e);
                    return;
                }
            }
            if (totalParts != null && totalParts > 1 && totalComplete < totalParts) {
                subscription.request(1);
            } else {
                log.debug(() -> String.format("Completing multipart download after a total of %d parts downloaded.", totalParts));
                subscription.cancel();
            }
        }
    }

    private void validateResponse(GetObjectResponse response) {
        if (response == null) {
            throw new IllegalStateException("Response cannot be null");
        }
        if (response.contentRange() == null) {
            throw new IllegalStateException("No Content-Range header in response");
        }
        Long contentLength = response.contentLength();
        if (contentLength == null || contentLength <= 0) {
            throw new IllegalStateException("Invalid or missing Content-Length in response");
        }
    }

    private long parseContentRangeForTotalSize(String contentRange) {
        Matcher matcher = CONTENT_RANGE_PATTERN.matcher(contentRange);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Content-Range header: " + contentRange);
        }
        return Long.parseLong(matcher.group(3));
    }

    private int calculateTotalParts(long contentLength, long partSize) {
        return (int) Math.ceil((double) contentLength / partSize);
    }

    private PresignedUrlDownloadRequest createPartRequest(int partIndex) {
        long startByte = partIndex * configuredPartSizeInBytes;
        long endByte;
        
        if (totalContentLength != null) {
            endByte = Math.min(startByte + configuredPartSizeInBytes - 1, totalContentLength - 1);
        } else {
            endByte = startByte + configuredPartSizeInBytes - 1;
        }
        String rangeHeader = BYTES_RANGE_PREFIX + startByte + "-" + endByte;
        return presignedUrlDownloadRequest.toBuilder()
                                          .range(rangeHeader)
                                          .build();
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
        return this.future;
    }
}