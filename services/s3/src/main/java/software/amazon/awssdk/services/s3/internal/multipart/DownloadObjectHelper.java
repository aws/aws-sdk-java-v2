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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

// todo
//  when we are done downloading, call on complete on the asyncResponseTransformer subscriber that is subscribed to the publisher
//  of the onStrem method
@SdkInternalApi
public class DownloadObjectHelper {
    private static final Logger log = Logger.loggerFor(DownloadObjectHelper.class);

    private final S3AsyncClient s3;
    private final long maxMemBufferSizeInBytes;

    DownloadObjectHelper(S3AsyncClient s3, long maxMemBufferSizeInBytes) {
        this.maxMemBufferSizeInBytes = maxMemBufferSizeInBytes;
        this.s3 = s3;
    }

    // Entry point
    <T> CompletableFuture<T> getObject(GetObjectRequest getObjectRequest,
                                          AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {
        // do not use multipart when byte range or part number is manually specified
        if (!StringUtils.isEmpty(getObjectRequest.range()) || !Objects.isNull(getObjectRequest.partNumber())) {
            return s3.getObject(getObjectRequest, asyncResponseTransformer);
        }
        // from failed attempt at splitting s3 request and buffering ...
            // CompletableFuture<T> returnFuture = new CompletableFuture<>();
            // MultipartDownloaderPublisher<T> downloader =
            //     new MultipartDownloaderPublisher<>(s3, getObjectRequest, asyncResponseTransformer);
            //
            // DelegatingResponseTransformer<GetObjectResponse, T> wrapped =
            //     new DelegatingResponseTransformer<>(asyncResponseTransformer, returnFuture);
            //
            // OrderedByteBufferPublisherAdapter adapter =
            //     new OrderedByteBufferPublisherAdapter(asyncResponseTransformer, downloader, 32L * 1024L * 1024L);
            // downloader.downstreamPublisher(adapter);
            //
            // // This future is handled by the asyncResponseTransformer passed in
            // CompletableFuture<T> prepared = wrapped.prepare();
            //
            // CompletableFutureUtils.forwardExceptionTo(returnFuture, prepared);
            // prepared.whenComplete((r, error) -> {
            //     log.info(() -> "DownloadObjectHelper future completed");
            //     if (error != null) {
            //         returnFuture.completeExceptionally(error);
            //         return;
            //     }
            //     returnFuture.complete(r);
            // });
            // downloader.start();
            // return returnFuture;

        // previous solution, not working for FileAsyncResponseTransformer
        MultipartDownloader<T> downloader = new MultipartDownloader<>(s3, maxMemBufferSizeInBytes);
        return downloader.getObject(getObjectRequest, asyncResponseTransformer.delegate());
    }

}
