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

import static software.amazon.awssdk.transfer.s3.internal.S3InternalSdkHttpExecutionAttribute.OPERATION_NAME;
import static software.amazon.awssdk.utils.CollectionUtils.isNullOrEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.s3.S3Client;
import software.amazon.awssdk.crt.s3.S3ClientOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An implementation of {@link SdkAsyncHttpClient} that uses an CRT S3 HTTP client {@link S3Client} to communicate with S3.
 * Note that it does not work with other services
 */
@SdkInternalApi
public final class S3CrtAsyncHttpClient implements SdkAsyncHttpClient {

    private final S3Client crtS3Client;
    private final S3NativeClientConfiguration s3NativeClientConfiguration;

    private S3CrtAsyncHttpClient(Builder builder) {
        s3NativeClientConfiguration =
            S3NativeClientConfiguration.builder()
                                       .targetThroughputInGbps(builder.targetThroughputInGbps)
                                       .partSizeInBytes(builder.minimalPartSizeInBytes)
                                       .maxConcurrency(builder.maxConcurrency)
                                       .signingRegion(builder.region == null ? null : builder.region.id())
                                       .credentialsProvider(builder.credentialsProvider)
                                       .build();

        S3ClientOptions s3ClientOptions =
            new S3ClientOptions().withRegion(s3NativeClientConfiguration.signingRegion())
                                 .withCredentialsProvider(s3NativeClientConfiguration.credentialsProvider())
                                 .withClientBootstrap(s3NativeClientConfiguration.clientBootstrap())
                                 .withPartSize(s3NativeClientConfiguration.partSizeBytes())
                                 .withThroughputTargetGbps(s3NativeClientConfiguration.targetThroughputInGbps());
        this.crtS3Client = new S3Client(s3ClientOptions);
    }

    @SdkTestInternalApi
    S3CrtAsyncHttpClient(S3Client crtS3Client, S3NativeClientConfiguration nativeClientConfiguration) {
        this.crtS3Client = crtS3Client;
        this.s3NativeClientConfiguration = nativeClientConfiguration;
    }

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        HttpRequest httpRequest = toCrtRequest(request);
        S3CrtResponseHandlerAdapter responseHandler = new S3CrtResponseHandlerAdapter(executeFuture, request.responseHandler());

        S3MetaRequestOptions.MetaRequestType requestType = requestType(request);

        S3MetaRequestOptions requestOptions = new S3MetaRequestOptions()
            .withHttpRequest(httpRequest)
            .withMetaRequestType(requestType)
            .withResponseHandler(responseHandler);

        try (S3MetaRequest s3MetaRequest = crtS3Client.makeMetaRequest(requestOptions)) {
            closeResourcesWhenComplete(executeFuture, s3MetaRequest);
        }

        return executeFuture;
    }

    @Override
    public String clientName() {
        return "s3crt";
    }

    private static S3MetaRequestOptions.MetaRequestType requestType(AsyncExecuteRequest request) {
        String operationName = request.httpExecutionAttributes().getAttribute(OPERATION_NAME);
        if (operationName != null) {
            switch (operationName) {
                case "GetObject":
                    return S3MetaRequestOptions.MetaRequestType.GET_OBJECT;
                case "PutObject":
                    return S3MetaRequestOptions.MetaRequestType.PUT_OBJECT;
                default:
                    return S3MetaRequestOptions.MetaRequestType.DEFAULT;
            }
        }
        return S3MetaRequestOptions.MetaRequestType.DEFAULT;
    }

    private static void closeResourcesWhenComplete(CompletableFuture<Void> executeFuture,
                                                   S3MetaRequest s3MetaRequest) {
        executeFuture.whenComplete((r, t) -> {
            if (executeFuture.isCancelled()) {
                s3MetaRequest.cancel();
            }

            s3MetaRequest.close();
        });
    }

    private static HttpRequest toCrtRequest(AsyncExecuteRequest asyncRequest) {
        URI uri = asyncRequest.request().getUri();
        SdkHttpRequest sdkRequest = asyncRequest.request();

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        if (encodedPath == null || encodedPath.isEmpty()) {
            encodedPath = "/";
        }

        String encodedQueryString = SdkHttpUtils.encodeAndFlattenQueryParameters(sdkRequest.rawQueryParameters())
                                                .map(value -> "?" + value)
                                                .orElse("");

        HttpHeader[] crtHeaderArray = createHttpHeaderList(uri, asyncRequest).toArray(new HttpHeader[0]);

        S3CrtRequestBodyStreamAdapter sdkToCrtRequestPublisher =
            new S3CrtRequestBodyStreamAdapter(asyncRequest.requestContentPublisher());

        return new HttpRequest(method, encodedPath + encodedQueryString, crtHeaderArray, sdkToCrtRequestPublisher);
    }

    @Override
    public void close() {
        s3NativeClientConfiguration.close();
        crtS3Client.close();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements SdkAsyncHttpClient.Builder<S3CrtAsyncHttpClient.Builder> {
        private AwsCredentialsProvider credentialsProvider;
        private Region region;
        private Long minimalPartSizeInBytes;
        private Double targetThroughputInGbps;
        private Integer maxConcurrency;

        /**
         * Configure the credentials that should be used to authenticate with S3.
         */
        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        /**
         * Configure the region with which the SDK should communicate.
         */
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the minimum part size for transfer parts. Decreasing the minimum part size causes
         * multipart transfer to be split into a larger number of smaller parts. Setting this value too low
         * has a negative effect on transfer speeds, causing extra latency and network communication for each part.
         */
        public Builder minimumPartSizeInBytes(Long partSizeBytes) {
            this.minimalPartSizeInBytes = partSizeBytes;
            return this;
        }

        /**
         * The target throughput for transfer requests. Higher value means more S3 connections
         * will be opened. Whether the transfer manager can achieve the configured target throughput depends
         * on various factors such as the network bandwidth of the environment and the configured {@link #maxConcurrency}.
         */
        public Builder targetThroughputInGbps(Double targetThroughputInGbps) {
            this.targetThroughputInGbps = targetThroughputInGbps;
            return this;
        }

        /**
         * Specifies the maximum number of S3 connections that should be established during
         * a transfer.
         *
         * <p>
         * If not provided, the TransferManager will calculate the optional number of connections
         * based on {@link #targetThroughputInGbps}. If the value is too low, the S3TransferManager
         * might not achieve the specified target throughput.
         *
         * @param maxConcurrency the max number of concurrent requests
         * @return this builder for method chaining.
         * @see #targetThroughputInGbps(Double)
         */
        public Builder maxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        @Override
        public SdkAsyncHttpClient build() {
            return new S3CrtAsyncHttpClient(this);
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
            // Intentionally ignore serviceDefaults
            return build();
        }
    }

    private static List<HttpHeader> createHttpHeaderList(URI uri, AsyncExecuteRequest asyncRequest) {
        SdkHttpRequest sdkRequest = asyncRequest.request();
        List<HttpHeader> crtHeaderList = new ArrayList<>();

        // Set Host Header if needed
        if (isNullOrEmpty(sdkRequest.headers().get(Header.HOST))) {
            crtHeaderList.add(new HttpHeader(Header.HOST, uri.getHost()));
        }

        // Set Content-Length if needed
        Optional<Long> contentLength = asyncRequest.requestContentPublisher().contentLength();
        if (isNullOrEmpty(sdkRequest.headers().get(Header.CONTENT_LENGTH)) && contentLength.isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength.get())));
        }

        // Add the rest of the Headers
        sdkRequest.headers().forEach((key, value) -> value.stream().map(val -> new HttpHeader(key, val))
                                                          .forEach(crtHeaderList::add));

        return crtHeaderList;
    }
}
