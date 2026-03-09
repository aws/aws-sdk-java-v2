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

package software.amazon.awssdk.endpoints;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Represents an endpoint computed by an {@link EndpointProvider}. And endpoint minimally defines the {@code URI}, but may also
 * declare any additional headers that needed to be used, and user-defined attributes using an {@link EndpointAttributeKey}.
 */
@SdkPublicApi
public final class Endpoint {
    private final EndpointUrl endpointUrl;
    private final Map<String, List<String>> headers;
    private final Map<EndpointAttributeKey<?>, Object> attributes;

    private Endpoint(BuilderImpl b) {
        this.endpointUrl = b.endpointUrl;
        this.headers = b.headers;
        this.attributes = b.attributes;
    }

    /**
     * Returns the URI. Preserved for backward compatibility.
     * Delegates to {@link EndpointUrl#toUri()} which lazily constructs the URI.
     */
    public URI url() {
        return endpointUrl.toUri();
    }

    /**
     * Returns the {@link EndpointUrl} for efficient access to URL components
     * without URI construction overhead.
     */
    public EndpointUrl endpointUrl() {
        return endpointUrl;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @SuppressWarnings("unchecked")
    public <T> T attribute(EndpointAttributeKey<T> key) {
        return (T) attributes.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Endpoint endpoint = (Endpoint) o;

        // Use toUri() for backward compatibility — ensures Endpoints built via url(URI) and
        // endpointUrl(EndpointUrl) are equal when the URLs are equivalent (e.g., IPv6 bracket differences).
        URI thisUrl = endpointUrl != null ? endpointUrl.toUri() : null;
        URI thatUrl = endpoint.endpointUrl != null ? endpoint.endpointUrl.toUri() : null;
        if (thisUrl != null ? !thisUrl.equals(thatUrl) : thatUrl != null) {
            return false;
        }
        if (headers != null ? !headers.equals(endpoint.headers) : endpoint.headers != null) {
            return false;
        }
        return attributes != null ? attributes.equals(endpoint.attributes) : endpoint.attributes == null;
    }

    @Override
    public int hashCode() {
        // Use toUri() for consistency with equals()
        URI uri = endpointUrl != null ? endpointUrl.toUri() : null;
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * Sets the endpoint URL from a {@link URI}. Preserved for backward compatibility.
         * Internally converts to an {@link EndpointUrl} via {@link EndpointUrl#fromUri(URI)}.
         */
        Builder url(URI url);

        /**
         * Sets the endpoint URL from an {@link EndpointUrl} directly.
         * This is the preferred path for code that already has an {@code EndpointUrl}
         * (e.g., generated endpoint providers).
         */
        Builder endpointUrl(EndpointUrl endpointUrl);

        Builder putHeader(String name, String value);

        <T> Builder putAttribute(EndpointAttributeKey<T> key, T value);

        Endpoint build();
    }

    private static class BuilderImpl implements Builder {
        private EndpointUrl endpointUrl;
        private final Map<String, List<String>> headers = new HashMap<>();
        private final Map<EndpointAttributeKey<?>, Object> attributes = new HashMap<>();

        private BuilderImpl() {
        }

        private BuilderImpl(Endpoint e) {
            this.endpointUrl = e.endpointUrl;
            if (e.headers != null) {
                e.headers.forEach((n, v) -> {
                    this.headers.put(n, new ArrayList<>(v));
                });
            }
            this.attributes.putAll(e.attributes);
        }

        @Override
        public Builder url(URI url) {
            this.endpointUrl = EndpointUrl.fromUri(url);
            return this;
        }

        @Override
        public Builder endpointUrl(EndpointUrl endpointUrl) {
            this.endpointUrl = endpointUrl;
            return this;
        }

        @Override
        public Builder putHeader(String name, String value) {
            List<String> values = this.headers.computeIfAbsent(name, (n) -> new ArrayList<>());
            values.add(value);
            return this;
        }

        @Override
        public <T> Builder putAttribute(EndpointAttributeKey<T> key, T value) {
            this.attributes.put(key, value);
            return this;
        }

        @Override
        public Endpoint build() {
            return new Endpoint(this);
        }
    }
}
