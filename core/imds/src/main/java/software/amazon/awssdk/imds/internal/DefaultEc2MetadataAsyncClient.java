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
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.imds.MetadataResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
@Immutable
@ThreadSafe
public final class DefaultEc2MetadataAsyncClient implements Ec2MetadataAsyncClient {

    private static final Logger log = Logger.loggerFor(DefaultEc2MetadataClient.class);
    private static final int DEFAULT_RETRY_THREAD_POOL_SIZE = 3;

    private final Ec2MetadataRetryPolicy retryPolicy;
    private final SdkAsyncHttpClient httpClient;
    private final EndpointMode endpointMode;
    private final URI endpoint;
    private final RequestMarshaller requestMarshaller;
    private final Duration tokenTtl;
    private final ScheduledExecutorService asyncRetryScheduler;

    private DefaultEc2MetadataAsyncClient(Ec2MetadataAsyncBuilder builder) {
        this.retryPolicy = builder.retryPolicy != null ? builder.retryPolicy
                                                       : Ec2MetadataRetryPolicy.builder().build();
        this.httpClient = builder.httpClient != null ? builder.httpClient
                                                     : AwsCrtAsyncHttpClient.create();
        this.endpointMode = builder.endpointMode != null ? builder.endpointMode
                                                         : DEFAULT_ENDPOINT_PROVIDER.resolveEndpointMode();
        this.endpoint = builder.endpoint != null ? builder.endpoint
                                                 : URI.create(DEFAULT_ENDPOINT_PROVIDER.resolveEndpoint(this.endpointMode));
        this.requestMarshaller = new RequestMarshaller(this.endpoint);
        this.tokenTtl = builder.tokenTtl != null ? builder.tokenTtl
                                                 : Duration.ofSeconds(21600);
        this.asyncRetryScheduler = builder.scheduledExecutorService != null
                                   ? builder.scheduledExecutorService
                                   : Executors.newScheduledThreadPool(DEFAULT_RETRY_THREAD_POOL_SIZE);
    }

    public static Ec2MetadataAsyncClient.Builder builder() {
        return new DefaultEc2MetadataAsyncClient.Ec2MetadataAsyncBuilder();
    }

    @Override
    public CompletableFuture<MetadataResponse> get(String path) {
        return get(path, RetryPolicyContext.builder().retriesAttempted(0).build());
    }

    private CompletableFuture<MetadataResponse> get(String path, RetryPolicyContext retryPolicyContext) {
        SdkHttpFullRequest baseTokenRequest = requestMarshaller.createTokenRequest(tokenTtl);

        CompletableFuture<String> tokenRequest = sendAsyncRequest(baseTokenRequest);
        CompletableFuture<MetadataResponse> result = tokenRequest
            .thenCompose(token -> {
                SdkHttpFullRequest baseMetadataRequest = requestMarshaller.createDataRequest(path, token, tokenTtl);
                return sendAsyncRequest(baseMetadataRequest);
            }).thenApply(MetadataResponse::create);

        return result.handle((response, error) -> {
            if (response != null || !shouldRetry(retryPolicyContext, error)) {
                return result;
            }
            int newAttempt = retryPolicyContext.retriesAttempted() + 1;
            log.debug(() -> "Retrying request: Attempt " + newAttempt);
            RetryPolicyContext newContext =
                RetryPolicyContext.builder()
                                  .request(baseTokenRequest)
                                  .retriesAttempted(newAttempt)
                                  .exception(SdkClientException.create(error.getMessage(), error))
                                  .build();
            return scheduledRetryAttempt(() -> get(path, newContext), newContext);
        }).thenCompose(Function.identity()); // only java 12 has .exceptionallyCompose()
    }

    private CompletableFuture<String> sendAsyncRequest(SdkHttpFullRequest baseRequest) {
        StringAsyncResponseHandler responseHandler = new StringAsyncResponseHandler();
        SdkHttpContentPublisher requestContentPublisher = new SimpleHttpContentPublisher(baseRequest);
        AsyncExecuteRequest metadataRequest = AsyncExecuteRequest.builder()
                                                                 .request(baseRequest)
                                                                 .requestContentPublisher(requestContentPublisher)
                                                                 .responseHandler(responseHandler)
                                                                 .build();
        httpClient.execute(metadataRequest);
        return responseHandler.getResult();
    }

    private CompletableFuture<MetadataResponse> scheduledRetryAttempt(Supplier<CompletableFuture<MetadataResponse>> supplier,
                                                                      RetryPolicyContext retryPolicyContext) {
        Duration retryDelay = retryPolicy.backoffStrategy().computeDelayBeforeNextRetry(retryPolicyContext);
        Executor retryExecutor = retryAttempt -> asyncRetryScheduler.schedule(retryAttempt, retryDelay.toMillis(),
                                                                              TimeUnit.MILLISECONDS);
        return CompletableFuture.supplyAsync(supplier, retryExecutor).thenCompose(Function.identity());
    }

    private boolean shouldRetry(RetryPolicyContext retryPolicyContext, Throwable error) {
        boolean maxAttemptReached = retryPolicyContext.retriesAttempted() >= retryPolicy.numRetries();
        if (maxAttemptReached) {
            return false;
        }
        return error instanceof RetryableException || error.getCause() instanceof RetryableException;
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

    private static final class StringResponseHandler implements HttpResponseHandler<String> {
        @Override
        public String handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
            return IoUtils.toUtf8String(response.content().orElseThrow(() ->
                SdkClientException.create("Unexpected error: empty response content")));
        }
    }

    private static final class StringAsyncResponseHandler implements TransformingAsyncResponseHandler<String> {

        private final AsyncResponseHandler<String> delegate;
        private final CompletableFuture<String> future;

        private StringAsyncResponseHandler() {
            this.delegate = new AsyncResponseHandler<>(new StringResponseHandler(),
                                                       Function.identity(),
                                                       new ExecutionAttributes());
            this.future = prepare();
        }

        public CompletableFuture<String> getResult() {
            return this.future;
        }

        @Override
        public CompletableFuture<String> prepare() {
            return delegate.prepare();
        }

        @Override
        public void onHeaders(SdkHttpResponse headers) {
            delegate.onHeaders(headers);
            HttpStatusFamily statusCode = HttpStatusFamily.of(headers.statusCode());
            if (statusCode.isOneOf(HttpStatusFamily.CLIENT_ERROR)) {
                // non-retryable error
                log.debug(() -> String.format("Error while executing EC2Metadata request: received http status %d",
                                              headers.statusCode()));
                future.completeExceptionally(SdkClientException.create("Error: Status code " + statusCode));
            } else if (statusCode.isOneOf(HttpStatusFamily.SERVER_ERROR)) {
                // retryable error
                log.debug(() -> String.format("Error while executing EC2Metadata request: received http status %d",
                                              headers.statusCode()));
                future.completeExceptionally(RetryableException.create("Error: Status code " + statusCode));
            }
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
        }

        @Override
        public void onError(Throwable error) {
            log.debug(() -> String.format("Error while executing EC2Metadata request: %s", error.getMessage()));
            future.completeExceptionally(RetryableException.create(error.getMessage(), error));
        }
    }

    private static final class Ec2MetadataAsyncBuilder implements Ec2MetadataAsyncClient.Builder {

        private Ec2MetadataRetryPolicy retryPolicy;

        private URI endpoint;

        private Duration tokenTtl;

        private EndpointMode endpointMode;

        private SdkAsyncHttpClient httpClient;

        private ScheduledExecutorService scheduledExecutorService;

        private Ec2MetadataAsyncBuilder() {
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
            this.httpClient = httpClient;
            return this;
        }

        @Override
        public Builder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
            return this;
        }

        @Override
        public Ec2MetadataAsyncClient build() {
            return new DefaultEc2MetadataAsyncClient(this);
        }
    }
}
