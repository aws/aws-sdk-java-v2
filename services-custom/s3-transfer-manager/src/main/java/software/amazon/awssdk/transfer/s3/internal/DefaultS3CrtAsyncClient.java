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

import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SDK_HTTP_EXECUTION_ATTRIBUTES;
import static software.amazon.awssdk.transfer.s3.internal.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SdkInternalApi
public final class DefaultS3CrtAsyncClient implements S3CrtAsyncClient {
    private final SdkAsyncHttpClient s3CrtAsyncHttpClient;
    private final S3AsyncClient s3AsyncClient;

    private DefaultS3CrtAsyncClient(DefaultS3CrtClientBuilder builder) {
        this.s3CrtAsyncHttpClient = S3CrtAsyncHttpClient.builder()
                                                        .targetThroughputInGbps(builder.targetThroughputInGbps())
                                                        .minimumPartSizeInBytes(builder.minimumPartSizeInBytes())
                                                        .maxConcurrency(builder.maxConcurrency)
                                                        .region(builder.region)
                                                        .credentialsProvider(builder.credentialsProvider)
                                                        .build();

        this.s3AsyncClient = initializeS3AsyncClient(builder);
    }

    @SdkTestInternalApi
    DefaultS3CrtAsyncClient(SdkAsyncHttpClient s3CrtAsyncHttpClient,
                            S3AsyncClient s3AsyncClient) {
        this.s3CrtAsyncHttpClient = s3CrtAsyncHttpClient;

        this.s3AsyncClient = s3AsyncClient;
    }

    private S3AsyncClient initializeS3AsyncClient(DefaultS3CrtClientBuilder builder) {
        return S3AsyncClient.builder()
                            // Disable checksum, retry policy and signer because they are handled in crt
                            .serviceConfiguration(S3Configuration.builder()
                                                                 .checksumValidationEnabled(false)
                                                                 .build())
                            .region(builder.region)
                            .credentialsProvider(builder.credentialsProvider)
                            .overrideConfiguration(o -> o.putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                            new NoOpSigner())
                                                         .retryPolicy(RetryPolicy.none())
                                                         .addExecutionInterceptor(new AttachHttpAttributesExecutionInterceptor()))
                            .httpClient(s3CrtAsyncHttpClient)
                            .build();
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest getObjectRequest, AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {
        validateOverrideConfiguration(getObjectRequest);
        return s3AsyncClient.getObject(getObjectRequest, asyncResponseTransformer);
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        validateOverrideConfiguration(putObjectRequest);
        return s3AsyncClient.putObject(putObjectRequest, requestBody);
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public void close() {
        s3CrtAsyncHttpClient.close();
        s3AsyncClient.close();
    }

    private static void validateOverrideConfiguration(AwsRequest request) {

        if (request.overrideConfiguration().isPresent()) {
            AwsRequestOverrideConfiguration overrideConfiguration = request.overrideConfiguration().get();
            if (overrideConfiguration.signer().isPresent()) {
                throw new UnsupportedOperationException("Request-level signer override is not supported");
            }

            // TODO: support request-level credential override
            if (overrideConfiguration.credentialsProvider().isPresent()) {
                throw new UnsupportedOperationException("Request-level credentials override is not supported");
            }
        }
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

    private static final class AttachHttpAttributesExecutionInterceptor implements ExecutionInterceptor {
        @Override
        public void afterMarshalling(Context.AfterMarshalling context,
                                     ExecutionAttributes executionAttributes) {
            SdkHttpExecutionAttributes attributes =
                SdkHttpExecutionAttributes.builder()
                                          .put(OPERATION_NAME,
                                               executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME))
                                          .build();

            executionAttributes.putAttribute(SDK_HTTP_EXECUTION_ATTRIBUTES,
                                             attributes);
        }
    }
}
