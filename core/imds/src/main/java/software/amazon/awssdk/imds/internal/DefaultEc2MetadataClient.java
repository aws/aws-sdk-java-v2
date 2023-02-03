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

import static software.amazon.awssdk.imds.internal.RequestMarshaller.EC2_METADATA_TOKEN_TTL_HEADER;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * An Implementation of the Ec2Metadata Interface.
 */
@SdkInternalApi
@Immutable
@ThreadSafe
public final class DefaultEc2MetadataClient extends BaseEc2MetadataClient implements Ec2MetadataClient {

    private static final Logger log = Logger.loggerFor(DefaultEc2MetadataClient.class);

    private final SdkHttpClient httpClient;
    private final Supplier<Token> tokenCache;
    private final boolean httpClientIsInternal;

    private DefaultEc2MetadataClient(Ec2MetadataBuilder builder) {
        super(builder);

        Validate.isTrue(builder.httpClient == null || builder.httpClientBuilder == null,
                        "The httpClient and the httpClientBuilder can't both be configured.");
        this.httpClient = Either
            .fromNullable(builder.httpClient, builder.httpClientBuilder)
            .map(e -> e.map(Function.identity(), SdkHttpClient.Builder::build))
            .orElseGet(() -> new DefaultSdkHttpClientBuilder().buildWithDefaults(IMDS_HTTP_DEFAULTS));
        this.httpClientIsInternal = builder.httpClient == null;

        this.tokenCache = CachedSupplier.builder(() -> RefreshResult.builder(this.getToken())
                                                                    .staleTime(Instant.now().plus(tokenTtl))
                                                                    .build())
                                        .build();
    }

    @Override
    public void close() {
        if (httpClientIsInternal) {
            httpClient.close();
        }
    }

    public static Ec2MetadataBuilder builder() {
        return new DefaultEc2MetadataClient.Ec2MetadataBuilder();
    }

    /**
     * Gets the specified instance metadata value by the given path. Will retry base on the
     * {@link Ec2MetadataRetryPolicy retry policy} provided, in the case of an IOException during request. Will not retry on
     * SdkClientException, like 4XX HTTP error.
     *
     * @param path Input path of the resource to get.
     * @return Instance metadata value as part of MetadataResponse Object
     * @throws SdkClientException if the request for a token or the request for the Metadata does not have a 2XX SUCCESS response,
     *                            if the maximum number of retries is reached, or if another IOException is thrown during the
     *                            request.
     */
    @Override
    public Ec2MetadataResponse get(String path) {
        Throwable lastCause = null;
        // 3 retries means 4 total attempts
        Token token = null;
        for (int attempt = 0; attempt < retryPolicy.numRetries() + 1; attempt++) {
            RetryPolicyContext retryPolicyContext = RetryPolicyContext.builder().retriesAttempted(attempt).build();
            try {
                if (token == null || token.isExpired()) {
                    token = tokenCache.get();
                }
                return sendRequest(path, token.value());
            } catch (UncheckedIOException | RetryableException e) {
                lastCause = e;
                int currentTry = attempt;
                log.debug(() -> "Error while executing EC2Metadata request, attempting retry. Current attempt: " + currentTry);
            } catch (SdkClientException sdkClientException) {
                int totalTries = attempt + 1;
                log.debug(() -> String.format("Error while executing EC2Metadata request. Total attempts: %d. %s",
                                              totalTries,
                                              sdkClientException.getMessage()));
                throw sdkClientException;
            } catch (IOException ioe) {
                lastCause = new UncheckedIOException(ioe);
                int currentTry = attempt;
                log.debug(() -> "Error while executing EC2Metadata request, attempting retry. Current attempt: " + currentTry);
            }
            pauseBeforeRetryIfNeeded(retryPolicyContext);
        }

        SdkClientException.Builder sdkClientExceptionBuilder = SdkClientException
            .builder()
            .message("Exceeded maximum number of retries. Total retry attempts: " + retryPolicy.numRetries());
        if (lastCause != null) {
            String msg = sdkClientExceptionBuilder.message();
            sdkClientExceptionBuilder.cause(lastCause).message(msg);
        }
        throw sdkClientExceptionBuilder.build();
    }

    private Ec2MetadataResponse sendRequest(String path, String token) throws IOException {

        HttpExecuteRequest httpExecuteRequest =
            HttpExecuteRequest.builder()
                              .request(requestMarshaller.createDataRequest(path, token, tokenTtl))
                              .build();
        HttpExecuteResponse response = httpClient.prepareRequest(httpExecuteRequest).call();

        int statusCode = response.httpResponse().statusCode();
        Optional<AbortableInputStream> responseBody = response.responseBody();

        if (HttpStatusFamily.of(statusCode).isOneOf(HttpStatusFamily.SERVER_ERROR)) {
            responseBody.map(BaseEc2MetadataClient::uncheckedInputStreamToUtf8)
                        .ifPresent(str -> log.debug(() -> "Metadata request response body: " + str));
            throw RetryableException
                .builder()
                .message(String.format("The requested metadata at path '%s' returned Http code %s", path, statusCode))
                .build();
        }

        if (!HttpStatusFamily.of(statusCode).isOneOf(HttpStatusFamily.SUCCESSFUL)) {
            responseBody.map(BaseEc2MetadataClient::uncheckedInputStreamToUtf8)
                        .ifPresent(str -> log.debug(() -> "Metadata request response body: " + str));
            throw SdkClientException
                .builder()
                .message(String.format("The requested metadata at path '%s' returned Http code %s", path, statusCode))
                .build();
        }

        AbortableInputStream abortableInputStream = responseBody.orElseThrow(
            SdkClientException.builder().message("Response body empty with Status Code " + statusCode)::build);
        String data = uncheckedInputStreamToUtf8(abortableInputStream);
        return Ec2MetadataResponse.create(data);
    }

    private void pauseBeforeRetryIfNeeded(RetryPolicyContext retryPolicyContext) {
        long backoffTimeMillis = retryPolicy.backoffStrategy()
                                            .computeDelayBeforeNextRetry(retryPolicyContext)
                                            .toMillis();
        if (backoffTimeMillis > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(backoffTimeMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw SdkClientException.builder().message("Thread interrupted while trying to sleep").cause(e).build();
            }
        }
    }

    private Token getToken() {
        HttpExecuteRequest httpExecuteRequest = HttpExecuteRequest.builder()
                                                                  .request(requestMarshaller.createTokenRequest(tokenTtl))
                                                                  .build();
        HttpExecuteResponse response = null;
        try {
            response = httpClient.prepareRequest(httpExecuteRequest).call();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        int statusCode = response.httpResponse().statusCode();

        if (HttpStatusFamily.of(statusCode).isOneOf(HttpStatusFamily.SERVER_ERROR)) {
            response.responseBody().map(BaseEc2MetadataClient::uncheckedInputStreamToUtf8)
                    .ifPresent(str -> log.debug(() -> "Metadata request response body: " + str));
            throw RetryableException.builder()
                                    .message("Could not retrieve token, " + statusCode + " error occurred").build();
        }

        if (!HttpStatusFamily.of(statusCode).isOneOf(HttpStatusFamily.SUCCESSFUL)) {
            response.responseBody().map(BaseEc2MetadataClient::uncheckedInputStreamToUtf8)
                    .ifPresent(body -> log.debug(() -> "Token request response body: " + body));
            throw SdkClientException.builder()
                                    .message("Could not retrieve token, " + statusCode + " error occurred.")
                                    .build();
        }

        String ttl = response.httpResponse()
                             .firstMatchingHeader(EC2_METADATA_TOKEN_TTL_HEADER)
                             .orElseThrow(() -> SdkClientException
                                 .builder()
                                 .message(EC2_METADATA_TOKEN_TTL_HEADER + " header not found in token response")
                                 .build());
        Duration ttlDuration;
        try {
            ttlDuration = Duration.ofSeconds(Long.parseLong(ttl));
        } catch (NumberFormatException nfe) {
            throw SdkClientException.create("Invalid token format received from IMDS server", nfe);
        }

        AbortableInputStream abortableInputStream = response.responseBody().orElseThrow(
            SdkClientException.builder().message("Empty response body")::build);

        String value = uncheckedInputStreamToUtf8(abortableInputStream);
        return new Token(value, ttlDuration);
    }

    protected static final class Ec2MetadataBuilder implements Ec2MetadataClient.Builder {

        private Ec2MetadataRetryPolicy retryPolicy;

        private URI endpoint;

        private Duration tokenTtl;

        private EndpointMode endpointMode;

        private SdkHttpClient httpClient;

        private SdkHttpClient.Builder<?> httpClientBuilder;

        private Ec2MetadataBuilder() {
        }

        @Override
        public Ec2MetadataBuilder retryPolicy(Ec2MetadataRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        @Override
        public Builder retryPolicy(Consumer<Ec2MetadataRetryPolicy.Builder> builderConsumer) {
            Validate.notNull(builderConsumer, "builderConsumer must not be null");
            Ec2MetadataRetryPolicy.Builder builder = Ec2MetadataRetryPolicy.builder();
            builderConsumer.accept(builder);
            return retryPolicy(builder.build());
        }

        @Override
        public Ec2MetadataBuilder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public Ec2MetadataBuilder tokenTtl(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
            return this;
        }

        @Override
        public Ec2MetadataBuilder endpointMode(EndpointMode endpointMode) {
            this.endpointMode = endpointMode;
            return this;
        }

        @Override
        public Ec2MetadataBuilder httpClient(SdkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        @Override
        public Builder httpClient(SdkHttpClient.Builder<?> builder) {
            this.httpClientBuilder = builder;
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
        public Ec2MetadataClient build() {
            return new DefaultEc2MetadataClient(this);
        }
    }
}