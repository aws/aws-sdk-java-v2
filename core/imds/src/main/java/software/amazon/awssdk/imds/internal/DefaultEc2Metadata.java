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
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.imds.Ec2Metadata;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
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

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";

    private static final Logger log = Logger.loggerFor(DefaultEc2Metadata.class);

    private static final RequestMarshaller REQUEST_MARSHALLER = new RequestMarshaller();

    private static final EndpointProvider ENDPOINT_PROVIDER = EndpointProvider.builder().build();

    private final Ec2MetadataRetryPolicy retryPolicy;

    private final URI endpoint;

    private final Duration tokenTtl;

    private final EndpointMode endpointMode;

    private final String httpDebugOutput;

    private final SdkHttpClient httpClient;

    private DefaultEc2Metadata(DefaultEc2Metadata.Ec2MetadataBuilder builder) {

        this.retryPolicy = builder.retryPolicy != null ? builder.retryPolicy : Ec2MetadataRetryPolicy.builder().build();
        this.endpoint = URI.create(ENDPOINT_PROVIDER.resolveEndpoint(builder.endpoint, builder.endpointMode));
        this.tokenTtl = builder.tokenTtl != null ? builder.tokenTtl : Duration.ofSeconds(21600);
        this.endpointMode = ENDPOINT_PROVIDER.resolveEndpointMode(builder.endpointMode);
        this.httpDebugOutput = builder.httpDebugOutput;
        this.httpClient = builder.httpClient != null ? builder.httpClient : ApacheHttpClient.create();
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
                        .httpDebugOutput(httpDebugOutput)
                        .httpClient(httpClient);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DefaultEc2Metadata ec2Metadata = (DefaultEc2Metadata) obj;

        if (!Objects.equals(retryPolicy, ec2Metadata.retryPolicy)) {
            return false;
        }
        if (!Objects.equals(endpoint, ec2Metadata.endpoint)) {
            return false;
        }
        if (!Objects.equals(tokenTtl, ec2Metadata.tokenTtl)) {
            return false;
        }
        if (!Objects.equals(endpointMode, ec2Metadata.endpointMode)) {
            return false;
        }
        if (!Objects.equals(httpDebugOutput, ec2Metadata.httpDebugOutput)) {
            return false;
        }
        return Objects.equals(httpClient.clientName(), ec2Metadata.httpClient.clientName());
    }

    @Override
    public int hashCode() {

        int result = retryPolicy.hashCode();
        result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
        result = 31 * result + tokenTtl.hashCode();
        result = 31 * result + (endpointMode != null ? endpointMode.hashCode() : 0);
        result = 31 * result + (httpDebugOutput != null ? httpDebugOutput.hashCode() : 0);
        result = 31 * result + httpClient.clientName().hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "DefaultEc2Metadata{" +
               "retries=" + retryPolicy +
               ", endpoint='" + endpoint + '\'' +
               ", tokenTtl=" + tokenTtl +
               ", endpointMode='" + endpointMode + '\'' +
               ", httpDebugOutput='" + httpDebugOutput + '\'' +
               ", httpClient= " + httpClient.clientName() +
               '}';
    }

    /**
     * Gets the specified instance metadata value by the given path.
     * @param path  Input path
     * @return Instance metadata value as part of MetadataResponse Object
     */
    @Override
    public MetadataResponse get(String path) {

        MetadataResponse metadataResponse = null;
        AbortableInputStream abortableInputStream = null;

        for (int tries = 1 ; tries <= retryPolicy.numRetries() + 1; tries ++) {

            try {
                Optional<String> token = getToken();
                if (token.isPresent()) {
                    HttpExecuteResponse response = getDataHttpResponse(path, token.get());

                    int statusCode = response.httpResponse().statusCode();
                    Optional<AbortableInputStream> responseBody = response.responseBody();

                    if (statusCode == 200) {
                        if (!responseBody.isPresent()) {
                            throw SdkClientException.builder()
                                                     .message("Response body empty with Status Code "  + statusCode).build();
                        }
                        abortableInputStream = responseBody.get();
                        String data = IoUtils.toUtf8String(abortableInputStream);
                        metadataResponse = new MetadataResponse(data);
                        return metadataResponse;
                    }
                    handleException(statusCode, path);
                }
                //TODO Create IMDS Custom Exception
            } catch (IOException io) {
                log.warn(() -> "Received an IOException ", io);
            } finally {
                IoUtils.closeQuietly(abortableInputStream, log.logger());
            }
            pauseBeforeRetryIfNeeded(tries);
        }
        return metadataResponse;
    }

    private void handleException(int statusCode, String path) {
        if (statusCode == 404) {
            throw SdkClientException.builder()
                                 .message("The requested metadata at path ( " + path + " ) is not found ").build();
        }
    }

    private void pauseBeforeRetryIfNeeded(int tries) {

        if (tries == retryPolicy.numRetries() + 1) {
            throw SdkClientException.builder().message("Exceeded maximum number of retries.").build();
        }

        try {
            long backoffTimeMillis = getBackoffDuration(tries);
            Thread.sleep(backoffTimeMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SdkClientException.builder().message("Thread interrupted while trying to sleep").cause(e).build();
        }
    }

    private HttpExecuteResponse getDataHttpResponse(String path, String token) throws IOException {

        URI uri = URI.create(endpoint + path);
        HttpExecuteRequest httpExecuteRequest = REQUEST_MARSHALLER.createDataRequest(uri, SdkHttpMethod.GET, token,
                                                                                     tokenTtl);
        return httpClient.prepareRequest(httpExecuteRequest).call();
    }

    private long getBackoffDuration(int tries) {

        long backoffTime = 0L;
        if (tries >= 1) {
            backoffTime = retryPolicy.backoffStrategy()
                                              .computeDelayBeforeNextRetry(RetryPolicyContext.builder()
                                                                                             .retriesAttempted(tries - 1)
                                                                                             .build()).toMillis();
        }

        return backoffTime;
    }

    private Optional<String> getToken() throws IOException {

        AbortableInputStream abortableInputStream = null;
        try {
            HttpExecuteResponse response = getTokenHttpResponse();
            int statusCode = response.httpResponse().statusCode();
            Optional<AbortableInputStream> responseBody = response.responseBody();

            if (statusCode == 200) {
                if (!responseBody.isPresent()) {
                    throw SdkClientException.builder()
                                             .message("Response body empty with Status Code "  + statusCode).build();
                }
                abortableInputStream = responseBody.get();
                return Optional.of(IoUtils.toUtf8String(abortableInputStream));
            }
            handleErrorResponse(statusCode);
        } catch (IOException e) {
            log.warn(() -> "Received an IOException ", e);
            throw e;
        } finally {
            IoUtils.closeQuietly(abortableInputStream, log.logger());
        }
        return Optional.empty();
    }

    private void handleErrorResponse(int statusCode) {

        if (statusCode == 403 || statusCode == 400) {
            throw SdkClientException.builder()
                                     .message("Could not retrieve token as " + statusCode + " error occurred.").build();
        }
    }


    private HttpExecuteResponse getTokenHttpResponse() throws IOException {

        URI uri = URI.create(endpoint + TOKEN_RESOURCE_PATH);
        HttpExecuteRequest httpExecuteRequest = REQUEST_MARSHALLER.createTokenRequest(uri, SdkHttpMethod.PUT, tokenTtl);
        return httpClient.prepareRequest(httpExecuteRequest).call();

    }

    private static final class Ec2MetadataBuilder implements Ec2Metadata.Builder {

        private Ec2MetadataRetryPolicy retryPolicy;

        private URI endpoint;

        private Duration tokenTtl;

        private EndpointMode endpointMode;

        private String httpDebugOutput;

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

        public void setHttpDebugOutput(String httpDebugOutput) {
            this.httpDebugOutput = httpDebugOutput;
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
        public Builder httpDebugOutput(String httpDebugOutput) {
            this.httpDebugOutput = httpDebugOutput;
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