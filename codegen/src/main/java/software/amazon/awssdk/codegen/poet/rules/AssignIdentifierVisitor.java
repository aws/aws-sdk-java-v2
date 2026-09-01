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

package software.amazon.awssdk.codegen.poet.rules;

/**
 * Assigns an identifier to each rule then we use as a name for the generated method.
 */
public final class AssignIdentifierVisitor extends RewriteRuleExpressionVisitor {
    private int index;

    @Override
    public RuleExpression visitRuleSetExpression(RuleSetExpression expr) {
        String ruleId = "endpointRule" + index;
        expr = expr.toBuilder().ruleId(ruleId).build();
        index += 1;
        return super.visitRuleSetExpression(expr);
    }
}
