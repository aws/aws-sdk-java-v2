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
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformerPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class DownloadObjectHelper {
    private static final Logger log = Logger.loggerFor(DownloadObjectHelper.class);

    private final S3AsyncClient s3AsyncClient;
    private final long bufferSizeInBytes;

    public DownloadObjectHelper(S3AsyncClient s3AsyncClient, long bufferSizeInBytes) {
        this.s3AsyncClient = s3AsyncClient;
        this.bufferSizeInBytes = bufferSizeInBytes;
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
        if (!split.supportParallel()) {
            return downloadPartsLinear(getObjectRequest, split);
        }

        // The publisher of AsyncResponseTransformer needs to know about s3 GetObjectResponse to write to the correct file offset.
        // The default publisher in the SplitResult may not be able to do so, so we need to create a new one that knows about s3
        FileAsyncResponseTransformerPublisher<GetObjectResponse> publisher =
            new FileAsyncResponseTransformerPublisher<>(asyncResponseTransformer);
        return downloadPartsNonLinear(getObjectRequest, publisher, split);

    }

    private <T> CompletableFuture<T> downloadPartsNonLinear(GetObjectRequest getObjectRequest,
                                                            FileAsyncResponseTransformerPublisher<GetObjectResponse> publisher,
                                                            AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split) {
        // TODO pause & resume
        NonLinearMultipartDownloaderSubscriber subscriber = new NonLinearMultipartDownloaderSubscriber(
            s3AsyncClient, getObjectRequest, (CompletableFuture<GetObjectResponse>) split.resultFuture());
        publisher.subscribe(subscriber);
        return split.resultFuture();
    }

    private <T> CompletableFuture<T> downloadPartsLinear(GetObjectRequest getObjectRequest,
                                                         AsyncResponseTransformer.SplitResult<GetObjectResponse, T> split) {
        MultipartDownloaderSubscriber subscriber = subscriber(getObjectRequest);
        split.publisher().subscribe(subscriber);
        return split.resultFuture();
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
