import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public abstract class Rule {
    public static final String CONDITIONS = "conditions";
    public static final String DOCUMENTATION = "documentation";
    public static final String ENDPOINT = "endpoint";
    public static final String ERROR = "error";

    public static final String TREE = "tree";
    public static final String RULES = "rules";
    public static final String TYPE = "type";

    protected final List<Condition> conditions;
    protected final String documentation;

    protected Rule(Builder builder) {
        this.conditions = builder.conditions;
        this.documentation = builder.documentation;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public abstract <T> T accept(RuleValueVisitor<T> v);

    public static Rule fromNode(JsonNode node) {
        Map<String, JsonNode> objNode = node.asObject();

        Builder builder = builder();

        objNode.get(CONDITIONS).asArray().forEach(cn -> builder.addCondition(Condition.fromNode(cn)));

        JsonNode documentation = objNode.get(DOCUMENTATION);
        if (documentation != null) {
            builder.documentation(documentation.asString());
        }

        String type = objNode.get(TYPE).asString();
        switch (type) {
            case ENDPOINT: return builder.endpoint(EndpointResult.fromNode(objNode.get(ENDPOINT)));
            case ERROR: return builder.error(objNode.get(ERROR).asString());
            case TREE: return builder.treeRule(objNode.get(RULES).asArray()
                                                 .stream()
                                                 .map(Rule::fromNode)
                                                 .collect(Collectors.toList()));
            default: throw new IllegalStateException("Unexpected rule type: " + type);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String documentation;
        private final List<Condition> conditions = new ArrayList<>();

        public Builder addCondition(Condition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public EndpointRule endpoint(EndpointResult endpoint) {
            return new EndpointRule(this, endpoint);
        }

        public ErrorRule error(String error) {
            return new ErrorRule(this, Literal.fromStr(error));
        }

        public TreeRule treeRule(List<Rule> rules) {
            return new TreeRule(this, rules);
        }
    }
}
