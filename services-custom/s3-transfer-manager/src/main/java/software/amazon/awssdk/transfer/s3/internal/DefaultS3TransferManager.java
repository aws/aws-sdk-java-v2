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

import java.nio.file.Files;
import java.nio.file.Path;
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
import software.amazon.awssdk.transfer.s3.UploadDirectory;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
public final class DefaultS3TransferManager implements S3TransferManager {
    private final S3CrtAsyncClient s3CrtAsyncClient;
    private final TransferConfiguration transferConfiguration;
    private final UploadDirectoryManager uploadDirectoryManager;

    public DefaultS3TransferManager(DefaultBuilder tmBuilder) {
        S3CrtAsyncClient.S3CrtAsyncClientBuilder clientBuilder = S3CrtAsyncClient.builder();
        TransferConfiguration.Builder transferConfigBuilder = TransferConfiguration.builder();
        if (tmBuilder.s3ClientConfiguration != null) {
            tmBuilder.s3ClientConfiguration.credentialsProvider().ifPresent(clientBuilder::credentialsProvider);
            tmBuilder.s3ClientConfiguration.maxConcurrency().ifPresent(clientBuilder::maxConcurrency);
            tmBuilder.s3ClientConfiguration.minimumPartSizeInBytes().ifPresent(clientBuilder::minimumPartSizeInBytes);
            tmBuilder.s3ClientConfiguration.region().ifPresent(clientBuilder::region);
            tmBuilder.s3ClientConfiguration.targetThroughputInGbps().ifPresent(clientBuilder::targetThroughputInGbps);
            tmBuilder.s3ClientConfiguration.asyncConfiguration().ifPresent(clientBuilder::asyncConfiguration);

            tmBuilder.s3ClientConfiguration.uploadDirectoryConfiguration().ifPresent(transferConfigBuilder::configuration);
        }

        s3CrtAsyncClient = clientBuilder.build();
        transferConfiguration = transferConfigBuilder.build();
        uploadDirectoryManager = new UploadDirectoryManager(transferConfiguration, this::upload);
    }

    @SdkTestInternalApi
    DefaultS3TransferManager(S3CrtAsyncClient s3CrtAsyncClient, UploadDirectoryManager uploadDirectoryManager) {
        this.s3CrtAsyncClient = s3CrtAsyncClient;
        this.transferConfiguration = TransferConfiguration.builder().build();
        this.uploadDirectoryManager = uploadDirectoryManager;
    }

    @Override
    public Upload upload(UploadRequest uploadRequest) {
        try {
            assertNotObjectLambdaArn(uploadRequest.putObjectRequest().bucket(), "upload");

            PutObjectRequest putObjectRequest = uploadRequest.putObjectRequest();
            AsyncRequestBody requestBody = requestBodyFor(uploadRequest);

            CompletableFuture<PutObjectResponse> putObjFuture = s3CrtAsyncClient.putObject(putObjectRequest, requestBody);

            CompletableFuture<CompletedUpload> future = putObjFuture.thenApply(r -> DefaultCompletedUpload.builder()
                                                                                                          .response(r)
                                                                                                          .build());
            return new DefaultUpload(CompletableFutureUtils.forwardExceptionTo(future, putObjFuture));
        } catch (Throwable throwable) {
            return new DefaultUpload(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public UploadDirectory uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        try {
            Path directory = uploadDirectoryRequest.sourceDirectory();

            if (!Files.exists(directory) || Files.isRegularFile(directory)) {
                throw new IllegalArgumentException("The source directory provided either does not exist or is not a directory");
            }
            assertNotObjectLambdaArn(uploadDirectoryRequest.bucket(), "downloadDirectory");

            return uploadDirectoryManager.uploadDirectory(uploadDirectoryRequest);
        } catch (Throwable throwable) {
            return new DefaultUploadDirectory(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public Download download(DownloadRequest downloadRequest) {
        try {
            assertNotObjectLambdaArn(downloadRequest.getObjectRequest().bucket(), "download");

            CompletableFuture<GetObjectResponse> getObjectFuture =
                s3CrtAsyncClient.getObject(downloadRequest.getObjectRequest(),
                                           AsyncResponseTransformer.toFile(downloadRequest.destination()));
            CompletableFuture<CompletedDownload> future =
                getObjectFuture.thenApply(r -> DefaultCompletedDownload.builder().response(r).build());

            return new DefaultDownload(CompletableFutureUtils.forwardExceptionTo(future, getObjectFuture));
        } catch (Throwable throwable) {
            return new DefaultDownload(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public void close() {
        s3CrtAsyncClient.close();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private static void assertNotObjectLambdaArn(String arn, String operation) {
        if (isObjectLambdaArn(arn)) {
            String error = String.format("%s does not support S3 Object Lambda resources", operation);
            throw new IllegalArgumentException(error);
        }
    }

    private static boolean isObjectLambdaArn(String arn) {
        if (arn == null) {
            return false;
        }

        return arn.startsWith("arn:") && arn.contains(":s3-object-lambda");
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
