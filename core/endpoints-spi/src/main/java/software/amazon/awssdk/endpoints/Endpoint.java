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
import java.util.Collections;
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
    /**
     * Initial capacity for the attribute and header maps. Endpoints carry very few of either, so the
     * default capacity of 16 is extra overhead.
     */
    private static final int ATTRIBUTE_MAP_CAPACITY = 4;
    private static final int HEADER_MAP_CAPACITY = 4;

    private final EndpointUrl endpointUrl;
    private final Map<String, List<String>> headers;
    private final Map<EndpointAttributeKey<?>, Object> attributes;

    private Endpoint(BuilderImpl b) {
        this.endpointUrl = b.endpointUrl;
        this.headers = b.headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(b.headers);
        this.attributes = b.buildAttributes();
    }

    /**
     * Returns the URI.
     * Delegates to {@link EndpointUrl#toUri()} which lazily constructs the URI.
     *
     * @deprecated Use {@link #endpointUrl()} instead, which provides direct access to URL components
     *             without the overhead of constructing a {@link URI}.
     */
    @Deprecated
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

        // ensures Endpoints built via url(URI) and
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
         * Sets the endpoint URL from a {@link URI}.
         * Internally converts to an {@link EndpointUrl} via {@link EndpointUrl#fromUri(URI)}.
         *
         * @deprecated Use {@link #endpointUrl(EndpointUrl)} instead.
         */
        @Deprecated
        Builder url(URI url);

        /**
         * Sets the endpoint URL from an {@link EndpointUrl} directly.
         * This is the preferred path for code that already has an {@code EndpointUrl}
         * (e.g., generated endpoint providers).
         */
        default Builder endpointUrl(EndpointUrl endpointUrl) {
            throw new UnsupportedOperationException();
        }

        Builder putHeader(String name, String value);

        <T> Builder putAttribute(EndpointAttributeKey<T> key, T value);

        Endpoint build();
    }

    private static class BuilderImpl implements Builder {
        private EndpointUrl endpointUrl;

        /**
         * Most endpoints declare no headers, so the map is allocated only once a header is added.
         */
        private Map<String, List<String>> headers;

        /**
         * Endpoints almost always carry zero or one attribute (typically {@code AUTH_SCHEMES}), so the first
         * entry is held in these two fields and {@link #attributes} is allocated only if a second distinct
         * key arrives. This keeps the common cases free of a {@code HashMap} and its backing table.
         */
        private EndpointAttributeKey<?> firstAttributeKey;
        private Object firstAttributeValue;
        private Map<EndpointAttributeKey<?>, Object> attributes;

        private BuilderImpl() {
        }

        private BuilderImpl(Endpoint e) {
            this.endpointUrl = e.endpointUrl;
            if (!e.headers.isEmpty()) {
                this.headers = new HashMap<>(Math.max(HEADER_MAP_CAPACITY, e.headers.size()));
                e.headers.forEach((n, v) -> {
                    this.headers.put(n, new ArrayList<>(v));
                });
            }
            e.attributes.forEach(this::putAttributeUnchecked);
        }

        /**
         * Collapses the staged attributes into the smallest immutable map that can hold them.
         */
        private Map<EndpointAttributeKey<?>, Object> buildAttributes() {
            if (attributes != null) {
                return Collections.unmodifiableMap(attributes);
            }
            if (firstAttributeKey != null) {
                return Collections.singletonMap(firstAttributeKey, firstAttributeValue);
            }
            return Collections.emptyMap();
        }

        /**
         * Stores an attribute without the generic key/value pairing, for use by callers that have already
         * had that relationship checked (the {@link #putAttribute} overload and the copy constructor).
         */
        private void putAttributeUnchecked(EndpointAttributeKey<?> key, Object value) {
            if (attributes != null) {
                attributes.put(key, value);
            } else if (firstAttributeKey == null || firstAttributeKey.equals(key)) {
                firstAttributeKey = key;
                firstAttributeValue = value;
            } else {
                // Sized for the realistic maximum rather than the default 16, whose backing table alone
                // costs more than every other allocation on this path combined.
                attributes = new HashMap<>(ATTRIBUTE_MAP_CAPACITY);
                attributes.put(firstAttributeKey, firstAttributeValue);
                attributes.put(key, value);
                firstAttributeKey = null;
                firstAttributeValue = null;
            }
        }

        @SuppressWarnings("deprecation")
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
            if (this.headers == null) {
                this.headers = new HashMap<>(HEADER_MAP_CAPACITY);
            }
            List<String> values = this.headers.computeIfAbsent(name, (n) -> new ArrayList<>());
            values.add(value);
            return this;
        }

        @Override
        public <T> Builder putAttribute(EndpointAttributeKey<T> key, T value) {
            putAttributeUnchecked(key, value);
            return this;
        }

        @Override
        public Endpoint build() {
            return new Endpoint(this);
        }
    }
}
