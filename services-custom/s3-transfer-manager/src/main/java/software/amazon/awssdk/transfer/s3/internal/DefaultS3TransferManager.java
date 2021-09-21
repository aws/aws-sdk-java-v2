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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedDownload;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.Download;
import software.amazon.awssdk.transfer.s3.DownloadRequest;
import software.amazon.awssdk.transfer.s3.S3ClientConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.Upload;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
public final class DefaultS3TransferManager implements S3TransferManager {
    private final S3CrtAsyncClient s3CrtAsyncClient;

    public DefaultS3TransferManager(DefaultBuilder tmBuilder) {
        S3CrtAsyncClient.S3CrtAsyncClientBuilder clientBuilder = S3CrtAsyncClient.builder();
        if (tmBuilder.s3ClientConfiguration != null) {
            tmBuilder.s3ClientConfiguration.credentialsProvider().ifPresent(clientBuilder::credentialsProvider);
            tmBuilder.s3ClientConfiguration.maxConcurrency().ifPresent(clientBuilder::maxConcurrency);
            tmBuilder.s3ClientConfiguration.minimumPartSizeInBytes().ifPresent(clientBuilder::minimumPartSizeInBytes);
            tmBuilder.s3ClientConfiguration.region().ifPresent(clientBuilder::region);
            tmBuilder.s3ClientConfiguration.targetThroughputInGbps().ifPresent(clientBuilder::targetThroughputInGbps);
            tmBuilder.s3ClientConfiguration.asyncConfiguration().ifPresent(clientBuilder::asyncConfiguration);
        }

        s3CrtAsyncClient = clientBuilder.build();
    }

    @SdkTestInternalApi
    DefaultS3TransferManager(S3CrtAsyncClient s3CrtAsyncClient) {
        this.s3CrtAsyncClient = s3CrtAsyncClient;
    }

    @Override
    public Upload upload(UploadRequest uploadRequest) {
        PutObjectRequest putObjectRequest = uploadRequest.putObjectRequest();
        AsyncRequestBody requestBody = requestBodyFor(uploadRequest);

        CompletableFuture<PutObjectResponse> putObjFuture = s3CrtAsyncClient.putObject(putObjectRequest, requestBody);

        CompletableFuture<CompletedUpload> future = putObjFuture.thenApply(r -> DefaultCompletedUpload.builder()
                                                                                                      .response(r)
                                                                                                      .build());
        return new DefaultUpload(CompletableFutureUtils.forwardExceptionTo(future, putObjFuture));
    }

    @Override
    public Download download(DownloadRequest downloadRequest) {
        CompletableFuture<GetObjectResponse> getObjectFuture =
            s3CrtAsyncClient.getObject(downloadRequest.getObjectRequest(),
                                       AsyncResponseTransformer.toFile(downloadRequest.destination()));
        CompletableFuture<CompletedDownload> future =
            getObjectFuture.thenApply(r -> DefaultCompletedDownload.builder().response(r).build());

        return new DefaultDownload(CompletableFutureUtils.forwardExceptionTo(future, getObjectFuture));
    }

    @Override
    public void close() {
        s3CrtAsyncClient.close();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private AsyncRequestBody requestBodyFor(UploadRequest uploadRequest) {
        return AsyncRequestBody.fromFile(uploadRequest.source());
    }

    private static class DefaultBuilder implements S3TransferManager.Builder {
        private S3ClientConfiguration s3ClientConfiguration;

        @Override
        public Builder s3ClientConfiguration(S3ClientConfiguration configuration) {
            this.s3ClientConfiguration = configuration;
            return this;
        }

        @Override
        public S3TransferManager build() {
            return new DefaultS3TransferManager(this);
        }
    }
}
