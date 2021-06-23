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

import com.amazonaws.s3.S3NativeClient;
import com.amazonaws.s3.model.GetObjectOutput;
import com.amazonaws.s3.model.PutObjectOutput;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
public final class DefaultS3CrtAsyncClient implements S3CrtAsyncClient {
    private final S3NativeClient s3NativeClient;
    private final S3NativeClientConfiguration configuration;

    public DefaultS3CrtAsyncClient(DefaultS3CrtClientBuilder builder) {
        S3NativeClientConfiguration.Builder configBuilder =
            S3NativeClientConfiguration.builder()
                                       .targetThroughputInGbps(builder.targetThroughputInGbps())
                                       .partSizeInBytes(builder.minimumPartSizeInBytes())
                                       .maxConcurrency(builder.maxConcurrency)
                                       .credentialsProvider(builder.credentialsProvider);
        if (builder.region() != null) {
            configBuilder.signingRegion(builder.region().id());
        }

        configuration = configBuilder.build();

        this.s3NativeClient = new S3NativeClient(configuration.signingRegion(),
                                                 configuration.clientBootstrap(),
                                                 configuration.credentialsProvider(),
                                                 configuration.partSizeBytes(),
                                                 configuration.targetThroughputInGbps(),
                                                 configuration.maxConcurrency());
    }

    @SdkTestInternalApi
    DefaultS3CrtAsyncClient(S3NativeClientConfiguration configuration,
                            S3NativeClient nativeClient) {
        this.configuration = configuration;
        this.s3NativeClient = nativeClient;
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {

        CompletableFuture<ReturnT> future = new CompletableFuture<>();
        com.amazonaws.s3.model.GetObjectRequest crtGetObjectRequest = S3CrtPojoConversion.toCrtGetObjectRequest(getObjectRequest);
        CrtResponseDataConsumerAdapter<ReturnT> adapter = new CrtResponseDataConsumerAdapter<>(asyncResponseTransformer);

        CompletableFuture<ReturnT> adapterFuture = adapter.transformerFuture();

        CompletableFuture<GetObjectOutput> crtFuture = s3NativeClient.getObject(crtGetObjectRequest, adapter);
        CompletableFutureUtils.forwardExceptionTo(future, crtFuture);

        adapterFuture.whenComplete((r, t) -> {
            if (t == null) {
                future.complete(r);
            } else {
                future.completeExceptionally(t);
            }
            // TODO: Offload to future completion thread
        });

        return CompletableFutureUtils.forwardExceptionTo(future, adapterFuture);
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        com.amazonaws.s3.model.PutObjectRequest adaptedRequest = S3CrtPojoConversion.toCrtPutObjectRequest(putObjectRequest);

        if (adaptedRequest.contentLength() == null && requestBody.contentLength().isPresent()) {
            adaptedRequest = adaptedRequest.toBuilder()
                                           .contentLength(requestBody.contentLength().get())
                                           .build();
        }

        RequestDataSupplierAdapter requestDataSupplier = new RequestDataSupplierAdapter(requestBody);
        CompletableFuture<PutObjectOutput> putObjectOutputCompletableFuture = s3NativeClient.putObject(adaptedRequest,
                                                                                                       requestDataSupplier);

        CompletableFuture<SdkHttpResponse> httpResponseFuture = requestDataSupplier.sdkHttpResponseFuture();
        CompletableFuture<PutObjectResponse> executeFuture =
            httpResponseFuture.thenCombine(putObjectOutputCompletableFuture,
                                           (header, putObjectOutput) -> S3CrtPojoConversion.fromCrtPutObjectOutput(
                                               putObjectOutput, header));

        return CompletableFutureUtils.forwardExceptionTo(executeFuture, putObjectOutputCompletableFuture);
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

    public static final class DefaultS3CrtClientBuilder implements S3CrtAsyncClientBuilder {
        private AwsCredentialsProvider credentialsProvider;
        private Region region;
        private Long minimalPartSizeInBytes;
        private Double targetThroughputInGbps;
        private Integer maxConcurrency;

        public AwsCredentialsProvider credentialsProvider() {
            return credentialsProvider;
        }

        public Region region() {
            return region;
        }

        public Long minimumPartSizeInBytes() {
            return minimalPartSizeInBytes;
        }

        public Double targetThroughputInGbps() {
            return targetThroughputInGbps;
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
            this.minimalPartSizeInBytes = partSizeBytes;
            return this;
        }

        @Override
        public S3CrtAsyncClientBuilder targetThroughputInGbps(Double targetThroughputInGbps) {
            this.targetThroughputInGbps = targetThroughputInGbps;
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
