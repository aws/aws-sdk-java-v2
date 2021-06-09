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


import com.amazonaws.s3.RequestDataSupplier;
import com.amazonaws.s3.S3NativeClient;
import com.amazonaws.s3.model.PutObjectOutput;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SdkInternalApi
public final class DefaultS3CrtAsyncClient implements S3CrtAsyncClient {
    private final S3NativeClient s3NativeClient;
    private final S3NativeClientConfiguration configuration;

    public DefaultS3CrtAsyncClient(DefaultS3CrtClientBuilder builder) {
        S3NativeClientConfiguration.Builder configBuilder =
            S3NativeClientConfiguration.builder()
                                       .targetThroughputGbps(builder.targetThroughputGbps())
                                       .partSizeBytes(builder.partSizeBytes());
        if (builder.region() != null) {
            configBuilder.signingRegion(builder.region().id());
        }

        if (builder.credentialsProvider() != null) {
            configBuilder.credentialsProvider(S3CrtUtils.createCrtCredentialsProvider(builder.credentialsProvider()));
        }
        configuration = configBuilder.build();

        this.s3NativeClient = new S3NativeClient(configuration.signingRegion(),
                                                 configuration.clientBootstrap(),
                                                 configuration.credentialsProvider(),
                                                 configuration.partSizeBytes(),
                                                 configuration.targetThroughputGbps(),
                                                 configuration.maxConcurrency());
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {

        CompletableFuture<ReturnT> future = new CompletableFuture<>();
        com.amazonaws.s3.model.GetObjectRequest crtGetObjectRequest = S3CrtUtils.toCrtGetObjectRequest(getObjectRequest);
        CrtResponseDataConsumerAdapter<ReturnT> adapter = new CrtResponseDataConsumerAdapter<>(asyncResponseTransformer);

        CompletableFuture<ReturnT> adapterFuture = adapter.transformerFuture();

        s3NativeClient.getObject(crtGetObjectRequest, adapter);

        adapterFuture.whenComplete((r, t) -> {
            if (t == null) {
                future.complete(r);
            } else {
                future.completeExceptionally(t);
            }
            // TODO: Offload to future completion thread
        });

        return future;
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        com.amazonaws.s3.model.PutObjectRequest adaptedRequest = S3CrtUtils.toCrtPutObjectRequest(putObjectRequest);

        if (adaptedRequest.contentLength() == null && requestBody.contentLength().isPresent()) {
            adaptedRequest = adaptedRequest.toBuilder().contentLength(requestBody.contentLength().get())
                    .build();
        }

        CompletableFuture<PutObjectOutput> putObjectOutputCompletableFuture = s3NativeClient.putObject(adaptedRequest,
                adaptToDataSupplier(requestBody));

        return putObjectOutputCompletableFuture.thenApply(S3CrtUtils::fromCrtPutObjectOutput);
    }

    @Override
    public String serviceName() {
        return "s3";
    }

    @Override
    public void close() {
        s3NativeClient.close();
        configuration.close();
    }

    private static RequestDataSupplier adaptToDataSupplier(AsyncRequestBody requestBody) {
        return new RequestDataSupplierAdapter(requestBody);
    }

    public static final class DefaultS3CrtClientBuilder implements S3CrtAsyncClientBuilder {
        private AwsCredentialsProvider credentialsProvider;
        private Region region;
        private Long partSizeBytes;
        private Double targetThroughputGbps;
        private Integer maxConcurrency;

        public AwsCredentialsProvider credentialsProvider() {
            return credentialsProvider;
        }

        public Region region() {
            return region;
        }

        public Long partSizeBytes() {
            return partSizeBytes;
        }

        public Double targetThroughputGbps() {
            return targetThroughputGbps;
        }

        public Integer maxConcurrency() {
            return maxConcurrency;
        }

        @Override
        public S3CrtAsyncClientBuilder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder region(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder minimumPartSizeInBytes(Long partSizeBytes) {
            this.partSizeBytes = partSizeBytes;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder targetThroughputGbps(Double targetThroughputGbps) {
            this.targetThroughputGbps = targetThroughputGbps;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        @Override
        public S3CrtAsyncClient build() {
            return new DefaultS3CrtAsyncClient(this);
        }
    }
}
