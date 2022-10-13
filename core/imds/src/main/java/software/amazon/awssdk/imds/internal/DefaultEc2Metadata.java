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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.imds.Ec2Metadata;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.imds.MetadataResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * An Implementation of the Ec2Metadata Interface.
 */
@SdkInternalApi
@Immutable
@ThreadSafe
public final class DefaultEc2Metadata implements Ec2Metadata {

    private static final Logger log = Logger.loggerFor(DefaultEc2Metadata.class);

    private static final Ec2MetadataEndpointProvider ENDPOINT_PROVIDER =
        Ec2MetadataEndpointProvider.builder().build();

    private final Ec2MetadataRetryPolicy retryPolicy;

    private final URI endpoint;

    private final Duration tokenTtl;

    private final EndpointMode endpointMode;

    private final SdkHttpClient httpClient;

    private final RequestMarshaller requestMarshaller;

    private DefaultEc2Metadata(DefaultEc2Metadata.Ec2MetadataBuilder builder) {
        this.retryPolicy = builder.retryPolicy != null ? builder.retryPolicy
                                                       : Ec2MetadataRetryPolicy.builder().build();
        this.endpointMode = builder.endpointMode != null ? builder.endpointMode
                                                         : ENDPOINT_PROVIDER.resolveEndpointMode();
        this.endpoint = builder.endpoint != null ? builder.endpoint
                                                 : URI.create(ENDPOINT_PROVIDER.resolveEndpoint(this.endpointMode));
        this.tokenTtl = builder.tokenTtl != null ? builder.tokenTtl
                                                 : Duration.ofSeconds(21600);
        this.httpClient = builder.httpClient != null ? builder.httpClient
                                                     : UrlConnectionHttpClient.create();
        this.requestMarshaller = new RequestMarshaller(this.endpoint);
    }

    public static Ec2Metadata.Builder builder() {
        return new DefaultEc2Metadata.Ec2MetadataBuilder();
    }

    @Override
    public Ec2Metadata.Builder toBuilder() {
        return builder().retryPolicy(retryPolicy)
                        .endpoint(endpoint)
                        .tokenTtl(tokenTtl)
                        .endpointMode(endpointMode)
                        .httpClient(httpClient);
    }

    /**
     * Gets the specified instance metadata value by the given path. Will retry base on the {@link Ec2MetadataRetryPolicy retry
     * policy} provided, in the case of an IOException during request. Will not retry on SdkClientException, like 4XX/5XX HTTP
     * error.
     * @param path  Input path of the resource to get.
     * @throws SdkClientException if the request for a token or the request for the Metadata does not have a 2XX SUCCESS response,
     *                            if the maximum number of retries is reached,
     *                            or if another IOException is thrown during the request.
     * @return Instance metadata value as part of MetadataResponse Object
     */
    @Override
    public MetadataResponse get(String path) {
        Throwable lastCause = null;
        for (int tries = 0; tries < retryPolicy.numRetries(); tries++) {
            RetryPolicyContext retryPolicyContext = RetryPolicyContext.builder().retriesAttempted(tries).build();
            try {
                String token = getToken();
                return sendRequest(path, token);
            } catch (SdkClientException sdkClientException) {
                String msg = String.format("Error while executing EC2Metadata request. Total attempts: %d. %s",
                                           tries + 1,
                                           sdkClientException.getMessage());
                log.debug(() -> msg);
                throw sdkClientException;
            } catch (UncheckedIOException ioe) {
                lastCause = ioe;
                String msg = "Error while executing EC2Metadata request, attempting retry. Current attempt: " + tries;
                log.debug(() -> msg);
            } catch (IOException ioe) {
                lastCause = new UncheckedIOException(ioe);
                String msg = "Error while executing EC2Metadata request, attempting retry. Current attempt: " + tries;
                log.debug(() -> msg);
            }
            pauseBeforeRetryIfNeeded(retryPolicyContext);
        }

        SdkClientException.Builder sdkClientExceptionBuilder = SdkClientException
            .builder()
            .message("Exceeded maximum number of retries. Total attempts: " + retryPolicy.numRetries() + ".");
        if (lastCause != null) {
            String msg = sdkClientExceptionBuilder.message() + " " + lastCause.getMessage();
            sdkClientExceptionBuilder.cause(lastCause).message(msg);
        }
        throw sdkClientExceptionBuilder.build();
    }

    private MetadataResponse sendRequest(String path, String token) throws IOException {

        HttpExecuteRequest httpExecuteRequest = requestMarshaller.createDataRequest(path, token, tokenTtl);
        HttpExecuteResponse response = httpClient.prepareRequest(httpExecuteRequest).call();

        int statusCode = response.httpResponse().statusCode();
        Optional<AbortableInputStream> responseBody = response.responseBody();

        if (!HttpStatusFamily.of(statusCode).isOneOf(HttpStatusFamily.SUCCESSFUL)) {
            responseBody.map(this::uncheckedInputStreamToUtf8)
                        .ifPresent(str -> log.debug(() -> "Metadata request response body: " + str));
            throw SdkClientException.builder()
                        .message("The requested metadata at path ( " + path + " ) return Http code " + statusCode).build();
        }

        AbortableInputStream abortableInputStream = responseBody.orElseThrow(
            SdkClientException.builder().message("Response body empty with Status Code " + statusCode)::build);
        String data = uncheckedInputStreamToUtf8(abortableInputStream);
        return MetadataResponse.create(data);
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

    private String getToken() throws IOException {
        HttpExecuteRequest httpExecuteRequest = requestMarshaller.createTokenRequest(tokenTtl);
        HttpExecuteResponse response = httpClient.prepareRequest(httpExecuteRequest).call();

        int statusCode = response.httpResponse().statusCode();
        if (!HttpStatusFamily.of(statusCode).isOneOf(HttpStatusFamily.SUCCESSFUL)) {
            response.responseBody().map(this::uncheckedInputStreamToUtf8)
                    .ifPresent(body -> log.debug(() -> "Token request response body: " + body));
            throw SdkClientException.builder()
                                    .message("Could not retrieve token, " + statusCode + " error occurred.")
                                    .build();
        }

        AbortableInputStream abortableInputStream = response.responseBody().orElseThrow(
            SdkClientException.builder().message("Empty response body")::build);

        return IoUtils.toUtf8String(abortableInputStream);
    }

    private String uncheckedInputStreamToUtf8(AbortableInputStream inputStream) {
        try {
            return IoUtils.toUtf8String(inputStream);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        } finally {
            IoUtils.closeQuietly(inputStream, log.logger());
        }
    }

    private static final class Ec2MetadataBuilder implements Ec2Metadata.Builder {

        private Ec2MetadataRetryPolicy retryPolicy;

        private URI endpoint;

        private Duration tokenTtl;

        private EndpointMode endpointMode;

        private SdkHttpClient httpClient;

        private Ec2MetadataBuilder() {
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

        public void setHttpClient(SdkHttpClient httpClient) {
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
        public Builder httpClient(SdkHttpClient httpClient) {

            this.httpClient = httpClient;
            return this;
        }

        @Override
        public Ec2Metadata build() {
            return new DefaultEc2Metadata(this);
        }
    }
}