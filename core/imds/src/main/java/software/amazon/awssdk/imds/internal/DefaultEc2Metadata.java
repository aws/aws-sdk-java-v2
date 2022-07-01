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
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.imds.Ec2Metadata;
import software.amazon.awssdk.utils.IoUtils;

/**
 * An Implementation of the Ec2Metadata Interface.
 */
@SdkInternalApi
@Immutable
@ThreadSafe
public final class DefaultEc2Metadata implements Ec2Metadata {

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";

    private final RetryPolicy retryPolicy;

    private final URI endpoint;

    private final Duration tokenTtl;

    private final EndpointMode endpointMode;

    private final String httpDebugOutput;

    private final SdkHttpClient httpClient;

    private final Logger log = LoggerFactory.getLogger(DefaultEc2Metadata.class);

    private RequestMarshaller requestMarshaller = new RequestMarshaller();

    private DefaultEc2Metadata(DefaultEc2Metadata.Ec2MetadataBuilder builder) {

        this.retryPolicy = builder.retryPolicy != null ? builder.retryPolicy : RetryPolicy.builder().build();
        this.endpoint = builder.endpoint;
        this.tokenTtl = builder.tokenTtl != null ? builder.tokenTtl : Duration.ofSeconds(21600);
        this.endpointMode = builder.endpointMode;
        this.httpDebugOutput = builder.httpDebugOutput;
        this.httpClient = builder.httpClient != null ? builder.httpClient : UrlConnectionHttpClient.create();
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
     * @return Instance metadata value
     */
    @Override
    public String get(String path) {

        String data = null;
        AbortableInputStream abortableInputStream = null;
        try {
            String token = getToken();
            URI uri = URI.create(endpoint + path);
            HttpExecuteRequest httpExecuteRequest = requestMarshaller.createDataRequest(uri , SdkHttpMethod.GET, token,
                                                                                tokenTtl);
            HttpExecuteResponse response =  httpClient.prepareRequest(httpExecuteRequest).call();
            int statusCode = response.httpResponse().statusCode();

            if (statusCode == HttpURLConnection.HTTP_OK) {
                abortableInputStream = response.responseBody().get();
                data = IoUtils.toUtf8String(abortableInputStream);
            } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw SdkServiceException.builder().message("The requested metadata is not found ").build();
            } else {
                throw SdkClientException.builder()
                                        .message("Unexpected Exception occurred with Status Code " + statusCode).build();
            }
        } catch (SdkServiceException sd) {
            throw SdkServiceException.builder().message(sd.getMessage()).cause(sd).build();
        } catch (IOException | SdkClientException  io) {
            // TODO Retry Logic will be added
            log.warn("Received an IOException " + io);
        } finally {
            IoUtils.closeQuietly(abortableInputStream, log);
        }

        return data;
    }

    private String getToken() throws IOException {

        AbortableInputStream abortableInputStream = null;
        try {
            URI uri = URI.create(endpoint + TOKEN_RESOURCE_PATH);
            HttpExecuteRequest httpExecuteRequest = requestMarshaller.createTokenRequest(uri , SdkHttpMethod.PUT, tokenTtl);
            HttpExecuteResponse response =  httpClient.prepareRequest(httpExecuteRequest).call();
            int statusCode = response.httpResponse().statusCode();

            if (statusCode == HttpURLConnection.HTTP_OK) {
                abortableInputStream = response.responseBody().get();
                String token = IoUtils.toUtf8String(abortableInputStream);
                return token;
            } else if (statusCode == HttpURLConnection.HTTP_FORBIDDEN || statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw SdkServiceException.builder().message("Could not retrieve token").build();
            } else {
                throw SdkClientException.builder()
                                        .message("Unexpected Exception during token retrieval with Status Code " + statusCode)
                                        .build();
            }

        } catch (IOException e) {
            throw e;
        } finally {
            IoUtils.closeQuietly(abortableInputStream, log);
        }
    }

    private static final class Ec2MetadataBuilder implements Ec2Metadata.Builder {

        private RetryPolicy retryPolicy;

        private URI endpoint;

        private Duration tokenTtl;

        private EndpointMode endpointMode;

        private String httpDebugOutput;

        private SdkHttpClient httpClient;

        private Ec2MetadataBuilder() {
        }

        public void setRetryPolicy(RetryPolicy retryPolicy) {
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
        public Builder retryPolicy(RetryPolicy retryPolicy) {
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