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

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

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
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.AsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkAsyncHttpClientBuilder;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.imds.MetadataResponse;
import software.amazon.awssdk.imds.TokenCacheStrategy;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
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
    private final Supplier<CompletableFuture<Token>> tokenCache;
    private final boolean httpClientIsInternal;
    private final boolean retryExecutorIsInternal;

    private DefaultEc2MetadataAsyncClient(Ec2MetadataAsyncBuilder builder) {
        super(builder);
        this.httpClient = Validate.getOrDefault(
            builder.httpClient,
            () -> new DefaultSdkAsyncHttpClientBuilder().buildWithDefaults(IMDS_HTTP_DEFAULTS));
        this.asyncRetryScheduler = Validate.getOrDefault(
            builder.scheduledExecutorService,
            () -> {
                ThreadFactory threadFactory = new ThreadFactoryBuilder().threadNamePrefix("IMDS-ScheduledExecutor").build();
                return Executors.newScheduledThreadPool(DEFAULT_RETRY_THREAD_POOL_SIZE, threadFactory);
            });
        TokenCacheStrategy tokenCacheStrategy = Validate.getOrDefault(builder.tokenCacheStrategy, () -> TokenCacheStrategy.NONE);
        Supplier<CompletableFuture<Token>> valueSupplier =
            () -> {
                CompletableFuture<String> tokenValue = this.sendRequest(requestMarshaller.createTokenRequest(tokenTtl));
                return tokenValue.thenApply(value -> new Token(value, this.tokenTtl));
            };
        this.tokenCache = tokenCacheStrategy.getCachedSupplier(valueSupplier, this.tokenTtl);
        this.httpClientIsInternal = builder.httpClient == null;
        this.retryExecutorIsInternal = builder.scheduledExecutorService == null;
    }

    public static Ec2MetadataAsyncClient.Builder builder() {
        return new DefaultEc2MetadataAsyncClient.Ec2MetadataAsyncBuilder();
    }

    @Override
    public CompletableFuture<MetadataResponse> get(String path) {
        CompletableFuture<String> resultFuture = new CompletableFuture<>();
        CompletableFuture<Token> tokenFuture = tokenCache.get();
        CompletableFutureUtils.forwardExceptionTo(resultFuture, tokenFuture);
        tokenFuture.whenComplete((token, throwable) -> {
            if (throwable != null) {
                resultFuture.completeExceptionally(throwable);
                return;
            }
            SdkHttpFullRequest metadataHttpRequest = requestMarshaller.createDataRequest(path, token.value(), tokenTtl);
            RetryPolicyContext initialRetryContext = RetryPolicyContext.builder().request(metadataHttpRequest).build();
            sendRequestWithToken(metadataHttpRequest, token, initialRetryContext, resultFuture);
        });
        CompletableFuture<MetadataResponse> returnFuture = resultFuture.thenApply(MetadataResponse::create);
        CompletableFutureUtils.forwardExceptionTo(returnFuture, resultFuture);
        return returnFuture;
    }

    //////
    private CompletableFuture<String> sendRequest(SdkHttpFullRequest request) {
        RetryPolicyContext initialRetryContext = RetryPolicyContext.builder()
                                                                   .retriesAttempted(0)
                                                                   .request(request)
                                                                   .build();
        CompletableFuture<String> initialFuture = new CompletableFuture<>();
        sendRequest(request, initialRetryContext, initialFuture);
        return initialFuture;
    }

    public void sendRequest(SdkHttpFullRequest request, RetryPolicyContext retryPolicyContext,
                                                 CompletableFuture<String> initialFuture) {
        sendRequestWithRetry(request, retryPolicyContext, initialFuture,
                             newContext -> sendRequest(request, newContext, initialFuture));
    }

    private void sendRequestWithRetry(SdkHttpFullRequest request, RetryPolicyContext retryPolicyContext,
                                      CompletableFuture<String> initialFuture,
                                      Consumer<RetryPolicyContext> retryConsumer) {

        SdkHttpContentPublisher requestContentPublisher = new SimpleHttpContentPublisher(request);
        StringResponseHandler stringResponseHandler = new StringResponseHandler();
        TransformingAsyncResponseHandler<String> responseHandler =
            new AsyncResponseHandler<>(stringResponseHandler, Function.identity(), new ExecutionAttributes());
        CompletableFuture<String> responseHandlerFuture = responseHandler.prepare();
        stringResponseHandler.setFuture(responseHandlerFuture);
        AsyncExecuteRequest metadataRequest = AsyncExecuteRequest.builder()
                                                                 .request(request)
                                                                 .requestContentPublisher(requestContentPublisher)
                                                                 .responseHandler(responseHandler)
                                                                 .build();
        CompletableFuture<Void> executeFuture = httpClient.execute(metadataRequest);
        CompletableFutureUtils.forwardExceptionTo(initialFuture, responseHandlerFuture);
        CompletableFutureUtils.forwardExceptionTo(initialFuture, executeFuture);
        responseHandlerFuture.whenComplete((response, error) -> {
            if (response != null) {
                log.debug(() -> String.format("Completed request to %s in %d retry attempt", request.encodedPath(),
                                             retryPolicyContext.retriesAttempted()));
                initialFuture.complete(response);
                return;
            }
            if (!shouldRetry(retryPolicyContext, error)) {
                initialFuture.completeExceptionally(error);
                return;
            }
            int newAttempt = retryPolicyContext.retriesAttempted() + 1;
            log.debug(() -> String.format("Retrying request to %s: Retry attempt %d", request.encodedPath(), newAttempt));
            RetryPolicyContext newContext =
                RetryPolicyContext.builder()
                                  .retriesAttempted(newAttempt)
                                  .request(request)
                                  .exception(SdkClientException.create(error.getMessage(), error))
                                  .build();
            scheduledRetryAttempt(() -> retryConsumer.accept(newContext), newContext);
        });
    }

    private void sendRequestWithToken(SdkHttpFullRequest request, Token token, RetryPolicyContext retryPolicyContext,
                                     CompletableFuture<String> initialFuture) {
        log.debug(() -> String.format("Making metadata request with token %s", token));
        if (token.isExpired()) {
            log.debug(() -> "Token is expired, refreshing");
            CompletableFuture<Token> tokenFuture = tokenCache.get();
            CompletableFutureUtils.forwardExceptionTo(initialFuture, tokenFuture);
            tokenFuture.thenAccept(newToken -> sendRequestWithToken(request, newToken, retryPolicyContext, initialFuture));
        }

        sendRequestWithRetry(request, retryPolicyContext, initialFuture,
                             newContext -> sendRequestWithToken(request, token, newContext, initialFuture));
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
            runAndLogError(log.logger(), "Error while closing IMDS Http Client", httpClient::close);
        }
        if (retryExecutorIsInternal) {
            runAndLogError(log.logger(), "Error while closing IMDS retry executor", asyncRetryScheduler::shutdown);
        }
        if (tokenCache instanceof SdkAutoCloseable) {
            runAndLogError(log.logger(), "Error while closing IMDS token cache", ((SdkAutoCloseable) tokenCache)::close);
        }
    }

    private static final class Ec2MetadataAsyncBuilder implements Ec2MetadataAsyncClient.Builder {

        private Ec2MetadataRetryPolicy retryPolicy;

        private URI endpoint;

        private Duration tokenTtl;

        private EndpointMode endpointMode;

        private SdkAsyncHttpClient httpClient;

        private ScheduledExecutorService scheduledExecutorService;

        private TokenCacheStrategy tokenCacheStrategy;

        private Ec2MetadataAsyncBuilder() {
        }

        @Override
        public Ec2MetadataAsyncBuilder retryPolicy(Ec2MetadataRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
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
        public Ec2MetadataAsyncBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
            return this;
        }

        @Override
        public Ec2MetadataRetryPolicy getRetryPolicy() {
            return this.retryPolicy;
        }

        @Override
        public URI getEndpoint() {
            return this.endpoint;
        }

        @Override
        public Duration getTokenTtl() {
            return this.tokenTtl;
        }

        @Override
        public EndpointMode getEndpointMode() {
            return this.endpointMode;
        }

        @Override
        public Ec2MetadataAsyncBuilder tokenCacheStrategy(TokenCacheStrategy tokenCacheStrategy) {
            this.tokenCacheStrategy = tokenCacheStrategy;
            return this;
        }

        @Override
        public TokenCacheStrategy getTokenCacheStrategy() {
            return this.tokenCacheStrategy;
        }


        @Override
        public Ec2MetadataAsyncClient build() {
            return new DefaultEc2MetadataAsyncClient(this);
        }
    }
}
