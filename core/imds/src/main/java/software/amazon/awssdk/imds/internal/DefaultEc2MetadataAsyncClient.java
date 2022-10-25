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

package software.amazon.awssdk.imds.internal;

import static software.amazon.awssdk.imds.internal.Ec2MetadataEndpointProvider.DEFAULT_ENDPOINT_PROVIDER;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.imds.MetadataResponse;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
@Immutable
@ThreadSafe
public final class DefaultEc2MetadataAsyncClient implements Ec2MetadataAsyncClient {

    private static final Logger log = Logger.loggerFor(DefaultEc2Metadata.class);

    private final Ec2MetadataRetryPolicy retryPolicy;
    private final SdkAsyncHttpClient httpClient;
    private final RequestMarshaller requestMarshaller;
    private final URI endpoint;
    private final Duration tokenTtl;
    private final EndpointMode endpointMode;

    private DefaultEc2MetadataAsyncClient(Ec2MetadataAsyncBuilder builder) {
        this.retryPolicy = builder.retryPolicy != null ? builder.retryPolicy
                                                       : Ec2MetadataRetryPolicy.builder().build();
        this.endpointMode = builder.endpointMode != null ? builder.endpointMode
                                                         : DEFAULT_ENDPOINT_PROVIDER.resolveEndpointMode();
        this.endpoint = builder.endpoint != null ? builder.endpoint
                                                 : URI.create(DEFAULT_ENDPOINT_PROVIDER.resolveEndpoint(this.endpointMode));
        this.tokenTtl = builder.tokenTtl != null ? builder.tokenTtl
                                                 : Duration.ofSeconds(21600);
        this.requestMarshaller = new RequestMarshaller(this.endpoint);

        this.httpClient = builder.httpClient != null ? builder.httpClient
                                                     : AwsCrtAsyncHttpClient.create();
    }

    public static Ec2MetadataAsyncClient.Builder builder() {
        return new DefaultEc2MetadataAsyncClient.Ec2MetadataAsyncBuilder();
    }

    @Override
    public CompletableFuture<MetadataResponse> get(String path) {
        return get(path, RetryPolicyContext.builder().retriesAttempted(0).build());
    }

    public CompletableFuture<MetadataResponse> get(String path, RetryPolicyContext retryPolicyContext) {
        log.error(() -> String.format("Attempt %d", retryPolicyContext.retriesAttempted()));
        CompletableFuture<String> tokenFuture = new CompletableFuture<>();
        SdkHttpFullRequest baseTokenRequest = requestMarshaller.createTokenRequest(tokenTtl);
        AsyncExecuteRequest tokenRequest =
            AsyncExecuteRequest.builder()
                               .request(baseTokenRequest)
                               .requestContentPublisher(new SimpleHttpContentPublisher(baseTokenRequest))
                               .responseHandler(new StringResponseHandler(tokenFuture))
                               .build();
        httpClient.execute(tokenRequest);
        CompletableFuture<MetadataResponse> result = tokenFuture.thenComposeAsync(token -> sendRequest(path, token));
        return result.handleAsync((response, error) -> {
            if (!shouldRetry(retryPolicyContext, response, error)) {
                return result;
            }
            RetryPolicyContext newContext =
                RetryPolicyContext.builder()
                                  .request(baseTokenRequest)
                                  .retriesAttempted(retryPolicyContext.retriesAttempted() + 1)
                                  .exception(SdkClientException.create(error.getMessage(), error))
                                  .build();
            return get(path, newContext);
        }).thenComposeAsync(Function.identity()); // only java 12 has .exceptionallyCompose()
    }

    // todo encapsulate this logic so it can be reused by both the sync and async client
    private boolean shouldRetry(RetryPolicyContext retryPolicyContext, MetadataResponse response, Throwable error) {
        if (response != null) {
            return false;
        }
        boolean maxAttemptReached = retryPolicyContext.retriesAttempted() >= retryPolicy.numRetries();
        if (maxAttemptReached) {
            return false;
        }
        return error instanceof RetryableException || error.getCause() instanceof RetryableException;
    }

    private CompletableFuture<MetadataResponse> sendRequest(String path, String token) {
        CompletableFuture<String> metadataFuture = new CompletableFuture<>();
        SdkHttpFullRequest baseMetadataRequest = requestMarshaller.createDataRequest(path, token, tokenTtl);
        AsyncExecuteRequest metadataRequest =
            AsyncExecuteRequest.builder()
                               .request(baseMetadataRequest)
                               .requestContentPublisher(new SimpleHttpContentPublisher(baseMetadataRequest))
                               .responseHandler(new StringResponseHandler(metadataFuture))
                               .build();
        httpClient.execute(metadataRequest);
        return metadataFuture.thenApply(MetadataResponse::create);
    }

    @Override
    public Builder toBuilder() {
        return builder()
            .endpoint(this.endpoint)
            .endpointMode(this.endpointMode)
            .httpClient(this.httpClient)
            .retryPolicy(this.retryPolicy)
            .tokenTtl(this.tokenTtl);
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private static final class StringResponseHandler implements SdkAsyncHttpResponseHandler {

        private final CompletableFuture<String> future;

        private StringResponseHandler(CompletableFuture<String> future) {
            this.future = future;
        }

        @Override
        public void onHeaders(SdkHttpResponse headers) {
            HttpStatusFamily statusCode = HttpStatusFamily.of(headers.statusCode());
            if (statusCode.isOneOf(HttpStatusFamily.CLIENT_ERROR)) {
                // non-retryable error
                log.debug(() -> String.format("Error while executing EC2Metadata request, received status %d",
                                              headers.statusCode()));
                future.completeExceptionally(SdkClientException.create("Error: Status code " + statusCode));
            }
            if (statusCode.isOneOf(HttpStatusFamily.SERVER_ERROR)) {
                // retryable error
                log.debug(() -> String.format("Error while executing EC2Metadata request, received status %d",
                                              headers.statusCode()));
                future.completeExceptionally(RetryableException.create("Error: Status code " + statusCode));
            }
        }

        @Override
        public void onError(Throwable error) {
            log.debug(() -> String.format("Error while executing EC2Metadata request: %s", error.getMessage()));
            future.completeExceptionally(RetryableException.create(error.getMessage(), error));
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(b -> future.complete(new String(b.array(), StandardCharsets.UTF_8))));
        }
    }

    private static final class Ec2MetadataAsyncBuilder implements Ec2MetadataAsyncClient.Builder {

        private Ec2MetadataRetryPolicy retryPolicy;

        private URI endpoint;

        private Duration tokenTtl;

        private EndpointMode endpointMode;

        private SdkAsyncHttpClient httpClient;

        private Ec2MetadataAsyncBuilder() {
        }

        public void setRetryPolicy(Ec2MetadataRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
        }

        public void setEndpoint(URI endpoint) {
            this.endpoint = endpoint;
        }

        public void setTokenTtl(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
        }

        public void setEndpointMode(EndpointMode endpointMode) {
            this.endpointMode = endpointMode;
        }

        public void setHttpClient(SdkAsyncHttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        public Builder retryPolicy(Ec2MetadataRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        @Override
        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public Builder tokenTtl(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
            return this;
        }

        @Override
        public Builder endpointMode(EndpointMode endpointMode) {
            this.endpointMode = endpointMode;
            return this;
        }

        @Override
        public Builder httpClient(SdkAsyncHttpClient httpClient) {
            return null;
        }

        @Override
        public Ec2MetadataAsyncClient build() {
            return new DefaultEc2MetadataAsyncClient(this);
        }
    }
}
