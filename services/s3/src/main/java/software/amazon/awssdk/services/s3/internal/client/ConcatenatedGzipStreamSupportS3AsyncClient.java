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

package software.amazon.awssdk.services.s3.internal.client;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.internal.async.ConfigurableAsyncResponseTransformer;
import software.amazon.awssdk.services.s3.DelegatingS3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

/**
 * Configures the caller's transformer before an S3 decorator can capture it by calling {@code split()}.
 */
@SdkInternalApi
final class ConcatenatedGzipStreamSupportS3AsyncClient extends DelegatingS3AsyncClient {
    private final boolean enabled;

    ConcatenatedGzipStreamSupportS3AsyncClient(S3AsyncClient delegate, boolean enabled) {
        super(delegate);
        this.enabled = enabled;
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest request, AsyncResponseTransformer<GetObjectResponse, ReturnT> transformer) {
        return super.getObject(request, ConfigurableAsyncResponseTransformer.configure(transformer, enabled));
    }

    @Override
    public AsyncPresignedUrlExtension presignedUrlExtension() {
        AsyncPresignedUrlExtension delegateExtension = super.presignedUrlExtension();
        return new AsyncPresignedUrlExtension() {
            @Override
            public <ReturnT> CompletableFuture<ReturnT> getObject(
                PresignedUrlDownloadRequest request,
                AsyncResponseTransformer<GetObjectResponse, ReturnT> transformer) {
                return delegateExtension.getObject(
                    request, ConfigurableAsyncResponseTransformer.configure(transformer, enabled));
            }
        };
    }
}
