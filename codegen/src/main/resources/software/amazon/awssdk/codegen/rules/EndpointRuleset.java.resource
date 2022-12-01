import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

/**
 * The set of rules that are used to compute the endpoint to use for a request.
 */
@SdkInternalApi
public final class EndpointRuleset {
    private static final String SERVICE_ID = "serviceId";
    private static final String VERSION = "version";
    private static final String PARAMETERS = "parameters";
    private static final String RULES = "rules";


    private final String serviceId;
    private final List<Rule> rules;
    private final String version;
    private final Parameters parameters;

    private EndpointRuleset(Builder b) {
        this.serviceId = b.serviceId;
        this.rules = b.rules;
        this.version = b.version;
        this.parameters = b.parameters;
    }

    public String getServiceId() {
        return serviceId;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public String getVersion() {
        return version;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EndpointRuleset fromNode(JsonNode node) {
        Builder b = builder();

        Map<String, JsonNode> obj = node.asObject();

        JsonNode serviceIdNode = obj.get(SERVICE_ID);
        if (serviceIdNode != null) {
            b.serviceId(serviceIdNode.asString());
        }

        JsonNode versionNode = obj.get(VERSION);
        if (versionNode != null) {
            b.version(versionNode.asString());
        }

        b.parameters(Parameters.fromNode(obj.get(PARAMETERS)));

        obj.get(RULES).asArray().forEach(rn -> b.addRule(Rule.fromNode(rn)));

        return b.build();
    }

    @Override
    public String toString() {
        return "EndpointRuleset{" +
               "serviceId='" + serviceId + '\'' +
               ", rules=" + rules +
               ", version='" + version + '\'' +
               ", parameters=" + parameters +
               '}';
    }

    public static class Builder {
        private String serviceId;
        private final List<Rule> rules = new ArrayList<>();
        private String version;
        private Parameters parameters;

        public Builder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder withDefaultVersion() {
            this.version = "1.0";
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder addRule(Rule rule) {
            rules.add(rule);
            return this;
        }

        public Builder parameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public EndpointRuleset build() {
            return new EndpointRuleset(this);
        }
    }
}
