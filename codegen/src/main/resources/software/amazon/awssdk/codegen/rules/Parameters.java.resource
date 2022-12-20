import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class Parameters {
    private final List<Parameter> parameters;

    private Parameters(Builder b) {
        this.parameters = b.parameters;
    }

    public List<Parameter> toList() {
        return parameters;
    }

    public Optional<Parameter> get(Identifier name) {
        return parameters.stream().filter((param) -> param.getName().equals(name)).findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Parameters that = (Parameters) o;

        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;
    }

    @Override
    public int hashCode() {
        return parameters != null ? parameters.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Parameters{" +
               "parameters=" + parameters +
               '}';
    }

    public static Parameters fromNode(JsonNode node) {
        Map<String, JsonNode> paramsObj = node.asObject();

        Builder b = builder();

        paramsObj.forEach((name, obj) -> {
            b.addParameter(Parameter.fromNode(name, obj));
        });

        return b.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Parameter> parameters = new ArrayList<>();

        public Builder addParameter(Parameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public Parameters build() {
            return new Parameters(this);
        }
    }

}
