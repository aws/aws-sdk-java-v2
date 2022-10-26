package software.amazon.awssdk.services.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.EndpointRuleset;

public class EndpointTestSuite {
    public static final String SERVICE = "service";
    public static final String TEST_CASES = "testCases";

    private final List<EndpointTest> testCases;
    private final String service;


    public EndpointTestSuite(String service, List<EndpointTest> testCases) {
        this.service = service;
        this.testCases = testCases;
    }

    private EndpointTestSuite(Builder b) {
        this(b.service, b.testCases);
    }

    public void execute(EndpointRuleset ruleset) {
        for (EndpointTest test : this.getTestCases()) {
            test.execute(ruleset);
        }
    }

    public static EndpointTestSuite fromNode(JsonNode node) {
        Map<String, JsonNode> objNode = node.asObject();

        Builder b = builder();

        b.service(objNode.get(SERVICE).asString());
        objNode.get(TEST_CASES).asArray()
               .stream().map(EndpointTest::fromNode)
               .forEach(b::addTestCase);

        return b.build();
    }

    public String getService() {
        return service;
    }

    public List<EndpointTest> getTestCases() {
        return testCases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EndpointTestSuite that = (EndpointTestSuite) o;

        if (!testCases.equals(that.testCases)) return false;
        return service.equals(that.service);
    }

    @Override
    public int hashCode() {
        int result = testCases.hashCode();
        result = 31 * result + service.hashCode();
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String service;
        private final List<EndpointTest> testCases = new ArrayList<>();

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder addTestCase(EndpointTest testCase) {
            this.testCases.add(testCase);
            return this;
        }

        public EndpointTestSuite build() {
            return new EndpointTestSuite(this);
        }
    }

}
