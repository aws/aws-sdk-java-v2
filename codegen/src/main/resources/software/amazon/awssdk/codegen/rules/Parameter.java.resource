import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public final class Parameter implements ToParameterReference {
    public static final String TYPE = "type";
    public static final String DEPRECATED = "deprecated";
    public static final String DOCUMENTATION = "documentation";
    public static final String DEFAULT = "default";
    private static final String BUILT_IN = "builtIn";
    private static final String REQUIRED = "required";

    private final ParameterType type;
    private final Identifier name;
    private final Value value;
    private final String builtIn;
    private final Value defaultValue;
    private final Deprecated deprecated;
    private final String documentation;
    private final boolean required;

    public Parameter(Builder builder) {
        if (builder.defaultValue != null && builder.builtIn == null) {
            throw new RuntimeException("Cannot set a default value for non-builtin parameters");
        }
        if (builder.defaultValue != null && !builder.required) {
            throw new RuntimeException("When a default value is set, the field must also be marked as required");
        }
        this.type = builder.type;
        this.name = builder.name;
        this.builtIn = builder.builtIn;
        this.value = builder.value;
        this.required = builder.required;
        this.deprecated = builder.deprecated;
        this.documentation = builder.documentation;
        this.defaultValue = builder.defaultValue;
    }

    public Optional<String> getBuiltIn() {
        return Optional.ofNullable(builtIn);
    }

    public Optional<Value> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public boolean isRequired() {
        return required;
    }

    public Optional<Deprecated> getDeprecated() {
        return Optional.ofNullable(deprecated);
    }

    public static Parameter fromNode(String name, JsonNode node) throws RuleError {
        Map<String, JsonNode> objNode = node.asObject();

        Builder b = builder();
        b.name(name);
        b.type(ParameterType.fromNode(objNode.get(TYPE)));

        JsonNode builtIn = objNode.get(BUILT_IN);
        if (builtIn != null) {
            b.builtIn(builtIn.asString());
        }

        JsonNode documentation = objNode.get(DOCUMENTATION);
        if (documentation != null) {
            b.documentation(documentation.asString());
        }

        JsonNode defaultNode = objNode.get(DEFAULT);
        if (defaultNode != null) {
            b.defaultValue(Value.fromNode(defaultNode));
        }

        JsonNode required = objNode.get(REQUIRED);
        if (required != null) {
            b.required(required.asBoolean());
        } else {
            b.required(false);
        }

        JsonNode deprecated = objNode.get(DEPRECATED);
        if (deprecated != null) {
            b.deprecated(Deprecated.fromNode(deprecated));
        }

        return b.build();
    }

    public ParameterType getType() {
        return type;
    }

    public Identifier getName() {
        return name;
    }

    public boolean isBuiltIn() {
        return builtIn != null;
    }

    public Optional<Value> getValue() {
        return Optional.ofNullable(value);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(type);
        if (builtIn != null) {
            sb.append("; builtIn(").append(builtIn).append(")");
        }
        if (required) {
            sb.append("; required");
        }
        getDeprecated().ifPresent(dep -> sb.append("; ").append(deprecated).append("!"));
        return sb.toString();
    }

    @Override
    public ParameterReference toParameterReference() {
        return ParameterReference.builder()
                .name(getName().asString())
                .build();
    }

    public String template() {
        return "{" + name + "}";
    }

    public Expr expr() {
        return Expr.ref(this.name);
    }

    public BooleanEqualsFn eq(boolean b) {
        return BooleanEqualsFn.fromParam(this, Expr.of(b));
    }

    public BooleanEqualsFn eq(Expr e) {
        return BooleanEqualsFn.fromParam(this, e);
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * The default value for this Parameter
     * @return The value. This value must match the type of this parameter.
     */
    public Optional<Value> getDefault() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Parameter parameter = (Parameter) o;

        if (required != parameter.required) {
            return false;
        }
        if (type != parameter.type) {
            return false;
        }
        if (name != null ? !name.equals(parameter.name) : parameter.name != null) {
            return false;
        }
        if (value != null ? !value.equals(parameter.value) : parameter.value != null) {
            return false;
        }
        if (builtIn != null ? !builtIn.equals(parameter.builtIn) : parameter.builtIn != null) {
            return false;
        }
        if (defaultValue != null ? !defaultValue.equals(parameter.defaultValue) : parameter.defaultValue != null) {
            return false;
        }
        if (deprecated != null ? !deprecated.equals(parameter.deprecated) : parameter.deprecated != null) {
            return false;
        }
        return documentation != null ? documentation.equals(parameter.documentation) : parameter.documentation == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (builtIn != null ? builtIn.hashCode() : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (required ? 1 : 0);
        result = 31 * result + (deprecated != null ? deprecated.hashCode() : 0);
        result = 31 * result + (documentation != null ? documentation.hashCode() : 0);
        return result;
    }

    public static final class Deprecated {
        private static final String MESSAGE = "message";
        private static final String SINCE = "since";
        private final String message;
        private final String since;

        public Deprecated(String message, String since) {
            this.message = message;
            this.since = since;
        }

        public static Deprecated fromNode(JsonNode node) {
            Map<String, JsonNode> objNode = node.asObject();

            String message = null;
            String since = null;

            JsonNode messageNode = objNode.get(MESSAGE);
            if (messageNode != null) {
                message = messageNode.asString();
            }

            JsonNode sinceNode = objNode.get(SINCE);
            if (sinceNode != null) {
                since = sinceNode.asString();
            }

            return new Deprecated(message, since);
        }

        public String message() {
            return message;
        }

        public String since() {
            return since;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Deprecated that = (Deprecated) o;

            if (message != null ? !message.equals(that.message) : that.message != null) {
                return false;
            }
            return since != null ? since.equals(that.since) : that.since == null;
        }

        @Override
        public int hashCode() {
            int result = message != null ? message.hashCode() : 0;
            result = 31 * result + (since != null ? since.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Deprecated[" +
                    "message=" + message + ", " +
                    "since=" + since + ']';
        }

    }

    public static final class Builder {
        private ParameterType type;
        private Identifier name;
        private String builtIn;

        private Deprecated deprecated;

        private Value value;
        private boolean required;
        private String documentation;

        private Value defaultValue;

        public Builder type(ParameterType type) {
            this.type = type;
            return this;
        }

        public Builder deprecated(Deprecated deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder name(String name) {
            this.name = Identifier.of(name);
            return this;
        }

        public Builder name(Identifier name) {
            this.name = name;
            return this;
        }

        public Builder builtIn(String builtIn) {
            this.builtIn = builtIn;
            return this;
        }

        public Builder value(Value value) {
            this.value = value;
            return this;
        }

        public Builder defaultValue(Value defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Parameter build() {
            return new Parameter(this);
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder documentation(String s) {
            this.documentation = s;
            return this;
        }
    }
}
