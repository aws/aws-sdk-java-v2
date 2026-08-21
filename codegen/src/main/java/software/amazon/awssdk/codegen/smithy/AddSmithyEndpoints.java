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

package software.amazon.awssdk.codegen.smithy;

import java.io.IOException;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

/**
 * Reads the endpoint rule-set and endpoint tests from the service's Smithy traits, where C2J reads
 * them from the {@code endpoint-rule-set.json} and {@code endpoint-tests.json} sidecar files.
 *
 * <p>The trait content already uses the schema these POJOs expect, so this is a parse rather than a
 * translation. It goes through the same {@link Jackson} mapper the sidecar path uses, so the two
 * sources produce the same concrete types and tolerate unknown keys the same way.
 *
 * <p>The {@code endpointBdd} trait encodes the same rules as a binary decision diagram. Every
 * service also carries {@code endpointRuleSet} in tree form, so it is ignored.
 */
final class AddSmithyEndpoints {

    private AddSmithyEndpoints() {
    }

    /**
     * Returns null when the service has no rule-set trait. {@code IntermediateModel} then
     * substitutes the default rules.
     */
    static EndpointRuleSetModel endpointRuleSet(ServiceShape service) {
        return service.getTrait(EndpointRuleSetTrait.class)
                      .map(trait -> parse(EndpointRuleSetModel.class, trait.getRuleSet(), service))
                      .orElse(null);
    }

    /**
     * Returns null when the service has no endpoint-tests trait. {@code IntermediateModel} then
     * substitutes an empty test suite. Uses {@code toNode()} rather than {@code getTestCases()}
     * because the POJO expects the whole {@code {version, testCases}} object.
     */
    static EndpointTestSuiteModel endpointTests(ServiceShape service) {
        return service.getTrait(EndpointTestsTrait.class)
                      .map(trait -> parse(EndpointTestSuiteModel.class, trait.toNode(), service))
                      .orElse(null);
    }

    private static <T> T parse(Class<T> clazz, Node node, ServiceShape service) {
        try {
            return Jackson.load(clazz, Node.printJson(node));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + clazz.getSimpleName() + " from a Smithy trait on "
                                       + service.getId(), e);
        }
    }
}
