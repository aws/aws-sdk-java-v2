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

package software.amazon.awssdk.core.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public final class Endpoint {
    private static final String URL = "url";
    private static final String PROPERTIES = "properties";
    private static final String HEADERS = "headers";

    private Expr url;
    private Map<Identifier, Literal> properties;
    private Map<String, List<Literal>> headers;

    private Endpoint(Builder builder) {
        this.url = builder.url;
        this.properties = builder.properties;
        this.headers = builder.headers;
    }

    public Expr getUrl() {
        return url;
    }

    public Map<Identifier, Literal> getProperties() {
        return properties;
    }

    public Map<String, List<Literal>> getHeaders() {
        return headers;
    }

    public static Endpoint fromNode(JsonNode node) {
        Map<String, JsonNode> objNode = node.asObject();

        Builder b = builder();

        b.url(Expr.fromNode(objNode.get(URL)));

        JsonNode propertiesNode = objNode.get(PROPERTIES);
        if (propertiesNode != null) {
            propertiesNode.asObject().forEach((k, v) -> {
                b.addProperty(Identifier.of(k), Literal.fromNode(v));
            });
        }

        JsonNode headersNode = objNode.get(HEADERS);
        if (headersNode != null) {
            headersNode.asObject().forEach((k, v) -> {
                b.addHeader(k, v.asArray().stream().map(Literal::fromNode).collect(Collectors.toList()));
            });
        }

        return b.build();
    }

    @Override
    public String toString() {
        return "Endpoint{" +
               "url=" + url +
               ", properties=" + properties +
               ", headers=" + headers +
               '}';
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
        if (properties != null ? !properties.equals(endpoint.properties) : endpoint.properties != null) {
            return false;
        }
        return headers != null ? headers.equals(endpoint.headers) : endpoint.headers == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Expr url;
        private final Map<Identifier, Literal> properties = new HashMap<>();
        private final Map<String, List<Literal>> headers = new HashMap<>();

        public Builder url(Expr url) {
            this.url = url;
            return this;
        }

        public Builder addProperty(Identifier name, Literal value) {
            properties.put(name, value);
            return this;
        }

        public Builder addHeader(String name, List<Literal> value) {
            this.headers.put(name, value);
            return this;
        }

        public Endpoint build() {
            return new Endpoint(this);
        }
    }
}
