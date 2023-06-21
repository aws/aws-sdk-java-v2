/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.endpoints.internal;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface RuleEngine {
    /**
     * Evaluate the given {@link EndpointRuleset} using the named values in {@code args} as input into the rule set.
     *
     * @param ruleset
     *        The rule set to evaluate.
     * @param args
     *        The arguments.
     * @return The computed value.
     */
    Value evaluate(EndpointRuleset ruleset, Map<Identifier, Value> args);

    static RuleEngine defaultEngine() {
        return new DefaultRuleEngine();
    }
}
