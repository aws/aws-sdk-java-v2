import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public final class EndpointResult {
    private static final String URL = "url";
    private static final String PROPERTIES = "properties";
    private static final String HEADERS = "headers";

    private Expr url;
    private Map<Identifier, Expr> properties;
    private Map<String, List<Expr>> headers;

    private EndpointResult(Builder builder) {
        this.url = builder.url;
        this.properties = builder.properties;
        this.headers = builder.headers;
    }

    public Expr getUrl() {
        return url;
    }

    public Map<Identifier, Expr> getProperties() {
        return properties;
    }

    public Map<String, List<Expr>> getHeaders() {
        return headers;
    }

    public static EndpointResult fromNode(JsonNode node) {
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

        EndpointResult endpoint = (EndpointResult) o;

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
        private final Map<Identifier, Expr> properties = new HashMap<>();
        private final Map<String, List<Expr>> headers = new HashMap<>();

        public Builder url(Expr url) {
            this.url = url;
            return this;
        }

        public Builder addProperty(Identifier name, Expr value) {
            properties.put(name, value);
            return this;
        }

        public Builder addHeader(String name, List<Expr> value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder addHeaderValue(String name, Expr value) {
            List<Expr> values = this.headers.computeIfAbsent(name, n -> new ArrayList<>());
            values.add(value);
            return this;
        }

        public EndpointResult build() {
            return new EndpointResult(this);
        }
    }
}
