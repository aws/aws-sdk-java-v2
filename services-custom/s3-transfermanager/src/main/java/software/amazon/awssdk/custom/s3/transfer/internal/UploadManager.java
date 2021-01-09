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

package software.amazon.awssdk.custom.s3.transfer.internal;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.custom.s3.transfer.CompletedUpload;
import software.amazon.awssdk.custom.s3.transfer.MultipartUploadConfiguration;
import software.amazon.awssdk.custom.s3.transfer.TransferOverrideConfiguration;
import software.amazon.awssdk.custom.s3.transfer.Upload;
import software.amazon.awssdk.custom.s3.transfer.UploadRequest;
import software.amazon.awssdk.custom.s3.transfer.UploadState;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Performs upload operations for s3 transfer manager.
 */
@SdkInternalApi
@ThreadSafe
public final class UploadManager {

    private final S3AsyncClient s3Client;
    private final MultipartUploadConfiguration configuration;
    private final MultiPartUploadManager multiPartUploadManager;
    private final SinglePartUploadManager singlePartUploadManager;

    private UploadManager(Builder builder) {
        this.s3Client = builder.s3Client;
        this.configuration = builder.configuration;
        this.multiPartUploadManager = new MultiPartUploadManager(s3Client);
        this.singlePartUploadManager = new SinglePartUploadManager(s3Client);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Handles uploading via either api request or presigned Url.
     *
     * @param request the uploadRequest
     * @param requestBody the requestBody
     * @return {@link Upload}
     */
    Upload uploadObject(UploadRequest request, TransferRequestBody requestBody) {
        if (request.uploadSpecification().isPresignedUrl()) {
            throw new UnsupportedOperationException("Uploading Presigned URL not supported");
        }

        return apiRequestUpload(request, requestBody);
    }

    private Upload apiRequestUpload(UploadRequest request, TransferRequestBody requestBody) {
        MultipartUploadConfiguration resolvedConfiguration = resolveConfiguration(request, this.configuration);

        if (!resolvedConfiguration.enableMultipartUploads()) {
            return singlePartUpload(request, requestBody);
        }

        return multipartUpload(request, requestBody, resolvedConfiguration);
    }

    private Upload singlePartUpload(UploadRequest request, TransferRequestBody requestBody) {
        CompletableFuture<Void> partsCompleteFuture = singlePartUploadManager.apiRequestUpload(request, requestBody);
        return new UploadImpl(partsCompleteFuture);
    }

    private Upload multipartUpload(UploadRequest request,
                                   TransferRequestBody requestBody,
                                   MultipartUploadConfiguration resolvedConfiguration) {

        CompletableFuture<Long> objectSizeFuture = determineObjectSize(request, requestBody);

        CompletableFuture<Void> partsCompleteFuture = objectSizeFuture.thenCompose(size -> {

            long threshold = resolvedConfiguration.multipartUploadThreshold();
            if (size >= threshold) {
                return multiPartUploadManager.apiRequestUpload(size,
                                                               request.uploadSpecification().asApiRequest(),
                                                               requestBody,
                                                               resolvedConfiguration);
            }

            return singlePartUploadManager.apiRequestUpload(request, requestBody);
        });

        return new UploadImpl(partsCompleteFuture);
    }

    private CompletableFuture<Long> determineObjectSize(UploadRequest uploadRequest, TransferRequestBody requestBody) {
        Optional<Long> providedSize = uploadRequest.size();

        return providedSize.map(CompletableFuture::completedFuture)
                           .orElseGet(() -> CompletableFuture.completedFuture(requestBody.contentLength()));

    }

    private MultipartUploadConfiguration resolveConfiguration(UploadRequest uploadRequest,
                                                              MultipartUploadConfiguration configuration) {
        return uploadRequest.overrideConfiguration()
                            .map(TransferOverrideConfiguration::multipartUploadConfiguration)
                            .orElse(configuration);
    }

    public static final class Builder {
        private S3AsyncClient s3Client;
        private MultipartUploadConfiguration configuration = MultipartUploadConfiguration.defaultConfig();

        private Builder() {
        }

        public Builder s3Client(S3AsyncClient s3Client) {
            this.s3Client = s3Client;
            return this;
        }

        public Builder configuration(MultipartUploadConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public UploadManager build() {
            return new UploadManager(this);
        }

    }

    private static final class UploadImpl implements Upload {

        private final CompletableFuture<CompletedUpload> completionFuture;
        private final CompletableFuture<?> partsCompleteFuture;

        private UploadImpl(CompletableFuture<?> partsCompleteFuture) {
            this.partsCompleteFuture = partsCompleteFuture;
            this.completionFuture = this.partsCompleteFuture.thenApply(ignored -> null);
            CompletableFutureUtils.forwardExceptionTo(this.completionFuture, this.partsCompleteFuture);
        }

        @Override
        public CompletableFuture<CompletedUpload> completionFuture() {
            return completionFuture;
        }

        @Override
        public UploadState pause() {
            throw new UnsupportedOperationException();
        }
    }
}
