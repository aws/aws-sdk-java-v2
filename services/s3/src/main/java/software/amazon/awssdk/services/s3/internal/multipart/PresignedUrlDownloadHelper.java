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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class PresignedUrlDownloadHelper {
    private static final Logger log = Logger.loggerFor(PresignedUrlDownloadHelper.class);
    private static final String BYTES_RANGE_PREFIX = "bytes=";
    private static final Pattern CONTENT_RANGE_PATTERN = Pattern.compile("bytes\\s+(\\d+)-(\\d+)/(\\d+)");
    private static final long MAX_DOWNLOAD_PARTS = 10_000; // S3 limit

    private final S3AsyncClient s3AsyncClient;
    private final long bufferSizeInBytes;
    private final long configuredPartSizeInBytes;
    private final long multipartDownloadThresholdInBytes;

    public PresignedUrlDownloadHelper(S3AsyncClient s3AsyncClient,
                                      long bufferSizeInBytes,
                                      long configuredPartSizeInBytes,
                                      long multipartDownloadThresholdInBytes) {
        this.s3AsyncClient = Validate.paramNotNull(s3AsyncClient, "s3AsyncClient");
        this.bufferSizeInBytes = bufferSizeInBytes;
        this.configuredPartSizeInBytes = configuredPartSizeInBytes;
        this.multipartDownloadThresholdInBytes = multipartDownloadThresholdInBytes;
    }

    public <T> CompletableFuture<T> downloadObject(
            PresignedUrlDownloadRequest presignedRequest,
            AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {

        // If range is specified, do single part download
        if (presignedRequest.range() != null) {
            log.debug(() -> "Range specified in presigned URL request, performing single part download");
            return s3AsyncClient.presignedUrlExtension().getObject(presignedRequest, asyncResponseTransformer);
        }

        // Use first part request for size discovery (SEP approach)
        CompletableFuture<FirstPartInfo> firstPartFuture = getFirstPartInfo(presignedRequest);

        CompletableFuture<T> returnFuture = new CompletableFuture<>();

        firstPartFuture.whenComplete((firstPartInfo, throwable) -> {
            if (throwable != null) {
                log.debug(() -> "First part request failed, falling back to single part download", throwable);
                CompletableFuture<T> singlePartFuture =
                        s3AsyncClient.presignedUrlExtension().getObject(presignedRequest, asyncResponseTransformer);
                CompletableFutureUtils.forwardResultTo(singlePartFuture, returnFuture);
                return;
            }

            Long totalContentLength = firstPartInfo.totalLength;
            if (totalContentLength == null || totalContentLength <= 0) {
                log.debug(() -> "Content length not available from Content-Range header, performing single part download");
                CompletableFuture<T> singlePartFuture =
                        s3AsyncClient.presignedUrlExtension().getObject(presignedRequest, asyncResponseTransformer);
                CompletableFutureUtils.forwardResultTo(singlePartFuture, returnFuture);
                return;
            }

            // Check if content is below threshold
            if (totalContentLength < multipartDownloadThresholdInBytes) {
                log.debug(() -> String.format("Content length %d is below threshold %d, performing single part download",
                        totalContentLength, multipartDownloadThresholdInBytes));
                CompletableFuture<T> singlePartFuture =
                        s3AsyncClient.presignedUrlExtension().getObject(presignedRequest, asyncResponseTransformer);
                CompletableFutureUtils.forwardResultTo(singlePartFuture, returnFuture);
                return;
            }

            // Check if first part contains the entire object
            if (totalContentLength <= configuredPartSizeInBytes) {
                log.debug(() -> "First part contains entire object, using single part download");
                CompletableFuture<T> singlePartFuture =
                        s3AsyncClient.presignedUrlExtension().getObject(presignedRequest, asyncResponseTransformer);
                CompletableFutureUtils.forwardResultTo(singlePartFuture, returnFuture);
                return;
            }

            log.debug(() -> String.format("Starting multipart download for presigned URL with total content length: %d",
                    totalContentLength));
            performMultipartDownload(presignedRequest, asyncResponseTransformer, firstPartInfo, returnFuture);
        });

        return returnFuture;
    }

    /**
     * Makes the first part request following SEP specification: bytes=0-{partSizeInBytes-1}
     * This serves dual purpose: downloads first part data AND discovers total object size
     */
    private CompletableFuture<FirstPartInfo> getFirstPartInfo(PresignedUrlDownloadRequest request) {
        // Calculate optimal part size first
        long actualPartSize = configuredPartSizeInBytes;
        
        // Create first part request: bytes=0-{partSize-1}
        long endByte = actualPartSize - 1;
        String firstPartRange = String.format("%s0-%d", BYTES_RANGE_PREFIX, endByte);
        
        PresignedUrlDownloadRequest firstPartRequest = request.toBuilder()
                .range(firstPartRange)
                .build();

        log.debug(() -> String.format("Making first part request with range: %s", firstPartRange));

        return s3AsyncClient.presignedUrlExtension()
                .getObject(firstPartRequest, AsyncResponseTransformer.toBytes())
                .thenApply(response -> {
                    GetObjectResponse getObjectResponse = response.response();
                    String contentRange = getObjectResponse.contentRange();

                    if (contentRange == null) {
                        throw SdkClientException.create("Content-Range header not found in first part response");
                    }

                    FirstPartInfo firstPartInfo = parseFirstPartContentRange(contentRange, getObjectResponse, response.asByteArray());
                    
                    // SEP Step 4: Validate first part response
                    validateFirstPartResponse(firstPartInfo, firstPartRequest, getObjectResponse);
                    
                    return firstPartInfo;
                });
    }

    private FirstPartInfo parseFirstPartContentRange(String contentRange, GetObjectResponse response, byte[] firstPartData) {
        Matcher matcher = CONTENT_RANGE_PATTERN.matcher(contentRange);
        if (!matcher.matches()) {
            throw SdkClientException.create("Invalid Content-Range header format: " + contentRange);
        }

        long startByte = Long.parseLong(matcher.group(1));
        long endByte = Long.parseLong(matcher.group(2));
        long totalLength = Long.parseLong(matcher.group(3));
        long firstPartSize = endByte - startByte + 1;

        log.debug(() -> String.format("Parsed Content-Range: start=%d, end=%d, total=%d, firstPartSize=%d", 
                                    startByte, endByte, totalLength, firstPartSize));

        return new FirstPartInfo(totalLength, response, firstPartData, firstPartSize);
    }

    /**
     * SEP Step 4: Validate first part response according to SEP specification
     */
    private void validateFirstPartResponse(FirstPartInfo firstPartInfo, 
                                         PresignedUrlDownloadRequest firstPartRequest, 
                                         GetObjectResponse response) {
        // Validate that we got the expected range
        String requestedRange = firstPartRequest.range();
        String responseContentRange = response.contentRange();
        
        log.debug(() -> String.format("Validating first part: requested=%s, received=%s", 
                                    requestedRange, responseContentRange));
        
        // SEP Step 4: Compare parsed total content length with ContentLength of the response
        Long responseContentLength = response.contentLength();
        if (responseContentLength != null) {
            if (!responseContentLength.equals(firstPartInfo.firstPartSize)) {
                log.warn(() -> String.format(
                    "First part size mismatch: Content-Length=%d, calculated from range=%d", 
                    responseContentLength, firstPartInfo.firstPartSize));
            }
        }
        
        // Validate that total length is reasonable
        if (firstPartInfo.totalLength <= 0) {
            throw SdkClientException.create(String.format(
                "Invalid total content length from Content-Range: %d", firstPartInfo.totalLength));
        }
        
        // SEP Step 4: If parsed total content length equals ContentLength, 
        // it indicates this request contains all of the data
        if (firstPartInfo.totalLength.equals(responseContentLength)) {
            log.debug(() -> String.format(
                "First part contains entire object (size=%d), will use single-stream download", 
                firstPartInfo.totalLength));
        } else {
            // SEP Step 4: Add validation that ContentLength equals to the targetPartSizeBytes
            // (This is more of a warning since the last part or adjusted part sizes may differ)
            if (responseContentLength != null && 
                !responseContentLength.equals(configuredPartSizeInBytes) &&
                firstPartInfo.totalLength > configuredPartSizeInBytes) {
                log.debug(() -> String.format(
                    "First part size (%d) differs from configured part size (%d), may indicate adjusted part sizing", 
                    responseContentLength, configuredPartSizeInBytes));
            }
        }
        
        log.debug(() -> String.format("First part validation passed: totalLength=%d, firstPartSize=%d", 
                                    firstPartInfo.totalLength, firstPartInfo.firstPartSize));
    }

    private long calculateOptimalPartSizeFor(long totalContentLength) {
        // Same logic as GenericMultipartHelper but for downloads
        double optimalPartSize = totalContentLength / (double) MAX_DOWNLOAD_PARTS;
        optimalPartSize = Math.ceil(optimalPartSize);
        
        // Use the larger of: optimal size OR user's configured size
        return (long) Math.max(optimalPartSize, configuredPartSizeInBytes);
    }

    private <T> void performMultipartDownload(
            PresignedUrlDownloadRequest presignedRequest,
            AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer,
            FirstPartInfo firstPartInfo,
            CompletableFuture<T> returnFuture) {

        // Calculate the actual part size to use (respects user config + S3 limits)
        long actualPartSize = calculateOptimalPartSizeFor(firstPartInfo.totalLength);
        
        log.debug(() -> String.format(
            "Multipart download: object=%d bytes, configured_part=%d bytes, actual_part=%d bytes, estimated_parts=%d",
            firstPartInfo.totalLength, configuredPartSizeInBytes, actualPartSize, 
            (firstPartInfo.totalLength / actualPartSize)));

        SplittingTransformerConfiguration splittingConfig = SplittingTransformerConfiguration.builder()
                .bufferSizeInBytes(bufferSizeInBytes)
                .build();

        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split =
                asyncResponseTransformer.split(splittingConfig);

        PresignedUrlMultipartDownloaderSubscriber subscriber =
                new PresignedUrlMultipartDownloaderSubscriber(
                        s3AsyncClient,
                        presignedRequest,
                        firstPartInfo.totalLength,
                        actualPartSize,
                        firstPartInfo);  // Pass first part info

        split.publisher().subscribe(subscriber);
        CompletableFutureUtils.forwardResultTo(split.resultFuture(), returnFuture);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, split.resultFuture());
    }

    private static class FirstPartInfo {
        final Long totalLength;
        final GetObjectResponse response;
        final byte[] firstPartData;
        final long firstPartSize;

        FirstPartInfo(Long totalLength, GetObjectResponse response, byte[] firstPartData, long firstPartSize) {
            this.totalLength = totalLength;
            this.response = response;
            this.firstPartData = firstPartData;
            this.firstPartSize = firstPartSize;
        }
    }
}
