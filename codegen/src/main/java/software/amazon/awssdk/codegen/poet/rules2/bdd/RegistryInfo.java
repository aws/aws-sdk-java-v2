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

package software.amazon.awssdk.codegen.poet.rules2.bdd;

import software.amazon.awssdk.codegen.poet.rules2.RuleSetExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleType;

public class RegistryInfo {
    private final String name;
    private final int index;
    // ruleType is mutable, we may not know the type when we initially construct it
    private RuleType ruleType;
    // set only when this value is assigned from a condition, NOT set for parameters
    private final RuleSetExpression ruleSetExpression;
    // defaults to true, set to false only when we guarantee that the value cannot be null.
    private boolean nullable;

    // set only when this is an endpoint parameter, EXCEPT in the case of Region built in which is a special case
    private final String nonRegionParamKey;

    public RegistryInfo(String name, int index, RuleType ruleType, RuleSetExpression ruleSetExpression, boolean nullable,
                        String nonRegionParamKey) {
        this.name = name;
        this.index = index;
        this.ruleType = ruleType;
        this.ruleSetExpression = ruleSetExpression;
        this.nullable = nullable;
        this.nonRegionParamKey = nonRegionParamKey;
    }

    public RegistryInfo(String name, int index, RuleType ruleType, String paramKey) {
        this(name, index, ruleType, null, true, paramKey);
    }

    public RegistryInfo(String name, int index, RuleSetExpression ruleSetExpression) {
        this(name, index, null, ruleSetExpression, true, null);
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public RuleSetExpression getRuleSetExpression() {
        return ruleSetExpression;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getNonRegionParamKey() {
        return nonRegionParamKey;
    }

    public boolean isNonRegionParam() {
        return nonRegionParamKey != null;
    }
}
