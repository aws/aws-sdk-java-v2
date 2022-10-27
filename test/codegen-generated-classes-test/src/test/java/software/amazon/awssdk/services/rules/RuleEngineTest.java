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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.EndpointRuleset;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Identifier;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.RuleEngine;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Value;
import software.amazon.awssdk.utils.MapUtils;

public class RuleEngineTest {
    private EndpointRuleset parse(String resource) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        JsonNode node = JsonNodeParser.create().parse(is);
        return EndpointRuleset.fromNode(node);
    }

    @Test
    void testRuleEval() {
        EndpointRuleset actual = parse("rules/valid-rules/minimal-ruleset.json");
        Value result = RuleEngine.defaultEngine().evaluate(actual, MapUtils.of(Identifier.of("Region"), Value.fromStr("us-east-1")));
        Value.Endpoint expected = Value.Endpoint.builder()
            .url("https://us-east-1.amazonaws.com")
            .property("authSchemes", Value.fromArray(Collections.singletonList(
                Value.fromRecord(MapUtils.of(
                    Identifier.of("name"), Value.fromStr("v4"),
                    Identifier.of("signingScope"), Value.fromStr("us-east-1"),
                    Identifier.of("signingName"), Value.fromStr("serviceName")
                ))
            )))
            .build();

        assertThat(result.expectEndpoint()).isEqualTo(expected);
    }
}
