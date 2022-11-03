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

package software.amazon.awssdk.services.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.EndpointRuleset;
import software.amazon.awssdk.services.rules.testutil.TestDiscovery;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {
    private static final TestDiscovery TEST_DISCOVERY = new TestDiscovery();

    @ParameterizedTest
    @MethodSource("validTestcases")
    void checkValidRules(ValidationTestCase validationTestCase) {
        EndpointRuleset ruleset = EndpointRuleset.fromNode(validationTestCase.contents());
        List<ValidationError> errors = new ValidateUriScheme().visitRuleset(ruleset)
                                                              .collect(Collectors.toList());
        assertEquals(errors, Collections.emptyList());
    }

    @ParameterizedTest
    @MethodSource("checkableTestCases")
    void executeTestSuite(TestDiscovery.RulesTestcase testcase) {
        testcase.testcase().execute(testcase.ruleset());
    }

    private Stream<ValidationTestCase> validTestcases() {
        return TEST_DISCOVERY.getValidRules()
                        .stream()
                        .map(name -> new ValidationTestCase(name, TEST_DISCOVERY.validRulesetUrl(name), TEST_DISCOVERY.testCaseUrl(name)));
    }

    private Stream<TestDiscovery.RulesTestcase> checkableTestCases() {
        return TEST_DISCOVERY.testSuites()
                                  .flatMap(
                                      suite -> suite.testSuites()
                                                    .stream()
                                                    .flatMap(ts -> ts.getTestCases()
                                                                     .stream()
                                                                     .map(tc -> new TestDiscovery.RulesTestcase(suite.ruleset(), tc))));
    }

    public static final class ValidationTestCase {
        private final String name;
        private final URL ruleSet;
        private final URL testCase;

        public ValidationTestCase(String name, URL ruleSet, URL testCase) {
            this.name = name;
            this.ruleSet = ruleSet;
            this.testCase = testCase;
        }

        JsonNode contents() {
            try {
                return JsonNode.parser().parse(ruleSet.openStream());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public URL ruleSet() {
            return ruleSet;
        }

        public URL testCase() {
            return testCase;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
