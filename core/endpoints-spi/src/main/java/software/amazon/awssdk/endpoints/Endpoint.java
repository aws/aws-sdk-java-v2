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
    private final URI url;
    private final Map<String, List<String>> headers;
    private final Map<EndpointAttributeKey<?>, Object> attributes;

    private Endpoint(BuilderImpl b) {
        this.url = b.url;
        this.headers = b.headers;
        this.attributes = b.attributes;
    }

    public URI url() {
        return url;
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

        if (url != null ? !url.equals(endpoint.url) : endpoint.url != null) {
            return false;
        }
        if (headers != null ? !headers.equals(endpoint.headers) : endpoint.headers != null) {
            return false;
        }
        return attributes != null ? attributes.equals(endpoint.attributes) : endpoint.attributes == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        Builder url(URI url);

        Builder putHeader(String name, String value);

        <T> Builder putAttribute(EndpointAttributeKey<T> key, T value);

        Endpoint build();
    }

    private static class BuilderImpl implements Builder {
        private URI url;
        private final Map<String, List<String>> headers = new HashMap<>();
        private final Map<EndpointAttributeKey<?>, Object> attributes = new HashMap<>();

        private BuilderImpl() {
        }

        private BuilderImpl(Endpoint e) {
            this.url = e.url;
            if (e.headers != null) {
                e.headers.forEach((n, v) -> {
                    this.headers.put(n, new ArrayList<>(v));
                });
            }
            this.attributes.putAll(e.attributes);
        }

        @Override
        public Builder url(URI url) {
            this.url = url;
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
