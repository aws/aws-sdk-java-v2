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
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
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
    private final CrtErrorHandler crtErrorHandler;

    public DefaultS3CrtAsyncClient(DefaultS3CrtClientBuilder builder) {
        S3NativeClientConfiguration.Builder configBuilder =
            S3NativeClientConfiguration.builder()
                                       .targetThroughputInGbps(builder.targetThroughputInGbps())
                                       .partSizeInBytes(builder.minimumPartSizeInBytes())
                                       .maxConcurrency(builder.maxConcurrency)
                                       .credentialsProvider(builder.credentialsProvider)
                                       .asyncConfiguration(builder.asyncConfiguration);
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
        this.crtErrorHandler = new CrtErrorHandler();
    }

    @SdkTestInternalApi
    DefaultS3CrtAsyncClient(S3NativeClientConfiguration configuration,
                            S3NativeClient nativeClient) {
        this.configuration = configuration;
        this.s3NativeClient = nativeClient;
        this.crtErrorHandler = new CrtErrorHandler();

    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {

        CompletableFuture<ReturnT> returnFuture = new CompletableFuture<>();
        com.amazonaws.s3.model.GetObjectRequest crtGetObjectRequest = S3CrtPojoConversion.toCrtGetObjectRequest(getObjectRequest);
        CrtResponseDataConsumerAdapter<ReturnT> adapter = new CrtResponseDataConsumerAdapter<>(asyncResponseTransformer);

        CompletableFuture<ReturnT> adapterFuture = adapter.transformerFuture();

        CompletableFuture<GetObjectOutput> crtFuture = s3NativeClient.getObject(crtGetObjectRequest, adapter);

        // Forward the cancellation to crtFuture to cancel the request
        CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);


        // Forward the exception from the CRT future to the return future in case
        // the adapter callback didn't get it
        CompletableFutureUtils.forwardTransformedExceptionTo(crtFuture, returnFuture,
                t -> t instanceof Exception ? crtErrorHandler.transformException((Exception) t) :  t);

        returnFuture.whenComplete((r, t) -> {
            if (t == null) {
                returnFuture.complete(r);
            } else {
                returnFuture.completeExceptionally(t instanceof Exception
                        ? crtErrorHandler.transformException((Exception) t) : t);
            }
        });

        CompletableFutureUtils.forwardResultTo(adapterFuture, returnFuture, configuration.futureCompletionExecutor());

        return CompletableFutureUtils.forwardExceptionTo(returnFuture, adapterFuture);
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        CompletableFuture<PutObjectResponse> returnFuture = new CompletableFuture<>();

        com.amazonaws.s3.model.PutObjectRequest adaptedRequest = S3CrtPojoConversion.toCrtPutObjectRequest(putObjectRequest);

        if (adaptedRequest.contentLength() == null && requestBody.contentLength().isPresent()) {
            adaptedRequest = adaptedRequest.toBuilder()
                                           .contentLength(requestBody.contentLength().get())
                                           .build();
        }

        RequestDataSupplierAdapter requestDataSupplier = new RequestDataSupplierAdapter(requestBody);
        CompletableFuture<PutObjectOutput> crtFuture = s3NativeClient.putObject(adaptedRequest,
                                                                                requestDataSupplier);
        // Forward the cancellation to crtFuture to cancel the request
        CompletableFutureUtils.forwardExceptionTo(returnFuture, crtFuture);

        CompletableFuture<SdkHttpResponse> httpResponseFuture = requestDataSupplier.sdkHttpResponseFuture();
        CompletableFuture<PutObjectResponse> executeFuture =
            // If the header is not available, passing empty SDK HTTP response
            crtFuture.thenApply(putObjectOutput -> S3CrtPojoConversion.fromCrtPutObjectOutput(
                putObjectOutput, httpResponseFuture.getNow(SdkHttpResponse.builder().build())));

        executeFuture.whenComplete((r, t) -> {
            if (t == null) {
                returnFuture.complete(r);
            } else {
                returnFuture.completeExceptionally(t instanceof Exception
                        ? crtErrorHandler.transformException((Exception) t) : t);
            }
        });

        CompletableFutureUtils.forwardResultTo(executeFuture, returnFuture, configuration.futureCompletionExecutor());

        return CompletableFutureUtils.forwardExceptionTo(returnFuture, executeFuture);
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
        private ClientAsyncConfiguration asyncConfiguration;

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
        public S3CrtAsyncClientBuilder asyncConfiguration(ClientAsyncConfiguration configuration) {
            this.asyncConfiguration = configuration;
            return this;
        }

        @Override
        public S3CrtAsyncClient build() {
            return new DefaultS3CrtAsyncClient(this);
        }
    }
}
