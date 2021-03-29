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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.custom.s3.transfer.CompletedUpload;
import software.amazon.awssdk.custom.s3.transfer.Download;
import software.amazon.awssdk.custom.s3.transfer.DownloadRequest;
import software.amazon.awssdk.custom.s3.transfer.S3TransferManager;
import software.amazon.awssdk.custom.s3.transfer.Upload;
import software.amazon.awssdk.custom.s3.transfer.UploadRequest;
import software.amazon.awssdk.services.s3.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@SdkInternalApi
public final class DefaultS3TransferManager implements S3TransferManager {
    private final S3CrtAsyncClient s3CrtAsyncClient;
    private final List<SdkAutoCloseable> closables = new ArrayList<>();

    public DefaultS3TransferManager(DefaultBuilder builder) {
        if (builder.s3CrtAsyncClient == null) {
            s3CrtAsyncClient = S3CrtAsyncClient.builder()
                                               .build();
            closables.add(s3CrtAsyncClient);
        } else {
            s3CrtAsyncClient = builder.s3CrtAsyncClient;
        }
    }

    @Override
    public Upload upload(UploadRequest uploadRequest) {
        PutObjectRequest putObjectRequest = uploadRequest.toApiRequest();
        AsyncRequestBody requestBody = requestBodyFor(uploadRequest);

        CompletableFuture<PutObjectResponse> putObjFuture = s3CrtAsyncClient.putObject(putObjectRequest, requestBody);

        return new DefaultUpload(putObjFuture.thenApply(r -> CompletedUpload.builder()
                .response(r)
                .build()));
    }

    @Override
    public Download download(DownloadRequest downloadRequest) {
        CompletableFuture<GetObjectResponse> future =
            s3CrtAsyncClient.getObject(downloadRequest.toApiRequest(),
                                       AsyncResponseTransformer.toFile(downloadRequest.destination()));
        return new DefaultDownload(future.thenApply(r -> DefaultCompletedDownload.builder().response(r).build()));
    }

    @Override
    public void close() {
        closables.forEach(SdkAutoCloseable::close);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private AsyncRequestBody requestBodyFor(UploadRequest uploadRequest) {
        return AsyncRequestBody.fromFile(uploadRequest.source());
    }

    private static class DefaultBuilder implements S3TransferManager.Builder {
        private S3CrtAsyncClient s3CrtAsyncClient;


        @Override
        public Builder s3CrtClient(S3CrtAsyncClient s3CrtAsyncClient) {
            this.s3CrtAsyncClient = s3CrtAsyncClient;
            return this;
        }

        @Override
        public S3TransferManager build() {
            return new DefaultS3TransferManager(this);
        }
    }
}
