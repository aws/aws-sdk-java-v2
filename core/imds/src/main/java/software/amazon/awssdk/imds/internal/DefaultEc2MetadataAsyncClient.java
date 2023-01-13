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

import static software.amazon.awssdk.imds.internal.AsyncHttpRequestHelper.sendAsyncTokenRequest;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkAsyncHttpClientBuilder;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
@Immutable
@ThreadSafe
public final class DefaultEc2MetadataAsyncClient extends BaseEc2MetadataClient implements Ec2MetadataAsyncClient {

    private static final Logger log = Logger.loggerFor(DefaultEc2MetadataClient.class);
    private static final int DEFAULT_RETRY_THREAD_POOL_SIZE = 3;

    private final SdkAsyncHttpClient httpClient;
    private final ScheduledExecutorService asyncRetryScheduler;
    private final boolean httpClientIsInternal;
    private final boolean retryExecutorIsInternal;
    private final AsyncTokenCache tokenCache;

    private DefaultEc2MetadataAsyncClient(Ec2MetadataAsyncBuilder builder) {
        super(builder);

        Validate.isTrue(builder.httpClient == null || builder.httpClientBuilder == null,
                        "The httpClient and the httpClientBuilder can't both be configured.");
        this.httpClient = Either
            .fromNullable(builder.httpClient, builder.httpClientBuilder)
            .map(e -> e.map(Function.identity(), SdkAsyncHttpClient.Builder::build))
            .orElseGet(() -> new DefaultSdkAsyncHttpClientBuilder().buildWithDefaults(IMDS_HTTP_DEFAULTS));
        this.httpClientIsInternal = builder.httpClient == null;

        this.asyncRetryScheduler = Validate.getOrDefault(
            builder.scheduledExecutorService,
            () -> {
                ThreadFactory threadFactory =
                    new ThreadFactoryBuilder().threadNamePrefix("IMDS-ScheduledExecutor").build();
                return Executors.newScheduledThreadPool(DEFAULT_RETRY_THREAD_POOL_SIZE, threadFactory);
            });
        this.retryExecutorIsInternal = builder.scheduledExecutorService == null;
        Supplier<CompletableFuture<Token>> tokenSupplier = () -> {
            SdkHttpFullRequest baseTokenRequest = requestMarshaller.createTokenRequest(tokenTtl);
            return sendAsyncTokenRequest(httpClient, baseTokenRequest);
        };

        this.tokenCache = new AsyncTokenCache(tokenSupplier);
    }

    public static Ec2MetadataAsyncClient.Builder builder() {
        return new DefaultEc2MetadataAsyncClient.Ec2MetadataAsyncBuilder();
    }

    @Override
    public CompletableFuture<Ec2MetadataResponse> get(String path) {
        CompletableFuture<Ec2MetadataResponse> returnFuture = new CompletableFuture<>();
        get(path, RetryPolicyContext.builder().retriesAttempted(0).build(), returnFuture);
        return returnFuture;
    }

    private void get(String path, RetryPolicyContext retryPolicyContext, CompletableFuture<Ec2MetadataResponse> returnFuture) {
        CompletableFuture<Token> tokenFuture = tokenCache.get();

        CompletableFuture<Ec2MetadataResponse> result = tokenFuture.thenCompose(token -> {
            SdkHttpFullRequest baseMetadataRequest = requestMarshaller.createDataRequest(path, token.value(), tokenTtl);
            return AsyncHttpRequestHelper.sendAsyncMetadataRequest(httpClient, baseMetadataRequest, returnFuture);
        }).thenApply(Ec2MetadataResponse::create);

        CompletableFutureUtils.forwardExceptionTo(returnFuture, result);

        result.whenComplete((response, error) -> {
            if (response != null) {
                returnFuture.complete(response);
                return;
            }
            if (!shouldRetry(retryPolicyContext, error)) {
                returnFuture.completeExceptionally(error);
                return;
            }
            int newAttempt = retryPolicyContext.retriesAttempted() + 1;
            log.debug(() -> "Retrying request: Attempt " + newAttempt);
            RetryPolicyContext newContext =
                RetryPolicyContext.builder()
                                  .retriesAttempted(newAttempt)
                                  .exception(SdkClientException.create(error.getMessage(), error))
                                  .build();
            scheduledRetryAttempt(() -> get(path, newContext, returnFuture), newContext);
        });
    }

    private void scheduledRetryAttempt(Runnable runnable, RetryPolicyContext retryPolicyContext) {
        Duration retryDelay = retryPolicy.backoffStrategy().computeDelayBeforeNextRetry(retryPolicyContext);
        Executor retryExecutor = retryAttempt ->
            asyncRetryScheduler.schedule(retryAttempt, retryDelay.toMillis(), TimeUnit.MILLISECONDS);
        CompletableFuture.runAsync(runnable, retryExecutor);
    }

    @Override
    public void close() {
        if (httpClientIsInternal) {
            httpClient.close();
        }
        if (retryExecutorIsInternal) {
            asyncRetryScheduler.shutdown();
        }
    }

    protected static final class Ec2MetadataAsyncBuilder implements Ec2MetadataAsyncClient.Builder {

        private Ec2MetadataRetryPolicy retryPolicy;

        private URI endpoint;

        private Duration tokenTtl;

        private EndpointMode endpointMode;

        private SdkAsyncHttpClient httpClient;

        private SdkAsyncHttpClient.Builder<?> httpClientBuilder;

        private ScheduledExecutorService scheduledExecutorService;

        private Ec2MetadataAsyncBuilder() {
        }

        @Override
        public Ec2MetadataAsyncBuilder retryPolicy(Ec2MetadataRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        @Override
        public Ec2MetadataAsyncBuilder retryPolicy(Consumer<Ec2MetadataRetryPolicy.Builder> builderConsumer) {
            Validate.notNull(builderConsumer, "builderConsumer must not be null");
            Ec2MetadataRetryPolicy.Builder builder = Ec2MetadataRetryPolicy.builder();
            builderConsumer.accept(builder);
            this.retryPolicy = builder.build();
            return this;
        }

        @Override
        public Ec2MetadataAsyncBuilder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public Ec2MetadataAsyncBuilder tokenTtl(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
            return this;
        }

        @Override
        public Ec2MetadataAsyncBuilder endpointMode(EndpointMode endpointMode) {
            this.endpointMode = endpointMode;
            return this;
        }

        @Override
        public Ec2MetadataAsyncBuilder httpClient(SdkAsyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        @Override
        public Builder httpClient(SdkAsyncHttpClient.Builder<?> builder) {
            this.httpClientBuilder = builder;
            return this;
        }

        @Override
        public Ec2MetadataAsyncBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
            return this;
        }

        public Ec2MetadataRetryPolicy getRetryPolicy() {
            return this.retryPolicy;
        }

        public URI getEndpoint() {
            return this.endpoint;
        }

        public Duration getTokenTtl() {
            return this.tokenTtl;
        }

        public EndpointMode getEndpointMode() {
            return this.endpointMode;
        }

        @Override
        public Ec2MetadataAsyncClient build() {
            return new DefaultEc2MetadataAsyncClient(this);
        }
    }
}