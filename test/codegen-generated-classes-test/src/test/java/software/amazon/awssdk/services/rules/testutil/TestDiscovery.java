package software.amazon.awssdk.services.rules.testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.EndpointRuleset;
import software.amazon.awssdk.services.rules.EndpointTest;
import software.amazon.awssdk.services.rules.EndpointTestSuite;

public class TestDiscovery {
    private static final String RESOURCE_ROOT = "/rules";

    public static final class RulesTestcase {
        private final EndpointRuleset ruleset;
        private final EndpointTest testcase;

        public RulesTestcase(EndpointRuleset ruleset, EndpointTest testcase) {
            this.ruleset = ruleset;
            this.testcase = testcase;
        }

        @Override
        public String toString() {
            return testcase.getDocumentation();
        }

        public EndpointRuleset ruleset() {
            return ruleset;
        }

        public EndpointTest testcase() {
            return testcase;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            RulesTestcase that = (RulesTestcase) obj;
            return Objects.equals(this.ruleset, that.ruleset) &&
                    Objects.equals(this.testcase, that.testcase);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleset, testcase);
        }

    }

    public static final class RulesTestSuite {
        private final EndpointRuleset ruleset;
        private final List<EndpointTestSuite> testSuites;

        public RulesTestSuite(EndpointRuleset ruleset, List<EndpointTestSuite> testSuites) {
            this.ruleset = ruleset;
            this.testSuites = testSuites;
        }

        @Override
        public String toString() {
            return ruleset.toString();
        }

        public EndpointRuleset ruleset() {
            return ruleset;
        }

        public List<EndpointTestSuite> testSuites() {
            return testSuites;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            RulesTestSuite that = (RulesTestSuite) obj;
            return Objects.equals(this.ruleset, that.ruleset) &&
                    Objects.equals(this.testSuites, that.testSuites);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ruleset, testSuites);
        }

    }

    public Stream<RulesTestSuite> testSuites() {
        JsonNode.parser();
        List<JsonNode> rulesetNodes = getValidRules()
            .stream()
            .map(e -> JsonNode.parser().parse(getResourceStream("valid-rules/" + e)))
            .collect(Collectors.toList());

        List<JsonNode> testSuiteFiles = getManifestEntries("test-cases/manifest.txt")
            .stream()
            .map(e -> JsonNode.parser().parse(getResourceStream("test-cases/" + e)))
            .collect(Collectors.toList());

        List<EndpointRuleset> rulesets = rulesetNodes.stream()
                                                     .map(EndpointRuleset::fromNode)
                                                     .collect(Collectors.toList());
        List<String> rulesetIds = rulesets.stream()
                                 .map(EndpointRuleset::getServiceId)
                                 .collect(Collectors.toList());
        if (rulesetIds.stream()
                      .distinct()
                      .count() != rulesets.size()) {
            throw new RuntimeException(String.format("Duplicate service ids discovered: %s", rulesets.stream()
                                                                                                .map(EndpointRuleset::getServiceId)
                                                                                                .sorted()
                                                                                                .collect(Collectors.toList())));
        }

        List<EndpointTestSuite> testSuites = testSuiteFiles.stream()
                                                           .map(EndpointTestSuite::fromNode)
                                                           .collect(Collectors.toList());
        testSuites.stream()
                  .filter(testSuite -> !rulesetIds.contains(testSuite.getService()))
                  .forEach(bad -> {
                      throw new RuntimeException("did not find service for " + bad.getService());
                  });
        return rulesets.stream()
                       .map(ruleset -> {
                           List<EndpointTestSuite> matchingTestSuites = testSuites.stream()
                                                              .filter(test -> test.getService()
                                                                                  .equals(ruleset.getServiceId()))
                                                              .collect(Collectors.toList());
                           return new RulesTestSuite(ruleset, matchingTestSuites);
                       });
    }

    private List<String> getManifestEntries(String path) {
        String absPath = RESOURCE_ROOT + "/" + path;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(absPath)))) {
            List<String> entries = new ArrayList<>();
            while (true) {
                String e = br.readLine();
                if (e == null) {
                    break;
                }
                entries.add(e);
            }
            return entries;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream getResourceStream(String path) {
        String absPath = RESOURCE_ROOT + "/" + path;
        return getClass().getResourceAsStream(absPath);
    }

    private URL getResource(String path) {
        String absPath = RESOURCE_ROOT + "/" + path;
        return getClass().getResource(absPath);
    }

    public RulesTestSuite getTestSuite(String name) {
        return new RulesTestSuite(rulesetFromPath(name), Collections.singletonList(testSuiteFromPath(name)));
    }

    public List<String> getValidRules() {
        return getManifestEntries("valid-rules/manifest.txt");
    }

    public URL validRulesetUrl(String name) {
        return getResource("valid-rules/" + name);
    }

    public URL testCaseUrl(String name) {
        return getResource("test-cases/" + name);
    }

    private EndpointRuleset rulesetFromPath(String name) {
        return EndpointRuleset.fromNode(JsonNode.parser().parse(Objects.requireNonNull(this.getClass()
                                                                              .getResourceAsStream(String.format("valid-rules/%s", name)))));
    }

    private EndpointTestSuite testSuiteFromPath(String name) {
        return EndpointTestSuite.fromNode(JsonNode.parser().parse(Objects.requireNonNull(this.getClass()
                                                                                .getResourceAsStream(String.format("test-cases/%s", name)))));
    }
}
