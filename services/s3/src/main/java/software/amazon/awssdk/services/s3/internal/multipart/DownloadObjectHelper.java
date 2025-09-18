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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class DownloadObjectHelper {
    private static final Logger log = Logger.loggerFor(DownloadObjectHelper.class);

    private final S3AsyncClient s3AsyncClient;
    private final long bufferSizeInBytes;
    private final int maxInFlightParts;

    public DownloadObjectHelper(S3AsyncClient s3AsyncClient, long bufferSizeInBytes, int maxInFlightParts) {
        this.s3AsyncClient = s3AsyncClient;
        this.bufferSizeInBytes = bufferSizeInBytes;
        this.maxInFlightParts = maxInFlightParts;
    }

    public <T> CompletableFuture<T> downloadObject(
        GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {
        if (getObjectRequest.range() != null || getObjectRequest.partNumber() != null) {
            logSinglePartMessage(getObjectRequest);
            return s3AsyncClient.getObject(getObjectRequest, asyncResponseTransformer);
        }
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split =
            asyncResponseTransformer.split(SplittingTransformerConfiguration.builder()
                                                                            .bufferSizeInBytes(bufferSizeInBytes)
                                                                            .build());
        if (!split.parallelSplitSupported()) {
            return downloadPartsSerially(getObjectRequest, split);
        }

        return downloadPartsNonSerially(getObjectRequest, split, maxInFlightParts);

    }

    private <T> CompletableFuture<T> downloadPartsNonSerially(
        GetObjectRequest getObjectRequest,
        AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split,
        int maxInFlight) {
        NonLinearMultipartDownloaderSubscriber subscriber = new NonLinearMultipartDownloaderSubscriber(
            s3AsyncClient, getObjectRequest, (CompletableFuture<GetObjectResponse>) split.resultFuture(), maxInFlight);
        split.publisher().subscribe(subscriber);
        return split.resultFuture();
    }

    private <T> CompletableFuture<T> downloadPartsSerially(GetObjectRequest getObjectRequest,
                                                           AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split) {
        MultipartDownloaderSubscriber subscriber = subscriber(getObjectRequest);
        split.publisher().subscribe(subscriber);
        CompletableFuture<T> splitFuture = split.resultFuture();
        CompletableFutureUtils.forwardExceptionTo(subscriber.future(), splitFuture);
        return splitFuture;
    }

    private MultipartDownloaderSubscriber subscriber(GetObjectRequest getObjectRequest) {
        Optional<MultipartDownloadResumeContext> multipartDownloadContext =
            MultipartDownloadUtils.multipartDownloadResumeContext(getObjectRequest);
        return multipartDownloadContext
            .map(ctx -> new MultipartDownloaderSubscriber(s3AsyncClient, getObjectRequest, ctx.highestSequentialCompletedPart()))
            .orElseGet(() -> new MultipartDownloaderSubscriber(s3AsyncClient, getObjectRequest));
    }

    private void logSinglePartMessage(GetObjectRequest getObjectRequest) {
        log.debug(() -> {
            String reason = "";
            if (getObjectRequest.range() != null) {
                reason = " because getObjectRequest range is included in the request."
                         + " range = " + getObjectRequest.range();
            } else if (getObjectRequest.partNumber() != null) {
                reason = " because getObjectRequest part number is included in the request."
                         + " part number = " + getObjectRequest.partNumber();
            }
            return "Using single part download" + reason;
        });
    }
}
