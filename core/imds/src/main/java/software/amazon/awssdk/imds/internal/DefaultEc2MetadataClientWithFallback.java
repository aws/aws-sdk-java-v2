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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkSystemSetting;
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
import software.amazon.awssdk.imds.Ec2MetadataClientException;
import software.amazon.awssdk.imds.Ec2MetadataResponse;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * An Implementation of the Ec2Metadata Interface with IMDSv1 fallback support.
 * This client is identical to DefaultEc2MetadataClient but provides automatic fallback
 * to IMDSv1 when IMDSv2 token retrieval fails (except for 400 errors).
 */
@SdkInternalApi
@Immutable
@ThreadSafe
public final class DefaultEc2MetadataClientWithFallback extends BaseEc2MetadataClient implements Ec2MetadataClient {

    private static final Logger log = Logger.loggerFor(DefaultEc2MetadataClientWithFallback.class);

    private final SdkHttpClient httpClient;
    private final Supplier<Token> tokenCache;
    private final boolean httpClientIsInternal;
    private final boolean imdsV1FallbackEnabled;

    private DefaultEc2MetadataClientWithFallback(Ec2MetadataBuilder builder) {
        super(builder);

        Validate.isTrue(builder.httpClient == null || builder.httpClientBuilder == null,
                        "The httpClient and the httpClientBuilder can't both be configured.");
        this.httpClient = Either
            .fromNullable(builder.httpClient, builder.httpClientBuilder)
            .map(e -> e.map(Function.identity(), SdkHttpClient.Builder::build))
            .orElseGet(() -> new DefaultSdkHttpClientBuilder().buildWithDefaults(imdsHttpDefaults()));
        this.httpClientIsInternal = builder.httpClient == null;

        this.imdsV1FallbackEnabled = !resolveImdsV1Disabled();

        this.tokenCache = CachedSupplier.builder(() -> RefreshResult.builder(this.getTokenWithFallback())
                                                                    .staleTime(Instant.now().plus(tokenTtl))
                                                                    .build())
                                        .cachedValueName(toString())
                                        .build();
    }

    @Override
    public String toString() {
        return ToString.create("Ec2MetadataClientWithFallback");
    }

    @Override
    public void close() {
        if (httpClientIsInternal) {
            httpClient.close();
        }
    }

    public static Ec2MetadataBuilder builder() {
        return new DefaultEc2MetadataClientWithFallback.Ec2MetadataBuilder();
    }

    /**
     * Gets the specified instance metadata value by the given path with IMDSv1 fallback support.
     * Will retry based on the {@link Ec2MetadataRetryPolicy retry policy} provided.
     * Follows the same behavior as EC2MetadataUtils: if IMDSv2 token retrieval fails with 400 error,
     * throws exception; otherwise falls back to IMDSv1 (null token).
     *
     * @param path Input path of the resource to get.
     * @return Instance metadata value as part of MetadataResponse Object
     * @throws SdkClientException if the request fails after all retries and fallback attempts
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
                return sendRequest(path, token == null ? null : token.value());
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

    /**
     * Gets token with fallback logic that can be cached.
     * If token retrieval fails with 400 error, throws exception.
     * Otherwise, returns null to indicate fallback to IMDSv1.
     */
    private Token getTokenWithFallback() {
        try {
            return getToken();
        } catch (Exception e) {
            boolean is400ServiceException = e instanceof Ec2MetadataClientException
                    && ((Ec2MetadataClientException) e).statusCode() == 400;

            // metadata resolution must not continue to the token-less flow for a 400
            if (is400ServiceException) {
                throw SdkClientException.builder()
                        .message("Unable to fetch metadata token")
                        .cause(e)
                        .build();
            }
            return handleTokenErrorResponse(e);
        }
    }

    /**
     * Handles token error response following EC2MetadataUtils pattern.
     */
    private Token handleTokenErrorResponse(Exception e) {
        if (!imdsV1FallbackEnabled) {
            String message = String.format("Failed to retrieve IMDS token, and fallback to IMDS v1 is disabled via the "
                                           + "%s system property, %s environment variable, or %s configuration file profile"
                                           + " setting.",
                                           SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.environmentVariable(),
                                           SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.property(),
                                           ProfileProperty.EC2_METADATA_V1_DISABLED);
            throw SdkClientException.builder()
                                    .message(message)
                                    .cause(e)
                                    .build();
        }
        return null; // null token indicates fallback to IMDSv1
    }


    /**
     * Resolves whether IMDSv1 is disabled from system settings and profile file.
     */
    private boolean resolveImdsV1Disabled() {
        return OptionalUtils.firstPresent(
                            fromSystemSettingsMetadataV1Disabled(),
                            () -> fromProfileFileMetadataV1Disabled()
                        )
                        .orElse(false);
    }

    private Optional<Boolean> fromSystemSettingsMetadataV1Disabled() {
        return SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.getStringValue()
                                                            .map(Boolean::parseBoolean);
    }

    private Optional<Boolean> fromProfileFileMetadataV1Disabled() {
        return Ec2MetadataConfigProvider.instance()
                                       .resolveProfile()
                                       .flatMap(p -> p.property(ProfileProperty.EC2_METADATA_V1_DISABLED))
                                       .map(Boolean::parseBoolean);
    }

    private void handleUnsuccessfulResponse(int statusCode, Optional<AbortableInputStream> responseBody,
                                            HttpExecuteResponse response, Supplier<String> errorMessageSupplier) {
        String responseContent = responseBody.map(BaseEc2MetadataClient::uncheckedInputStreamToUtf8)
                                             .orElse("");

        throw Ec2MetadataClientException.builder()
                                        .statusCode(statusCode)
                                        .sdkHttpResponse(response.httpResponse())
                                        .rawResponse(SdkBytes.fromUtf8String(responseContent))
                                        .message(errorMessageSupplier.get())
                                        .build();
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
            handleUnsuccessfulResponse(statusCode, responseBody, response,
                () -> String.format("The requested metadata at path '%s' returned Http code %s", path, statusCode)
            );
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
            handleUnsuccessfulResponse(statusCode, response.responseBody(), response,
                () -> String.format("Could not retrieve token, %d error occurred", statusCode)
            );
        }

        String ttl = response.httpResponse()
                             .firstMatchingHeader(RequestMarshaller.EC2_METADATA_TOKEN_TTL_HEADER)
                             .orElseThrow(() -> SdkClientException
                                 .builder()
                                 .message(RequestMarshaller.EC2_METADATA_TOKEN_TTL_HEADER + " header not found in token response")
                                 .build());
        java.time.Duration ttlDuration;
        try {
            ttlDuration = java.time.Duration.ofSeconds(Long.parseLong(ttl));
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
        public Builder retryPolicy(java.util.function.Consumer<Ec2MetadataRetryPolicy.Builder> builderConsumer) {
            Validate.notNull(builderConsumer, "builderConsumer must not be null");
            Ec2MetadataRetryPolicy.Builder builder = Ec2MetadataRetryPolicy.builder();
            builderConsumer.accept(builder);
            return retryPolicy(builder.build());
        }

        @Override
        public Ec2MetadataBuilder endpoint(java.net.URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public Ec2MetadataBuilder tokenTtl(java.time.Duration tokenTtl) {
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

        public java.net.URI getEndpoint() {
            return this.endpoint;
        }

        public java.time.Duration getTokenTtl() {
            return this.tokenTtl;
        }

        public EndpointMode getEndpointMode() {
            return this.endpointMode;
        }

        @Override
        public Ec2MetadataClient build() {
            return new DefaultEc2MetadataClientWithFallback(this);
        }
    }
}
