import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

/**
 * Base class for the types of values computable by the {@link RuleEngine}.
 */
@SdkInternalApi
public abstract class Value {

    public boolean isNone() {
        return false;
    }

    public String expectString() {
        throw new RuntimeException("Expected string but was: " + this);
    }

    public boolean expectBool() {
        throw new RuntimeException("Expected bool but was: " + this);
    }

    public Record expectRecord() {
        throw new RuntimeException("Expected object but was: " + this);
    }

    public Endpoint expectEndpoint() {
        throw new RuntimeException("Expected endpoint, found " + this);
    }

    public Array expectArray() {
        throw new RuntimeException("Expected array, found " + this);
    }

    public int expectInt() {
        throw new RuntimeException("Expected int, found " + this);
    }

    public static Value fromNode(JsonNode node) {
        if (node.isArray()) {
            return new Array(node.asArray().stream().map(Value::fromNode).collect(Collectors.toList()));
        } else if (node.isBoolean()) {
            return fromBool(node.asBoolean());
        } else if (node.isNull()) {
            throw SdkClientException.create("null cannot be used as a literal");
        } else if (node.isNumber()) {
            return fromInteger(Integer.parseInt(node.asNumber()));
        } else if (node.isObject()) {
            HashMap<Identifier, Value> out = new HashMap<>();
            node.asObject().forEach((k, v) -> out.put(Identifier.of(k), fromNode(v)));
            return fromRecord(out);
        } else if (node.isString()) {
            return fromStr(node.asString());
        }
        throw SdkClientException.create("Unable to create Value from " + node);
    }

    public static Endpoint endpointFromNode(JsonNode source) {
        return Endpoint.fromNode(source);
    }

    /**
     * A string value.
     */
    public static class Str extends Value {
        private final String value;

        private Str(String value) {
            this.value = value;
        }

        @Override
        public String expectString() {
            return value;
        }

        @Override
        public String toString() {
            return "Str{" +
                   "value='" + value + '\'' +
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

            Str str = (Str) o;

            return value != null ? value.equals(str.value) : str.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    /**
     * An integer value.
     */
    public static class Int extends Value {
        private final int value;

        private Int(int value) {
            this.value = value;
        }

        @Override
        public int expectInt() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Int anInt = (Int) o;

            return value == anInt.value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    /**
     * A boolean value.
     */
    public static class Bool extends Value {
        private final boolean value;

        private Bool(boolean value) {
            this.value = value;
        }

        @Override
        public boolean expectBool() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Bool bool = (Bool) o;

            return value == bool.value;
        }

        @Override
        public int hashCode() {
            return value ? 1 : 0;
        }
    }

    /**
     * An array value.
     */
    public static class Array extends Value {
        private List<Value> inner;

        private Array(List<Value> inner) {
            this.inner = inner;
        }

        @Override
        public Array expectArray() {
            return this;
        }

        public Value get(int idx) {
            if (this.inner.size() > idx) {
                return this.inner.get(idx);
            } else {
                return new Value.None();
            }
        }

        public int size() {
            return inner.size();
        }

        @Override
        public String toString() {
            return "Array{" +
                   "inner=" + inner +
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

            Array array = (Array) o;

            return inner != null ? inner.equals(array.inner) : array.inner == null;
        }

        @Override
        public int hashCode() {
            return inner != null ? inner.hashCode() : 0;
        }
    }

    /**
     * A record (map) value.
     */
    public static class Record extends Value {
        private final Map<Identifier, Value> value;

        private Record(Map<Identifier, Value> value) {
            this.value = value;
        }

        public Value get(Identifier id) {
            return value.get(id);
        }

        public Map<Identifier, Value> getValue() {
            return value;
        }

        public void forEach(BiConsumer<Identifier, Value> fn) {
            value.forEach(fn);
        }

        @Override
        public Record expectRecord() {
            return this;
        }

        @Override
        public String toString() {
            return "Record{" +
                   "value=" + value +
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

            Record record = (Record) o;

            return value != null ? value.equals(record.value) : record.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    public static class Endpoint extends Value {
        private static final String URL = "url";
        private static final String PROPERTIES = "properties";
        private static final String HEADERS = "headers";

        private final String url;
        private final Map<String, Value> properties;
        private final Map<String, List<String>> headers;

        private Endpoint(Builder b) {
            this.url = b.url;
            this.properties = b.properties;
            this.headers = b.headers;
        }

        public String getUrl() {
            return url;
        }

        public Map<String, Value> getProperties() {
            return properties;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        @Override
        public Endpoint expectEndpoint() {
            return this;
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

        @Override
        public String toString() {
            return "Endpoint{" +
                   "url='" + url + '\'' +
                   ", properties=" + properties +
                   ", headers=" + headers +
                   '}';
        }

        public static Endpoint fromNode(JsonNode node) {
            Builder b = builder();

            Map<String, JsonNode> objNode = node.asObject();

            b.url(objNode.get(URL).asString());

            JsonNode propertiesNode = objNode.get(PROPERTIES);
            if (propertiesNode != null) {
                propertiesNode.asObject()
                    .forEach((k, v) -> {
                        b.property(k, Value.fromNode(v));
                    });
            }

            JsonNode headersNode = objNode.get(HEADERS);
            if (headersNode != null) {
                headersNode.asObject()
                    .forEach((k, v) -> v.asArray().forEach(e -> b.addHeader(k, e.asString())));
            }

            return b.build();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String url;
            private final Map<String, Value> properties = new HashMap<>();
            private final Map<String, List<String>> headers = new HashMap<>();

            public Builder url(String url) {
                this.url = url;
                return this;
            }

            public Builder properties(Map<String, Value> properties) {
                this.properties.clear();
                this.properties.putAll(properties);
                return this;
            }

            public Builder property(String name, Value value) {
                this.properties.put(name, value);
                return this;
            }

            public Builder addHeader(String name, String value) {
                List<String> values = this.headers.computeIfAbsent(name, (k) -> new ArrayList<>());
                values.add(value);
                return this;
            }

            public Endpoint build() {
                return new Endpoint(this);
            }
        }
    }

    public static class None extends Value {
        @Override
        public boolean isNone() {
            return true;
        }
    }

    public static Str fromStr(String value) {
        return new Str(value);
    }

    public static Int fromInteger(int value) {
        return new Int(value);
    }

    public static Bool fromBool(boolean value) {
        return new Bool(value);
    }

    public static Array fromArray(List<Value> value) {
        return new Array(value);
    }

    public static Record fromRecord(Map<Identifier, Value> value) {
        return new Record(value);
    }

    public static None none() {
        return new None();
    }
}
