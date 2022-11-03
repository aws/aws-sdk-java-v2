package software.amazon.awssdk.services.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.EndpointRuleset;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Identifier;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Parameter;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.ParameterType;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.RuleEngine;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.RuleError;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Value;
import software.amazon.awssdk.utils.Pair;

public class EndpointTest {
    public static final String EXPECT = "expect";
    public static final String PARAMS = "params";
    public static final String DOCUMENTATION = "documentation";

    private final String documentation;

    public Expectation getExpectation() {
        return expectation;
    }

    private final Expectation expectation;

    private final Value.Record params;

    private EndpointTest(Builder builder) {
        this.documentation = builder.documentation;
        this.expectation = Optional.ofNullable(builder.expectation).orElseThrow(NoSuchElementException::new);
        this.params = Optional.ofNullable(builder.params).orElseThrow(NoSuchElementException::new);;
    }

    public String getDocumentation() {
        return documentation;
    }

    public List<Pair<Identifier, Value>> getParams() {
        ArrayList<Pair<Identifier, Value>> out = new ArrayList<>();
        params.forEach((name, value) -> {
            out.add(Pair.of(name, value));
        });
        return out;
    }

    public List<Parameter> getParameters() {
        ArrayList<Parameter> result = new ArrayList<Parameter>();
        params.forEach((name, value) -> {

            Parameter.Builder pb = Parameter.builder().name(name);

            if (value instanceof Value.Str) {
                pb.type(ParameterType.STRING);
                result.add(pb.build());
            } else if (value instanceof Value.Bool) {
                pb.type(ParameterType.BOOLEAN);
                result.add(pb.build());
            }
        });
        return result;
    }

    public void execute(EndpointRuleset ruleset) {
        Value actual = RuleEngine.defaultEngine().evaluate(ruleset, this.params.getValue());
        RuleError.ctx(
                String.format("while executing test case%s", Optional
                        .ofNullable(documentation)
                        .map(d -> " " + d)
                        .orElse("")),
                () -> expectation.check(actual)
        );
    }

    public static EndpointTest fromNode(JsonNode node) {
        Map<String, JsonNode> objNode = node.asObject();

        Builder b = builder();

        JsonNode documentationNode = objNode.get(DOCUMENTATION);
        if (documentationNode != null) {
            b.documentation(documentationNode.asString());
        }

        b.params(Value.fromNode(objNode.get(PARAMS)).expectRecord());
        b.expectation(Expectation.fromNode(objNode.get(EXPECT)));

        return b.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EndpointTest that = (EndpointTest) o;

        if (documentation != null ? !documentation.equals(that.documentation) : that.documentation != null)
            return false;
        if (!expectation.equals(that.expectation)) return false;
        return params.equals(that.params);
    }

    @Override
    public int hashCode() {
        int result = documentation != null ? documentation.hashCode() : 0;
        result = 31 * result + expectation.hashCode();
        result = 31 * result + params.hashCode();
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static abstract class Expectation {
        public static final String ERROR = "error";

        public static Expectation fromNode(JsonNode node) {
            Map<String, JsonNode> objNode = node.asObject();

            Expectation result;
            JsonNode errorNode = objNode.get(ERROR);
            if (errorNode != null) {
                result = new Error(errorNode.asString());
            } else {
                result = new Endpoint(Value.endpointFromNode(node));
            }
            return result;
        }

        abstract void check(Value value);

        public static Error error(String message) {
            return new Error(message);
        }

        public static class Error extends Expectation {
            public String getMessage() {
                return message;
            }

            private final String message;

            public Error(String message) {
                this.message = message;
            }

            @Override
            void check(Value value) {
                RuleError.ctx("While checking endpoint test (expecting an error)", () -> {
                    if (!value.expectString().equals(this.message)) {
                        throw new AssertionError(String.format("Expected error %s but got %s", this.message, value));
                    }
                });
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Error error = (Error) o;

                return message.equals(error.message);
            }

            @Override
            public int hashCode() {
                return message.hashCode();
            }
        }

        public static class Endpoint extends Expectation {
            public Value.Endpoint getEndpoint() {
                return endpoint;
            }

            private final Value.Endpoint endpoint;

            public Endpoint(Value.Endpoint endpoint) {
                this.endpoint = endpoint;
            }

            @Override
            void check(Value value) {
                Value.Endpoint actual = value.expectEndpoint();
                if (!actual.equals(this.endpoint)) {
                    throw new AssertionError(
                            String.format("Expected endpoint:\n%s but got:\n%s",
                                    this.endpoint.toString(),
                                    actual));
                }
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Endpoint endpoint1 = (Endpoint) o;

                return endpoint != null ? endpoint.equals(endpoint1.endpoint) : endpoint1.endpoint == null;
            }

            @Override
            public int hashCode() {
                return endpoint != null ? endpoint.hashCode() : 0;
            }
        }
    }

    public static class Builder {
        private String documentation;
        private Expectation expectation;
        private Value.Record params;

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder expectation(Expectation expectation) {
            this.expectation = expectation;
            return this;
        }

        public Builder params(Value.Record params) {
            this.params = params;
            return this;
        }

        public EndpointTest build() {
            return new EndpointTest(this);
        }
    }
}
