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

package software.amazon.awssdk.auth.credentials.internal;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;
import software.amazon.awssdk.regions.util.ResourcesEndpointRetryPolicy;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class StaticResourcesEndpointProvider implements ResourcesEndpointProvider {
    private final URI endpoint;
    private final Map<String, String> headers;
    private final Duration connectionTimeout;
    private final ResourcesEndpointRetryPolicy retryPolicy;

    private StaticResourcesEndpointProvider(URI endpoint,
                                            Map<String, String> additionalHeaders,
                                            Duration customTimeout,
                                            ResourcesEndpointRetryPolicy retryPolicy) {
        this.endpoint = Validate.paramNotNull(endpoint, "endpoint");
        this.headers = ResourcesEndpointProvider.super.headers();
        if (additionalHeaders != null) {
            this.headers.putAll(additionalHeaders);
        }
        this.connectionTimeout = customTimeout;
        this.retryPolicy = Validate.getOrDefault(retryPolicy, () -> ResourcesEndpointRetryPolicy.NO_RETRY);
    }

    @Override
    public Optional<Duration> connectionTimeout() {
        return Optional.ofNullable(connectionTimeout);
    }

    @Override
    public URI endpoint() throws IOException {
        return endpoint;
    }

    @Override
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }

    @Override
    public ResourcesEndpointRetryPolicy retryPolicy() {
        return this.retryPolicy;
    }

    public static class Builder {
        private URI endpoint;
        private Map<String, String> additionalHeaders = new HashMap<>();
        private Duration customTimeout;
        private ResourcesEndpointRetryPolicy retryPolicy;

        public Builder endpoint(URI endpoint) {
            this.endpoint = Validate.paramNotNull(endpoint, "endpoint");
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                this.additionalHeaders.putAll(headers);
            }
            return this;
        }

        public Builder connectionTimeout(Duration timeout) {
            this.customTimeout = timeout;
            return this;
        }

        public Builder retryPolicy(ResourcesEndpointRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public StaticResourcesEndpointProvider build() {
            return new StaticResourcesEndpointProvider(endpoint, additionalHeaders, customTimeout, retryPolicy);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
